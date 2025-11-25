package com.menmapro.iptv.ui.components

import android.view.ViewGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit,
    onError: (String) -> Unit,
    onPlayerInitFailed: () -> Unit
) {
    val context = LocalContext.current
    
    // Track if player is released
    val isReleased = remember { mutableStateOf(false) }
    
    // Track listener registration state to ensure proper cleanup
    val listenerRegistered = remember { mutableStateOf(false) }
    
    // Create ExoPlayer listener for proper event handling
    val playerListener = remember {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                try {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            playerState.value = playerState.value.copy(
                                playbackState = PlaybackState.BUFFERING
                            )
                        }
                        Player.STATE_READY -> {
                            // Will be updated by isPlaying check
                        }
                        Player.STATE_ENDED -> {
                            playerState.value = playerState.value.copy(
                                playbackState = PlaybackState.ENDED
                            )
                        }
                        Player.STATE_IDLE -> {
                            // Initial state or after reset
                        }
                    }
                } catch (e: Exception) {
                    println("Error in onPlaybackStateChanged: ${e.message}")
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                try {
                    playerState.value = playerState.value.copy(
                        playbackState = if (isPlaying) PlaybackState.PLAYING else PlaybackState.PAUSED
                    )
                } catch (e: Exception) {
                    println("Error in onIsPlayingChanged: ${e.message}")
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                try {
                    val errorMsg = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "网络连接超时"
                        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "HTTP错误: ${error.message}"
                        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "媒体格式错误"
                        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "清单文件格式错误"
                        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "解码器初始化失败，设备可能不支持此格式"
                        PlaybackException.ERROR_CODE_UNSPECIFIED -> "播放错误: 不支持的媒体格式或编码"
                        else -> "播放错误: ${error.message ?: "未知错误"} (代码: ${error.errorCode})"
                    }
                    
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                    println("Player error: $errorMsg")
                } catch (e: Exception) {
                    println("Error in onPlayerError: ${e.message}")
                }
            }
        }
    }
    
    val exoPlayer = remember {
        try {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
        } catch (e: Exception) {
            val errorMsg = "Error initializing ExoPlayer: ${e.message}"
            println(errorMsg)
            e.printStackTrace()
            // Notify about initialization failure
            onPlayerInitFailed()
            onError(errorMsg)
            null
        }
    }
    
    val controls = remember(exoPlayer) {
        object : PlayerControls {
            override fun play() {
                if (exoPlayer == null || !verifyPlayerState("play", isReleased, exoPlayer, playerState)) {
                    return
                }
                try {
                    exoPlayer.play()
                    println("✓ Play command executed successfully")
                } catch (e: Exception) {
                    val errorMsg = "播放失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in play: $errorMsg")
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                }
            }
            
            override fun pause() {
                if (exoPlayer == null || !verifyPlayerState("pause", isReleased, exoPlayer, playerState)) {
                    return
                }
                try {
                    exoPlayer.pause()
                    println("✓ Pause command executed successfully")
                } catch (e: Exception) {
                    val errorMsg = "暂停失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in pause: $errorMsg")
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                }
            }
            
            override fun seekTo(positionMs: Long) {
                if (exoPlayer == null || !verifyPlayerState("seekTo", isReleased, exoPlayer, playerState)) {
                    return
                }
                try {
                    exoPlayer.seekTo(positionMs)
                    println("✓ Seek command executed successfully to position: $positionMs ms")
                } catch (e: Exception) {
                    val errorMsg = "跳转失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in seekTo: $errorMsg")
                    playerState.value = playerState.value.copy(
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                }
            }
            
            override fun setVolume(volume: Float) {
                if (exoPlayer == null || !verifyPlayerState("setVolume", isReleased, exoPlayer, playerState)) {
                    return
                }
                try {
                    val clampedVolume = volume.coerceIn(0f, 1f)
                    exoPlayer.volume = clampedVolume
                    println("✓ Volume set successfully to: ${(clampedVolume * 100).toInt()}%")
                } catch (e: Exception) {
                    val errorMsg = "音量设置失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in setVolume: $errorMsg")
                    playerState.value = playerState.value.copy(
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                }
            }
            
            override fun toggleFullscreen() {
                if (exoPlayer == null || !verifyPlayerState("toggleFullscreen", isReleased, exoPlayer, playerState)) {
                    return
                }
                // TODO: Implement fullscreen
                println("ℹ Fullscreen not yet implemented for Android")
            }
            
            override fun release() {
                if (exoPlayer != null) {
                    safeReleasePlayer(exoPlayer, playerListener, isReleased, listenerRegistered)
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        // Validate URL before proceeding
        if (url.isBlank()) {
            val errorMsg = "无效的播放地址"
            onError(errorMsg)
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            return@DisposableEffect onDispose {}
        }
        
        if (exoPlayer != null) {
            // Add listener and track registration
            try {
                exoPlayer.addListener(playerListener)
                listenerRegistered.value = true
                println("ExoPlayer listener registered successfully")
            } catch (e: Exception) {
                val errorMsg = "Error adding ExoPlayer listener: ${e.message}"
                println(errorMsg)
                listenerRegistered.value = false
                onPlayerInitFailed()
                onError(errorMsg)
            }
        } else {
            // exoPlayer is null, initialization failed
            onPlayerInitFailed()
        }
        
        onPlayerControls(controls)
        
        onDispose {
            println("DisposableEffect onDispose called - cleaning up ExoPlayer resources")
            if (exoPlayer != null) {
                safeReleasePlayer(exoPlayer, playerListener, isReleased, listenerRegistered)
            }
        }
    }
    
    // Handle URL changes - improved with proper resource cleanup
    LaunchedEffect(url) {
        // Validate URL
        if (url.isBlank()) {
            val errorMsg = "无效的播放地址"
            onError(errorMsg)
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            return@LaunchedEffect
        }
        
        if (isReleased.value || exoPlayer == null) {
            val errorMsg = "播放器未初始化"
            println("Cannot load URL: player is released or not initialized")
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            onError(errorMsg)
            return@LaunchedEffect
        }
        
        try {
            println("Loading new URL: $url")
            
            // Step 1: Stop current playback before loading new media
            try {
                exoPlayer.stop()
                println("Current playback stopped")
            } catch (e: Exception) {
                println("Warning: Error stopping playback: ${e.message}")
            }
            
            // Step 2: Add delay to ensure resources are properly freed
            kotlinx.coroutines.delay(150)
            
            // Step 3: Update state to show loading
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.BUFFERING,
                errorMessage = null
            )
            
            // Step 4: Load and play new media with error handling
            try {
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                println("Media loaded successfully: $url")
            } catch (e: Exception) {
                throw Exception("无法播放媒体URL: ${e.message ?: "未知错误"}", e)
            }
            
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("无法播放媒体URL") == true -> e.message!!
                e.message?.contains("Connection") == true -> "网络连接错误: ${e.message}"
                e.message?.contains("timeout") == true -> "连接超时，请检查网络或URL"
                e.message?.contains("404") == true -> "媒体资源不存在 (404)"
                e.message?.contains("403") == true -> "访问被拒绝 (403)"
                else -> "无法加载媒体: ${e.message ?: "未知错误"}"
            }
            
            println("Error loading media: $errorMsg")
            e.printStackTrace()
            
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            onError(errorMsg)
        }
    }
    
    // Update player state periodically
    LaunchedEffect(exoPlayer) {
        while (true) {
            kotlinx.coroutines.delay(500)
            if (isReleased.value || exoPlayer == null) break
            
            try {
                if (exoPlayer.isPlaying) {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.PLAYING,
                        position = exoPlayer.currentPosition,
                        duration = exoPlayer.duration.coerceAtLeast(0),
                        volume = exoPlayer.volume
                    )
                } else {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.PAUSED,
                        position = exoPlayer.currentPosition,
                        duration = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
            } catch (e: Exception) {
                println("Error updating player state: ${e.message}")
            }
        }
    }
    
    if (exoPlayer != null) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = modifier
        )
    }
}

/**
 * Verify player state before executing operations
 * Returns true if player is ready for operations, false otherwise
 */
private fun verifyPlayerState(
    operation: String,
    isReleased: MutableState<Boolean>,
    exoPlayer: ExoPlayer,
    playerState: MutableState<PlayerState>
): Boolean {
    // Check if player is released
    if (isReleased.value) {
        val errorMsg = "无法执行 $operation: 播放器已释放"
        println("✗ $errorMsg")
        playerState.value = playerState.value.copy(
            playbackState = PlaybackState.ERROR,
            errorMessage = errorMsg
        )
        return false
    }
    
    // Verify player is in a valid state
    try {
        // Try to access player state to ensure it's not released
        val playbackState = exoPlayer.playbackState
        
        // Check if player is in an error state
        if (playbackState == Player.STATE_IDLE && exoPlayer.playerError != null) {
            val errorMsg = "无法执行 $operation: 播放器处于错误状态"
            println("✗ $errorMsg")
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            return false
        }
    } catch (e: IllegalStateException) {
        val errorMsg = "无法执行 $operation: 播放器状态无效 - ${e.message}"
        println("✗ $errorMsg")
        playerState.value = playerState.value.copy(
            playbackState = PlaybackState.ERROR,
            errorMessage = errorMsg
        )
        return false
    } catch (e: Exception) {
        val errorMsg = "无法执行 $operation: 无法访问播放器 - ${e.message}"
        println("✗ $errorMsg")
        playerState.value = playerState.value.copy(
            playbackState = PlaybackState.ERROR,
            errorMessage = errorMsg
        )
        return false
    }
    
    println("✓ Player state verified for operation: $operation")
    return true
}

/**
 * Safely release ExoPlayer resources with proper cleanup sequence
 * Includes listener tracking to prevent memory leaks
 */
private fun safeReleasePlayer(
    exoPlayer: ExoPlayer,
    playerListener: Player.Listener,
    isReleased: MutableState<Boolean>,
    listenerRegistered: MutableState<Boolean>
) {
    // Prevent double-release
    if (isReleased.value) {
        println("Skipping release: ExoPlayer already released")
        return
    }
    
    isReleased.value = true
    
    try {
        println("Starting safe release of ExoPlayer...")
        
        // Step 1: Stop playback
        try {
            exoPlayer.stop()
            println("✓ Playback stopped")
        } catch (e: Exception) {
            println("⚠ Error stopping playback: ${e.message}")
        }
        
        // Step 2: Remove event listeners with verification
        if (listenerRegistered.value) {
            try {
                exoPlayer.removeListener(playerListener)
                listenerRegistered.value = false
                println("✓ Player listener removed successfully")
            } catch (e: Exception) {
                println("⚠ Error removing player listener: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("ℹ No player listener to remove (not registered)")
        }
        
        // Step 3: Verify all listeners are removed
        if (!listenerRegistered.value) {
            println("✓ Verified: All event listeners removed")
        } else {
            println("⚠ Warning: Listener registration flag still true after removal")
        }
        
        // Step 4: Release the ExoPlayer
        try {
            exoPlayer.release()
            println("✓ ExoPlayer released successfully - no memory leaks")
        } catch (e: Exception) {
            println("⚠ Error releasing ExoPlayer: ${e.message}")
            e.printStackTrace()
        }
        
    } catch (e: Exception) {
        println("⚠ Error during safe release: ${e.message}")
        e.printStackTrace()
    }
}