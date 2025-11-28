package com.menmapro.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menmapro.iptv.player.ffmpeg.FFmpegPlayerEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.awt.Canvas

/**
 * FFmpeg-based VideoPlayer Composable for Desktop
 * 
 * This implementation uses FFmpegPlayerEngine to provide video playback functionality
 * while maintaining complete API compatibility with the existing VideoPlayer interface.
 * 
 * Features:
 * - FFmpeg-based decoding and rendering
 * - Hardware acceleration support
 * - Audio-video synchronization
 * - Live stream optimization
 * - Full playback controls (play, pause, seek, volume)
 * - Fullscreen support
 * - Comprehensive error handling
 * 
 * Requirements:
 * - 9.1: Same function signature as existing VideoPlayer
 * - 9.2: Same PlayerControls interface
 * - 9.3: Same PlayerState updates
 * - 9.4: Same error callback interface
 * - 9.5: Works without code modifications
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
fun FFmpegVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit,
    onError: (String) -> Unit = {},
    onPlayerInitFailed: () -> Unit = {},
    isFullscreen: Boolean = false
) {
    // Track release state to prevent double-release
    val isReleased = remember { mutableStateOf(false) }
    val isReleasing = remember { mutableStateOf(false) }
    
    // Track if component is ready for playback
    val componentReady = remember { mutableStateOf(false) }
    
    // Create Canvas for video rendering
    val canvas = remember { Canvas() }
    
    // Create FFmpeg player engine
    val playerEngine = remember {
        FFmpegPlayerEngine(
            onStateChange = { newState ->
                // Update player state when engine state changes
                playerState.value = newState
            },
            onError = { errorMessage ->
                // Forward errors to callback
                onError(errorMessage)
            }
        )
    }
    
    // Create PlayerControls implementation
    val controls = remember(playerEngine) {
        object : PlayerControls {
            override fun play() {
                if (!verifyPlayerState("play", isReleased, isReleasing, playerEngine, playerState)) {
                    return
                }
                try {
                    // FFmpegPlayerEngine.play() is called in LaunchedEffect when URL changes
                    // This method is for resuming paused playback
                    playerEngine.resume()
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
                if (!verifyPlayerState("pause", isReleased, isReleasing, playerEngine, playerState)) {
                    return
                }
                try {
                    playerEngine.pause()
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
                if (!verifyPlayerState("seekTo", isReleased, isReleasing, playerEngine, playerState)) {
                    return
                }
                try {
                    playerEngine.seekTo(positionMs)
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
                if (!verifyPlayerState("setVolume", isReleased, isReleasing, playerEngine, playerState)) {
                    return
                }
                try {
                    playerEngine.setVolume(volume)
                    println("✓ Volume set successfully to: ${(volume * 100).toInt()}%")
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
                if (!verifyPlayerState("toggleFullscreen", isReleased, isReleasing, playerEngine, playerState)) {
                    return
                }
                try {
                    if (playerEngine.isFullscreen()) {
                        playerEngine.exitFullscreen()
                    } else {
                        playerEngine.enterFullscreen()
                    }
                    println("✓ Fullscreen toggled successfully")
                } catch (e: Exception) {
                    val errorMsg = "全屏切换失败: ${e.message ?: "未知错误"}"
                    println("✗ Error in toggleFullscreen: $errorMsg")
                    playerState.value = playerState.value.copy(
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                }
            }
            
            override fun release() {
                safeReleasePlayer(playerEngine, isReleased, isReleasing)
            }
        }
    }
    
    // Setup player controls callback
    DisposableEffect(playerEngine) {
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
        
        try {
            println("=== FFmpeg VideoPlayer Initialization ===")
            println("URL: $url")
            println("Fullscreen: $isFullscreen")
            
            // Provide controls to caller
            onPlayerControls(controls)
            println("✓ Player controls provided")
            println("==========================================")
            
        } catch (e: Exception) {
            val errorMsg = "Error setting up FFmpeg player: ${e.message}"
            println(errorMsg)
            e.printStackTrace()
            onPlayerInitFailed()
            onError(errorMsg)
        }
        
        onDispose {
            println("DisposableEffect onDispose called - cleaning up resources")
            safeReleasePlayer(playerEngine, isReleased, isReleasing)
        }
    }
    
    // Handle URL changes and start playback
    LaunchedEffect(url, componentReady.value) {
        // Wait for component to be ready
        if (!componentReady.value) {
            println("Waiting for component to be ready before loading media...")
            return@LaunchedEffect
        }
        
        // Validate URL first
        if (url.isBlank()) {
            val errorMsg = "无效的播放地址"
            onError(errorMsg)
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            return@LaunchedEffect
        }
        
        if (isReleased.value) {
            val errorMsg = "播放器未初始化"
            println("Cannot load URL: player is released")
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            onError(errorMsg)
            return@LaunchedEffect
        }
        
        println("✓ Component is ready, proceeding with media loading")
        
        try {
            println("Loading new URL: $url")
            
            // Start playback with FFmpeg engine
            playerEngine.play(url, canvas)
            
            println("Media loaded successfully: $url")
            
        } catch (e: Exception) {
            // Only handle exceptions if the coroutine is still active
            if (!isActive) {
                println("Coroutine cancelled during media loading, ignoring error")
                return@LaunchedEffect
            }
            
            val errorMsg = "无法播放媒体: ${e.message ?: "未知错误"}"
            println("Error loading media: $errorMsg")
            e.printStackTrace()
            
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            onError(errorMsg)
        }
    }
    
    // Handle fullscreen mode changes
    LaunchedEffect(isFullscreen) {
        if (!componentReady.value || isReleased.value) {
            return@LaunchedEffect
        }
        
        try {
            if (isFullscreen) {
                playerEngine.enterFullscreen(canvas)
            } else {
                playerEngine.exitFullscreen(canvas)
            }
        } catch (e: Exception) {
            println("Error handling fullscreen change: ${e.message}")
        }
    }
    
    // Render the video player using SwingPanel with Canvas
    SwingPanel(
        background = Color.Black,
        modifier = modifier,
        factory = {
            // Return the Canvas for video rendering
            canvas.apply {
                try {
                    // Set initial size
                    setSize(800, 600)
                    
                    // Ensure canvas is visible
                    isVisible = true
                    
                    println("✓ Canvas initialized in factory")
                    println("  Canvas size: ${width}x${height}")
                    println("  Canvas visible: $isVisible")
                    
                    // Mark component as ready after a short delay to ensure it's fully displayed
                    javax.swing.SwingUtilities.invokeLater {
                        Thread.sleep(100) // Give Swing time to fully initialize
                        componentReady.value = true
                        println("✓ Component marked as ready for playback")
                    }
                } catch (e: Exception) {
                    println("⚠️ Error initializing canvas in factory: ${e.message}")
                    e.printStackTrace()
                }
            }
        },
        update = { canvasComponent ->
            // Monitor and respond to size changes
            try {
                val currentSize = canvasComponent.size
                
                // Only update if component has valid size
                if (currentSize.width > 0 && currentSize.height > 0) {
                    // Check if canvas size differs from component size
                    if (canvasComponent.width != currentSize.width || 
                        canvasComponent.height != currentSize.height) {
                        
                        println("=== Canvas Size Update ===")
                        println("Canvas size changed:")
                        println("  Previous: ${canvasComponent.width}x${canvasComponent.height}")
                        println("  New: ${currentSize.width}x${currentSize.height}")
                        
                        // Update canvas to match new size
                        canvasComponent.setSize(currentSize.width, currentSize.height)
                        
                        // Notify player engine of size change
                        playerEngine.handleSizeChange()
                        
                        println("✓ Canvas updated to match component size")
                        println("==========================")
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Error updating canvas size: ${e.message}")
            }
        }
    )
}

/**
 * Verify player state before executing operations
 * Returns true if player is ready for operations, false otherwise
 */
private fun verifyPlayerState(
    operation: String,
    isReleased: MutableState<Boolean>,
    isReleasing: MutableState<Boolean>,
    playerEngine: FFmpegPlayerEngine,
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
    
    println("✓ Player state verified for operation: $operation")
    return true
}

/**
 * Safely release FFmpeg player resources with proper cleanup sequence
 */
private fun safeReleasePlayer(
    playerEngine: FFmpegPlayerEngine,
    isReleased: MutableState<Boolean>,
    isReleasing: MutableState<Boolean>
) {
    // Prevent double-release
    if (isReleased.value || isReleasing.value) {
        println("Skipping release: already released or in progress")
        return
    }
    
    isReleasing.value = true
    
    try {
        println("Starting safe release of FFmpeg player...")
        
        // Release the player engine
        playerEngine.release()
        
        isReleased.value = true
        println("✓ FFmpeg player released successfully")
        
    } catch (e: Exception) {
        println("⚠ Error during safe release: ${e.message}")
        e.printStackTrace()
    } finally {
        isReleasing.value = false
    }
}
