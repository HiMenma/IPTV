package com.menmapro.iptv.player.ffmpeg

import org.bytedeco.javacv.Frame
import java.nio.ByteBuffer
import java.nio.ShortBuffer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.*
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

/**
 * 音频播放器
 * 
 * 负责从音频队列中获取音频帧，并通过音频输出设备播放。
 * 更新音频时钟，作为音视频同步的主时钟。
 * 支持音量控制。
 * 
 * Requirements:
 * - 1.3: 音频帧通过音频输出设备播放
 * - 6.1: 更新音频时钟用于音视频同步
 * - 2.4: 音量控制
 */
class AudioPlayer(
    private val audioQueue: BlockingQueue<Frame>,
    private val isPlaying: AtomicBoolean,
    private val isPaused: AtomicBoolean,
    private val audioClock: AudioClock,
    private val onError: (String) -> Unit
) : Runnable {
    
    /**
     * 音频输出线
     */
    private var sourceDataLine: SourceDataLine? = null
    
    /**
     * 音频播放线程
     */
    private var audioThread: Thread? = null
    
    /**
     * 音量（0.0 - 1.0）
     */
    @Volatile
    private var volume: Float = 1.0f
    
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
     * 音频格式
     */
    private var audioFormat: AudioFormat? = null
    
    /**
     * 是否已初始化
     */
    @Volatile
    private var isInitialized = false
    
    /**
     * 启动音频播放线程
     */
    fun start() {
        if (audioThread == null || !audioThread!!.isAlive) {
            audioThread = thread(
                start = true,
                name = "FFmpeg-Audio",
                priority = Thread.MAX_PRIORITY
            ) {
                run()
            }
        }
    }
    
    /**
     * 停止音频播放线程
     */
    fun stop() {
        isPlaying.set(false)
        audioThread?.join(1000)
        cleanup()
    }
    
    /**
     * 设置音量
     * 
     * @param newVolume 音量值（0.0 - 1.0）
     */
    fun setVolume(newVolume: Float) {
        volume = newVolume.coerceIn(0.0f, 1.0f)
        println("[AudioPlayer] Volume set to: $volume")
    }
    
    /**
     * 获取当前音量
     * 
     * @return 当前音量（0.0 - 1.0）
     */
    fun getVolume(): Float {
        return volume
    }
    
    /**
     * 音频播放线程主循环
     * 
     * 持续从音频队列获取帧，应用音量，播放音频，并更新音频时钟。
     */
    override fun run() {
        println("[AudioPlayer] Audio thread started")
        
        try {
            while (isPlaying.get()) {
                // 处理暂停状态
                if (isPaused.get()) {
                    // 暂停时停止音频输出但保持线路打开
                    sourceDataLine?.stop()
                    Thread.sleep(pauseSleepTime)
                    continue
                }
                
                // 确保音频线已启动
                if (sourceDataLine?.isActive == false) {
                    sourceDataLine?.start()
                }
                
                try {
                    // 从队列获取音频帧
                    val frame = audioQueue.poll(queueTimeout, TimeUnit.MILLISECONDS)
                    
                    if (frame == null) {
                        // 队列为空，继续等待
                        continue
                    }
                    
                    // 处理并播放音频帧
                    processAndPlayFrame(frame)
                    
                } catch (e: Exception) {
                    handleAudioError(e)
                }
            }
            
        } catch (e: InterruptedException) {
            println("[AudioPlayer] Audio thread interrupted")
        } catch (e: Exception) {
            println("[AudioPlayer] Fatal error in audio thread: ${e.message}")
            e.printStackTrace()
            onError("音频播放器发生致命错误: ${e.message}")
        } finally {
            println("[AudioPlayer] Audio thread stopped")
            cleanup()
        }
    }
    
    /**
     * 处理并播放音频帧
     * 
     * 初始化音频线（如果需要），应用音量，播放音频，更新音频时钟。
     * 
     * @param frame 音频帧
     */
    private fun processAndPlayFrame(frame: Frame) {
        try {
            // 检查帧是否包含音频数据
            if (frame.samples == null) {
                frame.close()
                return
            }
            
            // 初始化音频线（如果尚未初始化）
            if (!isInitialized) {
                initializeAudioLine(frame)
            }
            
            // 更新音频时钟
            audioClock.update(frame.timestamp)
            
            // 转换音频样本并应用音量
            val audioData = convertAndApplyVolume(frame)
            
            // 播放音频
            if (audioData != null && sourceDataLine != null) {
                sourceDataLine!!.write(audioData, 0, audioData.size)
            }
            
        } finally {
            // 确保帧资源被释放
            frame.close()
        }
    }
    
    /**
     * 初始化音频输出线
     * 
     * 根据第一个音频帧的参数配置音频格式和输出线。
     * 
     * @param frame 音频帧
     */
    private fun initializeAudioLine(frame: Frame) {
        try {
            // 从帧中提取音频参数
            var sampleRate = frame.sampleRate.toFloat()
            var channels = frame.audioChannels
            val sampleSizeInBits = 16 // 假设 16 位音频
            
            // 检测实际的通道数
            val actualChannels = frame.samples?.size ?: channels
            if (actualChannels > 0 && actualChannels != channels) {
                println("[AudioPlayer] WARNING: Frame reports $channels channels but has $actualChannels sample buffers")
                channels = actualChannels
            }
            
            // 如果通道数为 0 或无效，使用立体声作为默认值
            if (channels <= 0) {
                println("[AudioPlayer] WARNING: Invalid channel count ($channels), defaulting to stereo (2)")
                channels = 2
            }
            
            // 如果采样率为 0 或无效，使用 48kHz 作为默认值
            if (sampleRate <= 0) {
                println("[AudioPlayer] WARNING: Invalid sample rate ($sampleRate Hz), defaulting to 48000 Hz")
                sampleRate = 48000f
            }
            
            println("[AudioPlayer] Initializing audio line:")
            println("  Sample Rate: $sampleRate Hz")
            println("  Channels: $channels")
            println("  Sample Size: $sampleSizeInBits bits")
            
            // 创建音频格式
            audioFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                sampleSizeInBits,
                channels,
                channels * (sampleSizeInBits / 8), // frameSize
                sampleRate,
                false // bigEndian
            )
            
            // 获取音频线
            val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
            
            if (!AudioSystem.isLineSupported(info)) {
                throw LineUnavailableException("Audio line not supported: $audioFormat")
            }
            
            sourceDataLine = AudioSystem.getLine(info) as SourceDataLine
            
            // 配置缓冲区大小（100ms 缓冲）
            val bufferSize = (sampleRate * channels * (sampleSizeInBits / 8) / 10).toInt()
            
            // 打开并启动音频线
            sourceDataLine!!.open(audioFormat, bufferSize)
            sourceDataLine!!.start()
            
            // 启动音频时钟
            audioClock.start()
            
            isInitialized = true
            println("[AudioPlayer] Audio line initialized successfully")
            
        } catch (e: Exception) {
            println("[AudioPlayer] Failed to initialize audio line: ${e.message}")
            e.printStackTrace()
            onError("音频初始化失败: ${e.message}")
            throw e
        }
    }
    
    /**
     * 转换音频样本并应用音量
     * 
     * 将 FFmpeg Frame 中的音频样本转换为字节数组，并应用音量调整。
     * 优化：减少不必要的检查，使用更高效的循环
     * 
     * @param frame 音频帧
     * @return 处理后的音频数据字节数组
     */
    private fun convertAndApplyVolume(frame: Frame): ByteArray? {
        try {
            val samples = frame.samples ?: return null
            
            // 检查样本数组是否为空
            if (samples.isEmpty()) {
                println("[AudioPlayer] No sample buffers in frame")
                return null
            }
            
            // 获取实际的通道数（基于样本缓冲区数量）
            val actualChannels = samples.size
            val targetChannels = audioFormat?.channels ?: 2
            
            // 获取第一个通道的样本数
            val firstBuffer = samples[0]
            if (firstBuffer == null || !firstBuffer.hasRemaining()) {
                return null
            }
            
            val sampleCount = firstBuffer.remaining()
            
            // 创建输出缓冲区（基于目标通道数）
            val outputSize = sampleCount * targetChannels * 2 // 2 bytes per sample (16-bit)
            val outputBuffer = ByteArray(outputSize)
            
            // 重置所有缓冲区的位置
            samples.forEach { it?.rewind() }
            
            var outputIndex = 0
            
            // 处理每个样本
            for (i in 0 until sampleCount) {
                // 处理每个目标通道
                for (ch in 0 until targetChannels) {
                    // 获取样本值
                    val sample = try {
                        // 如果源通道数少于目标通道数，复制现有通道
                        val sourceChannel = ch.coerceAtMost(actualChannels - 1)
                        val sampleBuffer = samples[sourceChannel]
                        
                        when (sampleBuffer) {
                            is ShortBuffer -> {
                                if (sampleBuffer.hasRemaining()) {
                                    sampleBuffer.get()
                                } else {
                                    0
                                }
                            }
                            is ByteBuffer -> {
                                // 处理字节缓冲区（8位音频）
                                if (sampleBuffer.hasRemaining()) {
                                    (sampleBuffer.get().toInt() shl 8).toShort()
                                } else {
                                    0
                                }
                            }
                            else -> {
                                // 尝试从 FloatBuffer 转换
                                val floatBuffer = sampleBuffer as? java.nio.FloatBuffer
                                if (floatBuffer != null && floatBuffer.hasRemaining()) {
                                    (floatBuffer.get() * Short.MAX_VALUE).toInt().toShort()
                                } else {
                                    0
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[AudioPlayer] Error reading sample at index $i, channel $ch: ${e.message}")
                        0
                    }
                    
                    // 应用音量（如果需要）
                    val finalSample = if (volume != 1.0f) {
                        applySampleVolume(sample, volume)
                    } else {
                        sample
                    }
                    
                    // 转换为字节（小端序）
                    outputBuffer[outputIndex++] = (finalSample.toInt() and 0xFF).toByte()
                    outputBuffer[outputIndex++] = ((finalSample.toInt() shr 8) and 0xFF).toByte()
                }
            }
            
            return outputBuffer
            
        } catch (e: Exception) {
            println("[AudioPlayer] Error converting audio samples: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * 应用音量到单个音频样本
     * 
     * 将音频样本值乘以音量系数，并进行削波处理防止溢出。
     * 
     * @param sample 原始样本值（-32768 到 32767）
     * @param volumeLevel 音量级别（0.0 到 1.0）
     * @return 调整后的样本值
     */
    private fun applySampleVolume(sample: Short, volumeLevel: Float): Short {
        // 将样本值转换为浮点数
        val floatSample = sample.toFloat()
        
        // 应用音量
        val adjustedSample = floatSample * volumeLevel
        
        // 削波处理，防止溢出
        val clampedSample = adjustedSample.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat())
        
        return clampedSample.toInt().toShort()
    }
    
    /**
     * 处理音频错误
     * 
     * @param exception 音频异常
     */
    private fun handleAudioError(exception: Exception) {
        val errorMessage = exception.message ?: "Unknown error"
        println("[AudioPlayer] Audio error: $errorMessage")
        
        // 对于非致命错误，继续播放
        // 只记录错误，不停止播放
    }
    
    /**
     * 清理资源
     * 
     * 停止并关闭音频线，释放相关资源
     */
    private fun cleanup() {
        try {
            println("[AudioPlayer] Cleaning up audio resources")
            
            // 停止并关闭音频线
            sourceDataLine?.let { line ->
                if (line.isActive) {
                    line.stop()
                }
                if (line.isOpen) {
                    line.drain() // 等待缓冲区播放完毕
                    line.close()
                }
            }
            
            sourceDataLine = null
            audioFormat = null
            isInitialized = false
            
            println("[AudioPlayer] Audio resources cleaned up")
            
        } catch (e: Exception) {
            println("[AudioPlayer] Error during cleanup: ${e.message}")
        }
    }
    
    /**
     * 获取音频播放器状态信息
     */
    fun getStatus(): String {
        return buildString {
            append("Audio Status: ")
            append("Initialized: $isInitialized, ")
            append("Queue: ${audioQueue.size}, ")
            append("Volume: ${"%.2f".format(volume)}, ")
            append("Active: ${sourceDataLine?.isActive ?: false}")
        }
    }
    
    /**
     * 检查音频线是否活动
     */
    fun isActive(): Boolean {
        return sourceDataLine?.isActive ?: false
    }
    
    /**
     * 获取音频缓冲区可用空间
     */
    fun getAvailableBufferSpace(): Int {
        return sourceDataLine?.available() ?: 0
    }
}
