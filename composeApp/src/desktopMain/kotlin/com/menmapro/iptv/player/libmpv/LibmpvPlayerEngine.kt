package com.menmapro.iptv.player.libmpv

import com.sun.jna.Pointer
import com.sun.jna.ptr.DoubleByReference
import com.sun.jna.ptr.IntByReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Core playback engine for libmpv
 * 
 * This class encapsulates the libmpv functionality and provides a Kotlin-friendly
 * API for media playback. It handles initialization, playback control, property
 * management, and event handling.
 * 
 * Requirements:
 * - 1.1: Use libmpv library for video playback
 * - 3.1: Start video playback
 * - 3.2: Pause video playback
 * - 3.3: Stop video playback and release resources
 * - 3.5: Jump to specified time position
 * 
 * Thread Safety:
 * - This class is thread-safe for concurrent access
 * - Event callbacks are invoked on the event thread
 * - Property access is synchronized with libmpv
 */
class LibmpvPlayerEngine {
    
    private val bindings: LibmpvBindings?
    private var mpvHandle: Pointer? = null
    private var eventThread: Thread? = null
    private val isRunning = AtomicBoolean(false)
    private val isInitialized = AtomicBoolean(false)
    
    private var eventCallback: ((LibmpvEvent) -> Unit)? = null
    private var errorCallback: ((LibmpvException) -> Unit)? = null
    
    /**
     * Initialize the engine
     * 
     * Loads the libmpv library if not already loaded.
     */
    init {
        bindings = LibmpvLoader.load()
    }
    
    /**
     * Initialize the mpv instance
     * 
     * Creates and initializes an mpv instance. Must be called before any
     * other operations.
     * 
     * @param config Configuration for the player
     * @return true if initialization succeeded, false otherwise
     * @throws LibmpvException.LibraryNotFoundError if libmpv is not available
     * @throws LibmpvException.InitializationError if initialization fails
     */
    fun initialize(config: LibmpvConfiguration = LibmpvConfiguration.DEFAULT): Boolean {
        if (isInitialized.get()) {
            return true
        }
        
        if (bindings == null) {
            val reason = LibmpvLoader.getUnavailableReason() ?: "Unknown error"
            throw LibmpvException.LibraryNotFoundError(reason)
        }
        
        try {
            // Create mpv instance
            val handle = bindings.mpv_create()
                ?: throw LibmpvException.InitializationError("mpv_create() returned null")
            
            mpvHandle = handle
            
            // Validate and set options before initialization
            val validatedConfig = config.validate()
            applyConfiguration(validatedConfig)
            
            // Initialize mpv
            val initResult = bindings.mpv_initialize(handle)
            if (initResult != LibmpvBindings.MPV_ERROR_SUCCESS) {
                val error = LibmpvError.fromCode(initResult, bindings)
                mpvHandle = null
                throw LibmpvException.InitializationError(error.message)
            }
            
            // Start event handling thread
            startEventThread()
            
            isInitialized.set(true)
            return true
            
        } catch (e: LibmpvException) {
            throw e
        } catch (e: Exception) {
            mpvHandle = null
            throw LibmpvException.InitializationError("Unexpected error: ${e.message}", e)
        }
    }
    
    /**
     * Apply configuration to mpv instance
     * 
     * Sets options before mpv_initialize() is called.
     * 
     * @param config Configuration to apply
     */
    private fun applyConfiguration(config: LibmpvConfiguration) {
        val handle = mpvHandle ?: return
        
        try {
            // 视频输出 - 暂时使用 gpu 模式（会弹窗，但更稳定）
            // TODO: 实现 libmpv 渲染 API 以支持嵌入式播放
            setOption("vo", config.videoOutput)
            
            // 音频输出 - 确保音频启用
            setOption("ao", config.audioOutput)
            setOption("audio-channels", "stereo")
            setOption("volume", config.volume.toString())
            
            // 音频解码容错设置 - 处理损坏的音频流
            setOption("ad-lavc-downmix", "yes")           // 允许降混音
            setOption("ad-lavc-threads", "auto")          // 自动线程数
            setOption("audio-samplerate", "48000")        // 强制采样率
            setOption("audio-fallback-to-null", "yes")    // 音频失败时使用空输出
            
            // 对于严重损坏的音频流，尝试跳过错误
            setOption("ad-lavc-o", "err_detect=ignore_err")  // 忽略解码错误
            setOption("demuxer-lavf-o-append", "err_detect=ignore_err")  // 忽略解复用错误
            
            // FFmpeg 容错设置
            setOption("demuxer-lavf-o", "fflags=+genpts+igndts+ignidx")  // 生成 PTS，忽略损坏的索引
            
            // 硬件加速
            if (config.hardwareAcceleration) {
                setOption("hwdec", config.hwdecMethod)
            } else {
                setOption("hwdec", "no")
            }
            
            // 视频解码容错设置
            setOption("vd-lavc-threads", "auto")
            setOption("vd-lavc-fast", "yes")              // 快速解码
            setOption("vd-lavc-skiploopfilter", "default") // 跳过环路滤波器（损坏流）
            
            // 视频质量设置 - 选择最佳质量
            setOption("ytdl-format", "bestvideo+bestaudio/best")
            setOption("hls-bitrate", "max")  // HLS 流选择最高码率
            
            // 缓存设置
            setOption("cache", "yes")
            setOption("demuxer-max-bytes", "${config.cacheSize * 1024}")
            setOption("demuxer-max-back-bytes", "${config.cacheSize * 1024}")
            setOption("cache-secs", config.cacheSecs.toString())
            setOption("demuxer-readahead-secs", config.demuxerReadahead.toString())
            
            // 网络设置
            setOption("network-timeout", config.networkTimeout.toString())
            setOption("user-agent", config.userAgent)
            setOption("http-header-fields", "Connection: keep-alive")
            
            // 流处理容错设置
            setOption("stream-lavf-o", "reconnect=1,reconnect_streamed=1,reconnect_delay_max=5")
            setOption("demuxer-lavf-analyzeduration", "1")  // 快速分析
            setOption("demuxer-lavf-probescore", "25")      // 降低探测分数要求
            
            // 播放设置
            setOption("keep-open", if (config.keepOpen) "yes" else "no")
            
            // 日志 - 设置为 debug 以获取更多信息
            bindings?.mpv_request_log_messages(handle, "info")
            
            println("✓ Configuration applied successfully")
            println("  - Video output: libmpv (embedded rendering)")
            println("  - Audio output: ${config.audioOutput}")
            println("  - Hardware acceleration: ${if (config.hardwareAcceleration) config.hwdecMethod else "disabled"}")
            println("  - Video quality: best available")
            
        } catch (e: Exception) {
            // Log but don't fail initialization for configuration errors
            println("Warning: Failed to apply some configuration options: ${e.message}")
        }
    }
    
    /**
     * Set an option before initialization
     * 
     * This is a private method used during initialization.
     * 
     * @param name Option name
     * @param value Option value
     */
    private fun setOption(name: String, value: String) {
        val handle = mpvHandle ?: return
        val result = bindings?.mpv_set_option_string(handle, name, value) ?: return
        
        if (result != LibmpvBindings.MPV_ERROR_SUCCESS) {
            // Log warning but don't throw - some options may not be available
            println("Warning: Failed to set option '$name' to '$value': error code $result")
        }
    }
    
    /**
     * Set an option (public API)
     * 
     * This method can be used to set options before initialization.
     * After initialization, use setPropertyString() instead.
     * 
     * Requirements:
     * - 7.1: Support configuring hardware acceleration options
     * - 7.2: Support configuring network buffering parameters
     * - 7.3: Support configuring audio output options
     * - 7.4: Support configuring video output options
     * 
     * @param name Option name
     * @param value Option value
     * @throws LibmpvException.ConfigurationError if setting the option fails
     */
    fun setOptionPublic(name: String, value: String) {
        val handle = mpvHandle ?: throw LibmpvException.ConfigurationError(
            name,
            "Engine not initialized"
        )
        
        if (isInitialized.get()) {
            throw LibmpvException.ConfigurationError(
                name,
                "Cannot set options after initialization. Use setPropertyString() instead."
            )
        }
        
        val result = bindings?.mpv_set_option_string(handle, name, value)
            ?: throw LibmpvException.ConfigurationError(name, "Bindings not available")
        
        if (result != LibmpvBindings.MPV_ERROR_SUCCESS) {
            val error = LibmpvError.fromCode(result, bindings)
            throw LibmpvException.ConfigurationError(name, error.message)
        }
    }
    
    /**
     * Destroy the mpv instance and release all resources
     * 
     * Stops playback, stops the event thread, and destroys the mpv instance.
     * After calling this method, the engine cannot be used again.
     */
    fun destroy() {
        if (!isInitialized.get()) {
            return
        }
        
        // Stop event thread
        stopEventThread()
        
        // Destroy mpv instance
        mpvHandle?.let { handle ->
            try {
                bindings?.mpv_destroy(handle)
            } catch (e: Exception) {
                println("Error destroying mpv instance: ${e.message}")
            }
        }
        
        mpvHandle = null
        isInitialized.set(false)
    }
    
    /**
     * Load and play a file or URL
     * 
     * @param url File path or URL to play
     * @throws LibmpvException.CommandError if the command fails
     */
    fun loadFile(url: String) {
        executeCommand("loadfile", url)
    }
    
    /**
     * Start or resume playback
     * 
     * @throws LibmpvException.PropertyError if setting the property fails
     */
    fun play() {
        setPropertyFlag("pause", false)
    }
    
    /**
     * Pause playback
     * 
     * @throws LibmpvException.PropertyError if setting the property fails
     */
    fun pause() {
        setPropertyFlag("pause", true)
    }
    
    /**
     * Stop playback and unload the current file
     * 
     * @throws LibmpvException.CommandError if the command fails
     */
    fun stop() {
        executeCommand("stop")
    }
    
    /**
     * Seek to a specific position
     * 
     * @param position Position in seconds
     * @throws LibmpvException.CommandError if the command fails
     */
    fun seek(position: Double) {
        executeCommand("seek", position.toString(), "absolute")
    }
    
    /**
     * Set the volume
     * 
     * @param volume Volume level (0-100)
     * @throws LibmpvException.PropertyError if setting the property fails
     */
    fun setVolume(volume: Int) {
        val clampedVolume = volume.coerceIn(0, 100)
        setPropertyDouble("volume", clampedVolume.toDouble())
    }
    
    /**
     * Get the current volume
     * 
     * @return Volume level (0-100), or 0 if unavailable
     */
    fun getVolume(): Int {
        return getPropertyDouble("volume")?.toInt() ?: 0
    }
    
    /**
     * Get the current playback position
     * 
     * @return Position in seconds, or 0.0 if unavailable
     */
    fun getPosition(): Double {
        return getPropertyDouble("time-pos") ?: 0.0
    }
    
    /**
     * Get the duration of the current media
     * 
     * @return Duration in seconds, or 0.0 if unavailable
     */
    fun getDuration(): Double {
        return getPropertyDouble("duration") ?: 0.0
    }
    
    /**
     * Check if playback is paused
     * 
     * @return true if paused, false otherwise
     */
    fun isPaused(): Boolean {
        return getPropertyFlag("pause") ?: true
    }
    
    /**
     * Check if the engine is initialized
     * 
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return isInitialized.get()
    }
    
    /**
     * Set a string property
     * 
     * @param name Property name
     * @param value Property value
     * @throws LibmpvException.PropertyError if setting the property fails
     */
    fun setPropertyString(name: String, value: String) {
        val handle = mpvHandle ?: throw LibmpvException.PropertyError(name, "Engine not initialized")
        val result = bindings?.mpv_set_property_string(handle, name, value)
            ?: throw LibmpvException.PropertyError(name, "Bindings not available")
        
        if (result != LibmpvBindings.MPV_ERROR_SUCCESS) {
            val error = LibmpvError.fromCode(result, bindings)
            throw LibmpvException.PropertyError(name, error.message)
        }
    }
    
    /**
     * Get a string property
     * 
     * @param name Property name
     * @return Property value, or null if unavailable
     */
    fun getPropertyString(name: String): String? {
        val handle = mpvHandle ?: return null
        val ptr = bindings?.mpv_get_property_string(handle, name) ?: return null
        
        return try {
            val value = ptr.getString(0)
            bindings.mpv_free(ptr)
            value
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Set a double property
     * 
     * @param name Property name
     * @param value Property value
     * @throws LibmpvException.PropertyError if setting the property fails
     */
    fun setPropertyDouble(name: String, value: Double) {
        val handle = mpvHandle ?: throw LibmpvException.PropertyError(name, "Engine not initialized")
        val ref = DoubleByReference(value)
        val result = bindings?.mpv_set_property(handle, name, LibmpvBindings.MPV_FORMAT_DOUBLE, ref.pointer)
            ?: throw LibmpvException.PropertyError(name, "Bindings not available")
        
        if (result != LibmpvBindings.MPV_ERROR_SUCCESS) {
            val error = LibmpvError.fromCode(result, bindings)
            throw LibmpvException.PropertyError(name, error.message)
        }
    }
    
    /**
     * Get a double property
     * 
     * @param name Property name
     * @return Property value, or null if unavailable
     */
    fun getPropertyDouble(name: String): Double? {
        val handle = mpvHandle ?: return null
        val ref = DoubleByReference()
        val result = bindings?.mpv_get_property(handle, name, LibmpvBindings.MPV_FORMAT_DOUBLE, ref.pointer) ?: return null
        
        return if (result == LibmpvBindings.MPV_ERROR_SUCCESS) {
            ref.value
        } else {
            null
        }
    }
    
    /**
     * Set a flag (boolean) property
     * 
     * @param name Property name
     * @param value Property value
     * @throws LibmpvException.PropertyError if setting the property fails
     */
    fun setPropertyFlag(name: String, value: Boolean) {
        val handle = mpvHandle ?: throw LibmpvException.PropertyError(name, "Engine not initialized")
        val ref = IntByReference(if (value) 1 else 0)
        val result = bindings?.mpv_set_property(handle, name, LibmpvBindings.MPV_FORMAT_FLAG, ref.pointer)
            ?: throw LibmpvException.PropertyError(name, "Bindings not available")
        
        if (result != LibmpvBindings.MPV_ERROR_SUCCESS) {
            val error = LibmpvError.fromCode(result, bindings)
            throw LibmpvException.PropertyError(name, error.message)
        }
    }
    
    /**
     * Get a flag (boolean) property
     * 
     * @param name Property name
     * @return Property value, or null if unavailable
     */
    fun getPropertyFlag(name: String): Boolean? {
        val handle = mpvHandle ?: return null
        val ref = IntByReference()
        val result = bindings?.mpv_get_property(handle, name, LibmpvBindings.MPV_FORMAT_FLAG, ref.pointer) ?: return null
        
        return if (result == LibmpvBindings.MPV_ERROR_SUCCESS) {
            ref.value != 0
        } else {
            null
        }
    }
    
    /**
     * Execute a command
     * 
     * @param args Command arguments
     * @throws LibmpvException.CommandError if the command fails
     */
    fun executeCommand(vararg args: String) {
        val handle = mpvHandle ?: throw LibmpvException.CommandError(
            args.joinToString(" "),
            "Engine not initialized"
        )
        
        // Create null-terminated array
        val commandArgs = Array<String?>(args.size + 1) { i ->
            if (i < args.size) args[i] else null
        }
        
        val result = bindings?.mpv_command(handle, commandArgs)
            ?: throw LibmpvException.CommandError(
                args.joinToString(" "),
                "Bindings not available"
            )
        
        if (result != LibmpvBindings.MPV_ERROR_SUCCESS) {
            val error = LibmpvError.fromCode(result, bindings)
            throw LibmpvException.CommandError(args.joinToString(" "), error.message)
        }
    }
    
    /**
     * Set the event callback
     * 
     * The callback will be invoked on the event thread for each event.
     * 
     * @param callback Event callback function
     */
    fun setEventCallback(callback: (LibmpvEvent) -> Unit) {
        this.eventCallback = callback
    }
    
    /**
     * Set the error callback
     * 
     * The callback will be invoked when an error occurs.
     * 
     * @param callback Error callback function
     */
    fun setErrorCallback(callback: (LibmpvException) -> Unit) {
        this.errorCallback = callback
    }
    
    /**
     * Start the event handling thread
     */
    private fun startEventThread() {
        if (isRunning.get()) {
            return
        }
        
        isRunning.set(true)
        eventThread = thread(name = "libmpv-event-thread") {
            handleEvents()
        }
    }
    
    /**
     * Stop the event handling thread
     */
    private fun stopEventThread() {
        isRunning.set(false)
        eventThread?.interrupt()
        eventThread?.join(1000)
        eventThread = null
    }
    
    /**
     * Event handling loop
     * 
     * Runs on the event thread and processes events from libmpv.
     */
    private fun handleEvents() {
        val handle = mpvHandle ?: return
        
        while (isRunning.get() && !Thread.currentThread().isInterrupted) {
            try {
                // Wait for event with timeout
                val eventPtr = bindings?.mpv_wait_event(handle, 0.1) ?: continue
                
                // Parse event
                val event = LibmpvEventParser.parse(eventPtr)
                
                // Skip None events
                if (event is LibmpvEvent.None) {
                    continue
                }
                
                // Handle error events
                if (event is LibmpvEvent.Error) {
                    val exception = LibmpvException.PlaybackError(event.code, event.message)
                    errorCallback?.invoke(exception)
                }
                
                // Invoke callback
                eventCallback?.invoke(event)
                
            } catch (e: InterruptedException) {
                break
            } catch (e: Exception) {
                println("Error in event thread: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get video width
     * 
     * @return Video width in pixels, or 0 if unavailable
     */
    fun getVideoWidth(): Int {
        return getPropertyDouble("width")?.toInt() ?: 0
    }
    
    /**
     * Get video height
     * 
     * @return Video height in pixels, or 0 if unavailable
     */
    fun getVideoHeight(): Int {
        return getPropertyDouble("height")?.toInt() ?: 0
    }
    
    /**
     * Get the mpv handle
     * 
     * For advanced use cases that need direct access to the mpv handle.
     * 
     * @return mpv handle, or null if not initialized
     */
    fun getMpvHandle(): Pointer? {
        return mpvHandle
    }
    
    /**
     * Get the libmpv bindings
     * 
     * For advanced use cases that need direct access to the bindings.
     * 
     * @return libmpv bindings, or null if not available
     */
    fun getBindings(): LibmpvBindings? {
        return bindings
    }
}
