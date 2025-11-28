package com.menmapro.iptv.player.ffmpeg

import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import java.lang.management.ThreadMXBean
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 性能监控器
 * 
 * 监控播放器的性能指标，包括：
 * - CPU 使用率
 * - 内存占用
 * - 线程状态
 * - GC 活动
 * 
 * Requirements: 7.4 - 性能监控
 */
class PerformanceMonitor(
    private val statistics: PlaybackStatistics,
    private val logger: PlaybackLogger? = null
) {
    
    /**
     * Runtime 实例
     */
    private val runtime = Runtime.getRuntime()
    
    /**
     * 内存 MXBean
     */
    private val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
    
    /**
     * 线程 MXBean
     */
    private val threadBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
    
    /**
     * 操作系统 MXBean
     */
    private val osBean = try {
        ManagementFactory.getOperatingSystemMXBean()
    } catch (e: Exception) {
        null
    }
    
    /**
     * GC MXBeans
     */
    private val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
    
    /**
     * 上次 CPU 测量时间
     */
    private var lastCpuTime = 0L
    private var lastSystemTime = System.nanoTime()
    
    /**
     * 上次 GC 统计
     */
    private var lastGcCount = 0L
    private var lastGcTime = 0L
    
    /**
     * 性能快照
     */
    data class PerformanceSnapshot(
        val timestamp: Long,
        val cpuUsage: Double,
        val memoryUsed: Long,
        val memoryMax: Long,
        val memoryPercent: Double,
        val heapUsed: Long,
        val heapMax: Long,
        val heapPercent: Double,
        val threadCount: Int,
        val gcCount: Long,
        val gcTime: Long,
        val gcRate: Double
    )
    
    /**
     * 性能历史（最多保留最近 60 个快照，每秒一个）
     */
    private val performanceHistory = mutableListOf<PerformanceSnapshot>()
    private val maxHistorySize = 60
    
    /**
     * 更新性能指标
     * 
     * Requirements: 7.4
     */
    fun updateMetrics() {
        // 更新 CPU 使用率
        val cpuUsage = measureCpuUsage()
        if (cpuUsage >= 0) {
            statistics.updateCpuUsage(cpuUsage)
        }
        
        // 更新内存使用
        val memoryUsage = measureMemoryUsage()
        statistics.updateMemoryUsage(memoryUsage.used)
        
        // 创建性能快照
        val snapshot = createSnapshot(cpuUsage, memoryUsage)
        recordSnapshot(snapshot)
        
        // 检查性能问题
        checkPerformanceIssues(snapshot)
        
        // 记录性能指标
        logger?.logDebug(
            category = "PERFORMANCE",
            message = "Performance metrics updated",
            context = mapOf(
                "cpu_usage" to cpuUsage,
                "memory_mb" to (memoryUsage.used / 1024 / 1024),
                "memory_percent" to memoryUsage.percent,
                "heap_mb" to (memoryUsage.heapUsed / 1024 / 1024),
                "thread_count" to snapshot.threadCount,
                "gc_count" to snapshot.gcCount
            )
        )
    }
    
    /**
     * 测量 CPU 使用率
     * 
     * @return CPU 使用率（0-100），如果无法测量则返回 -1
     */
    private fun measureCpuUsage(): Double {
        return try {
            val bean = osBean ?: return -1.0
            
            // 尝试使用 getProcessCpuLoad 方法
            val method = bean.javaClass.getMethod("getProcessCpuLoad")
            val cpuLoad = method.invoke(bean) as Double
            
            if (cpuLoad >= 0) {
                cpuLoad * 100.0
            } else {
                // 如果 getProcessCpuLoad 不可用，尝试使用线程 CPU 时间
                measureCpuUsageFromThreads()
            }
        } catch (e: Exception) {
            // 回退到线程 CPU 时间测量
            measureCpuUsageFromThreads()
        }
    }
    
    /**
     * 从线程 CPU 时间测量 CPU 使用率
     * 
     * @return CPU 使用率（0-100），如果无法测量则返回 -1
     */
    private fun measureCpuUsageFromThreads(): Double {
        return try {
            if (!threadBean.isCurrentThreadCpuTimeSupported) {
                return -1.0
            }
            
            val currentTime = System.nanoTime()
            val currentCpuTime = threadBean.allThreadIds.sumOf { id ->
                try {
                    threadBean.getThreadCpuTime(id)
                } catch (e: Exception) {
                    0L
                }
            }
            
            if (lastCpuTime == 0L) {
                lastCpuTime = currentCpuTime
                lastSystemTime = currentTime
                return -1.0
            }
            
            val cpuTimeDiff = currentCpuTime - lastCpuTime
            val systemTimeDiff = currentTime - lastSystemTime
            
            lastCpuTime = currentCpuTime
            lastSystemTime = currentTime
            
            if (systemTimeDiff > 0) {
                val cpuUsage = (cpuTimeDiff.toDouble() / systemTimeDiff) * 100.0
                cpuUsage.coerceIn(0.0, 100.0 * runtime.availableProcessors())
            } else {
                -1.0
            }
        } catch (e: Exception) {
            -1.0
        }
    }
    
    /**
     * 内存使用信息
     */
    data class MemoryUsage(
        val used: Long,
        val max: Long,
        val percent: Double,
        val heapUsed: Long,
        val heapMax: Long,
        val heapPercent: Double
    )
    
    /**
     * 测量内存使用
     * 
     * @return 内存使用信息
     */
    private fun measureMemoryUsage(): MemoryUsage {
        // 总内存使用
        val totalUsed = runtime.totalMemory() - runtime.freeMemory()
        val totalMax = runtime.maxMemory()
        val totalPercent = (totalUsed.toDouble() / totalMax) * 100.0
        
        // 堆内存使用
        val heapMemory = memoryBean.heapMemoryUsage
        val heapUsed = heapMemory.used
        val heapMax = heapMemory.max
        val heapPercent = (heapUsed.toDouble() / heapMax) * 100.0
        
        return MemoryUsage(
            used = totalUsed,
            max = totalMax,
            percent = totalPercent,
            heapUsed = heapUsed,
            heapMax = heapMax,
            heapPercent = heapPercent
        )
    }
    
    /**
     * 测量 GC 活动
     * 
     * @return GC 统计信息 (count, time, rate)
     */
    private fun measureGcActivity(): Triple<Long, Long, Double> {
        var totalGcCount = 0L
        var totalGcTime = 0L
        
        gcBeans.forEach { gc ->
            totalGcCount += gc.collectionCount
            totalGcTime += gc.collectionTime
        }
        
        val gcRate = if (lastGcCount > 0) {
            (totalGcCount - lastGcCount).toDouble()
        } else {
            0.0
        }
        
        lastGcCount = totalGcCount
        lastGcTime = totalGcTime
        
        return Triple(totalGcCount, totalGcTime, gcRate)
    }
    
    /**
     * 创建性能快照
     * 
     * @param cpuUsage CPU 使用率
     * @param memoryUsage 内存使用信息
     * @return 性能快照
     */
    private fun createSnapshot(cpuUsage: Double, memoryUsage: MemoryUsage): PerformanceSnapshot {
        val (gcCount, gcTime, gcRate) = measureGcActivity()
        val threadCount = threadBean.threadCount
        
        return PerformanceSnapshot(
            timestamp = System.currentTimeMillis(),
            cpuUsage = cpuUsage,
            memoryUsed = memoryUsage.used,
            memoryMax = memoryUsage.max,
            memoryPercent = memoryUsage.percent,
            heapUsed = memoryUsage.heapUsed,
            heapMax = memoryUsage.heapMax,
            heapPercent = memoryUsage.heapPercent,
            threadCount = threadCount,
            gcCount = gcCount,
            gcTime = gcTime,
            gcRate = gcRate
        )
    }
    
    /**
     * 记录性能快照
     * 
     * @param snapshot 性能快照
     */
    private fun recordSnapshot(snapshot: PerformanceSnapshot) {
        synchronized(performanceHistory) {
            performanceHistory.add(snapshot)
            
            // 限制历史大小
            while (performanceHistory.size > maxHistorySize) {
                performanceHistory.removeAt(0)
            }
        }
    }
    
    /**
     * 检查性能问题
     * 
     * @param snapshot 性能快照
     */
    private fun checkPerformanceIssues(snapshot: PerformanceSnapshot) {
        // 检查 CPU 使用率
        if (snapshot.cpuUsage > 80.0) {
            logger?.logPerformanceWarning(
                message = "High CPU usage: ${String.format("%.1f", snapshot.cpuUsage)}%",
                context = mapOf("cpu_usage" to snapshot.cpuUsage)
            )
        }
        
        // 检查内存使用
        if (snapshot.memoryPercent > 80.0) {
            logger?.logPerformanceWarning(
                message = "High memory usage: ${snapshot.memoryUsed / 1024 / 1024}MB (${String.format("%.1f", snapshot.memoryPercent)}%)",
                context = mapOf(
                    "memory_mb" to (snapshot.memoryUsed / 1024 / 1024),
                    "memory_percent" to snapshot.memoryPercent
                )
            )
        }
        
        // 检查堆内存使用
        if (snapshot.heapPercent > 90.0) {
            logger?.logPerformanceWarning(
                message = "High heap usage: ${snapshot.heapUsed / 1024 / 1024}MB (${String.format("%.1f", snapshot.heapPercent)}%)",
                context = mapOf(
                    "heap_mb" to (snapshot.heapUsed / 1024 / 1024),
                    "heap_percent" to snapshot.heapPercent
                )
            )
        }
        
        // 检查 GC 活动
        if (snapshot.gcRate > 5.0) {
            logger?.logPerformanceWarning(
                message = "High GC activity: ${snapshot.gcRate} collections/sec",
                context = mapOf(
                    "gc_rate" to snapshot.gcRate,
                    "gc_count" to snapshot.gcCount,
                    "gc_time" to snapshot.gcTime
                )
            )
        }
        
        // 检查线程数
        if (snapshot.threadCount > 100) {
            logger?.logPerformanceWarning(
                message = "High thread count: ${snapshot.threadCount}",
                context = mapOf("thread_count" to snapshot.threadCount)
            )
        }
    }
    
    /**
     * 获取性能历史
     * 
     * @param count 数量
     * @return 性能快照列表
     */
    fun getPerformanceHistory(count: Int = 60): List<PerformanceSnapshot> {
        synchronized(performanceHistory) {
            return performanceHistory.takeLast(count)
        }
    }
    
    /**
     * 获取平均 CPU 使用率
     * 
     * @param seconds 时间范围（秒）
     * @return 平均 CPU 使用率
     */
    fun getAverageCpuUsage(seconds: Int = 10): Double {
        synchronized(performanceHistory) {
            val snapshots = performanceHistory.takeLast(seconds)
            return if (snapshots.isNotEmpty()) {
                snapshots.map { it.cpuUsage }.average()
            } else {
                0.0
            }
        }
    }
    
    /**
     * 获取平均内存使用率
     * 
     * @param seconds 时间范围（秒）
     * @return 平均内存使用率
     */
    fun getAverageMemoryUsage(seconds: Int = 10): Double {
        synchronized(performanceHistory) {
            val snapshots = performanceHistory.takeLast(seconds)
            return if (snapshots.isNotEmpty()) {
                snapshots.map { it.memoryPercent }.average()
            } else {
                0.0
            }
        }
    }
    
    /**
     * 清空性能历史
     */
    fun clearHistory() {
        synchronized(performanceHistory) {
            performanceHistory.clear()
        }
    }
    
    /**
     * 生成性能报告
     * 
     * @return 格式化的性能报告
     */
    fun generateReport(): String {
        synchronized(performanceHistory) {
            val latest = performanceHistory.lastOrNull()
            
            return buildString {
                appendLine("=== Performance Monitor Report ===")
                appendLine()
                
                if (latest != null) {
                    appendLine("Current Metrics:")
                    appendLine("  CPU Usage: ${String.format("%.1f", latest.cpuUsage)}%")
                    appendLine("  Memory: ${latest.memoryUsed / 1024 / 1024}MB / ${latest.memoryMax / 1024 / 1024}MB (${String.format("%.1f", latest.memoryPercent)}%)")
                    appendLine("  Heap: ${latest.heapUsed / 1024 / 1024}MB / ${latest.heapMax / 1024 / 1024}MB (${String.format("%.1f", latest.heapPercent)}%)")
                    appendLine("  Threads: ${latest.threadCount}")
                    appendLine("  GC Count: ${latest.gcCount}")
                    appendLine("  GC Time: ${latest.gcTime}ms")
                    appendLine()
                }
                
                if (performanceHistory.size >= 10) {
                    appendLine("Average (last 10s):")
                    appendLine("  CPU Usage: ${String.format("%.1f", getAverageCpuUsage(10))}%")
                    appendLine("  Memory Usage: ${String.format("%.1f", getAverageMemoryUsage(10))}%")
                    appendLine()
                }
                
                appendLine("System Information:")
                appendLine("  Available Processors: ${runtime.availableProcessors()}")
                appendLine("  Max Memory: ${runtime.maxMemory() / 1024 / 1024}MB")
                appendLine("  Total Memory: ${runtime.totalMemory() / 1024 / 1024}MB")
                appendLine("  Free Memory: ${runtime.freeMemory() / 1024 / 1024}MB")
                appendLine()
                
                appendLine("==================================")
            }
        }
    }
}
