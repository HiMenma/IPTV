package com.menmapro.iptv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import com.menmapro.iptv.player.PlayerImplementation
import org.koin.compose.koinInject

/**
 * Configurable VideoPlayer that uses the player implementation from Koin
 * 
 * This composable delegates to the configured player implementation
 * (VLC or FFmpeg) based on the Koin configuration.
 * 
 * The implementation is selected at runtime based on:
 * 1. System properties (-D flags)
 * 2. Environment variables
 * 3. Default configuration (VLC with FFmpeg fallback)
 * 
 * This allows seamless switching between player implementations without
 * code changes, enabling:
 * - Testing both implementations
 * - Gradual migration from VLC to FFmpeg
 * - Fallback when preferred implementation is unavailable
 * 
 * Requirements:
 * - 9.1: Use configuration-based player selection
 * - 9.2: Provide same PlayerControls interface
 * - 9.3: Provide same PlayerState updates
 * - 9.4: Use same error callback interface
 * - 9.5: Work without code modifications
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
fun ConfigurableVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit,
    onError: (String) -> Unit = {},
    onPlayerInitFailed: () -> Unit = {},
    isFullscreen: Boolean = false
) {
    // Get the configured player implementation from Koin
    val playerImplementation: PlayerImplementation = koinInject()
    
    // Delegate to the configured implementation
    playerImplementation.VideoPlayer(
        url = url,
        modifier = modifier,
        playerState = playerState,
        onPlayerControls = onPlayerControls,
        onError = onError,
        onPlayerInitFailed = onPlayerInitFailed,
        isFullscreen = isFullscreen
    )
}
