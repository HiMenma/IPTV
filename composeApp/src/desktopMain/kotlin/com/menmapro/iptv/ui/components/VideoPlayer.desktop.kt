package com.menmapro.iptv.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Desktop Video Player - FFmpeg Implementation Only
 * 
 * This is the actual implementation of VideoPlayer for desktop platforms.
 * It directly uses FFmpegVideoPlayer as the only player implementation.
 * 
 * VLC support has been removed - FFmpeg is now the default and only player.
 * 
 * Requirements:
 * - 1.1: FFmpeg-based video decoding and rendering
 * - 9.1: API compatibility with existing VideoPlayer interface
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
    // Use FFmpeg player directly - no VLC fallback
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
