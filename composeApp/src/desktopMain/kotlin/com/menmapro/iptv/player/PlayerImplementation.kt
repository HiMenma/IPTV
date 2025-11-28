package com.menmapro.iptv.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.menmapro.iptv.ui.components.PlayerControls
import com.menmapro.iptv.ui.components.PlayerState

/**
 * Player implementation type enumeration
 * 
 * Defines the available video player implementations that can be used
 * for desktop video playback.
 * 
 * Requirements:
 * - 9.1: Support configuration-based player selection
 */
enum class PlayerImplementationType {
    /**
     * VLC-based player implementation using VLCJ library
     * 
     * Features:
     * - Mature and stable
     * - Wide format support
     * - Hardware acceleration
     * - Requires VLC installation
     */
    VLC,
    
    /**
     * FFmpeg-based player implementation using JavaCV
     * 
     * Features:
     * - Direct FFmpeg integration
     * - Fine-grained control
     * - Custom audio-video synchronization
     * - Live stream optimization
     * - No external dependencies
     */
    FFMPEG
}

/**
 * Abstract interface for video player implementations
 * 
 * This interface defines the contract that all player implementations must follow,
 * ensuring API compatibility regardless of the underlying player technology.
 * 
 * Requirements:
 * - 9.1: Define common player interface
 * - 9.2: Provide same PlayerControls interface
 * - 9.3: Provide same PlayerState updates
 * - 9.4: Use same error callback interface
 * - 9.5: Enable seamless switching between implementations
 */
interface PlayerImplementation {
    /**
     * Get the implementation type
     */
    val type: PlayerImplementationType
    
    /**
     * Get a human-readable name for this implementation
     */
    val name: String
    
    /**
     * Get a description of this implementation
     */
    val description: String
    
    /**
     * Check if this implementation is available on the current system
     * 
     * For example, VLC implementation requires VLC to be installed,
     * while FFmpeg implementation is always available.
     * 
     * @return true if the implementation can be used, false otherwise
     */
    fun isAvailable(): Boolean
    
    /**
     * Get the reason why this implementation is not available (if applicable)
     * 
     * @return Error message explaining why the implementation cannot be used,
     *         or null if it is available
     */
    fun getUnavailableReason(): String?
    
    /**
     * Render the video player composable
     * 
     * This method must maintain complete API compatibility with the existing
     * VideoPlayer composable interface.
     * 
     * @param url Media URL to play
     * @param modifier Compose modifier
     * @param playerState Mutable state for player status
     * @param onPlayerControls Callback to receive player controls
     * @param onError Callback for error messages
     * @param onPlayerInitFailed Callback when player initialization fails
     * @param isFullscreen Whether player is in fullscreen mode
     */
    @Composable
    fun VideoPlayer(
        url: String,
        modifier: Modifier,
        playerState: MutableState<PlayerState>,
        onPlayerControls: (PlayerControls) -> Unit,
        onError: (String) -> Unit,
        onPlayerInitFailed: () -> Unit,
        isFullscreen: Boolean
    )
}

/**
 * Configuration for player implementation selection
 * 
 * This class encapsulates the configuration for selecting and managing
 * video player implementations.
 * 
 * Requirements:
 * - 9.1: Support configuration-based player selection
 */
data class PlayerConfiguration(
    /**
     * The preferred player implementation type
     */
    val preferredImplementation: PlayerImplementationType = PlayerImplementationType.VLC,
    
    /**
     * Whether to automatically fallback to alternative implementation
     * if the preferred one is not available
     */
    val enableAutoFallback: Boolean = true,
    
    /**
     * The fallback implementation to use if preferred is not available
     * and auto-fallback is enabled
     */
    val fallbackImplementation: PlayerImplementationType = PlayerImplementationType.FFMPEG
) {
    companion object {
        /**
         * Default configuration using VLC with FFmpeg fallback
         */
        val DEFAULT = PlayerConfiguration(
            preferredImplementation = PlayerImplementationType.VLC,
            enableAutoFallback = true,
            fallbackImplementation = PlayerImplementationType.FFMPEG
        )
        
        /**
         * Configuration that always uses VLC (no fallback)
         */
        val VLC_ONLY = PlayerConfiguration(
            preferredImplementation = PlayerImplementationType.VLC,
            enableAutoFallback = false
        )
        
        /**
         * Configuration that always uses FFmpeg (no fallback)
         */
        val FFMPEG_ONLY = PlayerConfiguration(
            preferredImplementation = PlayerImplementationType.FFMPEG,
            enableAutoFallback = false
        )
        
        /**
         * Configuration using FFmpeg with VLC fallback
         */
        val FFMPEG_FIRST = PlayerConfiguration(
            preferredImplementation = PlayerImplementationType.FFMPEG,
            enableAutoFallback = true,
            fallbackImplementation = PlayerImplementationType.VLC
        )
    }
}
