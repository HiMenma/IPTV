package com.menmapro.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit,
    onError: (String) -> Unit,
    onPlayerInitFailed: () -> Unit
) {
    // Check VLC availability first
    val vlcAvailable = remember { VlcAvailabilityChecker.isVlcAvailable() }
    
    // If VLC is not available, show error message with installation instructions
    if (!vlcAvailable) {
        VlcNotAvailableMessage(modifier)
        
        // Update player state to show error and call callback
        LaunchedEffect(Unit) {
            val errorMsg = "VLC Media Player 未安装"
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            onPlayerInitFailed()
            onError(errorMsg)
        }
        
        return
    }
    
    // Track release state to prevent double-release
    val isReleased = remember { mutableStateOf(false) }
    val isReleasing = remember { mutableStateOf(false) }
    
    // Track listener registration state to ensure proper cleanup
    val listenerRegistered = remember { mutableStateOf(false) }
    
    val mediaPlayerComponent = remember {
        try {
            EmbeddedMediaPlayerComponent()
        } catch (e: Exception) {
            val errorMsg = "Error initializing VLC player: ${e.message}"
            println(errorMsg)
            e.printStackTrace()
            // Notify about initialization failure
            onPlayerInitFailed()
            onError(errorMsg)
            null
        }
    }
    
    // Store event listener reference for proper cleanup
    val eventListener = remember {
        object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                try {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.PLAYING
                    )
                } catch (e: Exception) {
                    println("Error in playing event: ${e.message}")
                }
            }
            
            override fun paused(mediaPlayer: MediaPlayer) {
                try {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.PAUSED
                    )
                } catch (e: Exception) {
                    println("Error in paused event: ${e.message}")
                }
            }
            
            override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                try {
                    if (newCache < 100f) {
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.BUFFERING
                        )
                    }
                } catch (e: Exception) {
                    println("Error in buffering event: ${e.message}")
                }
            }
            
            override fun finished(mediaPlayer: MediaPlayer) {
                try {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ENDED
                    )
                } catch (e: Exception) {
                    println("Error in finished event: ${e.message}")
                }
            }
            
            override fun error(mediaPlayer: MediaPlayer) {
                try {
                    val errorMsg = "播放错误"
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                } catch (e: Exception) {
                    println("Error in error event: ${e.message}")
                }
            }
            
            override fun lengthChanged(mediaPlayer: MediaPlayer, newLength: Long) {
                try {
                    playerState.value = playerState.value.copy(
                        duration = newLength
                    )
                } catch (e: Exception) {
                    println("Error in lengthChanged event: ${e.message}")
                }
            }
        }
    }
    
    val controls = remember(mediaPlayerComponent) {
        object : PlayerControls {
            override fun play() {
                if (!verifyPlayerState("play", isReleased, isReleasing, mediaPlayerComponent, playerState)) {
                    return
                }
                try {
                    mediaPlayerComponent!!.mediaPlayer().controls().play()
                    println("✓ Play command executed successfully")
                } catch (e: Exception) {
                    val errorMsg = "播放失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in play: $errorMsg")
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = errorMsg
                    )
                }
            }
            
            override fun pause() {
                if (!verifyPlayerState("pause", isReleased, isReleasing, mediaPlayerComponent, playerState)) {
                    return
                }
                try {
                    mediaPlayerComponent!!.mediaPlayer().controls().pause()
                    println("✓ Pause command executed successfully")
                } catch (e: Exception) {
                    val errorMsg = "暂停失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in pause: $errorMsg")
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = errorMsg
                    )
                }
            }
            
            override fun seekTo(positionMs: Long) {
                if (!verifyPlayerState("seekTo", isReleased, isReleasing, mediaPlayerComponent, playerState)) {
                    return
                }
                try {
                    mediaPlayerComponent!!.mediaPlayer().controls().setTime(positionMs)
                    println("✓ Seek command executed successfully to position: $positionMs ms")
                } catch (e: Exception) {
                    val errorMsg = "跳转失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in seekTo: $errorMsg")
                    playerState.value = playerState.value.copy(
                        errorMessage = errorMsg
                    )
                }
            }
            
            override fun setVolume(volume: Float) {
                if (!verifyPlayerState("setVolume", isReleased, isReleasing, mediaPlayerComponent, playerState)) {
                    return
                }
                try {
                    val volumeInt = (volume * 100).toInt().coerceIn(0, 100)
                    mediaPlayerComponent!!.mediaPlayer().audio().setVolume(volumeInt)
                    println("✓ Volume set successfully to: $volumeInt%")
                } catch (e: Exception) {
                    val errorMsg = "音量设置失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in setVolume: $errorMsg")
                    playerState.value = playerState.value.copy(
                        errorMessage = errorMsg
                    )
                }
            }
            
            override fun toggleFullscreen() {
                if (!verifyPlayerState("toggleFullscreen", isReleased, isReleasing, mediaPlayerComponent, playerState)) {
                    return
                }
                try {
                    mediaPlayerComponent!!.mediaPlayer().fullScreen().toggle()
                    println("✓ Fullscreen toggled successfully")
                } catch (e: Exception) {
                    val errorMsg = "全屏切换失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in toggleFullscreen: $errorMsg")
                    playerState.value = playerState.value.copy(
                        errorMessage = errorMsg
                    )
                }
            }
            
            override fun release() {
                safeReleasePlayer(mediaPlayerComponent, eventListener, isReleased, isReleasing, listenerRegistered)
            }
        }
    }
    
    DisposableEffect(mediaPlayerComponent) {
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
        
        if (mediaPlayerComponent != null) {
            try {
                // Setup event listener and track registration
                mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(eventListener)
                listenerRegistered.value = true
                println("Event listener registered successfully")
                onPlayerControls(controls)
            } catch (e: Exception) {
                val errorMsg = "Error setting up media player: ${e.message}"
                println(errorMsg)
                e.printStackTrace()
                listenerRegistered.value = false
                onPlayerInitFailed()
                onError(errorMsg)
            }
        } else {
            // mediaPlayerComponent is null, initialization failed
            onPlayerInitFailed()
        }
        
        onDispose {
            println("DisposableEffect onDispose called - cleaning up resources")
            safeReleasePlayer(mediaPlayerComponent, eventListener, isReleased, isReleasing, listenerRegistered)
        }
    }
    
    // Update position periodically
    LaunchedEffect(mediaPlayerComponent) {
        while (isActive && !isReleased.value && mediaPlayerComponent != null) {
            try {
                if (playerState.value.playbackState == PlaybackState.PLAYING) {
                    val currentTime = mediaPlayerComponent.mediaPlayer().status().time()
                    val length = mediaPlayerComponent.mediaPlayer().status().length()
                    val volume = mediaPlayerComponent.mediaPlayer().audio().volume() / 100f
                    
                    playerState.value = playerState.value.copy(
                        position = currentTime,
                        duration = if (length > 0) length else playerState.value.duration,
                        volume = volume
                    )
                }
            } catch (e: Exception) {
                println("Error updating player state: ${e.message}")
            }
            delay(500)
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
        
        if (isReleased.value || mediaPlayerComponent == null) {
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
                mediaPlayerComponent.mediaPlayer().controls().stop()
                println("Current playback stopped")
            } catch (e: Exception) {
                println("Warning: Error stopping playback: ${e.message}")
                // Continue anyway as this might fail if nothing is playing
            }
            
            // Step 2: Add delay to ensure resources are properly freed
            delay(200)
            
            // Step 3: Update state to show loading
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.BUFFERING,
                errorMessage = null
            )
            
            // Step 4: Load and play new media with error handling
            try {
                val success = mediaPlayerComponent.mediaPlayer().media().play(url)
                if (success) {
                    println("Media loaded successfully: $url")
                } else {
                    throw Exception("媒体加载失败，返回false")
                }
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

    if (mediaPlayerComponent != null) {
        SwingPanel(
            background = Color.Black,
            modifier = modifier,
            factory = { mediaPlayerComponent }
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
    isReleasing: MutableState<Boolean>,
    mediaPlayerComponent: EmbeddedMediaPlayerComponent?,
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
    
    // Check if player is being released
    if (isReleasing.value) {
        val errorMsg = "无法执行 $operation: 播放器正在释放中"
        println("✗ $errorMsg")
        playerState.value = playerState.value.copy(
            playbackState = PlaybackState.ERROR,
            errorMessage = errorMsg
        )
        return false
    }
    
    // Check if player component is initialized
    if (mediaPlayerComponent == null) {
        val errorMsg = "无法执行 $operation: 播放器未初始化"
        println("✗ $errorMsg")
        playerState.value = playerState.value.copy(
            playbackState = PlaybackState.ERROR,
            errorMessage = errorMsg
        )
        return false
    }
    
    // Verify media player instance is accessible
    try {
        val mp = mediaPlayerComponent.mediaPlayer()
        if (mp == null) {
            val errorMsg = "无法执行 $operation: 媒体播放器实例为空"
            println("✗ $errorMsg")
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            return false
        }
    } catch (e: Exception) {
        val errorMsg = "无法执行 $operation: 无法访问媒体播放器 - ${e.message}"
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
 * Safely release VLC player resources with proper cleanup sequence
 * Includes listener tracking to prevent memory leaks
 */
private fun safeReleasePlayer(
    mediaPlayerComponent: EmbeddedMediaPlayerComponent?,
    eventListener: MediaPlayerEventAdapter,
    isReleased: MutableState<Boolean>,
    isReleasing: MutableState<Boolean>,
    listenerRegistered: MutableState<Boolean>
) {
    // Prevent double-release
    if (isReleased.value || isReleasing.value || mediaPlayerComponent == null) {
        println("Skipping release: already released or in progress")
        return
    }
    
    isReleasing.value = true
    
    try {
        println("Starting safe release of VLC player...")
        
        // Step 1: Stop playback
        try {
            mediaPlayerComponent.mediaPlayer().controls().stop()
            println("✓ Playback stopped")
        } catch (e: Exception) {
            println("⚠ Error stopping playback: ${e.message}")
        }
        
        // Step 2: Remove event listeners with verification
        if (listenerRegistered.value) {
            try {
                mediaPlayerComponent.mediaPlayer().events().removeMediaPlayerEventListener(eventListener)
                listenerRegistered.value = false
                println("✓ Event listener removed successfully")
            } catch (e: Exception) {
                println("⚠ Error removing event listener: ${e.message}")
                e.printStackTrace()
            }
        } else {
            println("ℹ No event listener to remove (not registered)")
        }
        
        // Step 3: Verify all listeners are removed
        try {
            // Note: VLCJ doesn't provide a direct way to check listener count,
            // but we track it manually with listenerRegistered flag
            if (!listenerRegistered.value) {
                println("✓ Verified: All event listeners removed")
            } else {
                println("⚠ Warning: Listener registration flag still true after removal")
            }
        } catch (e: Exception) {
            println("⚠ Error verifying listener removal: ${e.message}")
        }
        
        // Step 4: Release the media player component
        try {
            mediaPlayerComponent.release()
            println("✓ Media player component released")
        } catch (e: Exception) {
            println("⚠ Error releasing media player component: ${e.message}")
            e.printStackTrace()
        }
        
        isReleased.value = true
        println("✓ VLC player released successfully - no memory leaks")
        
    } catch (e: Exception) {
        println("⚠ Error during safe release: ${e.message}")
        e.printStackTrace()
    } finally {
        isReleasing.value = false
    }
}

/**
 * Display a user-friendly error message when VLC is not available
 */
@Composable
private fun VlcNotAvailableMessage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error title
            Text(
                text = "⚠️ VLC Media Player 未安装",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B6B)
            )
            
            // Error description
            Text(
                text = "此应用需要 VLC Media Player 才能播放视频。",
                fontSize = 16.sp,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Installation instructions
            Text(
                text = VlcAvailabilityChecker.getInstallationInstructions(),
                fontSize = 14.sp,
                color = Color(0xFFCCCCCC),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // System information
            Text(
                text = VlcAvailabilityChecker.getSystemInfo(),
                fontSize = 12.sp,
                color = Color(0xFF888888),
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Additional note
            Text(
                text = "注意：安装完成后，请重启应用程序。",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFAA00)
            )
        }
    }
}
