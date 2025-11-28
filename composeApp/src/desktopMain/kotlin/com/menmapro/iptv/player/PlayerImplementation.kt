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
     * VLC-based player implementation using VLCJ library (deprecated)
     * 
     * Features:
     * - Mature and stable
     * - Wide format support
     * - Hardware acceleration
     * - Requires VLC installation
     * 
     * Note: VLC player has been removed. This enum value is kept for
     * backward compatibility with saved preferences.
     */
    @Deprecated("VLC player has been removed. Use LIBMPV instead.")
    VLC,
    
    /**
     * libmpv-based player implementation using JNA bindings
     * 
     * Features:
     * - Powerful and efficient
     * - Simple API
     * - Excellent format support
     * - Hardware acceleration
     * - Reliable streaming
     * - Requires libmpv installation
     */
    LIBMPV
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
    val preferredImplementation: PlayerImplementationType = PlayerImplementationType.LIBMPV,
    
    /**
     * Whether to automatically fallback to alternative implementation
     * if the preferred one is not available
     */
    val enableAutoFallback: Boolean = false
) {
    companion object {
        /**
         * Default configuration using libmpv
         */
        val DEFAULT = PlayerConfiguration(
            preferredImplementation = PlayerImplementationType.LIBMPV,
            enableAutoFallback = false
        )
        
        /**
         * Configuration that always uses libmpv (default)
         */
        val LIBMPV_ONLY = PlayerConfiguration(
            preferredImplementation = PlayerImplementationType.LIBMPV,
            enableAutoFallback = false
        )
    }
}
