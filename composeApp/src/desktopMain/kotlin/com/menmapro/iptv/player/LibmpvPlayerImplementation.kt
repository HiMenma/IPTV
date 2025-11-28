package com.menmapro.iptv.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.menmapro.iptv.player.libmpv.LibmpvLoader
import com.menmapro.iptv.ui.components.LibmpvVideoPlayer
import com.menmapro.iptv.ui.components.PlayerControls
import com.menmapro.iptv.ui.components.PlayerState

/**
 * libmpv-based player implementation adapter
 * 
 * This adapter wraps the libmpv-based VideoPlayer implementation
 * and provides it through the PlayerImplementation interface.
 * 
 * Features:
 * - Uses libmpv (MPV media player library) for video playback
 * - Simple and powerful API
 * - Excellent format and codec support
 * - Hardware acceleration support
 * - Reliable streaming (HLS, HTTP, RTSP, etc.)
 * - Requires libmpv installation
 * 
 * Requirements:
 * - 1.1: Use libmpv library for video playback
 * - 1.2: Provide clear error messages when libmpv is not available
 * - 1.5: Maintain the same PlayerImplementation interface for compatibility
 * - 5.1: Implement the PlayerImplementation interface
 */
class LibmpvPlayerImplementation : PlayerImplementation {
    override val type: PlayerImplementationType = PlayerImplementationType.LIBMPV
    
    override val name: String = "libmpv Player"
    
    override val description: String = 
        "libmpv-based player using MPV media player library. Provides a simple yet powerful " +
        "API with excellent format support including H.264, H.265, VP9, and AV1. Features " +
        "hardware acceleration, reliable streaming for HLS/HTTP/RTSP protocols, and robust " +
        "error handling. Requires libmpv to be installed on the system."
    
    /**
     * Check if libmpv player is available
     * 
     * Checks if the libmpv library can be loaded on the current system.
     * 
     * Requirements:
     * - 1.2: Check libmpv installation
     * 
     * @return true if libmpv is available, false otherwise
     */
    override fun isAvailable(): Boolean {
        return LibmpvLoader.isAvailable()
    }
    
    /**
     * Get the reason why libmpv is not available
     * 
     * Provides detailed installation instructions for the current platform
     * if libmpv is not available.
     * 
     * Requirements:
     * - 1.2: Provide clear error messages indicating installation requirements
     * 
     * @return Error message with installation instructions, or null if available
     */
    override fun getUnavailableReason(): String? {
        if (isAvailable()) {
            return null
        }
        return LibmpvLoader.getUnavailableReason()
    }
    
    /**
     * Render the libmpv-based video player
     * 
     * Delegates to the LibmpvVideoPlayer implementation, maintaining complete
     * API compatibility with the existing VideoPlayer interface.
     * 
     * Requirements:
     * - 5.1: Implement the PlayerImplementation interface
     * - 5.2: Provide the same PlayerControls callback interface
     * - 5.3: Update PlayerState in the same manner as existing implementations
     * - 5.4: Use the same error callback mechanism
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
    override fun VideoPlayer(
        url: String,
        modifier: Modifier,
        playerState: MutableState<PlayerState>,
        onPlayerControls: (PlayerControls) -> Unit,
        onError: (String) -> Unit,
        onPlayerInitFailed: () -> Unit,
        isFullscreen: Boolean
    ) {
        // Delegate to the libmpv VideoPlayer implementation
        LibmpvVideoPlayer(
            url = url,
            modifier = modifier,
            playerState = playerState,
            onPlayerControls = onPlayerControls,
            onError = onError,
            onPlayerInitFailed = onPlayerInitFailed,
            isFullscreen = isFullscreen
        )
    }
}
