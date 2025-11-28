package com.menmapro.iptv.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.menmapro.iptv.ui.components.FFmpegVideoPlayer
import com.menmapro.iptv.ui.components.PlayerControls
import com.menmapro.iptv.ui.components.PlayerState

/**
 * FFmpeg-based player implementation adapter
 * 
 * This adapter wraps the FFmpeg-based VideoPlayer implementation
 * and provides it through the PlayerImplementation interface.
 * 
 * Features:
 * - Uses JavaCV/FFmpeg for video playback
 * - Direct FFmpeg integration with fine-grained control
 * - Custom audio-video synchronization
 * - Live stream optimization
 * - Hardware acceleration support
 * - No external dependencies (FFmpeg bundled with JavaCV)
 * 
 * Requirements:
 * - 9.1: Implement PlayerImplementation interface for FFmpeg
 * - 9.2: Provide same PlayerControls interface
 * - 9.3: Provide same PlayerState updates
 * - 9.4: Use same error callback interface
 * - 9.5: Enable seamless integration with existing code
 */
class FFmpegPlayerImplementation : PlayerImplementation {
    override val type: PlayerImplementationType = PlayerImplementationType.FFMPEG
    
    override val name: String = "FFmpeg Player"
    
    override val description: String = 
        "FFmpeg-based player using JavaCV library. Provides direct FFmpeg integration " +
        "with fine-grained control over decoding and rendering. Features custom audio-video " +
        "synchronization, live stream optimization, and hardware acceleration. No external " +
        "dependencies required."
    
    /**
     * Check if FFmpeg player is available
     * 
     * FFmpeg player is always available as JavaCV bundles FFmpeg libraries.
     * 
     * @return true (always available)
     */
    override fun isAvailable(): Boolean {
        // FFmpeg player is always available as JavaCV bundles the FFmpeg libraries
        return true
    }
    
    /**
     * Get the reason why FFmpeg is not available
     * 
     * @return null (FFmpeg is always available)
     */
    override fun getUnavailableReason(): String? {
        // FFmpeg is always available
        return null
    }
    
    /**
     * Render the FFmpeg-based video player
     * 
     * Delegates to the FFmpegVideoPlayer implementation in
     * FFmpegVideoPlayer.desktop.kt, maintaining complete API compatibility.
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
        // Delegate to the FFmpeg VideoPlayer implementation
        FFmpegVideoPlayer(
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
