package com.menmapro.iptv.player.ffmpeg

/**
 * 错误处理器
 * 
 * 负责分类和处理播放过程中的各种错误，包括：
 * - 解码错误
 * - 网络错误
 * - 渲染错误
 * - 音频错误
 * 
 * 记录详细的错误信息，包括错误类型、时间戳和上下文。
 * 
 * Requirements: 7.3 - 错误日志记录
 */
class ErrorHandler(
    private val logger: PlaybackLogger,
    private val onError: (String) -> Unit
) {
    
    /**
     * 错误类型
     */
    enum class ErrorType {
        DECODING,
        NETWORK,
        RENDERING,
        AUDIO,
        INITIALIZATION,
        RESOURCE,
        SYNC,
        UNKNOWN
    }
    
    /**
     * 错误严重程度
     */
    enum class ErrorSeverity {
        FATAL,      // 致命错误，必须停止播放
        RECOVERABLE, // 可恢复错误，可以继续播放
        WARNING     // 警告，不影响播放
    }
    
    /**
     * 错误信息
     */
    data class ErrorInfo(
        val type: ErrorType,
        val severity: ErrorSeverity,
        val message: String,
        val exception: Exception?,
        val timestamp: Long,
        val context: Map<String, Any>
    )
    
    /**
     * 错误历史（最多保留最近 100 个错误）
     */
    private val errorHistory = mutableListOf<ErrorInfo>()
    private val maxErrorHistory = 100
    
    /**
     * 处理解码错误
     * 
     * @param exception 异常
     * @param frameTimestamp 帧时间戳（微秒）
     * @param context 上下文信息
     * @return 错误严重程度
     * 
     * Requirements: 7.3
     */
    fun handleDecodingError(
        exception: Exception,
        frameTimestamp: Long = 0,
        context: Map<String, Any> = emptyMap()
    ): ErrorSeverity {
        val errorMessage = exception.message ?: "Unknown decoding error"
        val severity = categorizeDecodingError(exception)
        
        val fullContext = context.toMutableMap()
        fullContext["exception_type"] = exception.javaClass.simpleName
        if (frameTimestamp > 0) {
            fullContext["frame_timestamp_us"] = frameTimestamp
            fullContext["frame_timestamp_ms"] = frameTimestamp / 1000
        }
        
        // 记录错误
        val errorInfo = ErrorInfo(
            type = ErrorType.DECODING,
            severity = severity,
            message = errorMessage,
            exception = exception,
            timestamp = System.currentTimeMillis(),
            context = fullContext
        )
        recordError(errorInfo)
        
        // 记录到日志
        logger.logDecodingError(
            error = errorMessage,
            timestamp = frameTimestamp,
            context = fullContext
        )
        
        // 根据严重程度决定是否通知用户
        when (severity) {
            ErrorSeverity.FATAL -> {
                onError("解码失败: $errorMessage")
            }
            ErrorSeverity.RECOVERABLE -> {
                // 可恢复错误，只记录日志
                println("[ErrorHandler] Recoverable decoding error: $errorMessage")
            }
            ErrorSeverity.WARNING -> {
                // 警告级别，只记录日志
                println("[ErrorHandler] Decoding warning: $errorMessage")
            }
        }
        
        return severity
    }
    
    /**
     * 处理网络错误
     * 
     * @param exception 异常
     * @param url 媒体 URL
     * @param context 上下文信息
     * @return 错误严重程度
     * 
     * Requirements: 7.3
     */
    fun handleNetworkError(
        exception: Exception,
        url: String,
        context: Map<String, Any> = emptyMap()
    ): ErrorSeverity {
        val errorMessage = exception.message ?: "Unknown network error"
        val severity = categorizeNetworkError(exception)
        
        val fullContext = context.toMutableMap()
        fullContext["exception_type"] = exception.javaClass.simpleName
        fullContext["url"] = url
        
        // 记录错误
        val errorInfo = ErrorInfo(
            type = ErrorType.NETWORK,
            severity = severity,
            message = errorMessage,
            exception = exception,
            timestamp = System.currentTimeMillis(),
            context = fullContext
        )
        recordError(errorInfo)
        
        // 记录到日志
        logger.logNetworkError(
            error = errorMessage,
            url = url,
            context = fullContext
        )
        
        // 根据严重程度决定是否通知用户
        when (severity) {
            ErrorSeverity.FATAL -> {
                onError("网络错误: $errorMessage")
            }
            ErrorSeverity.RECOVERABLE -> {
                // 可恢复错误（如临时网络中断），尝试重连
                println("[ErrorHandler] Recoverable network error: $errorMessage")
            }
            ErrorSeverity.WARNING -> {
                println("[ErrorHandler] Network warning: $errorMessage")
            }
        }
        
        return severity
    }
    
    /**
     * 处理渲染错误
     * 
     * @param exception 异常
     * @param context 上下文信息
     * @return 错误严重程度
     * 
     * Requirements: 7.3
     */
    fun handleRenderingError(
        exception: Exception,
        context: Map<String, Any> = emptyMap()
    ): ErrorSeverity {
        val errorMessage = exception.message ?: "Unknown rendering error"
        val severity = categorizeRenderingError(exception)
        
        val fullContext = context.toMutableMap()
        fullContext["exception_type"] = exception.javaClass.simpleName
        
        // 记录错误
        val errorInfo = ErrorInfo(
            type = ErrorType.RENDERING,
            severity = severity,
            message = errorMessage,
            exception = exception,
            timestamp = System.currentTimeMillis(),
            context = fullContext
        )
        recordError(errorInfo)
        
        // 记录到日志
        logger.logRenderingError(
            error = errorMessage,
            context = fullContext
        )
        
        // 根据严重程度决定是否通知用户
        when (severity) {
            ErrorSeverity.FATAL -> {
                onError("渲染失败: $errorMessage")
            }
            ErrorSeverity.RECOVERABLE -> {
                println("[ErrorHandler] Recoverable rendering error: $errorMessage")
            }
            ErrorSeverity.WARNING -> {
                println("[ErrorHandler] Rendering warning: $errorMessage")
            }
        }
        
        return severity
    }
    
    /**
     * 处理音频错误
     * 
     * @param exception 异常
     * @param context 上下文信息
     * @return 错误严重程度
     * 
     * Requirements: 7.3
     */
    fun handleAudioError(
        exception: Exception,
        context: Map<String, Any> = emptyMap()
    ): ErrorSeverity {
        val errorMessage = exception.message ?: "Unknown audio error"
        val severity = categorizeAudioError(exception)
        
        val fullContext = context.toMutableMap()
        fullContext["exception_type"] = exception.javaClass.simpleName
        
        // 记录错误
        val errorInfo = ErrorInfo(
            type = ErrorType.AUDIO,
            severity = severity,
            message = errorMessage,
            exception = exception,
            timestamp = System.currentTimeMillis(),
            context = fullContext
        )
        recordError(errorInfo)
        
        // 记录到日志
        logger.logAudioError(
            error = errorMessage,
            context = fullContext
        )
        
        // 根据严重程度决定是否通知用户
        when (severity) {
            ErrorSeverity.FATAL -> {
                onError("音频播放失败: $errorMessage")
            }
            ErrorSeverity.RECOVERABLE -> {
                println("[ErrorHandler] Recoverable audio error: $errorMessage")
            }
            ErrorSeverity.WARNING -> {
                println("[ErrorHandler] Audio warning: $errorMessage")
            }
        }
        
        return severity
    }
    
    /**
     * 处理初始化错误
     * 
     * @param exception 异常
     * @param context 上下文信息
     * 
     * Requirements: 7.3
     */
    fun handleInitializationError(
        exception: Exception,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorMessage = exception.message ?: "Unknown initialization error"
        
        val fullContext = context.toMutableMap()
        fullContext["exception_type"] = exception.javaClass.simpleName
        
        // 记录错误
        val errorInfo = ErrorInfo(
            type = ErrorType.INITIALIZATION,
            severity = ErrorSeverity.FATAL,
            message = errorMessage,
            exception = exception,
            timestamp = System.currentTimeMillis(),
            context = fullContext
        )
        recordError(errorInfo)
        
        // 记录到日志
        logger.logDecodingError(
            error = "Initialization failed: $errorMessage",
            context = fullContext
        )
        
        // 通知用户
        onError("初始化失败: $errorMessage")
    }
    
    /**
     * 处理资源错误
     * 
     * @param exception 异常
     * @param context 上下文信息
     * 
     * Requirements: 7.3
     */
    fun handleResourceError(
        exception: Exception,
        context: Map<String, Any> = emptyMap()
    ) {
        val errorMessage = exception.message ?: "Unknown resource error"
        
        val fullContext = context.toMutableMap()
        fullContext["exception_type"] = exception.javaClass.simpleName
        
        // 记录错误
        val errorInfo = ErrorInfo(
            type = ErrorType.RESOURCE,
            severity = ErrorSeverity.FATAL,
            message = errorMessage,
            exception = exception,
            timestamp = System.currentTimeMillis(),
            context = fullContext
        )
        recordError(errorInfo)
        
        // 记录到日志
        logger.logDecodingError(
            error = "Resource error: $errorMessage",
            context = fullContext
        )
        
        // 通知用户
        onError("资源错误: $errorMessage")
    }
    
    /**
     * 分类解码错误
     * 
     * @param exception 异常
     * @return 错误严重程度
     */
    private fun categorizeDecodingError(exception: Exception): ErrorSeverity {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            // 致命错误
            message.contains("unsupported") ||
            message.contains("invalid format") ||
            message.contains("codec not found") -> ErrorSeverity.FATAL
            
            // 可恢复错误
            message.contains("corrupted") ||
            message.contains("invalid data") ||
            message.contains("decode error") -> ErrorSeverity.RECOVERABLE
            
            // 默认为可恢复
            else -> ErrorSeverity.RECOVERABLE
        }
    }
    
    /**
     * 分类网络错误
     * 
     * @param exception 异常
     * @return 错误严重程度
     */
    private fun categorizeNetworkError(exception: Exception): ErrorSeverity {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            // 可恢复错误（临时网络问题）
            message.contains("timeout") ||
            message.contains("connection reset") ||
            message.contains("connection refused") ||
            message.contains("broken pipe") -> ErrorSeverity.RECOVERABLE
            
            // 致命错误（永久性问题）
            message.contains("unknown host") ||
            message.contains("invalid url") ||
            message.contains("not found") -> ErrorSeverity.FATAL
            
            // 默认为可恢复
            else -> ErrorSeverity.RECOVERABLE
        }
    }
    
    /**
     * 分类渲染错误
     * 
     * @param exception 异常
     * @return 错误严重程度
     */
    private fun categorizeRenderingError(exception: Exception): ErrorSeverity {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            // 致命错误
            message.contains("canvas") ||
            message.contains("graphics") ||
            message.contains("display") -> ErrorSeverity.FATAL
            
            // 可恢复错误
            message.contains("frame") ||
            message.contains("image") -> ErrorSeverity.RECOVERABLE
            
            // 默认为可恢复
            else -> ErrorSeverity.RECOVERABLE
        }
    }
    
    /**
     * 分类音频错误
     * 
     * @param exception 异常
     * @return 错误严重程度
     */
    private fun categorizeAudioError(exception: Exception): ErrorSeverity {
        val message = exception.message?.lowercase() ?: ""
        
        return when {
            // 致命错误
            message.contains("audio line") ||
            message.contains("audio device") ||
            message.contains("unsupported audio") -> ErrorSeverity.FATAL
            
            // 可恢复错误
            message.contains("buffer") ||
            message.contains("underrun") -> ErrorSeverity.RECOVERABLE
            
            // 默认为可恢复
            else -> ErrorSeverity.RECOVERABLE
        }
    }
    
    /**
     * 记录错误到历史
     * 
     * @param errorInfo 错误信息
     */
    private fun recordError(errorInfo: ErrorInfo) {
        synchronized(errorHistory) {
            errorHistory.add(errorInfo)
            
            // 限制历史大小
            while (errorHistory.size > maxErrorHistory) {
                errorHistory.removeAt(0)
            }
        }
    }
    
    /**
     * 获取错误历史
     * 
     * @param count 数量
     * @return 错误信息列表
     */
    fun getErrorHistory(count: Int = 10): List<ErrorInfo> {
        synchronized(errorHistory) {
            return errorHistory.takeLast(count)
        }
    }
    
    /**
     * 获取特定类型的错误
     * 
     * @param type 错误类型
     * @param count 数量
     * @return 错误信息列表
     */
    fun getErrorsByType(type: ErrorType, count: Int = 10): List<ErrorInfo> {
        synchronized(errorHistory) {
            return errorHistory.filter { it.type == type }.takeLast(count)
        }
    }
    
    /**
     * 获取特定严重程度的错误
     * 
     * @param severity 错误严重程度
     * @param count 数量
     * @return 错误信息列表
     */
    fun getErrorsBySeverity(severity: ErrorSeverity, count: Int = 10): List<ErrorInfo> {
        synchronized(errorHistory) {
            return errorHistory.filter { it.severity == severity }.takeLast(count)
        }
    }
    
    /**
     * 清空错误历史
     */
    fun clearErrorHistory() {
        synchronized(errorHistory) {
            errorHistory.clear()
        }
    }
    
    /**
     * 生成错误报告
     * 
     * @return 格式化的错误报告
     */
    fun generateErrorReport(): String {
        synchronized(errorHistory) {
            val fatalErrors = errorHistory.count { it.severity == ErrorSeverity.FATAL }
            val recoverableErrors = errorHistory.count { it.severity == ErrorSeverity.RECOVERABLE }
            val warnings = errorHistory.count { it.severity == ErrorSeverity.WARNING }
            
            return buildString {
                appendLine("=== Error Report ===")
                appendLine("Total Errors: ${errorHistory.size}")
                appendLine("  Fatal: $fatalErrors")
                appendLine("  Recoverable: $recoverableErrors")
                appendLine("  Warnings: $warnings")
                appendLine()
                
                if (errorHistory.isNotEmpty()) {
                    appendLine("Recent Errors:")
                    getErrorHistory(5).forEach { error ->
                        appendLine("  [${error.type}] [${error.severity}] ${error.message}")
                        if (error.context.isNotEmpty()) {
                            appendLine("    Context: ${error.context}")
                        }
                    }
                }
                
                appendLine("===================")
            }
        }
    }
}
