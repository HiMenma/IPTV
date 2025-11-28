package com.menmapro.iptv.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.menmapro.iptv.player.LibmpvPlayerImplementation

/**
 * Desktop Video Player - libmpv Implementation
 * 
 * This is the actual implementation of VideoPlayer for desktop platforms.
 * It uses LibmpvPlayerImplementation which provides libmpv-based video playback.
 * 
 * libmpv is now the default player, providing:
 * - Excellent format and codec support (H.264, H.265, VP9, AV1)
 * - Hardware acceleration
 * - Reliable streaming (HLS, HTTP, RTSP)
 * - Simple and powerful API
 * 
 * Requirements:
 * - 5.1: Implement the PlayerImplementation interface
 * - 5.2: Provide the same PlayerControls callback interface
 * - 5.3: Update PlayerState in the same manner as existing implementations
 * - 5.4: Use the same error callback mechanism
 */
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit,
    onError: (String) -> Unit,
    onPlayerInitFailed: () -> Unit,
    isFullscreen: Boolean
) {
    // Create libmpv player implementation
    val playerImplementation = remember { LibmpvPlayerImplementation() }
    
    // Use libmpv player implementation
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
