package com.menmapro.iptv.player.ffmpeg

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import java.awt.Canvas
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import com.menmapro.iptv.ui.components.PlaybackState
import com.menmapro.iptv.ui.components.PlayerState

/**
 * FFmpeg 播放引擎
 * 
 * 核心播放引擎，管理整个播放生命周期。
 * 创建和管理三个工作线程：解码线程、渲染线程、音频线程。
 * 提供播放控制接口：播放、暂停、跳转、音量控制等。
 * 
 * Requirements:
 * - 1.1: 使用 FFmpeg 解码并渲染音视频内容
 * - 1.2: 视频帧渲染到 Canvas 组件
 * - 1.3: 音频帧通过音频输出设备播放
 * - 1.4: 播放过程中发生错误时记录并通知用户
 * - 1.5: 停止播放时正确释放所有 FFmpeg 资源
 * - 2.1: 播放/恢复视频播放
 * - 2.2: 暂停视频播放并保持当前位置
 * - 2.3: 跳转到指定时间位置
 * - 2.4: 调整音频输出音量
 * - 8.1: 释放所有 FFmpeg 解码器资源
 * - 8.2: 切换视频时先释放旧资源再分配新资源
 * - 8.3: 应用退出时确保所有播放器资源已释放
 * - 8.5: 资源释放失败时记录错误并尝试强制清理
 */
class FFmpegPlayerEngine(
    private val onStateChange: (PlayerState) -> Unit,
    private val onError: (String) -> Unit
) {
    
    // ========== 核心组件 ==========
    
    /**
     * FFmpeg 帧抓取器
     */
    private var grabber: FFmpegFrameGrabber? = null
    
    /**
     * 解码器
     */
    private var decoder: FFmpegDecoder? = null
    
    /**
     * 视频渲染器
     */
    private var renderer: VideoRenderer? = null
    
    /**
     * 音频播放器
     */
    private var audioPlayer: AudioPlayer? = null
    
    /**
     * 音频时钟
     */
    private val audioClock = AudioClock()
    
    /**
     * 音视频同步器
     */
    private val synchronizer = AudioVideoSynchronizer(audioClock)
    
    /**
     * 播放统计
     */
    private val statistics = PlaybackStatistics()
    
    /**
     * 直播流优化器
     */
    private var liveStreamOptimizer: LiveStreamOptimizer? = null
    
    // ========== 队列 ==========
    
    /**
     * 视频帧队列
     * 优化：根据流类型动态调整队列大小
     * - 直播流：较小队列（15帧）以减少延迟
     * - VOD：较大队列（30帧）以提高流畅度
     */
    private var videoFrameQueue = LinkedBlockingQueue<Frame>(30)
    
    /**
     * 音频帧队列
     * 优化：根据流类型动态调整队列大小
     * - 直播流：较小队列（50帧）以减少延迟
     * - VOD：较大队列（100帧）以提高流畅度
     */
    private var audioFrameQueue = LinkedBlockingQueue<Frame>(100)
    
    // ========== 状态标志 ==========
    
    /**
     * 是否正在播放
     */
    private val isPlaying = AtomicBoolean(false)
    
    /**
     * 是否已暂停
     */
    private val isPaused = AtomicBoolean(false)
    
    /**
     * 是否已初始化
     */
    @Volatile
    private var isInitialized = false
    
    /**
     * 当前播放的 URL
     */
    @Volatile
    private var currentUrl: String? = null
    
    /**
     * 当前使用的 Canvas
     */
    @Volatile
    private var currentCanvas: Canvas? = null
    
    /**
     * 是否处于全屏模式
     */
    @Volatile
    private var isFullscreen = false
    
    /**
     * 当前播放状态
     */
    @Volatile
    private var currentState = PlaybackState.IDLE
    
    /**
     * 媒体信息
     */
    @Volatile
    private var mediaInfo: MediaInfo? = null
    
    /**
     * 使用的硬件加速类型
     */
    @Volatile
    private var hardwareAccelerationType: HardwareAccelerationType = HardwareAccelerationType.NONE
    
    // ========== 播放控制方法 ==========
    
    /**
     * 初始化并开始播放
     * 
     * 1. 释放旧资源（如果有）
     * 2. 创建并配置 FFmpegFrameGrabber
     * 3. 启动 grabber 并提取媒体信息
     * 4. 创建三个工作线程
     * 5. 启动播放
     * 
     * @param url 媒体 URL
     * @param canvas 渲染目标 Canvas
     * 
     * Requirements: 1.1, 1.2, 1.3, 8.2
     */
    fun play(url: String, canvas: Canvas) {
        println("[FFmpegPlayerEngine] Starting playback: $url")
        
        try {
            // 更新状态为缓冲中
            updateState(PlaybackState.BUFFERING)
            
            // 如果已经在播放，先释放旧资源
            if (isInitialized) {
                println("[FFmpegPlayerEngine] Releasing old resources before starting new playback")
                release()
            }
            
            // 保存当前 URL 和 Canvas
            currentUrl = url
            currentCanvas = canvas
            
            // 检测流类型
            val isLive = StreamTypeDetector.isLiveStream(url)
            println("[FFmpegPlayerEngine] Stream type: ${if (isLive) "Live" else "VOD"}")
            
            // 优化：根据流类型调整队列大小
            if (isLive) {
                // 直播流：使用较小队列以减少延迟
                videoFrameQueue = LinkedBlockingQueue<Frame>(15)
                audioFrameQueue = LinkedBlockingQueue<Frame>(50)
                println("[FFmpegPlayerEngine] Using optimized queue sizes for live stream (Video: 15, Audio: 50)")
            } else {
                // VOD：使用较大队列以提高流畅度
                videoFrameQueue = LinkedBlockingQueue<Frame>(30)
                audioFrameQueue = LinkedBlockingQueue<Frame>(100)
                println("[FFmpegPlayerEngine] Using standard queue sizes for VOD (Video: 30, Audio: 100)")
            }
            
            // 创建直播流优化器
            liveStreamOptimizer = LiveStreamOptimizer(isLive)
            
            // 创建并配置 FFmpegFrameGrabber
            println("[FFmpegPlayerEngine] Creating and configuring FFmpegFrameGrabber")
            val (newGrabber, hwType) = FFmpegGrabberConfigurator.createAndConfigure(url, true)
            grabber = newGrabber
            hardwareAccelerationType = hwType
            
            // 启动 grabber
            println("[FFmpegPlayerEngine] Starting FFmpegFrameGrabber")
            grabber!!.start()
            
            // 提取媒体信息
            extractMediaInfo()
            
            // 设置播放标志
            isPlaying.set(true)
            isPaused.set(false)
            
            // 重置统计信息
            statistics.reset()
            synchronizer.resetStatistics()
            audioClock.reset()
            
            // 创建并启动三个工作线程
            createAndStartThreads(canvas)
            
            // 标记为已初始化
            isInitialized = true
            
            // 更新状态为播放中
            updateState(PlaybackState.PLAYING)
            
            println("[FFmpegPlayerEngine] Playback started successfully")
            println(FFmpegGrabberConfigurator.getConfigurationSummary(url, hardwareAccelerationType))
            
        } catch (e: Exception) {
            val errorMessage = "播放初始化失败: ${e.message}"
            println("[FFmpegPlayerEngine] $errorMessage")
            e.printStackTrace()
            onError(errorMessage)
            updateState(PlaybackState.ERROR, errorMessage)
            
            // 清理资源
            release()
        }
    }
    
    /**
     * 暂停播放
     * 
     * 设置暂停标志，线程会检测到并暂停处理。
     * 保持当前位置，可以通过 resume() 恢复。
     * 
     * Requirements: 2.2
     */
    fun pause() {
        if (!isInitialized || !isPlaying.get()) {
            println("[FFmpegPlayerEngine] Cannot pause: not playing")
            return
        }
        
        println("[FFmpegPlayerEngine] Pausing playback")
        isPaused.set(true)
        updateState(PlaybackState.PAUSED)
    }
    
    /**
     * 恢复播放
     * 
     * 清除暂停标志，线程会继续处理。
     * 
     * Requirements: 2.1
     */
    fun resume() {
        if (!isInitialized || !isPlaying.get()) {
            println("[FFmpegPlayerEngine] Cannot resume: not initialized or not playing")
            return
        }
        
        if (!isPaused.get()) {
            println("[FFmpegPlayerEngine] Already playing")
            return
        }
        
        println("[FFmpegPlayerEngine] Resuming playback")
        isPaused.set(false)
        updateState(PlaybackState.PLAYING)
    }
    
    /**
     * 跳转到指定位置
     * 
     * 使用 FFmpeg 的 setTimestamp 方法跳转到指定时间。
     * 跳转后清空队列，避免显示旧帧。
     * 
     * @param timestampMs 目标时间戳（毫秒）
     * 
     * Requirements: 2.3
     */
    fun seekTo(timestampMs: Long) {
        if (!isInitialized || grabber == null) {
            println("[FFmpegPlayerEngine] Cannot seek: not initialized")
            return
        }
        
        try {
            println("[FFmpegPlayerEngine] Seeking to ${timestampMs}ms")
            
            // 暂停播放
            val wasPaused = isPaused.get()
            isPaused.set(true)
            
            // 等待队列处理完毕
            Thread.sleep(100)
            
            // 清空队列
            decoder?.clearQueues()
            
            // 执行跳转（FFmpeg 使用微秒）
            grabber!!.timestamp = timestampMs * 1000
            
            // 重置音频时钟
            audioClock.update(timestampMs * 1000)
            
            // 恢复播放状态
            if (!wasPaused) {
                isPaused.set(false)
            }
            
            println("[FFmpegPlayerEngine] Seek completed")
            
        } catch (e: Exception) {
            val errorMessage = "跳转失败: ${e.message}"
            println("[FFmpegPlayerEngine] $errorMessage")
            onError(errorMessage)
        }
    }
    
    /**
     * 设置音量
     * 
     * 调整音频播放器的音量。
     * 
     * @param volume 音量值（0.0 - 1.0）
     * 
     * Requirements: 2.4
     */
    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0.0f, 1.0f)
        println("[FFmpegPlayerEngine] Setting volume to $clampedVolume")
        
        audioPlayer?.setVolume(clampedVolume)
        
        // 更新状态
        updateState(currentState, volume = clampedVolume)
    }
    
    /**
     * 停止播放
     * 
     * 停止所有线程，但不释放资源。
     * 可以通过 play() 重新开始播放。
     */
    fun stop() {
        if (!isInitialized) {
            println("[FFmpegPlayerEngine] Already stopped")
            return
        }
        
        println("[FFmpegPlayerEngine] Stopping playback")
        
        // 设置停止标志
        isPlaying.set(false)
        isPaused.set(false)
        
        // 等待线程停止
        stopThreads()
        
        // 更新状态
        updateState(PlaybackState.IDLE)
        
        println("[FFmpegPlayerEngine] Playback stopped")
    }
    
    /**
     * 释放所有资源
     * 
     * 停止所有线程，释放 FFmpeg 资源，清空队列。
     * 确保资源正确释放，处理释放失败的情况。
     * 
     * Requirements: 1.5, 8.1, 8.3, 8.5
     */
    fun release() {
        println("[FFmpegPlayerEngine] Releasing all resources")
        
        try {
            // 1. 停止所有线程
            isPlaying.set(false)
            isPaused.set(false)
            stopThreads()
            
            // 2. 清空队列并释放帧资源
            clearQueues()
            
            // 3. 释放 FFmpeg grabber
            releaseGrabber()
            
            // 4. 重置状态
            isInitialized = false
            currentUrl = null
            currentCanvas = null
            mediaInfo = null
            hardwareAccelerationType = HardwareAccelerationType.NONE
            
            // 5. 重置时钟和统计
            audioClock.reset()
            synchronizer.resetStatistics()
            statistics.reset()
            liveStreamOptimizer?.resetStatistics()
            liveStreamOptimizer?.resetReconnectState()
            
            // 6. 更新状态
            updateState(PlaybackState.IDLE)
            
            println("[FFmpegPlayerEngine] All resources released successfully")
            
        } catch (e: Exception) {
            val errorMessage = "资源释放失败: ${e.message}"
            println("[FFmpegPlayerEngine] $errorMessage")
            e.printStackTrace()
            
            // 尝试强制清理
            forceCleanup()
        }
    }
    
    /**
     * 尝试重连
     * 
     * 当检测到网络中断时，尝试重新连接到媒体源。
     * 使用指数退避策略，避免频繁重连。
     * 
     * Requirements: 5.4, 5.5
     */
    fun attemptReconnection() {
        val optimizer = liveStreamOptimizer
        if (optimizer == null || !optimizer.isLiveStream()) {
            println("[FFmpegPlayerEngine] Reconnection not applicable for non-live streams")
            return
        }
        
        if (!optimizer.shouldReconnect()) {
            println("[FFmpegPlayerEngine] Reconnection not allowed at this time")
            return
        }
        
        val url = currentUrl
        val canvas = currentCanvas
        
        if (url == null || canvas == null) {
            println("[FFmpegPlayerEngine] Cannot reconnect: missing URL or Canvas")
            return
        }
        
        println("[FFmpegPlayerEngine] Attempting reconnection...")
        optimizer.recordReconnectAttempt()
        
        try {
            // 保存当前播放位置（对于直播流，这通常是最新位置）
            val currentPosition = getCurrentPosition()
            
            // 停止当前播放（但不释放所有资源）
            isPlaying.set(false)
            stopThreads()
            
            // 释放旧的 grabber
            releaseGrabber()
            
            // 清空队列
            clearQueues()
            
            // 短暂等待
            Thread.sleep(500)
            
            // 创建新的 grabber
            println("[FFmpegPlayerEngine] Creating new grabber for reconnection")
            val (newGrabber, hwType) = FFmpegGrabberConfigurator.createAndConfigure(url, true)
            grabber = newGrabber
            hardwareAccelerationType = hwType
            
            // 启动 grabber
            grabber!!.start()
            
            // 对于直播流，跳转到最新位置（如果可能）
            // 注意：某些直播流不支持跳转，会自动从最新位置开始
            if (currentPosition > 0) {
                try {
                    grabber!!.timestamp = currentPosition * 1000 // 转换为微秒
                } catch (e: Exception) {
                    println("[FFmpegPlayerEngine] Cannot seek in live stream, starting from current position")
                }
            }
            
            // 重新提取媒体信息
            extractMediaInfo()
            
            // 重新启动线程
            isPlaying.set(true)
            isPaused.set(false)
            createAndStartThreads(canvas)
            
            // 记录重连成功
            optimizer.recordReconnectSuccess()
            
            // 更新状态
            updateState(PlaybackState.PLAYING)
            
            println("[FFmpegPlayerEngine] Reconnection successful")
            
        } catch (e: Exception) {
            val errorMessage = "重连失败: ${e.message}"
            println("[FFmpegPlayerEngine] $errorMessage")
            e.printStackTrace()
            onError(errorMessage)
            
            // 检查是否还能继续尝试
            if (optimizer.shouldReconnect()) {
                println("[FFmpegPlayerEngine] Will retry reconnection later")
            } else {
                println("[FFmpegPlayerEngine] Maximum reconnection attempts reached")
                updateState(PlaybackState.ERROR, "连接失败，无法恢复")
                release()
            }
        }
    }
    
    // ========== 内部辅助方法 ==========
    
    /**
     * 创建并启动三个工作线程
     * 
     * @param canvas 渲染目标 Canvas
     */
    private fun createAndStartThreads(canvas: Canvas) {
        println("[FFmpegPlayerEngine] Creating and starting worker threads")
        
        // 创建解码器
        decoder = FFmpegDecoder(
            grabber = grabber!!,
            videoQueue = videoFrameQueue,
            audioQueue = audioFrameQueue,
            isPlaying = isPlaying,
            isPaused = isPaused,
            statistics = statistics,
            onError = onError,
            onNetworkError = {
                // 当检测到网络错误时，尝试重连
                if (liveStreamOptimizer?.isLiveStream() == true) {
                    println("[FFmpegPlayerEngine] Network error detected, attempting reconnection")
                    attemptReconnection()
                }
            }
        )
        
        // 创建视频渲染器
        renderer = VideoRenderer(
            canvas = canvas,
            videoQueue = videoFrameQueue,
            isPlaying = isPlaying,
            isPaused = isPaused,
            synchronizer = synchronizer,
            statistics = statistics,
            onError = onError,
            liveStreamOptimizer = liveStreamOptimizer
        )
        
        // 创建音频播放器
        audioPlayer = AudioPlayer(
            audioQueue = audioFrameQueue,
            isPlaying = isPlaying,
            isPaused = isPaused,
            audioClock = audioClock,
            onError = onError
        )
        
        // 启动线程
        decoder!!.start()
        renderer!!.start()
        audioPlayer!!.start()
        
        println("[FFmpegPlayerEngine] All worker threads started")
    }
    
    /**
     * 停止所有工作线程
     */
    private fun stopThreads() {
        println("[FFmpegPlayerEngine] Stopping worker threads")
        
        try {
            // 停止解码器
            decoder?.stop()
            
            // 停止渲染器
            renderer?.stop()
            
            // 停止音频播放器
            audioPlayer?.stop()
            
            // 清空引用
            decoder = null
            renderer = null
            audioPlayer = null
            
            println("[FFmpegPlayerEngine] All worker threads stopped")
            
        } catch (e: Exception) {
            println("[FFmpegPlayerEngine] Error stopping threads: ${e.message}")
        }
    }
    
    /**
     * 清空队列并释放帧资源
     */
    private fun clearQueues() {
        println("[FFmpegPlayerEngine] Clearing queues")
        
        try {
            // 清空视频队列
            while (videoFrameQueue.isNotEmpty()) {
                videoFrameQueue.poll()?.close()
            }
            
            // 清空音频队列
            while (audioFrameQueue.isNotEmpty()) {
                audioFrameQueue.poll()?.close()
            }
            
            println("[FFmpegPlayerEngine] Queues cleared")
            
        } catch (e: Exception) {
            println("[FFmpegPlayerEngine] Error clearing queues: ${e.message}")
        }
    }
    
    /**
     * 释放 FFmpeg grabber
     */
    private fun releaseGrabber() {
        println("[FFmpegPlayerEngine] Releasing FFmpeg grabber")
        
        try {
            grabber?.let { g ->
                try {
                    g.stop()
                } catch (e: Exception) {
                    println("[FFmpegPlayerEngine] Error stopping grabber: ${e.message}")
                }
                
                try {
                    g.release()
                } catch (e: Exception) {
                    println("[FFmpegPlayerEngine] Error releasing grabber: ${e.message}")
                }
            }
            
            grabber = null
            println("[FFmpegPlayerEngine] FFmpeg grabber released")
            
        } catch (e: Exception) {
            println("[FFmpegPlayerEngine] Error releasing grabber: ${e.message}")
        }
    }
    
    /**
     * 强制清理资源
     * 
     * 当正常释放失败时，尝试强制清理所有资源。
     * 
     * Requirements: 8.5
     */
    private fun forceCleanup() {
        println("[FFmpegPlayerEngine] Attempting force cleanup")
        
        try {
            // 强制设置标志
            isPlaying.set(false)
            isPaused.set(false)
            isInitialized = false
            
            // 清空引用
            decoder = null
            renderer = null
            audioPlayer = null
            grabber = null
            currentUrl = null
            currentCanvas = null
            mediaInfo = null
            
            // 清空队列（不释放帧，避免异常）
            videoFrameQueue.clear()
            audioFrameQueue.clear()
            
            println("[FFmpegPlayerEngine] Force cleanup completed")
            
        } catch (e: Exception) {
            println("[FFmpegPlayerEngine] Force cleanup failed: ${e.message}")
        }
    }
    
    /**
     * 提取媒体信息
     * 
     * 从 grabber 中提取媒体元数据。
     */
    private fun extractMediaInfo() {
        try {
            val g = grabber ?: return
            
            mediaInfo = MediaInfo(
                duration = g.lengthInTime / 1000, // 转换为毫秒
                videoCodec = g.videoCodecName ?: "Unknown",
                audioCodec = g.audioCodecName ?: "Unknown",
                videoWidth = g.imageWidth,
                videoHeight = g.imageHeight,
                videoFrameRate = g.frameRate,
                audioBitrate = g.audioBitrate,
                videoBitrate = g.videoBitrate,
                audioChannels = g.audioChannels,
                audioSampleRate = g.sampleRate
            )
            
            println("[FFmpegPlayerEngine] Media info extracted:")
            println("  Duration: ${mediaInfo!!.duration}ms")
            println("  Video: ${mediaInfo!!.videoCodec} ${mediaInfo!!.videoWidth}x${mediaInfo!!.videoHeight} @ ${mediaInfo!!.videoFrameRate}fps")
            println("  Audio: ${mediaInfo!!.audioCodec} ${mediaInfo!!.audioChannels}ch @ ${mediaInfo!!.audioSampleRate}Hz")
            
        } catch (e: Exception) {
            println("[FFmpegPlayerEngine] Error extracting media info: ${e.message}")
        }
    }
    
    /**
     * 更新播放状态
     * 
     * @param newState 新的播放状态
     * @param errorMessage 错误消息（可选）
     * @param volume 音量（可选）
     */
    private fun updateState(
        newState: PlaybackState,
        errorMessage: String? = null,
        volume: Float? = null
    ) {
        currentState = newState
        
        val state = PlayerState(
            playbackState = newState,
            position = getCurrentPosition(),
            duration = getDuration(),
            volume = volume ?: audioPlayer?.getVolume() ?: 1.0f,
            errorMessage = errorMessage
        )
        
        onStateChange(state)
    }
    
    /**
     * 获取当前播放位置
     * 
     * @return 当前位置（毫秒）
     */
    private fun getCurrentPosition(): Long {
        return if (isInitialized) {
            audioClock.getTimeMs()
        } else {
            0L
        }
    }
    
    /**
     * 获取媒体时长
     * 
     * @return 媒体时长（毫秒）
     */
    private fun getDuration(): Long {
        return mediaInfo?.duration ?: 0L
    }
    
    // ========== 公共查询方法 ==========
    
    /**
     * 检查是否已初始化
     * 
     * @return true 如果播放器已初始化
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * 检查是否有活动线程
     * 
     * @return true 如果有任何工作线程在运行
     */
    fun hasActiveThreads(): Boolean {
        return isPlaying.get()
    }
    
    /**
     * 检查是否有分配的资源
     * 
     * @return true 如果有资源被分配
     */
    fun hasAllocatedResources(): Boolean {
        return grabber != null || decoder != null || renderer != null || audioPlayer != null
    }
    
    /**
     * 获取同步漂移
     * 
     * @return 同步漂移（毫秒）
     */
    fun getSyncDrift(): Long {
        return synchronizer.getAverageSyncError().toLong()
    }
    
    /**
     * 获取播放统计信息
     * 
     * @return 播放统计
     */
    fun getStatistics(): PlaybackStatistics {
        return statistics
    }
    
    /**
     * 获取媒体信息
     * 
     * @return 媒体信息
     */
    fun getMediaInfo(): MediaInfo? {
        return mediaInfo
    }
    
    /**
     * 获取硬件加速类型
     * 
     * @return 硬件加速类型
     */
    fun getHardwareAccelerationType(): HardwareAccelerationType {
        return hardwareAccelerationType
    }
    
    /**
     * 切换到全屏模式
     * 
     * 检测全屏状态变化，调整渲染目标。
     * 如果提供了新的 Canvas，则切换到新的渲染目标。
     * 
     * @param fullscreenCanvas 全屏模式下的 Canvas（可选）
     * 
     * Requirements: 10.1
     */
    fun enterFullscreen(fullscreenCanvas: Canvas? = null) {
        println("[FFmpegPlayerEngine] Entering fullscreen mode")
        
        if (!isInitialized) {
            println("[FFmpegPlayerEngine] Cannot enter fullscreen: not initialized")
            return
        }
        
        try {
            // 标记为全屏模式
            isFullscreen = true
            
            // 如果提供了新的 Canvas，切换渲染目标
            if (fullscreenCanvas != null && fullscreenCanvas != currentCanvas) {
                println("[FFmpegPlayerEngine] Switching to fullscreen canvas")
                
                // 暂停渲染
                val wasPaused = isPaused.get()
                isPaused.set(true)
                
                // 等待当前帧渲染完成
                Thread.sleep(50)
                
                // 更新 Canvas
                currentCanvas = fullscreenCanvas
                
                // 更新渲染器的 Canvas
                renderer?.updateCanvas(fullscreenCanvas)
                
                // 恢复播放状态
                if (!wasPaused) {
                    isPaused.set(false)
                }
                
                println("[FFmpegPlayerEngine] Switched to fullscreen canvas")
            } else {
                // 使用当前 Canvas，只是标记为全屏模式
                // 渲染器会根据 Canvas 的新尺寸自动调整
                println("[FFmpegPlayerEngine] Using current canvas for fullscreen")
            }
            
        } catch (e: Exception) {
            val errorMessage = "切换到全屏模式失败: ${e.message}"
            println("[FFmpegPlayerEngine] $errorMessage")
            e.printStackTrace()
            onError(errorMessage)
        }
    }
    
    /**
     * 退出全屏模式
     * 
     * 恢复窗口模式渲染，保持播放状态。
     * 如果提供了新的 Canvas，则切换到新的渲染目标。
     * 
     * @param windowCanvas 窗口模式下的 Canvas（可选）
     * 
     * Requirements: 10.3
     */
    fun exitFullscreen(windowCanvas: Canvas? = null) {
        println("[FFmpegPlayerEngine] Exiting fullscreen mode")
        
        if (!isInitialized) {
            println("[FFmpegPlayerEngine] Cannot exit fullscreen: not initialized")
            return
        }
        
        try {
            // 标记为窗口模式
            isFullscreen = false
            
            // 如果提供了新的 Canvas，切换渲染目标
            if (windowCanvas != null && windowCanvas != currentCanvas) {
                println("[FFmpegPlayerEngine] Switching to window canvas")
                
                // 暂停渲染
                val wasPaused = isPaused.get()
                isPaused.set(true)
                
                // 等待当前帧渲染完成
                Thread.sleep(50)
                
                // 更新 Canvas
                currentCanvas = windowCanvas
                
                // 更新渲染器的 Canvas
                renderer?.updateCanvas(windowCanvas)
                
                // 恢复播放状态
                if (!wasPaused) {
                    isPaused.set(false)
                }
                
                println("[FFmpegPlayerEngine] Switched to window canvas")
            } else {
                // 使用当前 Canvas，只是标记为窗口模式
                // 渲染器会根据 Canvas 的新尺寸自动调整
                println("[FFmpegPlayerEngine] Using current canvas for window mode")
            }
            
        } catch (e: Exception) {
            val errorMessage = "退出全屏模式失败: ${e.message}"
            println("[FFmpegPlayerEngine] $errorMessage")
            e.printStackTrace()
            onError(errorMessage)
        }
    }
    
    /**
     * 检查是否处于全屏模式
     * 
     * @return true 如果处于全屏模式
     */
    fun isFullscreen(): Boolean {
        return isFullscreen
    }
    
    /**
     * 处理窗口尺寸变化
     * 
     * 当窗口大小改变时调用此方法，通知渲染器更新渲染尺寸。
     * 渲染器会自动调整以适应新的窗口尺寸，保持视频宽高比。
     * 
     * Requirements: 10.4
     */
    fun handleSizeChange() {
        if (!isInitialized) {
            return
        }
        
        println("[FFmpegPlayerEngine] Handling window size change")
        renderer?.handleSizeChange()
    }
    
    /**
     * 生成诊断报告
     * 
     * @return 包含所有关键指标的诊断报告
     */
    fun generateDiagnosticReport(): String {
        return buildString {
            appendLine("=== FFmpeg Player Diagnostic Report ===")
            appendLine("Timestamp: ${System.currentTimeMillis()}")
            appendLine()
            
            appendLine("System Information:")
            appendLine("  OS: ${System.getProperty("os.name")}")
            appendLine("  Java Version: ${System.getProperty("java.version")}")
            appendLine()
            
            appendLine("Hardware Acceleration:")
            appendLine("  Type: ${hardwareAccelerationType.name}")
            appendLine()
            
            appendLine("Media Information:")
            appendLine("  URL: ${currentUrl ?: "N/A"}")
            if (mediaInfo != null) {
                appendLine("  Duration: ${mediaInfo!!.duration}ms")
                appendLine("  Video Codec: ${mediaInfo!!.videoCodec}")
                appendLine("  Audio Codec: ${mediaInfo!!.audioCodec}")
                appendLine("  Resolution: ${mediaInfo!!.videoWidth}x${mediaInfo!!.videoHeight}")
                appendLine("  Frame Rate: ${mediaInfo!!.videoFrameRate}")
            }
            appendLine()
            
            appendLine("Playback Statistics:")
            appendLine("  Frames Decoded: ${statistics.framesDecoded}")
            appendLine("  Frames Rendered: ${statistics.framesRendered}")
            appendLine("  Frames Dropped: ${statistics.framesDropped}")
            appendLine("  Current FPS: ${"%.2f".format(statistics.currentFps)}")
            appendLine("  Buffer Level: ${statistics.bufferLevel}")
            appendLine()
            
            appendLine("Synchronization:")
            append(synchronizer.generateDiagnosticReport())
            appendLine()
            
            liveStreamOptimizer?.let { optimizer ->
                appendLine("Live Stream Optimization:")
                append(optimizer.generateDiagnosticReport())
                appendLine()
            }
            
            appendLine("Queue Status:")
            appendLine("  Video Queue: ${videoFrameQueue.size}/${videoFrameQueue.remainingCapacity() + videoFrameQueue.size}")
            appendLine("  Audio Queue: ${audioFrameQueue.size}/${audioFrameQueue.remainingCapacity() + audioFrameQueue.size}")
            appendLine()
            
            appendLine("Thread Status:")
            appendLine("  Is Playing: ${isPlaying.get()}")
            appendLine("  Is Paused: ${isPaused.get()}")
            appendLine("  Is Initialized: $isInitialized")
            appendLine()
            
            appendLine("==========================================")
        }
    }
}
