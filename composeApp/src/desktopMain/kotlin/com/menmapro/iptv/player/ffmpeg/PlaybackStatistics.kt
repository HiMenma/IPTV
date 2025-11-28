package com.menmapro.iptv.player.ffmpeg

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * 播放统计模型
 * 
 * 收集和跟踪播放过程中的各种统计指标，用于性能监控和诊断。
 * 所有字段都是线程安全的，可以在多个线程中安全访问和更新。
 * 
 * Requirements: 7.2 - 播放统计更新, 7.4 - 性能监控
 */
class PlaybackStatistics {
    /**
     * 已解码的视频帧总数
     */
    private val _framesDecoded = AtomicLong(0)
    val framesDecoded: Long
        get() = _framesDecoded.get()
    
    /**
     * 已渲染的视频帧总数
     */
    private val _framesRendered = AtomicLong(0)
    val framesRendered: Long
        get() = _framesRendered.get()
    
    /**
     * 丢弃的视频帧总数
     */
    private val _framesDropped = AtomicLong(0)
    val framesDropped: Long
        get() = _framesDropped.get()
    
    /**
     * 当前帧率（帧/秒）
     */
    private val _currentFps = AtomicReference(0.0)
    val currentFps: Double
        get() = _currentFps.get()
    
    /**
     * 当前缓冲区级别（帧数）
     */
    private val _bufferLevel = AtomicLong(0)
    val bufferLevel: Int
        get() = _bufferLevel.get().toInt()
    
    /**
     * 音视频同步偏移（毫秒）
     */
    private val _syncDrift = AtomicLong(0)
    val syncDrift: Long
        get() = _syncDrift.get()
    
    /**
     * CPU 使用率（百分比）
     */
    private val _cpuUsage = AtomicReference(0.0)
    val cpuUsage: Double
        get() = _cpuUsage.get()
    
    /**
     * 内存占用（字节）
     */
    private val _memoryUsage = AtomicLong(0)
    val memoryUsage: Long
        get() = _memoryUsage.get()
    
    /**
     * 统计开始时间（毫秒）
     */
    private val startTime = AtomicLong(System.currentTimeMillis())
    
    /**
     * 上次 FPS 计算时间（毫秒）
     */
    private val lastFpsCalculationTime = AtomicLong(System.currentTimeMillis())
    
    /**
     * 上次 FPS 计算时的帧数
     */
    private val lastFrameCount = AtomicLong(0)
    
    /**
     * 增加解码帧计数
     */
    fun incrementFramesDecoded() {
        _framesDecoded.incrementAndGet()
    }
    
    /**
     * 批量增加解码帧计数
     * 优化：减少原子操作次数
     * 
     * @param count 增加的帧数
     */
    fun incrementFramesDecoded(count: Long) {
        _framesDecoded.addAndGet(count)
    }
    
    /**
     * 增加渲染帧计数
     */
    fun incrementFramesRendered() {
        _framesRendered.incrementAndGet()
        updateFps()
    }
    
    /**
     * 批量增加渲染帧计数
     * 优化：减少原子操作次数
     * 
     * @param count 增加的帧数
     */
    fun incrementFramesRendered(count: Long) {
        _framesRendered.addAndGet(count)
        updateFps()
    }
    
    /**
     * 增加丢帧计数
     */
    fun incrementFramesDropped() {
        _framesDropped.incrementAndGet()
    }
    
    /**
     * 更新缓冲区级别
     * 
     * @param level 当前缓冲区中的帧数
     */
    fun updateBufferLevel(level: Int) {
        _bufferLevel.set(level.toLong())
    }
    
    /**
     * 更新音视频同步偏移
     * 
     * @param drift 同步偏移（毫秒）
     */
    fun updateSyncDrift(drift: Long) {
        _syncDrift.set(drift)
    }
    
    /**
     * 更新 CPU 使用率
     * 
     * @param usage CPU 使用率（0.0-100.0）
     */
    fun updateCpuUsage(usage: Double) {
        _cpuUsage.set(usage.coerceIn(0.0, 100.0))
    }
    
    /**
     * 更新内存占用
     * 
     * @param bytes 内存占用（字节）
     */
    fun updateMemoryUsage(bytes: Long) {
        _memoryUsage.set(bytes)
    }
    
    /**
     * 更新帧率
     * 基于渲染帧数和时间间隔计算
     */
    private fun updateFps() {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastFpsCalculationTime.get()
        val timeDiff = currentTime - lastTime
        
        // 每秒更新一次 FPS
        if (timeDiff >= 1000) {
            val currentFrames = _framesRendered.get()
            val lastFrames = lastFrameCount.get()
            val framesDiff = currentFrames - lastFrames
            
            val fps = (framesDiff * 1000.0) / timeDiff
            _currentFps.set(fps)
            
            lastFpsCalculationTime.set(currentTime)
            lastFrameCount.set(currentFrames)
        }
    }
    
    /**
     * 重置所有统计数据
     */
    fun reset() {
        _framesDecoded.set(0)
        _framesRendered.set(0)
        _framesDropped.set(0)
        _currentFps.set(0.0)
        _bufferLevel.set(0)
        _syncDrift.set(0)
        _cpuUsage.set(0.0)
        _memoryUsage.set(0)
        startTime.set(System.currentTimeMillis())
        lastFpsCalculationTime.set(System.currentTimeMillis())
        lastFrameCount.set(0)
    }
    
    /**
     * 获取运行时长（毫秒）
     */
    fun getUptime(): Long {
        return System.currentTimeMillis() - startTime.get()
    }
    
    /**
     * 获取平均帧率
     * 
     * @return 平均帧率（帧/秒）
     */
    fun getAverageFps(): Double {
        val uptime = getUptime()
        return if (uptime > 0) {
            (_framesRendered.get() * 1000.0) / uptime
        } else {
            0.0
        }
    }
    
    /**
     * 获取丢帧率
     * 
     * @return 丢帧率（百分比）
     */
    fun getDropRate(): Double {
        val total = _framesDecoded.get()
        return if (total > 0) {
            (_framesDropped.get() * 100.0) / total
        } else {
            0.0
        }
    }
    
    /**
     * 生成格式化的统计报告
     * 
     * @return 格式化的统计信息字符串
     */
    fun toFormattedString(): String {
        return buildString {
            appendLine("Playback Statistics:")
            appendLine("  Uptime: ${getUptime() / 1000}s")
            appendLine("  Frames:")
            appendLine("    Decoded: $framesDecoded")
            appendLine("    Rendered: $framesRendered")
            appendLine("    Dropped: $framesDropped (${String.format("%.2f", getDropRate())}%)")
            appendLine("  Performance:")
            appendLine("    Current FPS: ${String.format("%.2f", currentFps)}")
            appendLine("    Average FPS: ${String.format("%.2f", getAverageFps())}")
            appendLine("    Buffer Level: $bufferLevel frames")
            appendLine("    Sync Drift: ${syncDrift}ms")
            appendLine("  Resources:")
            appendLine("    CPU Usage: ${String.format("%.1f", cpuUsage)}%")
            appendLine("    Memory Usage: ${memoryUsage / 1024 / 1024}MB")
        }
    }
    
    /**
     * 创建统计数据的快照
     * 用于在特定时刻保存统计状态
     */
    fun snapshot(): StatisticsSnapshot {
        return StatisticsSnapshot(
            framesDecoded = framesDecoded,
            framesRendered = framesRendered,
            framesDropped = framesDropped,
            currentFps = currentFps,
            bufferLevel = bufferLevel,
            syncDrift = syncDrift,
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage,
            uptime = getUptime(),
            averageFps = getAverageFps(),
            dropRate = getDropRate()
        )
    }
    
    /**
     * 统计数据快照
     * 不可变的统计数据副本
     */
    data class StatisticsSnapshot(
        val framesDecoded: Long,
        val framesRendered: Long,
        val framesDropped: Long,
        val currentFps: Double,
        val bufferLevel: Int,
        val syncDrift: Long,
        val cpuUsage: Double,
        val memoryUsage: Long,
        val uptime: Long,
        val averageFps: Double,
        val dropRate: Double
    )
}
