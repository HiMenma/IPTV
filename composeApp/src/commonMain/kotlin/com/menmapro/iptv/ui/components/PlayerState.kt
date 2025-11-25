package com.menmapro.iptv.ui.components

import androidx.compose.runtime.*

/**
 * Represents the playback state of a media player
 */
enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}

/**
 * Interface for controlling media playback
 */
interface PlayerControls {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setVolume(volume: Float) // 0.0 to 1.0
    fun toggleFullscreen()
    fun release()
}

/**
 * Player state information
 */
@Stable
data class PlayerState(
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val position: Long = 0L,
    val duration: Long = 0L,
    val volume: Float = 1.0f,
    val isFullscreen: Boolean = false,
    val errorMessage: String? = null
) {
    val progress: Float
        get() = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
}
