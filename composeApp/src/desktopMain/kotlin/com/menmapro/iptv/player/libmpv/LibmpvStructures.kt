package com.menmapro.iptv.player.libmpv

import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * Data structures for libmpv parameters and configuration
 * 
 * This file contains JNA structures and data classes for configuring
 * and interacting with libmpv.
 * 
 * Requirements:
 * - 1.4: Create data structures for libmpv parameters
 */

/**
 * Configuration for libmpv player
 * 
 * This data class encapsulates all configuration options for the libmpv player.
 * Options can be set before initialization to customize playback behavior.
 * 
 * Requirements:
 * - 1.3: Support hardware acceleration through libmpv
 * - 7.1: Support configuring hardware acceleration options
 * - 7.2: Support configuring network buffering parameters
 * - 7.3: Support configuring audio output options
 * - 7.4: Support configuring video output options
 * - 7.5: Use safe default values when configuration is invalid
 * 
 * @param hardwareAcceleration Enable hardware acceleration
 * @param hwdecMethod Hardware decoding method (auto, videotoolbox, vaapi, vdpau, d3d11va, no)
 * @param videoOutput Video output driver (gpu, x11, wayland, etc.)
 * @param audioOutput Audio output driver (auto, coreaudio, pulse, alsa, wasapi, etc.)
 * @param cacheSize Cache size in kilobytes
 * @param cacheSecs Cache duration in seconds
 * @param demuxerReadahead Demuxer readahead in seconds
 * @param networkTimeout Network timeout in seconds
 * @param userAgent User agent string for HTTP requests
 * @param keepOpen Keep player open after playback ends
 * @param volume Initial volume (0-100)
 * @param logLevel Log level (no, fatal, error, warn, info, v, debug, trace)
 */
data class LibmpvConfiguration(
    val hardwareAcceleration: Boolean = true,
    val hwdecMethod: String = "auto",
    val videoOutput: String = "gpu",
    val audioOutput: String = "auto",
    val cacheSize: Int = 150000,
    val cacheSecs: Int = 10,
    val demuxerReadahead: Int = 5,
    val networkTimeout: Int = 30,
    val userAgent: String = "IPTV-Player/1.0",
    val keepOpen: Boolean = false,
    val volume: Int = 100,
    val logLevel: String = "info"
) {
    companion object {
        /**
         * Default configuration with safe values
         * 
         * This configuration provides sensible defaults that work across
         * different platforms and scenarios.
         */
        val DEFAULT = LibmpvConfiguration()
        
        /**
         * Configuration optimized for live streaming
         * 
         * Uses smaller cache and readahead for lower latency.
         */
        val LIVE_STREAMING = LibmpvConfiguration(
            cacheSize = 50000,
            cacheSecs = 5,
            demuxerReadahead = 2,
            networkTimeout = 15
        )
        
        /**
         * Configuration optimized for low latency live streams
         */
        val LOW_LATENCY = LibmpvConfiguration(
            cacheSize = 50000,
            cacheSecs = 2,
            demuxerReadahead = 1
        )
        
        /**
         * Configuration optimized for VOD (Video on Demand)
         * 
         * Uses larger cache for smoother playback.
         */
        val VOD = LibmpvConfiguration(
            cacheSize = 300000,
            cacheSecs = 20,
            demuxerReadahead = 10,
            networkTimeout = 60
        )
        
        /**
         * Configuration optimized for high quality playback
         */
        val HIGH_QUALITY = LibmpvConfiguration(
            cacheSize = 300000,
            cacheSecs = 20,
            demuxerReadahead = 10
        )
        
        /**
         * Configuration with hardware acceleration disabled
         * 
         * Useful for debugging or when hardware acceleration causes issues.
         */
        val SOFTWARE_ONLY = LibmpvConfiguration(
            hardwareAcceleration = false,
            hwdecMethod = "no"
        )
        
        /**
         * Configuration for streams with corrupted audio
         * 
         * Disables audio to allow video playback when audio is severely corrupted.
         */
        val VIDEO_ONLY = LibmpvConfiguration(
            audioOutput = "null",  // 禁用音频输出
            volume = 0
        )
        
        /**
         * Configuration for macOS with VideoToolbox
         */
        val MACOS = LibmpvConfiguration(
            hardwareAcceleration = true,
            hwdecMethod = "videotoolbox",
            videoOutput = "gpu",
            audioOutput = "coreaudio"
        )
        
        /**
         * Configuration for Linux with VAAPI
         */
        val LINUX_VAAPI = LibmpvConfiguration(
            hardwareAcceleration = true,
            hwdecMethod = "vaapi",
            videoOutput = "gpu",
            audioOutput = "pulse"
        )
        
        /**
         * Configuration for Linux with VDPAU
         */
        val LINUX_VDPAU = LibmpvConfiguration(
            hardwareAcceleration = true,
            hwdecMethod = "vdpau",
            videoOutput = "gpu",
            audioOutput = "pulse"
        )
        
        /**
         * Configuration for Windows with D3D11VA
         */
        val WINDOWS = LibmpvConfiguration(
            hardwareAcceleration = true,
            hwdecMethod = "d3d11va",
            videoOutput = "gpu",
            audioOutput = "wasapi"
        )
    }
    
    /**
     * Validate configuration values
     * 
     * Returns a new configuration with validated values, replacing
     * invalid values with safe defaults.
     * 
     * Requirement 7.5: Use safe default values when configuration is invalid
     * 
     * @return Validated configuration
     */
    fun validate(): LibmpvConfiguration {
        return copy(
            cacheSize = cacheSize.coerceIn(1000, 1000000),
            cacheSecs = cacheSecs.coerceIn(1, 300),
            demuxerReadahead = demuxerReadahead.coerceIn(1, 60),
            networkTimeout = networkTimeout.coerceIn(5, 300),
            volume = volume.coerceIn(0, 100),
            hwdecMethod = if (hardwareAcceleration) {
                when (hwdecMethod) {
                    "auto", "videotoolbox", "vaapi", "vdpau", "d3d11va", "no" -> hwdecMethod
                    else -> "auto"
                }
            } else {
                "no"
            },
            logLevel = when (logLevel) {
                "no", "fatal", "error", "warn", "info", "v", "debug", "trace" -> logLevel
                else -> "info"
            }
        )
    }
    
    /**
     * Create a configuration for the current platform
     * 
     * Automatically selects appropriate hardware acceleration and
     * output drivers based on the operating system.
     * 
     * @return Platform-specific configuration
     */
    fun forCurrentPlatform(): LibmpvConfiguration {
        val osName = System.getProperty("os.name").lowercase()
        
        return when {
            osName.contains("mac") -> copy(
                hwdecMethod = if (hardwareAcceleration) "videotoolbox" else "no",
                audioOutput = "coreaudio"
            )
            osName.contains("windows") -> copy(
                hwdecMethod = if (hardwareAcceleration) "d3d11va" else "no",
                audioOutput = "wasapi"
            )
            osName.contains("linux") -> copy(
                hwdecMethod = if (hardwareAcceleration) "vaapi" else "no",
                audioOutput = "pulse"
            )
            else -> this
        }
    }
}

/**
 * JNA structure for mpv_render_param
 * 
 * This structure is used to pass parameters to render context functions.
 */
@Structure.FieldOrder("type", "data")
open class MpvRenderParam(type: Int = 0, data: Pointer? = null) : Structure() {
    @JvmField var type: Int = type
    @JvmField var data: Pointer? = data
    
    init {
        write()
    }
    
    companion object {
        // Render parameter types
        const val MPV_RENDER_PARAM_INVALID = 0
        const val MPV_RENDER_PARAM_API_TYPE = 1
        const val MPV_RENDER_PARAM_OPENGL_INIT_PARAMS = 2
        const val MPV_RENDER_PARAM_OPENGL_FBO = 3
        const val MPV_RENDER_PARAM_FLIP_Y = 4
        const val MPV_RENDER_PARAM_DEPTH = 5
        const val MPV_RENDER_PARAM_ICC_PROFILE = 6
        const val MPV_RENDER_PARAM_AMBIENT_LIGHT = 7
        const val MPV_RENDER_PARAM_X11_DISPLAY = 8
        const val MPV_RENDER_PARAM_WL_DISPLAY = 9
        const val MPV_RENDER_PARAM_ADVANCED_CONTROL = 10
        const val MPV_RENDER_PARAM_NEXT_FRAME_INFO = 11
        const val MPV_RENDER_PARAM_BLOCK_FOR_TARGET_TIME = 12
        const val MPV_RENDER_PARAM_SKIP_RENDERING = 13
        const val MPV_RENDER_PARAM_DRM_DISPLAY = 14
        const val MPV_RENDER_PARAM_DRM_DRAW_SURFACE_SIZE = 15
        const val MPV_RENDER_PARAM_DRM_DISPLAY_V2 = 16
        const val MPV_RENDER_PARAM_SW_SIZE = 17
        const val MPV_RENDER_PARAM_SW_FORMAT = 18
        const val MPV_RENDER_PARAM_SW_STRIDE = 19
        const val MPV_RENDER_PARAM_SW_POINTER = 20
    }
}

/**
 * JNA structure for mpv_opengl_fbo
 * 
 * Used for OpenGL rendering.
 */
@Structure.FieldOrder("fbo", "w", "h", "internal_format")
open class MpvOpenglFbo(
    fbo: Int = 0,
    w: Int = 0,
    h: Int = 0,
    internal_format: Int = 0
) : Structure() {
    @JvmField var fbo: Int = fbo
    @JvmField var w: Int = w
    @JvmField var h: Int = h
    @JvmField var internal_format: Int = internal_format
    
    init {
        write()
    }
}

/**
 * Error information from libmpv
 * 
 * Provides structured error information from libmpv operations.
 * 
 * Requirements:
 * - 4.1: Report network errors to user interface
 * - 4.2: Report unsupported codec errors to user interface
 * - 4.3: Report file open errors to user interface
 * - 4.5: Log detailed error information for debugging
 * 
 * @param code Error code
 * @param message Error message
 */
data class LibmpvError(
    val code: Int,
    val message: String
) {
    /**
     * Check if this represents a success (no error)
     */
    fun isSuccess(): Boolean = code == LibmpvBindings.MPV_ERROR_SUCCESS
    
    /**
     * Check if this represents an error
     */
    fun isError(): Boolean = !isSuccess()
    
    /**
     * Check if this is a network-related error
     */
    fun isNetworkError(): Boolean = code == LibmpvBindings.MPV_ERROR_LOADING_FAILED
    
    /**
     * Check if this is a codec/format error
     */
    fun isFormatError(): Boolean = 
        code == LibmpvBindings.MPV_ERROR_UNKNOWN_FORMAT ||
        code == LibmpvBindings.MPV_ERROR_UNSUPPORTED
    
    /**
     * Check if this error is potentially recoverable
     */
    fun isRecoverable(): Boolean = when (code) {
        LibmpvBindings.MPV_ERROR_LOADING_FAILED,
        LibmpvBindings.MPV_ERROR_PROPERTY_UNAVAILABLE,
        LibmpvBindings.MPV_ERROR_AO_INIT_FAILED,
        LibmpvBindings.MPV_ERROR_VO_INIT_FAILED -> true
        else -> false
    }
    
    /**
     * Get user-friendly error message
     */
    fun getUserMessage(): String = when {
        isNetworkError() -> 
            "Network error: Unable to load the stream. Please check your internet connection."
        isFormatError() -> 
            "Format error: This video format or codec is not supported."
        code == LibmpvBindings.MPV_ERROR_AO_INIT_FAILED -> 
            "Audio output initialization failed. Please check your audio settings."
        code == LibmpvBindings.MPV_ERROR_VO_INIT_FAILED -> 
            "Video output initialization failed. Please check your display settings."
        code == LibmpvBindings.MPV_ERROR_NOTHING_TO_PLAY -> 
            "No media to play."
        else -> message
    }
    
    companion object {
        /**
         * Success (no error)
         */
        val SUCCESS = LibmpvError(LibmpvBindings.MPV_ERROR_SUCCESS, "Success")
        
        /**
         * Create error from error code
         * 
         * @param code Error code
         * @param bindings LibmpvBindings instance to get error string
         * @param context Additional context (property name, command, etc.)
         * @return LibmpvError instance
         */
        fun fromCode(code: Int, bindings: LibmpvBindings?, context: String? = null): LibmpvError {
            if (code == LibmpvBindings.MPV_ERROR_SUCCESS) {
                return SUCCESS
            }
            val errorMessage = try {
                bindings?.mpv_error_string(code) ?: "Unknown error: $code"
            } catch (e: Exception) {
                "Unknown error: $code"
            }
            val fullMessage = if (context != null) {
                "$errorMessage (context: $context)"
            } else {
                errorMessage
            }
            return LibmpvError(code, fullMessage)
        }
    }
}

/**
 * Sealed class hierarchy for libmpv exceptions
 * 
 * Provides structured exception handling for libmpv operations.
 * Each exception type corresponds to a specific category of errors.
 * 
 * Requirements:
 * - 4.1: Report network errors to user interface
 * - 4.2: Report unsupported codec errors to user interface
 * - 4.3: Report file open errors to user interface
 * - 4.4: Update player state when playback ends normally
 * - 4.5: Log detailed error information for debugging
 */
sealed class LibmpvException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * Library not found or failed to load
     * 
     * Requirements: 1.2 - Provide clear error messages indicating installation requirements
     */
    class LibraryNotFoundError(val reason: String) : 
        LibmpvException("libmpv library not found: $reason") {
        
        /**
         * Get installation instructions for the current platform
         */
        fun getInstallationInstructions(): String {
            val os = System.getProperty("os.name").lowercase()
            return when {
                os.contains("mac") -> """
                    libmpv is not installed. Please install it using Homebrew:
                    
                    brew install mpv
                    
                    After installation, restart the application.
                """.trimIndent()
                
                os.contains("linux") -> """
                    libmpv is not installed. Please install it using your package manager:
                    
                    Ubuntu/Debian: sudo apt install libmpv-dev
                    Fedora: sudo dnf install mpv-libs-devel
                    Arch: sudo pacman -S mpv
                    
                    After installation, restart the application.
                """.trimIndent()
                
                os.contains("windows") -> """
                    libmpv is not installed. Please download and install MPV:
                    
                    1. Download MPV from https://mpv.io/installation/
                    2. Extract the archive
                    3. Add the directory containing mpv-2.dll to your PATH
                    4. Restart the application
                """.trimIndent()
                
                else -> """
                    libmpv is not installed. Please install MPV for your operating system.
                    Visit https://mpv.io/installation/ for installation instructions.
                """.trimIndent()
            }
        }
    }
    
    /**
     * Error during initialization
     * 
     * Requirements: 1.1 - Use libmpv library for video playback
     */
    class InitializationError(val reason: String, val underlyingCause: Throwable? = null) : 
        LibmpvException("Failed to initialize libmpv: $reason", underlyingCause)
    
    /**
     * Error during playback
     * 
     * Requirements:
     * - 4.1: Report network errors to user interface
     * - 4.2: Report unsupported codec errors to user interface
     * - 4.3: Report file open errors to user interface
     */
    class PlaybackError(val code: Int, val errorMessage: String, val url: String? = null) : 
        LibmpvException(buildPlaybackErrorMessage(code, errorMessage, url)) {
        
        /**
         * Check if this is a network error
         */
        fun isNetworkError(): Boolean = code == LibmpvBindings.MPV_ERROR_LOADING_FAILED
        
        /**
         * Check if this is a format/codec error
         */
        fun isFormatError(): Boolean = 
            code == LibmpvBindings.MPV_ERROR_UNKNOWN_FORMAT ||
            code == LibmpvBindings.MPV_ERROR_UNSUPPORTED
        
        /**
         * Check if this error is recoverable
         */
        fun isRecoverable(): Boolean = when (code) {
            LibmpvBindings.MPV_ERROR_LOADING_FAILED,
            LibmpvBindings.MPV_ERROR_PROPERTY_UNAVAILABLE,
            LibmpvBindings.MPV_ERROR_AO_INIT_FAILED,
            LibmpvBindings.MPV_ERROR_VO_INIT_FAILED -> true
            else -> false
        }
        
        /**
         * Get user-friendly error message
         */
        fun getUserMessage(): String = when {
            isNetworkError() -> 
                "Network error: Unable to load the stream. Please check your internet connection and try again."
            isFormatError() -> 
                "Format error: This video format or codec is not supported."
            code == LibmpvBindings.MPV_ERROR_LOADING_FAILED -> 
                "Failed to load media: The file or stream could not be opened."
            else -> 
                "Playback error: $errorMessage"
        }
        
        companion object {
            private fun buildPlaybackErrorMessage(code: Int, message: String, url: String?): String {
                return if (url != null) {
                    "Playback error (code: $code): $message [URL: $url]"
                } else {
                    "Playback error (code: $code): $message"
                }
            }
        }
    }
    
    /**
     * Error setting configuration option
     * 
     * Requirements: 7.5 - Use safe default values when configuration is invalid
     */
    class ConfigurationError(val option: String, val reason: String) : 
        LibmpvException("Configuration error for '$option': $reason")
    
    /**
     * Error with property access
     */
    class PropertyError(val property: String, val reason: String) : 
        LibmpvException("Property error for '$property': $reason")
    
    /**
     * Error executing command
     */
    class CommandError(val command: String, val reason: String) : 
        LibmpvException("Command error for '$command': $reason")
    
    /**
     * Error with resource (memory, GPU, etc.)
     */
    class ResourceError(val resourceType: String, val reason: String) : 
        LibmpvException("Resource error ($resourceType): $reason")
    
    /**
     * Render context error
     * 
     * Requirements: 6.1 - Render frames to Compose UI
     */
    class RenderContextError(val reason: String) : 
        LibmpvException("Render context error: $reason")
}

/**
 * Video format information
 * 
 * @param width Video width in pixels
 * @param height Video height in pixels
 * @param pixelFormat Pixel format string
 * @param fps Frames per second
 */
data class VideoFormat(
    val width: Int,
    val height: Int,
    val pixelFormat: String,
    val fps: Double
) {
    /**
     * Get aspect ratio
     */
    fun getAspectRatio(): Double {
        return if (height > 0) width.toDouble() / height.toDouble() else 16.0 / 9.0
    }
}

/**
 * Audio format information
 * 
 * @param sampleRate Sample rate in Hz
 * @param channels Number of audio channels
 * @param format Audio format string
 */
data class AudioFormat(
    val sampleRate: Int,
    val channels: Int,
    val format: String
)

/**
 * Media information
 * 
 * @param duration Duration in seconds
 * @param videoFormat Video format information
 * @param audioFormat Audio format information
 * @param metadata Media metadata
 */
data class MediaInfo(
    val duration: Double,
    val videoFormat: VideoFormat?,
    val audioFormat: AudioFormat?,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if media is seekable
     */
    fun isSeekable(): Boolean = duration > 0
    
    /**
     * Check if media has video
     */
    fun hasVideo(): Boolean = videoFormat != null
    
    /**
     * Check if media has audio
     */
    fun hasAudio(): Boolean = audioFormat != null
}
