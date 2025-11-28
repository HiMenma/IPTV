package com.menmapro.iptv.player.ffmpeg

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.LinkedBlockingQueue
import org.bytedeco.javacv.Frame

/**
 * 统计监控器
 * 
 * 定期更新播放统计信息，包括：
 * - 帧率和比特率
 * - 缓冲状态
 * - CPU 和内存使用
 * 
 * Requirements: 7.2 - 播放统计更新, 7.4 - 性能监控
 */
class StatisticsMonitor(
    private val statistics: PlaybackStatistics,
    private val videoQueue: LinkedBlockingQueue<Frame>,
    private val audioQueue: LinkedBlockingQueue<Frame>,
    private val isPlaying: AtomicBoolean,
    private val logger: PlaybackLogger? = null
) : Runnable {
    
    /**
     * 监控线程
     */
    private var monitorThread: Thread? = null
    
    /**
     * 更新间隔（毫秒）
     */
    private val updateInterval = 1000L // 每秒更新一次
    
    /**
     * 性能监控间隔（毫秒）
     */
    private val performanceUpdateInterval = 5000L // 每 5 秒更新一次性能指标
    
    /**
     * 上次性能更新时间
     */
    private var lastPerformanceUpdate = System.currentTimeMillis()
    
    /**
     * Runtime 实例用于获取内存信息
     */
    private val runtime = Runtime.getRuntime()
    
    /**
     * 操作系统 MXBean 用于获取 CPU 信息
     */
    private val osBean = try {
        java.lang.management.ManagementFactory.getOperatingSystemMXBean()
    } catch (e: Exception) {
        null
    }
    
    /**
     * 启动监控
     */
    fun start() {
        if (monitorThread != null && monitorThread!!.isAlive) {
            println("[StatisticsMonitor] Already running")
            return
        }
        
        println("[StatisticsMonitor] Starting statistics monitor")
        monitorThread = Thread(this, "StatisticsMonitor").apply {
            isDaemon = true
            start()
        }
    }
    
    /**
     * 停止监控
     */
    fun stop() {
        println("[StatisticsMonitor] Stopping statistics monitor")
        monitorThread?.interrupt()
        monitorThread = null
    }
    
    /**
     * 监控主循环
     */
    override fun run() {
        try {
            while (isPlaying.get() && !Thread.currentThread().isInterrupted) {
                try {
                    // 更新统计信息
                    updateStatistics()
                    
                    // 定期更新性能指标
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastPerformanceUpdate >= performanceUpdateInterval) {
                        updatePerformanceMetrics()
                        lastPerformanceUpdate = currentTime
                    }
                    
                    // 等待下一次更新
                    Thread.sleep(updateInterval)
                    
                } catch (e: InterruptedException) {
                    println("[StatisticsMonitor] Interrupted")
                    break
                } catch (e: Exception) {
                    println("[StatisticsMonitor] Error updating statistics: ${e.message}")
                    logger?.logDebug(
                        category = "MONITOR",
                        message = "Statistics update error: ${e.message}"
                    )
                }
            }
        } finally {
            println("[StatisticsMonitor] Monitor stopped")
        }
    }
    
    /**
     * 更新统计信息
     * 
     * Requirements: 7.2
     */
    private fun updateStatistics() {
        // 更新缓冲区级别
        val videoBufferLevel = videoQueue.size
        val audioBufferLevel = audioQueue.size
        statistics.updateBufferLevel(videoBufferLevel)
        
        // 记录缓冲区状态（调试级别）
        logger?.logDebug(
            category = "BUFFER",
            message = "Buffer status",
            context = mapOf(
                "video_buffer" to videoBufferLevel,
                "audio_buffer" to audioBufferLevel,
                "video_capacity" to (videoQueue.remainingCapacity() + videoQueue.size),
                "audio_capacity" to (audioQueue.remainingCapacity() + audioQueue.size)
            )
        )
        
        // 检查缓冲区健康状态
        checkBufferHealth(videoBufferLevel, audioBufferLevel)
    }
    
    /**
     * 更新性能指标
     * 
     * Requirements: 7.4
     */
    private fun updatePerformanceMetrics() {
        // 更新内存使用
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        statistics.updateMemoryUsage(usedMemory)
        
        // 更新 CPU 使用率（如果可用）
        val cpuUsage = getCpuUsage()
        if (cpuUsage >= 0) {
            statistics.updateCpuUsage(cpuUsage)
        }
        
        // 记录性能指标
        logger?.logInfo(
            category = "PERFORMANCE",
            message = "Performance metrics updated",
            context = mapOf(
                "memory_mb" to (usedMemory / 1024 / 1024),
                "cpu_usage" to cpuUsage,
                "fps" to statistics.currentFps,
                "drop_rate" to statistics.getDropRate()
            )
        )
        
        // 检查性能问题
        checkPerformanceIssues(cpuUsage, usedMemory)
    }
    
    /**
     * 获取 CPU 使用率
     * 
     * @return CPU 使用率（0-100），如果无法获取则返回 -1
     */
    private fun getCpuUsage(): Double {
        return try {
            // 尝试使用 OperatingSystemMXBean 获取 CPU 使用率
            val bean = osBean
            if (bean != null) {
                // 使用反射访问 getProcessCpuLoad 方法（仅在某些 JVM 实现中可用）
                val method = bean.javaClass.getMethod("getProcessCpuLoad")
                val cpuLoad = method.invoke(bean) as Double
                
                // getProcessCpuLoad 返回 0-1 之间的值，转换为百分比
                if (cpuLoad >= 0) {
                    cpuLoad * 100.0
                } else {
                    -1.0
                }
            } else {
                -1.0
            }
        } catch (e: Exception) {
            // 如果无法获取 CPU 使用率，返回 -1
            -1.0
        }
    }
    
    /**
     * 检查缓冲区健康状态
     * 
     * @param videoBufferLevel 视频缓冲区级别
     * @param audioBufferLevel 音频缓冲区级别
     */
    private fun checkBufferHealth(videoBufferLevel: Int, audioBufferLevel: Int) {
        // 检查视频缓冲区是否过低
        if (videoBufferLevel < 3) {
            logger?.logPerformanceWarning(
                message = "Video buffer running low: $videoBufferLevel frames",
                context = mapOf("buffer_level" to videoBufferLevel)
            )
        }
        
        // 检查音频缓冲区是否过低
        if (audioBufferLevel < 10) {
            logger?.logPerformanceWarning(
                message = "Audio buffer running low: $audioBufferLevel frames",
                context = mapOf("buffer_level" to audioBufferLevel)
            )
        }
        
        // 检查缓冲区是否已满（可能表示消费速度过慢）
        val videoCapacity = videoQueue.remainingCapacity() + videoQueue.size
        val audioCapacity = audioQueue.remainingCapacity() + audioQueue.size
        
        if (videoBufferLevel >= videoCapacity * 0.9) {
            logger?.logPerformanceWarning(
                message = "Video buffer nearly full: $videoBufferLevel/$videoCapacity frames",
                context = mapOf(
                    "buffer_level" to videoBufferLevel,
                    "capacity" to videoCapacity
                )
            )
        }
        
        if (audioBufferLevel >= audioCapacity * 0.9) {
            logger?.logPerformanceWarning(
                message = "Audio buffer nearly full: $audioBufferLevel/$audioCapacity frames",
                context = mapOf(
                    "buffer_level" to audioBufferLevel,
                    "capacity" to audioCapacity
                )
            )
        }
    }
    
    /**
     * 检查性能问题
     * 
     * @param cpuUsage CPU 使用率
     * @param memoryUsage 内存使用量（字节）
     */
    private fun checkPerformanceIssues(cpuUsage: Double, memoryUsage: Long) {
        // 检查 CPU 使用率是否过高
        if (cpuUsage > 80.0) {
            logger?.logPerformanceWarning(
                message = "High CPU usage: ${String.format("%.1f", cpuUsage)}%",
                context = mapOf("cpu_usage" to cpuUsage)
            )
        }
        
        // 检查内存使用是否过高
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (memoryUsage.toDouble() / maxMemory) * 100
        
        if (memoryUsagePercent > 80.0) {
            logger?.logPerformanceWarning(
                message = "High memory usage: ${memoryUsage / 1024 / 1024}MB (${String.format("%.1f", memoryUsagePercent)}%)",
                context = mapOf(
                    "memory_mb" to (memoryUsage / 1024 / 1024),
                    "memory_percent" to memoryUsagePercent,
                    "max_memory_mb" to (maxMemory / 1024 / 1024)
                )
            )
        }
        
        // 检查丢帧率是否过高
        val dropRate = statistics.getDropRate()
        if (dropRate > 5.0) {
            logger?.logPerformanceWarning(
                message = "High frame drop rate: ${String.format("%.2f", dropRate)}%",
                context = mapOf(
                    "drop_rate" to dropRate,
                    "frames_dropped" to statistics.framesDropped,
                    "frames_decoded" to statistics.framesDecoded
                )
            )
        }
        
        // 检查帧率是否过低
        val currentFps = statistics.currentFps
        if (currentFps > 0 && currentFps < 20.0) {
            logger?.logPerformanceWarning(
                message = "Low frame rate: ${String.format("%.2f", currentFps)} fps",
                context = mapOf("fps" to currentFps)
            )
        }
    }
    
    /**
     * 生成统计报告
     * 
     * @return 格式化的统计报告
     */
    fun generateReport(): String {
        return buildString {
            appendLine("=== Statistics Monitor Report ===")
            appendLine()
            
            appendLine("Playback Statistics:")
            appendLine("  Uptime: ${statistics.getUptime() / 1000}s")
            appendLine("  Frames Decoded: ${statistics.framesDecoded}")
            appendLine("  Frames Rendered: ${statistics.framesRendered}")
            appendLine("  Frames Dropped: ${statistics.framesDropped} (${String.format("%.2f", statistics.getDropRate())}%)")
            appendLine("  Current FPS: ${String.format("%.2f", statistics.currentFps)}")
            appendLine("  Average FPS: ${String.format("%.2f", statistics.getAverageFps())}")
            appendLine()
            
            appendLine("Buffer Status:")
            appendLine("  Video Buffer: ${videoQueue.size}/${videoQueue.remainingCapacity() + videoQueue.size} frames")
            appendLine("  Audio Buffer: ${audioQueue.size}/${audioQueue.remainingCapacity() + audioQueue.size} frames")
            appendLine()
            
            appendLine("Performance Metrics:")
            appendLine("  CPU Usage: ${String.format("%.1f", statistics.cpuUsage)}%")
            appendLine("  Memory Usage: ${statistics.memoryUsage / 1024 / 1024}MB")
            appendLine("  Max Memory: ${runtime.maxMemory() / 1024 / 1024}MB")
            appendLine()
            
            appendLine("================================")
        }
    }
}
