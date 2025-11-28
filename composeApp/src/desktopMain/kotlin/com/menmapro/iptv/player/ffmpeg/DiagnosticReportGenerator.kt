package com.menmapro.iptv.player.ffmpeg

import java.text.SimpleDateFormat
import java.util.*

/**
 * 诊断报告生成器
 * 
 * 收集所有关键指标并生成格式化的诊断报告，包括：
 * - 系统信息
 * - 媒体信息
 * - 播放统计
 * - 性能指标
 * - 错误历史
 * - 日志摘要
 * 
 * Requirements: 7.5 - 诊断报告生成
 */
class DiagnosticReportGenerator(
    private val mediaInfo: MediaInfo?,
    private val statistics: PlaybackStatistics,
    private val synchronizer: AudioVideoSynchronizer,
    private val liveStreamOptimizer: LiveStreamOptimizer?,
    private val hardwareAccelerationType: HardwareAccelerationType,
    private val currentUrl: String?,
    private val logger: PlaybackLogger?,
    private val errorHandler: ErrorHandler?,
    private val performanceMonitor: PerformanceMonitor?
) {
    
    /**
     * 日期格式化器
     */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    /**
     * 生成完整的诊断报告
     * 
     * @return 格式化的诊断报告
     * 
     * Requirements: 7.5
     */
    fun generateFullReport(): String {
        return buildString {
            appendLine("=" .repeat(70))
            appendLine("FFmpeg Player Diagnostic Report".center(70))
            appendLine("=" .repeat(70))
            appendLine()
            appendLine("Generated: ${dateFormat.format(Date())}")
            appendLine()
            
            // 系统信息
            append(generateSystemInfo())
            appendLine()
            
            // 播放信息
            append(generatePlaybackInfo())
            appendLine()
            
            // 媒体信息
            append(generateMediaInfo())
            appendLine()
            
            // 播放统计
            append(generateStatistics())
            appendLine()
            
            // 同步信息
            append(generateSyncInfo())
            appendLine()
            
            // 直播流优化信息
            if (liveStreamOptimizer != null) {
                append(generateLiveStreamInfo())
                appendLine()
            }
            
            // 性能指标
            if (performanceMonitor != null) {
                append(generatePerformanceInfo())
                appendLine()
            }
            
            // 错误历史
            if (errorHandler != null) {
                append(generateErrorInfo())
                appendLine()
            }
            
            // 日志摘要
            if (logger != null) {
                append(generateLogSummary())
                appendLine()
            }
            
            appendLine("=" .repeat(70))
        }
    }
    
    /**
     * 生成系统信息部分
     * 
     * @return 系统信息字符串
     */
    private fun generateSystemInfo(): String {
        val runtime = Runtime.getRuntime()
        
        return buildString {
            appendLine("SYSTEM INFORMATION")
            appendLine("-" .repeat(70))
            appendLine("Operating System:")
            appendLine("  Name: ${System.getProperty("os.name")}")
            appendLine("  Version: ${System.getProperty("os.version")}")
            appendLine("  Architecture: ${System.getProperty("os.arch")}")
            appendLine()
            appendLine("Java Runtime:")
            appendLine("  Version: ${System.getProperty("java.version")}")
            appendLine("  Vendor: ${System.getProperty("java.vendor")}")
            appendLine("  VM: ${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}")
            appendLine()
            appendLine("Hardware:")
            appendLine("  Processors: ${runtime.availableProcessors()}")
            appendLine("  Max Memory: ${runtime.maxMemory() / 1024 / 1024}MB")
            appendLine("  Total Memory: ${runtime.totalMemory() / 1024 / 1024}MB")
            appendLine("  Free Memory: ${runtime.freeMemory() / 1024 / 1024}MB")
        }
    }
    
    /**
     * 生成播放信息部分
     * 
     * @return 播放信息字符串
     */
    private fun generatePlaybackInfo(): String {
        return buildString {
            appendLine("PLAYBACK INFORMATION")
            appendLine("-" .repeat(70))
            appendLine("URL: ${currentUrl ?: "N/A"}")
            appendLine("Hardware Acceleration: ${hardwareAccelerationType.name}")
            appendLine("Stream Type: ${if (liveStreamOptimizer?.isLiveStream() == true) "Live" else "VOD"}")
            appendLine("Uptime: ${statistics.getUptime() / 1000}s")
        }
    }
    
    /**
     * 生成媒体信息部分
     * 
     * @return 媒体信息字符串
     */
    private fun generateMediaInfo(): String {
        return buildString {
            appendLine("MEDIA INFORMATION")
            appendLine("-" .repeat(70))
            
            if (mediaInfo != null) {
                appendLine("Format: ${mediaInfo.format}")
                appendLine("Duration: ${mediaInfo.getFormattedDuration()}")
                appendLine()
                
                if (mediaInfo.hasVideo) {
                    appendLine("Video Stream:")
                    appendLine("  Codec: ${mediaInfo.videoCodec}")
                    appendLine("  Resolution: ${mediaInfo.getFormattedResolution()}")
                    appendLine("  Frame Rate: ${String.format("%.2f", mediaInfo.videoFrameRate)} fps")
                    appendLine("  Bitrate: ${String.format("%.2f", mediaInfo.videoBitrate / 1_000_000.0)} Mbps")
                }
                
                if (mediaInfo.hasAudio) {
                    appendLine()
                    appendLine("Audio Stream:")
                    appendLine("  Codec: ${mediaInfo.audioCodec}")
                    appendLine("  Channels: ${mediaInfo.audioChannels}")
                    appendLine("  Sample Rate: ${mediaInfo.audioSampleRate} Hz")
                    appendLine("  Bitrate: ${String.format("%.2f", mediaInfo.audioBitrate / 1_000.0)} Kbps")
                }
            } else {
                appendLine("No media information available")
            }
        }
    }
    
    /**
     * 生成播放统计部分
     * 
     * @return 播放统计字符串
     */
    private fun generateStatistics(): String {
        return buildString {
            appendLine("PLAYBACK STATISTICS")
            appendLine("-" .repeat(70))
            appendLine("Frames:")
            appendLine("  Decoded: ${statistics.framesDecoded}")
            appendLine("  Rendered: ${statistics.framesRendered}")
            appendLine("  Dropped: ${statistics.framesDropped} (${String.format("%.2f", statistics.getDropRate())}%)")
            appendLine()
            appendLine("Performance:")
            appendLine("  Current FPS: ${String.format("%.2f", statistics.currentFps)}")
            appendLine("  Average FPS: ${String.format("%.2f", statistics.getAverageFps())}")
            appendLine("  Buffer Level: ${statistics.bufferLevel} frames")
            appendLine()
            appendLine("Resources:")
            appendLine("  CPU Usage: ${String.format("%.1f", statistics.cpuUsage)}%")
            appendLine("  Memory Usage: ${statistics.memoryUsage / 1024 / 1024}MB")
        }
    }
    
    /**
     * 生成同步信息部分
     * 
     * @return 同步信息字符串
     */
    private fun generateSyncInfo(): String {
        return buildString {
            appendLine("AUDIO-VIDEO SYNCHRONIZATION")
            appendLine("-" .repeat(70))
            append(synchronizer.generateDiagnosticReport())
        }
    }
    
    /**
     * 生成直播流优化信息部分
     * 
     * @return 直播流优化信息字符串
     */
    private fun generateLiveStreamInfo(): String {
        return buildString {
            appendLine("LIVE STREAM OPTIMIZATION")
            appendLine("-" .repeat(70))
            append(liveStreamOptimizer?.generateDiagnosticReport() ?: "N/A")
        }
    }
    
    /**
     * 生成性能信息部分
     * 
     * @return 性能信息字符串
     */
    private fun generatePerformanceInfo(): String {
        return buildString {
            appendLine("PERFORMANCE METRICS")
            appendLine("-" .repeat(70))
            
            val history = performanceMonitor?.getPerformanceHistory(10) ?: emptyList()
            
            if (history.isNotEmpty()) {
                val latest = history.last()
                
                appendLine("Current:")
                appendLine("  CPU Usage: ${String.format("%.1f", latest.cpuUsage)}%")
                appendLine("  Memory: ${latest.memoryUsed / 1024 / 1024}MB / ${latest.memoryMax / 1024 / 1024}MB (${String.format("%.1f", latest.memoryPercent)}%)")
                appendLine("  Heap: ${latest.heapUsed / 1024 / 1024}MB / ${latest.heapMax / 1024 / 1024}MB (${String.format("%.1f", latest.heapPercent)}%)")
                appendLine("  Threads: ${latest.threadCount}")
                appendLine("  GC Count: ${latest.gcCount}")
                appendLine("  GC Time: ${latest.gcTime}ms")
                appendLine()
                
                if (history.size >= 10) {
                    val avgCpu = performanceMonitor?.getAverageCpuUsage(10) ?: 0.0
                    val avgMemory = performanceMonitor?.getAverageMemoryUsage(10) ?: 0.0
                    
                    appendLine("Average (last 10s):")
                    appendLine("  CPU Usage: ${String.format("%.1f", avgCpu)}%")
                    appendLine("  Memory Usage: ${String.format("%.1f", avgMemory)}%")
                }
            } else {
                appendLine("No performance data available")
            }
        }
    }
    
    /**
     * 生成错误信息部分
     * 
     * @return 错误信息字符串
     */
    private fun generateErrorInfo(): String {
        return buildString {
            appendLine("ERROR HISTORY")
            appendLine("-" .repeat(70))
            
            val errors = errorHandler?.getErrorHistory(10) ?: emptyList()
            
            if (errors.isNotEmpty()) {
                val fatalCount = errors.count { it.severity == ErrorHandler.ErrorSeverity.FATAL }
                val recoverableCount = errors.count { it.severity == ErrorHandler.ErrorSeverity.RECOVERABLE }
                val warningCount = errors.count { it.severity == ErrorHandler.ErrorSeverity.WARNING }
                
                appendLine("Summary:")
                appendLine("  Total Errors: ${errors.size}")
                appendLine("  Fatal: $fatalCount")
                appendLine("  Recoverable: $recoverableCount")
                appendLine("  Warnings: $warningCount")
                appendLine()
                
                appendLine("Recent Errors:")
                errors.takeLast(5).forEach { error ->
                    appendLine("  [${dateFormat.format(Date(error.timestamp))}] [${error.type}] [${error.severity}]")
                    appendLine("    ${error.message}")
                    if (error.context.isNotEmpty()) {
                        appendLine("    Context: ${error.context}")
                    }
                }
            } else {
                appendLine("No errors recorded")
            }
        }
    }
    
    /**
     * 生成日志摘要部分
     * 
     * @return 日志摘要字符串
     */
    private fun generateLogSummary(): String {
        return buildString {
            appendLine("LOG SUMMARY")
            appendLine("-" .repeat(70))
            append(logger?.generateLogSummary() ?: "No logs available")
        }
    }
    
    /**
     * 生成简化报告（仅包含关键信息）
     * 
     * @return 简化的诊断报告
     */
    fun generateSummaryReport(): String {
        return buildString {
            appendLine("=== FFmpeg Player Summary ===")
            appendLine()
            appendLine("URL: ${currentUrl ?: "N/A"}")
            appendLine("Uptime: ${statistics.getUptime() / 1000}s")
            appendLine("Hardware Acceleration: ${hardwareAccelerationType.name}")
            appendLine()
            appendLine("Statistics:")
            appendLine("  FPS: ${String.format("%.2f", statistics.currentFps)} (avg: ${String.format("%.2f", statistics.getAverageFps())})")
            appendLine("  Frames: ${statistics.framesRendered} rendered, ${statistics.framesDropped} dropped (${String.format("%.2f", statistics.getDropRate())}%)")
            appendLine("  CPU: ${String.format("%.1f", statistics.cpuUsage)}%")
            appendLine("  Memory: ${statistics.memoryUsage / 1024 / 1024}MB")
            appendLine()
            
            val errorCount = errorHandler?.getErrorHistory()?.size ?: 0
            if (errorCount > 0) {
                appendLine("Errors: $errorCount recorded")
            } else {
                appendLine("No errors")
            }
            
            appendLine("============================")
        }
    }
    
    /**
     * 生成 JSON 格式的报告
     * 
     * @return JSON 格式的诊断报告
     */
    fun generateJsonReport(): String {
        return buildString {
            appendLine("{")
            appendLine("  \"timestamp\": ${System.currentTimeMillis()},")
            appendLine("  \"url\": \"${currentUrl ?: ""}\",")
            appendLine("  \"hardware_acceleration\": \"${hardwareAccelerationType.name}\",")
            appendLine("  \"uptime_ms\": ${statistics.getUptime()},")
            
            // 媒体信息
            appendLine("  \"media\": {")
            if (mediaInfo != null) {
                appendLine("    \"format\": \"${mediaInfo.format}\",")
                appendLine("    \"duration_ms\": ${mediaInfo.duration},")
                appendLine("    \"video_codec\": \"${mediaInfo.videoCodec}\",")
                appendLine("    \"audio_codec\": \"${mediaInfo.audioCodec}\",")
                appendLine("    \"resolution\": \"${mediaInfo.getFormattedResolution()}\",")
                appendLine("    \"frame_rate\": ${mediaInfo.videoFrameRate}")
            }
            appendLine("  },")
            
            // 统计信息
            appendLine("  \"statistics\": {")
            appendLine("    \"frames_decoded\": ${statistics.framesDecoded},")
            appendLine("    \"frames_rendered\": ${statistics.framesRendered},")
            appendLine("    \"frames_dropped\": ${statistics.framesDropped},")
            appendLine("    \"drop_rate\": ${statistics.getDropRate()},")
            appendLine("    \"current_fps\": ${statistics.currentFps},")
            appendLine("    \"average_fps\": ${statistics.getAverageFps()},")
            appendLine("    \"buffer_level\": ${statistics.bufferLevel},")
            appendLine("    \"cpu_usage\": ${statistics.cpuUsage},")
            appendLine("    \"memory_usage_bytes\": ${statistics.memoryUsage}")
            appendLine("  },")
            
            // 同步信息
            appendLine("  \"sync\": {")
            appendLine("    \"average_error_ms\": ${synchronizer.getAverageSyncError()},")
            appendLine("    \"max_sync_diff_ms\": ${synchronizer.getMaxSyncDiff()},")
            appendLine("    \"frames_dropped\": ${synchronizer.getDroppedFrameCount()}")
            appendLine("  },")
            
            // 错误信息
            val errorCount = errorHandler?.getErrorHistory()?.size ?: 0
            appendLine("  \"errors\": {")
            appendLine("    \"count\": $errorCount")
            appendLine("  }")
            
            appendLine("}")
        }
    }
    
    /**
     * 将报告保存到文件
     * 
     * @param filePath 文件路径
     * @param format 报告格式（"full", "summary", "json"）
     * @return 是否成功保存
     */
    fun saveToFile(filePath: String, format: String = "full"): Boolean {
        return try {
            val report = when (format.lowercase()) {
                "summary" -> generateSummaryReport()
                "json" -> generateJsonReport()
                else -> generateFullReport()
            }
            
            java.io.File(filePath).writeText(report)
            println("[DiagnosticReportGenerator] Report saved to: $filePath")
            true
        } catch (e: Exception) {
            println("[DiagnosticReportGenerator] Failed to save report: ${e.message}")
            false
        }
    }
    
    /**
     * 字符串居中辅助函数
     */
    private fun String.center(width: Int): String {
        val padding = (width - this.length) / 2
        return " ".repeat(padding.coerceAtLeast(0)) + this
    }
}
