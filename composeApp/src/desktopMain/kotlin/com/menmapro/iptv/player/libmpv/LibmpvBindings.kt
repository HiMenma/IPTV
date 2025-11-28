package com.menmapro.iptv.player.libmpv

import com.sun.jna.*
import com.sun.jna.ptr.DoubleByReference
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference

/**
 * JNA bindings for libmpv C API
 * 
 * This interface provides Kotlin/JNA bindings to the core libmpv C API functions.
 * libmpv is the library form of the MPV media player, providing a simple and
 * powerful API for media playback.
 * 
 * Requirements:
 * - 1.4: Provide JNA bindings to libmpv C API
 * 
 * @see <a href="https://mpv.io/manual/master/#libmpv">libmpv documentation</a>
 */
interface LibmpvBindings : Library {
    
    // ============================================================================
    // Core lifecycle functions
    // ============================================================================
    
    /**
     * Create a new mpv instance
     * 
     * @return Handle to the mpv instance, or null on failure
     */
    fun mpv_create(): Pointer?
    
    /**
     * Initialize an mpv instance
     * 
     * Must be called after mpv_create() and before any other functions.
     * 
     * @param ctx The mpv handle
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_initialize(ctx: Pointer): Int
    
    /**
     * Destroy an mpv instance
     * 
     * Frees all resources associated with the mpv instance.
     * 
     * @param ctx The mpv handle
     */
    fun mpv_destroy(ctx: Pointer)
    
    // ============================================================================
    // Property management functions
    // ============================================================================
    
    /**
     * Set a string property
     * 
     * @param ctx The mpv handle
     * @param name Property name
     * @param value Property value
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int
    
    /**
     * Get a string property
     * 
     * The returned pointer must be freed with mpv_free().
     * 
     * @param ctx The mpv handle
     * @param name Property name
     * @return Pointer to the property value string, or null on error
     */
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    
    /**
     * Get a property with a specific format
     * 
     * @param ctx The mpv handle
     * @param name Property name
     * @param format Format type (MPV_FORMAT_*)
     * @param data Pointer to store the property value
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_get_property(ctx: Pointer, name: String, format: Int, data: Pointer): Int
    
    /**
     * Set a property with a specific format
     * 
     * @param ctx The mpv handle
     * @param name Property name
     * @param format Format type (MPV_FORMAT_*)
     * @param data Pointer to the property value
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_set_property(ctx: Pointer, name: String, format: Int, data: Pointer): Int
    
    // ============================================================================
    // Command execution functions
    // ============================================================================
    
    /**
     * Execute a command
     * 
     * @param ctx The mpv handle
     * @param args Array of command arguments (null-terminated)
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_command(ctx: Pointer, args: Array<String?>): Int
    
    /**
     * Execute a command from a string
     * 
     * @param ctx The mpv handle
     * @param command Command string
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_command_string(ctx: Pointer, command: String): Int
    
    /**
     * Execute a command asynchronously
     * 
     * @param ctx The mpv handle
     * @param replyUserdata User data for the reply event
     * @param args Array of command arguments (null-terminated)
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_command_async(ctx: Pointer, replyUserdata: Long, args: Array<String?>): Int
    
    // ============================================================================
    // Event handling functions
    // ============================================================================
    
    /**
     * Wait for an event
     * 
     * Blocks until an event is available or the timeout expires.
     * 
     * @param ctx The mpv handle
     * @param timeout Timeout in seconds (0 for non-blocking, negative for infinite)
     * @return Pointer to mpv_event structure
     */
    fun mpv_wait_event(ctx: Pointer, timeout: Double): Pointer?
    
    /**
     * Request a log message event
     * 
     * @param ctx The mpv handle
     * @param minLevel Minimum log level
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_request_log_messages(ctx: Pointer, minLevel: String): Int
    
    /**
     * Request an event
     * 
     * @param ctx The mpv handle
     * @param event Event ID
     * @param enable 1 to enable, 0 to disable
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_request_event(ctx: Pointer, event: Int, enable: Int): Int
    
    // ============================================================================
    // Option setting functions
    // ============================================================================
    
    /**
     * Set an option
     * 
     * Must be called before mpv_initialize().
     * 
     * @param ctx The mpv handle
     * @param name Option name
     * @param value Option value
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_set_option_string(ctx: Pointer, name: String, value: String): Int
    
    // ============================================================================
    // Render context functions
    // ============================================================================
    
    /**
     * Create a render context
     * 
     * @param res Reference to store the render context handle
     * @param mpv The mpv handle
     * @param params Array of render parameters (null-terminated)
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_render_context_create(
        res: PointerByReference,
        mpv: Pointer,
        params: Pointer
    ): Int
    
    /**
     * Render a frame
     * 
     * @param ctx The render context handle
     * @param params Array of render parameters (null-terminated)
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_render_context_render(ctx: Pointer, params: Pointer): Int
    
    /**
     * Set a render context parameter
     * 
     * @param ctx The render context handle
     * @param param Parameter type
     * @param data Parameter data
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_render_context_set_parameter(ctx: Pointer, param: Int, data: Pointer): Int
    
    /**
     * Get a render context parameter
     * 
     * @param ctx The render context handle
     * @param param Parameter type
     * @param data Reference to store parameter data
     * @return Error code (0 on success, negative on error)
     */
    fun mpv_render_context_get_info(ctx: Pointer, param: Int, data: Pointer): Int
    
    /**
     * Set the update callback for the render context
     * 
     * @param ctx The render context handle
     * @param callback Callback function
     * @param callbackCtx Callback context
     */
    fun mpv_render_context_set_update_callback(
        ctx: Pointer,
        callback: Callback?,
        callbackCtx: Pointer?
    )
    
    /**
     * Free a render context
     * 
     * @param ctx The render context handle
     */
    fun mpv_render_context_free(ctx: Pointer)
    
    // ============================================================================
    // Memory management functions
    // ============================================================================
    
    /**
     * Free memory allocated by libmpv
     * 
     * @param data Pointer to memory to free
     */
    fun mpv_free(data: Pointer)
    
    // ============================================================================
    // Error handling functions
    // ============================================================================
    
    /**
     * Get error string for an error code
     * 
     * @param error Error code
     * @return Error message string
     */
    fun mpv_error_string(error: Int): String
    
    // ============================================================================
    // Client API version functions
    // ============================================================================
    
    /**
     * Get the client API version
     * 
     * @return Client API version
     */
    fun mpv_client_api_version(): Long
    
    companion object {
        // Error codes
        const val MPV_ERROR_SUCCESS = 0
        const val MPV_ERROR_EVENT_QUEUE_FULL = -1
        const val MPV_ERROR_NOMEM = -2
        const val MPV_ERROR_UNINITIALIZED = -3
        const val MPV_ERROR_INVALID_PARAMETER = -4
        const val MPV_ERROR_OPTION_NOT_FOUND = -5
        const val MPV_ERROR_OPTION_FORMAT = -6
        const val MPV_ERROR_OPTION_ERROR = -7
        const val MPV_ERROR_PROPERTY_NOT_FOUND = -8
        const val MPV_ERROR_PROPERTY_FORMAT = -9
        const val MPV_ERROR_PROPERTY_UNAVAILABLE = -10
        const val MPV_ERROR_PROPERTY_ERROR = -11
        const val MPV_ERROR_COMMAND = -12
        const val MPV_ERROR_LOADING_FAILED = -13
        const val MPV_ERROR_AO_INIT_FAILED = -14
        const val MPV_ERROR_VO_INIT_FAILED = -15
        const val MPV_ERROR_NOTHING_TO_PLAY = -16
        const val MPV_ERROR_UNKNOWN_FORMAT = -17
        const val MPV_ERROR_UNSUPPORTED = -18
        const val MPV_ERROR_NOT_IMPLEMENTED = -19
        const val MPV_ERROR_GENERIC = -20
        
        // Event IDs
        const val MPV_EVENT_NONE = 0
        const val MPV_EVENT_SHUTDOWN = 1
        const val MPV_EVENT_LOG_MESSAGE = 2
        const val MPV_EVENT_GET_PROPERTY_REPLY = 3
        const val MPV_EVENT_SET_PROPERTY_REPLY = 4
        const val MPV_EVENT_COMMAND_REPLY = 5
        const val MPV_EVENT_START_FILE = 6
        const val MPV_EVENT_END_FILE = 7
        const val MPV_EVENT_FILE_LOADED = 8
        const val MPV_EVENT_IDLE = 11
        const val MPV_EVENT_TICK = 14
        const val MPV_EVENT_CLIENT_MESSAGE = 16
        const val MPV_EVENT_VIDEO_RECONFIG = 17
        const val MPV_EVENT_AUDIO_RECONFIG = 18
        const val MPV_EVENT_SEEK = 20
        const val MPV_EVENT_PLAYBACK_RESTART = 21
        const val MPV_EVENT_PROPERTY_CHANGE = 22
        const val MPV_EVENT_QUEUE_OVERFLOW = 24
        const val MPV_EVENT_HOOK = 25
        
        // End file reasons
        const val MPV_END_FILE_REASON_EOF = 0
        const val MPV_END_FILE_REASON_STOP = 2
        const val MPV_END_FILE_REASON_QUIT = 3
        const val MPV_END_FILE_REASON_ERROR = 4
        const val MPV_END_FILE_REASON_REDIRECT = 5
        
        // Format types
        const val MPV_FORMAT_NONE = 0
        const val MPV_FORMAT_STRING = 1
        const val MPV_FORMAT_OSD_STRING = 2
        const val MPV_FORMAT_FLAG = 3
        const val MPV_FORMAT_INT64 = 4
        const val MPV_FORMAT_DOUBLE = 5
        const val MPV_FORMAT_NODE = 6
        const val MPV_FORMAT_NODE_ARRAY = 7
        const val MPV_FORMAT_NODE_MAP = 8
        const val MPV_FORMAT_BYTE_ARRAY = 9
        
        // Log levels
        const val MPV_LOG_LEVEL_NONE = "no"
        const val MPV_LOG_LEVEL_FATAL = "fatal"
        const val MPV_LOG_LEVEL_ERROR = "error"
        const val MPV_LOG_LEVEL_WARN = "warn"
        const val MPV_LOG_LEVEL_INFO = "info"
        const val MPV_LOG_LEVEL_V = "v"
        const val MPV_LOG_LEVEL_DEBUG = "debug"
        const val MPV_LOG_LEVEL_TRACE = "trace"
    }
}
