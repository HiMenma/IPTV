package com.menmapro.iptv.player.ffmpeg

import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 播放日志记录器
 * 
 * 负责记录播放过程中的各种信息，包括：
 * - 媒体格式和编解码器信息
 * - 流信息
 * - 解码错误
 * - 性能指标
 * 
 * Requirements: 7.1 - 播放信息日志记录, 7.3 - 错误日志记录
 */
class PlaybackLogger(
    private val enableFileLogging: Boolean = false,
    private val logFilePath: String? = null
) {
    
    /**
     * 日志级别
     */
    enum class LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
    
    /**
     * 日志条目
     */
    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val category: String,
        val message: String,
        val context: Map<String, Any> = emptyMap()
    )
    
    /**
     * 内存中的日志队列（最多保留最近 1000 条）
     */
    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val maxLogEntries = 1000
    
    /**
     * 日期格式化器
     */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    
    /**
     * 日志文件
     */
    private val logFile: File? = if (enableFileLogging && logFilePath != null) {
        File(logFilePath).also { file ->
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                file.createNewFile()
            }
        }
    } else {
        null
    }
    
    /**
     * 记录播放开始信息
     * 
     * @param url 媒体 URL
     * @param mediaInfo 媒体信息
     * @param hardwareAcceleration 硬件加速类型
     * 
     * Requirements: 7.1
     */
    fun logPlaybackStart(
        url: String,
        mediaInfo: MediaInfo?,
        hardwareAcceleration: HardwareAccelerationType
    ) {
        val context = mutableMapOf<String, Any>(
            "url" to url,
            "hardware_acceleration" to hardwareAcceleration.name
        )
        
        mediaInfo?.let { info ->
            context["duration"] = info.duration
            context["video_codec"] = info.videoCodec
            context["audio_codec"] = info.audioCodec
            context["resolution"] = "${info.videoWidth}x${info.videoHeight}"
            context["frame_rate"] = info.videoFrameRate
            context["video_bitrate"] = info.videoBitrate
            context["audio_bitrate"] = info.audioBitrate
            context["audio_channels"] = info.audioChannels
            context["audio_sample_rate"] = info.audioSampleRate
            context["format"] = info.format
        }
        
        log(
            level = LogLevel.INFO,
            category = "PLAYBACK",
            message = "Playback started: $url",
            context = context
        )
        
        // 详细的媒体信息日志
        mediaInfo?.let { info ->
            log(
                level = LogLevel.INFO,
                category = "MEDIA_INFO",
                message = buildString {
                    appendLine("Media Information:")
                    appendLine("  Format: ${info.format}")
                    appendLine("  Duration: ${info.getFormattedDuration()}")
                    if (info.hasVideo) {
                        appendLine("  Video: ${info.videoCodec} ${info.getFormattedResolution()} @ ${String.format("%.2f", info.videoFrameRate)}fps")
                        appendLine("  Video Bitrate: ${String.format("%.1f", info.videoBitrate / 1_000_000.0)} Mbps")
                    }
                    if (info.hasAudio) {
                        appendLine("  Audio: ${info.audioCodec} ${info.audioChannels}ch @ ${info.audioSampleRate}Hz")
                        appendLine("  Audio Bitrate: ${String.format("%.1f", info.audioBitrate / 1_000.0)} Kbps")
                    }
                },
                context = context
            )
        }
        
        // 硬件加速信息
        log(
            level = LogLevel.INFO,
            category = "HARDWARE",
            message = "Hardware acceleration: ${hardwareAcceleration.name}",
            context = mapOf("type" to hardwareAcceleration.name)
        )
    }
    
    /**
     * 记录播放停止信息
     * 
     * @param statistics 播放统计
     * 
     * Requirements: 7.1
     */
    fun logPlaybackStop(statistics: PlaybackStatistics) {
        val context = mapOf(
            "uptime" to statistics.getUptime(),
            "frames_decoded" to statistics.framesDecoded,
            "frames_rendered" to statistics.framesRendered,
            "frames_dropped" to statistics.framesDropped,
            "average_fps" to statistics.getAverageFps(),
            "drop_rate" to statistics.getDropRate()
        )
        
        log(
            level = LogLevel.INFO,
            category = "PLAYBACK",
            message = "Playback stopped. Uptime: ${statistics.getUptime() / 1000}s, " +
                    "Frames: ${statistics.framesRendered}, " +
                    "Dropped: ${statistics.framesDropped} (${String.format("%.2f", statistics.getDropRate())}%)",
            context = context
        )
    }
    
    /**
     * 记录解码错误
     * 
     * @param error 错误信息
     * @param timestamp 时间戳（微秒）
     * @param context 上下文信息
     * 
     * Requirements: 7.3
     */
    fun logDecodingError(
        error: String,
        timestamp: Long = 0,
        context: Map<String, Any> = emptyMap()
    ) {
        val fullContext = context.toMutableMap()
        if (timestamp > 0) {
            fullContext["timestamp_us"] = timestamp
            fullContext["timestamp_ms"] = timestamp / 1000
        }
        
        log(
            level = LogLevel.ERROR,
            category = "DECODING",
            message = "Decoding error: $error",
            context = fullContext
        )
    }
    
    /**
     * 记录网络错误
     * 
     * @param error 错误信息
     * @param url 媒体 URL
     * @param context 上下文信息
     * 
     * Requirements: 7.3
     */
    fun logNetworkError(
        error: String,
        url: String,
        context: Map<String, Any> = emptyMap()
    ) {
        val fullContext = context.toMutableMap()
        fullContext["url"] = url
        
        log(
            level = LogLevel.ERROR,
            category = "NETWORK",
            message = "Network error: $error",
            context = fullContext
        )
    }
    
    /**
     * 记录渲染错误
     * 
     * @param error 错误信息
     * @param context 上下文信息
     * 
     * Requirements: 7.3
     */
    fun logRenderingError(
        error: String,
        context: Map<String, Any> = emptyMap()
    ) {
        log(
            level = LogLevel.ERROR,
            category = "RENDERING",
            message = "Rendering error: $error",
            context = context
        )
    }
    
    /**
     * 记录音频错误
     * 
     * @param error 错误信息
     * @param context 上下文信息
     * 
     * Requirements: 7.3
     */
    fun logAudioError(
        error: String,
        context: Map<String, Any> = emptyMap()
    ) {
        log(
            level = LogLevel.ERROR,
            category = "AUDIO",
            message = "Audio error: $error",
            context = context
        )
    }
    
    /**
     * 记录同步警告
     * 
     * @param syncError 同步误差（毫秒）
     * @param context 上下文信息
     * 
     * Requirements: 7.3
     */
    fun logSyncWarning(
        syncError: Double,
        context: Map<String, Any> = emptyMap()
    ) {
        val fullContext = context.toMutableMap()
        fullContext["sync_error_ms"] = syncError
        
        log(
            level = LogLevel.WARN,
            category = "SYNC",
            message = "Audio-video sync error: ${String.format("%.2f", syncError)}ms",
            context = fullContext
        )
    }
    
    /**
     * 记录性能警告
     * 
     * @param message 警告信息
     * @param context 上下文信息
     * 
     * Requirements: 7.4
     */
    fun logPerformanceWarning(
        message: String,
        context: Map<String, Any> = emptyMap()
    ) {
        log(
            level = LogLevel.WARN,
            category = "PERFORMANCE",
            message = message,
            context = context
        )
    }
    
    /**
     * 记录调试信息
     * 
     * @param category 类别
     * @param message 消息
     * @param context 上下文信息
     */
    fun logDebug(
        category: String,
        message: String,
        context: Map<String, Any> = emptyMap()
    ) {
        log(
            level = LogLevel.DEBUG,
            category = category,
            message = message,
            context = context
        )
    }
    
    /**
     * 记录信息
     * 
     * @param category 类别
     * @param message 消息
     * @param context 上下文信息
     */
    fun logInfo(
        category: String,
        message: String,
        context: Map<String, Any> = emptyMap()
    ) {
        log(
            level = LogLevel.INFO,
            category = category,
            message = message,
            context = context
        )
    }
    
    /**
     * 核心日志方法
     * 
     * @param level 日志级别
     * @param category 类别
     * @param message 消息
     * @param context 上下文信息
     */
    private fun log(
        level: LogLevel,
        category: String,
        message: String,
        context: Map<String, Any> = emptyMap()
    ) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            message = message,
            context = context
        )
        
        // 添加到内存队列
        logQueue.offer(entry)
        
        // 限制队列大小
        while (logQueue.size > maxLogEntries) {
            logQueue.poll()
        }
        
        // 输出到控制台
        println(formatLogEntry(entry))
        
        // 写入文件（如果启用）
        if (enableFileLogging && logFile != null) {
            try {
                logFile.appendText(formatLogEntry(entry) + "\n")
            } catch (e: Exception) {
                System.err.println("Failed to write to log file: ${e.message}")
            }
        }
    }
    
    /**
     * 格式化日志条目
     * 
     * @param entry 日志条目
     * @return 格式化的字符串
     */
    private fun formatLogEntry(entry: LogEntry): String {
        val timestamp = dateFormat.format(Date(entry.timestamp))
        val levelStr = entry.level.name.padEnd(5)
        val categoryStr = entry.category.padEnd(12)
        
        return buildString {
            append("[$timestamp] [$levelStr] [$categoryStr] ${entry.message}")
            
            if (entry.context.isNotEmpty()) {
                append(" {")
                entry.context.entries.forEachIndexed { index, (key, value) ->
                    if (index > 0) append(", ")
                    append("$key=$value")
                }
                append("}")
            }
        }
    }
    
    /**
     * 获取最近的日志条目
     * 
     * @param count 数量
     * @return 日志条目列表
     */
    fun getRecentLogs(count: Int = 100): List<LogEntry> {
        return logQueue.toList().takeLast(count)
    }
    
    /**
     * 获取特定级别的日志
     * 
     * @param level 日志级别
     * @param count 数量
     * @return 日志条目列表
     */
    fun getLogsByLevel(level: LogLevel, count: Int = 100): List<LogEntry> {
        return logQueue.filter { it.level == level }.takeLast(count)
    }
    
    /**
     * 获取特定类别的日志
     * 
     * @param category 类别
     * @param count 数量
     * @return 日志条目列表
     */
    fun getLogsByCategory(category: String, count: Int = 100): List<LogEntry> {
        return logQueue.filter { it.category == category }.takeLast(count)
    }
    
    /**
     * 清空日志
     */
    fun clear() {
        logQueue.clear()
    }
    
    /**
     * 生成日志摘要
     * 
     * @return 日志摘要字符串
     */
    fun generateLogSummary(): String {
        val logs = logQueue.toList()
        val errorCount = logs.count { it.level == LogLevel.ERROR }
        val warnCount = logs.count { it.level == LogLevel.WARN }
        val infoCount = logs.count { it.level == LogLevel.INFO }
        val debugCount = logs.count { it.level == LogLevel.DEBUG }
        
        return buildString {
            appendLine("Log Summary:")
            appendLine("  Total Entries: ${logs.size}")
            appendLine("  Errors: $errorCount")
            appendLine("  Warnings: $warnCount")
            appendLine("  Info: $infoCount")
            appendLine("  Debug: $debugCount")
            
            if (errorCount > 0) {
                appendLine()
                appendLine("Recent Errors:")
                getLogsByLevel(LogLevel.ERROR, 5).forEach { entry ->
                    appendLine("  [${dateFormat.format(Date(entry.timestamp))}] ${entry.message}")
                }
            }
            
            if (warnCount > 0) {
                appendLine()
                appendLine("Recent Warnings:")
                getLogsByLevel(LogLevel.WARN, 5).forEach { entry ->
                    appendLine("  [${dateFormat.format(Date(entry.timestamp))}] ${entry.message}")
                }
            }
        }
    }
}
