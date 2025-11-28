package com.menmapro.iptv.player.libmpv

import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Error handler and recovery strategies for libmpv
 * 
 * This class provides error handling, logging, and recovery strategies
 * for various error conditions that can occur during playback.
 * 
 * Requirements:
 * - 4.1: Report network errors to user interface
 * - 4.2: Report unsupported codec errors to user interface
 * - 4.3: Report file open errors to user interface
 * - 4.5: Log detailed error information for debugging
 */
class LibmpvErrorHandler(
    private val logger: LibmpvLogger = LibmpvLogger.default()
) {
    
    /**
     * Handle an error and determine recovery strategy
     * 
     * @param error The error that occurred
     * @param context Additional context about where the error occurred
     * @return Recovery action to take
     */
    fun handleError(error: LibmpvException, context: String = ""): RecoveryAction {
        // Log the error
        logError(error, context)
        
        // Determine recovery strategy
        return when (error) {
            is LibmpvException.LibraryNotFoundError -> {
                RecoveryAction.Fatal(error.getInstallationInstructions())
            }
            
            is LibmpvException.InitializationError -> {
                RecoveryAction.Fatal("Failed to initialize player: ${error.reason}")
            }
            
            is LibmpvException.PlaybackError -> {
                handlePlaybackError(error)
            }
            
            is LibmpvException.ConfigurationError -> {
                RecoveryAction.UseDefault("Using default configuration for ${error.option}")
            }
            
            is LibmpvException.PropertyError -> {
                // Property errors are usually non-fatal
                RecoveryAction.Ignore("Property error: ${error.reason}")
            }
            
            is LibmpvException.CommandError -> {
                RecoveryAction.Retry(
                    maxAttempts = 1,
                    message = "Command failed: ${error.reason}"
                )
            }
            
            is LibmpvException.ResourceError -> {
                RecoveryAction.Fatal("Resource error: ${error.reason}")
            }
            
            is LibmpvException.RenderContextError -> {
                RecoveryAction.Retry(
                    maxAttempts = 2,
                    message = "Render context error: ${error.reason}"
                )
            }
        }
    }
    
    /**
     * Handle playback errors specifically
     */
    private fun handlePlaybackError(error: LibmpvException.PlaybackError): RecoveryAction {
        return when {
            error.isNetworkError() -> {
                // Network errors are retryable
                RecoveryAction.Retry(
                    maxAttempts = 3,
                    message = error.getUserMessage(),
                    delayMs = 1000
                )
            }
            
            error.isFormatError() -> {
                // Format errors are not recoverable
                RecoveryAction.Fatal(error.getUserMessage())
            }
            
            error.code == LibmpvBindings.MPV_ERROR_LOADING_FAILED -> {
                // Loading failures might be retryable
                RecoveryAction.Retry(
                    maxAttempts = 2,
                    message = error.getUserMessage(),
                    delayMs = 500
                )
            }
            
            else -> {
                // Other playback errors
                RecoveryAction.Fatal(error.getUserMessage())
            }
        }
    }
    
    /**
     * Log an error with full details
     * 
     * Requirements: 4.5 - Log detailed error information for debugging
     */
    private fun logError(error: LibmpvException, context: String) {
        val errorType = error::class.simpleName ?: "Unknown"
        val contextStr = if (context.isNotEmpty()) " [Context: $context]" else ""
        
        logger.error("$errorType: ${error.message}$contextStr")
        
        // Log additional details based on error type
        when (error) {
            is LibmpvException.PlaybackError -> {
                logger.error("  Error code: ${error.code}")
                error.url?.let { logger.error("  URL: $it") }
                logger.error("  Network error: ${error.isNetworkError()}")
                logger.error("  Format error: ${error.isFormatError()}")
                logger.error("  Recoverable: ${error.isRecoverable()}")
            }
            
            is LibmpvException.ConfigurationError -> {
                logger.error("  Option: ${error.option}")
            }
            
            is LibmpvException.PropertyError -> {
                logger.error("  Property: ${error.property}")
            }
            
            is LibmpvException.CommandError -> {
                logger.error("  Command: ${error.command}")
            }
            
            is LibmpvException.ResourceError -> {
                logger.error("  Resource type: ${error.resourceType}")
            }
            
            else -> {
                // No additional details
            }
        }
        
        // Log stack trace if available
        error.cause?.let { cause ->
            logger.error("  Caused by: ${cause.message}")
            logger.debug("  Stack trace: ${cause.stackTraceToString()}")
        }
    }
    
    /**
     * Execute an operation with retry logic
     * 
     * @param maxAttempts Maximum number of attempts
     * @param delayMs Delay between attempts in milliseconds
     * @param operation Operation to execute
     * @return Result of the operation
     */
    suspend fun <T> withRetry(
        maxAttempts: Int = 3,
        delayMs: Long = 1000,
        operation: suspend (attempt: Int) -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                logger.debug("Attempt ${attempt + 1}/$maxAttempts")
                return operation(attempt + 1)
            } catch (e: Exception) {
                lastException = e
                logger.warn("Attempt ${attempt + 1} failed: ${e.message}")
                
                if (attempt < maxAttempts - 1) {
                    val backoffDelay = calculateBackoffDelay(attempt, delayMs)
                    logger.debug("Waiting ${backoffDelay}ms before retry")
                    delay(backoffDelay)
                }
            }
        }
        
        throw lastException ?: Exception("Operation failed after $maxAttempts attempts")
    }
    
    /**
     * Calculate exponential backoff delay
     */
    private fun calculateBackoffDelay(attempt: Int, baseDelay: Long): Long {
        val exponentialDelay = baseDelay * 2.0.pow(attempt).toLong()
        val maxDelay = 30000L // 30 seconds max
        return min(exponentialDelay, maxDelay)
    }
}

/**
 * Recovery action to take after an error
 */
sealed class RecoveryAction {
    /**
     * Fatal error - cannot recover
     */
    data class Fatal(val message: String) : RecoveryAction()
    
    /**
     * Retry the operation
     */
    data class Retry(
        val maxAttempts: Int,
        val message: String,
        val delayMs: Long = 1000
    ) : RecoveryAction()
    
    /**
     * Use default/fallback value
     */
    data class UseDefault(val message: String) : RecoveryAction()
    
    /**
     * Ignore the error and continue
     */
    data class Ignore(val message: String) : RecoveryAction()
}

/**
 * Logger interface for libmpv
 * 
 * Provides structured logging with different log levels.
 * 
 * Requirements: 4.5 - Log detailed error information for debugging
 */
interface LibmpvLogger {
    fun error(message: String)
    fun warn(message: String)
    fun info(message: String)
    fun debug(message: String)
    fun trace(message: String)
    
    companion object {
        /**
         * Create a default logger that prints to console
         */
        fun default(): LibmpvLogger = ConsoleLogger()
        
        /**
         * Create a no-op logger that discards all messages
         */
        fun noop(): LibmpvLogger = NoopLogger()
    }
}

/**
 * Console logger implementation
 */
private class ConsoleLogger : LibmpvLogger {
    override fun error(message: String) {
        println("[ERROR] [libmpv] $message")
    }
    
    override fun warn(message: String) {
        println("[WARN] [libmpv] $message")
    }
    
    override fun info(message: String) {
        println("[INFO] [libmpv] $message")
    }
    
    override fun debug(message: String) {
        println("[DEBUG] [libmpv] $message")
    }
    
    override fun trace(message: String) {
        println("[TRACE] [libmpv] $message")
    }
}

/**
 * No-op logger implementation
 */
private class NoopLogger : LibmpvLogger {
    override fun error(message: String) {}
    override fun warn(message: String) {}
    override fun info(message: String) {}
    override fun debug(message: String) {}
    override fun trace(message: String) {}
}
