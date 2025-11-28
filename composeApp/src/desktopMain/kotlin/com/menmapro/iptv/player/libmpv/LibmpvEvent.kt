package com.menmapro.iptv.player.libmpv

import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * Sealed class hierarchy for libmpv events
 * 
 * Represents the different types of events that can be received from libmpv.
 * This provides a type-safe way to handle events in Kotlin.
 * 
 * Requirements:
 * - 1.4: Create data structures for libmpv events
 */
sealed class LibmpvEvent {
    /**
     * No event or unknown event
     */
    object None : LibmpvEvent()
    
    /**
     * Player is shutting down
     */
    object Shutdown : LibmpvEvent()
    
    /**
     * Player is idle (no file loaded)
     */
    object Idle : LibmpvEvent()
    
    /**
     * Starting to load a file
     */
    object StartFile : LibmpvEvent()
    
    /**
     * File has finished loading and is ready to play
     */
    object FileLoaded : LibmpvEvent()
    
    /**
     * File playback has ended
     * 
     * @param reason The reason why playback ended
     * @param error Error code if playback ended due to error
     */
    data class EndFile(val reason: EndFileReason, val error: Int = 0) : LibmpvEvent()
    
    /**
     * Video configuration changed (resolution, format, etc.)
     */
    object VideoReconfig : LibmpvEvent()
    
    /**
     * Audio configuration changed
     */
    object AudioReconfig : LibmpvEvent()
    
    /**
     * Seek operation started
     */
    object Seek : LibmpvEvent()
    
    /**
     * Playback restarted after seek or pause
     */
    object PlaybackRestart : LibmpvEvent()
    
    /**
     * A property value changed
     * 
     * @param name Property name
     * @param value Property value (type depends on property)
     */
    data class PropertyChange(val name: String, val value: Any?) : LibmpvEvent()
    
    /**
     * Log message from libmpv
     * 
     * @param level Log level (error, warn, info, etc.)
     * @param prefix Log prefix (usually the component name)
     * @param message Log message
     */
    data class LogMessage(val level: String, val prefix: String, val message: String) : LibmpvEvent()
    
    /**
     * An error occurred
     * 
     * @param code Error code
     * @param message Error message
     */
    data class Error(val code: Int, val message: String) : LibmpvEvent()
    
    /**
     * Event queue overflow
     * 
     * This means events were lost because the queue was full.
     */
    object QueueOverflow : LibmpvEvent()
}

/**
 * Reason why file playback ended
 */
enum class EndFileReason(val code: Int) {
    /**
     * Playback reached end of file
     */
    EOF(0),
    
    /**
     * Playback was stopped by user
     */
    STOP(2),
    
    /**
     * Player is quitting
     */
    QUIT(3),
    
    /**
     * An error occurred
     */
    ERROR(4),
    
    /**
     * Playback was redirected to another file
     */
    REDIRECT(5),
    
    /**
     * Unknown reason
     */
    UNKNOWN(-1);
    
    companion object {
        fun fromCode(code: Int): EndFileReason {
            return values().find { it.code == code } ?: UNKNOWN
        }
    }
}

/**
 * JNA structure for mpv_event
 * 
 * This structure matches the C structure definition in libmpv.
 */
@Structure.FieldOrder("event_id", "error", "reply_userdata", "data")
open class MpvEventStructure(p: Pointer? = null) : Structure(p) {
    @JvmField var event_id: Int = 0
    @JvmField var error: Int = 0
    @JvmField var reply_userdata: Long = 0
    @JvmField var data: Pointer? = null
    
    init {
        if (p != null) read()
    }
}

/**
 * JNA structure for mpv_event_property
 */
@Structure.FieldOrder("name", "format", "data")
open class MpvEventPropertyStructure(p: Pointer? = null) : Structure(p) {
    @JvmField var name: String? = null
    @JvmField var format: Int = 0
    @JvmField var data: Pointer? = null
    
    init {
        if (p != null) read()
    }
}

/**
 * JNA structure for mpv_event_log_message
 */
@Structure.FieldOrder("prefix", "level", "text")
open class MpvEventLogMessageStructure(p: Pointer? = null) : Structure(p) {
    @JvmField var prefix: String? = null
    @JvmField var level: String? = null
    @JvmField var text: String? = null
    
    init {
        if (p != null) read()
    }
}

/**
 * JNA structure for mpv_event_end_file
 */
@Structure.FieldOrder("reason", "error")
open class MpvEventEndFileStructure(p: Pointer? = null) : Structure(p) {
    @JvmField var reason: Int = 0
    @JvmField var error: Int = 0
    
    init {
        if (p != null) read()
    }
}

/**
 * Parser for converting raw mpv events to LibmpvEvent instances
 */
object LibmpvEventParser {
    
    /**
     * Parse a raw mpv_event pointer into a LibmpvEvent
     * 
     * @param eventPtr Pointer to mpv_event structure
     * @return Parsed LibmpvEvent
     */
    fun parse(eventPtr: Pointer?): LibmpvEvent {
        if (eventPtr == null) {
            return LibmpvEvent.None
        }
        
        val event = MpvEventStructure(eventPtr)
        
        return when (event.event_id) {
            LibmpvBindings.MPV_EVENT_NONE -> LibmpvEvent.None
            LibmpvBindings.MPV_EVENT_SHUTDOWN -> LibmpvEvent.Shutdown
            LibmpvBindings.MPV_EVENT_IDLE -> LibmpvEvent.Idle
            LibmpvBindings.MPV_EVENT_START_FILE -> LibmpvEvent.StartFile
            LibmpvBindings.MPV_EVENT_FILE_LOADED -> LibmpvEvent.FileLoaded
            
            LibmpvBindings.MPV_EVENT_END_FILE -> {
                val endFileData = event.data?.let { MpvEventEndFileStructure(it) }
                LibmpvEvent.EndFile(
                    reason = EndFileReason.fromCode(endFileData?.reason ?: -1),
                    error = endFileData?.error ?: 0
                )
            }
            
            LibmpvBindings.MPV_EVENT_VIDEO_RECONFIG -> LibmpvEvent.VideoReconfig
            LibmpvBindings.MPV_EVENT_AUDIO_RECONFIG -> LibmpvEvent.AudioReconfig
            LibmpvBindings.MPV_EVENT_SEEK -> LibmpvEvent.Seek
            LibmpvBindings.MPV_EVENT_PLAYBACK_RESTART -> LibmpvEvent.PlaybackRestart
            
            LibmpvBindings.MPV_EVENT_PROPERTY_CHANGE -> {
                val propData = event.data?.let { MpvEventPropertyStructure(it) }
                LibmpvEvent.PropertyChange(
                    name = propData?.name ?: "",
                    value = parsePropertyValue(propData)
                )
            }
            
            LibmpvBindings.MPV_EVENT_LOG_MESSAGE -> {
                val logData = event.data?.let { MpvEventLogMessageStructure(it) }
                LibmpvEvent.LogMessage(
                    level = logData?.level ?: "unknown",
                    prefix = logData?.prefix ?: "",
                    message = logData?.text ?: ""
                )
            }
            
            LibmpvBindings.MPV_EVENT_QUEUE_OVERFLOW -> LibmpvEvent.QueueOverflow
            
            else -> {
                if (event.error != 0) {
                    LibmpvEvent.Error(
                        code = event.error,
                        message = "Event error: ${event.error}"
                    )
                } else {
                    LibmpvEvent.None
                }
            }
        }
    }
    
    /**
     * Parse property value from event data
     * 
     * @param propData Property event structure
     * @return Parsed property value
     */
    private fun parsePropertyValue(propData: MpvEventPropertyStructure?): Any? {
        if (propData == null || propData.data == null) {
            return null
        }
        
        return when (propData.format) {
            LibmpvBindings.MPV_FORMAT_STRING -> propData.data?.getString(0)
            LibmpvBindings.MPV_FORMAT_FLAG -> propData.data?.getInt(0) != 0
            LibmpvBindings.MPV_FORMAT_INT64 -> propData.data?.getLong(0)
            LibmpvBindings.MPV_FORMAT_DOUBLE -> propData.data?.getDouble(0)
            else -> null
        }
    }
}
