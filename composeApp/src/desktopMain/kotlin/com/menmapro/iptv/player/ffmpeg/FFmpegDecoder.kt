package com.menmapro.iptv.player.ffmpeg

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * FFmpeg 解码器
 * 
 * 负责从媒体源解码音视频帧，并将它们分发到相应的队列中。
 * 运行在独立线程中，持续解码直到播放停止。
 * 
 * Requirements: 1.1 - FFmpeg 解码, 1.4 - 错误处理和通知
 */
class FFmpegDecoder(
    private val grabber: FFmpegFrameGrabber,
    private val videoQueue: BlockingQueue<Frame>,
    private val audioQueue: BlockingQueue<Frame>,
    private val isPlaying: AtomicBoolean,
    private val isPaused: AtomicBoolean,
    private val statistics: PlaybackStatistics,
    private val onError: (String) -> Unit,
    private val onNetworkError: (() -> Unit)? = null
) : Runnable {
    
    /**
     * 解码线程
     */
    private var decoderThread: Thread? = null
    
    /**
     * 解码错误计数
     */
    private var errorCount = 0
    
    /**
     * 最大连续错误数，超过此数将停止解码
     */
    private val maxConsecutiveErrors = 10
    
    /**
     * 队列超时时间（毫秒）
     * 优化：减少超时时间以提高响应速度
     */
    private val queueTimeout = 50L
    
    /**
     * 暂停时的休眠时间（毫秒）
     * 优化：减少休眠时间以提高暂停/恢复响应速度
     */
    private val pauseSleepTime = 5L
    
    /**
     * 批处理计数器
     * 优化：每处理一定数量的帧后进行一次统计更新，减少频繁更新开销
     */
    private var batchCounter = 0
    private val batchSize = 10
    
    /**
     * 启动解码线程
     */
    fun start() {
        if (decoderThread == null || !decoderThread!!.isAlive) {
            decoderThread = thread(
                start = true,
                name = "FFmpeg-Decoder",
                priority = Thread.NORM_PRIORITY + 1
            ) {
                run()
            }
        }
    }
    
    /**
     * 停止解码线程
     */
    fun stop() {
        isPlaying.set(false)
        decoderThread?.join(1000)
    }
    
    /**
     * 解码线程主循环
     * 
     * 持续从 grabber 读取帧，根据帧类型分发到视频或音频队列。
     * 处理暂停状态、队列满等情况，并进行错误处理。
     */
    override fun run() {
        println("[FFmpegDecoder] Decoder thread started")
        errorCount = 0
        
        try {
            while (isPlaying.get()) {
                // 处理暂停状态
                if (isPaused.get()) {
                    Thread.sleep(pauseSleepTime)
                    continue
                }
                
                try {
                    // 抓取下一帧
                    val frame = grabber.grab()
                    
                    // 检查是否到达流末尾
                    if (frame == null) {
                        println("[FFmpegDecoder] End of stream reached")
                        break
                    }
                    
                    // 重置错误计数（成功解码）
                    errorCount = 0
                    
                    // 根据帧类型分发到相应队列
                    when {
                        frame.image != null -> {
                            // 视频帧
                            handleVideoFrame(frame)
                        }
                        frame.samples != null -> {
                            // 音频帧
                            handleAudioFrame(frame)
                        }
                        else -> {
                            // 未知帧类型，释放资源
                            frame.close()
                        }
                    }
                    
                } catch (e: Exception) {
                    handleDecodingError(e)
                }
            }
            
        } catch (e: InterruptedException) {
            println("[FFmpegDecoder] Decoder thread interrupted")
        } catch (e: Exception) {
            println("[FFmpegDecoder] Fatal error in decoder thread: ${e.message}")
            e.printStackTrace()
            onError("解码器发生致命错误: ${e.message}")
        } finally {
            println("[FFmpegDecoder] Decoder thread stopped")
        }
    }
    
    /**
     * 处理视频帧
     * 
     * 将视频帧添加到视频队列，如果队列满则等待或丢弃。
     * 优化：批量更新统计信息，减少同步开销
     * 
     * @param frame 视频帧
     */
    private fun handleVideoFrame(frame: Frame) {
        try {
            // 优化：使用非阻塞 offer 先尝试，失败后再使用超时 offer
            var added = videoQueue.offer(frame)
            
            if (!added) {
                // 队列满，尝试等待一小段时间
                added = videoQueue.offer(frame, queueTimeout, TimeUnit.MILLISECONDS)
            }
            
            if (added) {
                // 成功添加，批量更新统计
                batchCounter++
                if (batchCounter >= batchSize) {
                    statistics.incrementFramesDecoded(batchCounter.toLong())
                    statistics.updateBufferLevel(videoQueue.size)
                    batchCounter = 0
                }
            } else {
                // 队列满，丢弃帧
                statistics.incrementFramesDropped()
                frame.close()
            }
            
        } catch (e: InterruptedException) {
            // 线程被中断，释放帧
            frame.close()
            throw e
        }
    }
    
    /**
     * 处理音频帧
     * 
     * 将音频帧添加到音频队列，如果队列满则等待。
     * 音频帧不应该被丢弃，以保持音频连续性。
     * 优化：使用非阻塞 offer 先尝试，减少不必要的等待
     * 
     * @param frame 音频帧
     */
    private fun handleAudioFrame(frame: Frame) {
        try {
            // 优化：先尝试非阻塞添加
            var added = audioQueue.offer(frame)
            
            if (!added) {
                // 队列满，尝试等待一小段时间
                added = audioQueue.offer(frame, queueTimeout, TimeUnit.MILLISECONDS)
                
                if (!added) {
                    // 仍然失败，使用阻塞 put（音频不应丢弃）
                    audioQueue.put(frame)
                }
            }
            
        } catch (e: InterruptedException) {
            // 线程被中断，释放帧
            frame.close()
            throw e
        }
    }
    
    /**
     * 处理解码错误
     * 
     * 根据错误类型采取不同的处理策略：
     * - 数据损坏：跳过并继续
     * - 网络错误：通知上层处理
     * - 连续错误过多：停止解码
     * 
     * @param exception 解码异常
     */
    private fun handleDecodingError(exception: Exception) {
        errorCount++
        
        val errorCategory = categorizeDecodingError(exception)
        val errorMessage = exception.message ?: "Unknown error"
        
        println("[FFmpegDecoder] Decoding error ($errorCount/$maxConsecutiveErrors): $errorMessage")
        
        when (errorCategory) {
            DecodingErrorType.CORRUPTED_DATA -> {
                // 数据损坏，跳过并继续
                println("[FFmpegDecoder] Corrupted data detected, skipping frame")
                statistics.incrementFramesDropped()
                
                // 如果连续错误过多，停止解码
                if (errorCount >= maxConsecutiveErrors) {
                    println("[FFmpegDecoder] Too many consecutive errors, stopping decoder")
                    onError("解码失败：连续错误过多")
                    isPlaying.set(false)
                }
            }
            
            DecodingErrorType.NETWORK_ERROR -> {
                // 网络错误，通知上层处理重连
                println("[FFmpegDecoder] Network error detected")
                onError("网络错误：$errorMessage")
                
                // 触发重连回调
                onNetworkError?.invoke()
                
                // 短暂等待后继续尝试
                Thread.sleep(100)
            }
            
            DecodingErrorType.UNSUPPORTED_FORMAT -> {
                // 不支持的格式，停止解码
                println("[FFmpegDecoder] Unsupported format")
                onError("不支持的媒体格式：$errorMessage")
                isPlaying.set(false)
            }
            
            DecodingErrorType.END_OF_STREAM -> {
                // 流结束
                println("[FFmpegDecoder] End of stream")
                isPlaying.set(false)
            }
            
            DecodingErrorType.UNKNOWN -> {
                // 未知错误
                println("[FFmpegDecoder] Unknown error: $errorMessage")
                
                if (errorCount >= maxConsecutiveErrors) {
                    onError("解码错误：$errorMessage")
                    isPlaying.set(false)
                }
            }
        }
    }
    
    /**
     * 分类解码错误
     * 
     * 根据异常信息判断错误类型，以便采取相应的处理策略。
     * 
     * @param exception 解码异常
     * @return 错误类型
     */
    private fun categorizeDecodingError(exception: Exception): DecodingErrorType {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            message.contains("corrupt") || 
            message.contains("invalid") ||
            message.contains("decode error") -> DecodingErrorType.CORRUPTED_DATA
            
            message.contains("network") ||
            message.contains("connection") ||
            message.contains("timeout") ||
            message.contains("i/o error") -> DecodingErrorType.NETWORK_ERROR
            
            message.contains("unsupported") ||
            message.contains("not found") ||
            message.contains("no decoder") -> DecodingErrorType.UNSUPPORTED_FORMAT
            
            message.contains("end of file") ||
            message.contains("eof") -> DecodingErrorType.END_OF_STREAM
            
            else -> DecodingErrorType.UNKNOWN
        }
    }
    
    /**
     * 清空队列
     * 
     * 释放队列中所有帧的资源
     */
    fun clearQueues() {
        println("[FFmpegDecoder] Clearing queues")
        
        // 清空视频队列
        while (videoQueue.isNotEmpty()) {
            videoQueue.poll()?.close()
        }
        
        // 清空音频队列
        while (audioQueue.isNotEmpty()) {
            audioQueue.poll()?.close()
        }
    }
    
    /**
     * 获取队列状态信息
     */
    fun getQueueStatus(): String {
        return "Video: ${videoQueue.size}, Audio: ${audioQueue.size}"
    }
}

/**
 * 解码错误类型
 */
enum class DecodingErrorType {
    /**
     * 数据损坏
     */
    CORRUPTED_DATA,
    
    /**
     * 网络错误
     */
    NETWORK_ERROR,
    
    /**
     * 不支持的格式
     */
    UNSUPPORTED_FORMAT,
    
    /**
     * 流结束
     */
    END_OF_STREAM,
    
    /**
     * 未知错误
     */
    UNKNOWN
}
