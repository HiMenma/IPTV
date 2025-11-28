package com.menmapro.iptv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menmapro.iptv.player.libmpv.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * libmpv-based VideoPlayer Composable for Desktop
 * 
 * This implementation uses LibmpvPlayerEngine to provide video playback functionality
 * while maintaining complete API compatibility with the existing VideoPlayer interface.
 * 
 * Features:
 * - libmpv-based decoding and rendering
 * - Hardware acceleration support
 * - Excellent format and codec support
 * - Reliable streaming (HLS, HTTP, RTSP, etc.)
 * - Full playback controls (play, pause, seek, volume)
 * - Fullscreen support
 * - Comprehensive error handling
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
fun LibmpvVideoPlayer(
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
    
    // Create libmpv player engine
    val playerEngine = remember {
        try {
            LibmpvPlayerEngine()
        } catch (e: LibmpvException.LibraryNotFoundError) {
            println("libmpv not available: ${e.message}")
            onPlayerInitFailed()
            onError(e.message ?: "libmpv not available")
            null
        } catch (e: Exception) {
            println("Error creating libmpv engine: ${e.message}")
            onPlayerInitFailed()
            onError("Failed to create player: ${e.message}")
            null
        }
    }
    
    // Create frame renderer (will be initialized after engine is ready)
    val frameRenderer = remember(playerEngine) {
        playerEngine?.let { LibmpvFrameRenderer(it) }
    }
    
    // Current frame to display
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    
    // Create PlayerControls implementation
    val controls = remember(playerEngine) {
        if (playerEngine == null) {
            null
        } else {
            object : PlayerControls {
                override fun play() {
                    if (isReleased.value || isReleasing.value) {
                        println("Cannot play: player is released or releasing")
                        return
                    }
                    try {
                        playerEngine.play()
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.PLAYING
                        )
                        println("✓ Play command executed successfully")
                    } catch (e: Exception) {
                        val errorMsg = "Play failed: ${e.message ?: "Unknown error"}"
                        println("✗ Error in play: $errorMsg")
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.ERROR,
                            errorMessage = errorMsg
                        )
                        onError(errorMsg)
                    }
                }
                
                override fun pause() {
                    if (isReleased.value || isReleasing.value) {
                        println("Cannot pause: player is released or releasing")
                        return
                    }
                    try {
                        playerEngine.pause()
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.PAUSED
                        )
                        println("✓ Pause command executed successfully")
                    } catch (e: Exception) {
                        val errorMsg = "Pause failed: ${e.message ?: "Unknown error"}"
                        println("✗ Error in pause: $errorMsg")
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.ERROR,
                            errorMessage = errorMsg
                        )
                        onError(errorMsg)
                    }
                }
                
                override fun seekTo(positionMs: Long) {
                    if (isReleased.value || isReleasing.value) {
                        println("Cannot seek: player is released or releasing")
                        return
                    }
                    try {
                        val positionSec = positionMs / 1000.0
                        playerEngine.seek(positionSec)
                        println("✓ Seek command executed successfully to position: $positionMs ms")
                    } catch (e: Exception) {
                        val errorMsg = "Seek failed: ${e.message ?: "Unknown error"}"
                        println("✗ Error in seekTo: $errorMsg")
                        playerState.value = playerState.value.copy(
                            errorMessage = errorMsg
                        )
                        onError(errorMsg)
                    }
                }
                
                override fun setVolume(volume: Float) {
                    if (isReleased.value || isReleasing.value) {
                        println("Cannot set volume: player is released or releasing")
                        return
                    }
                    try {
                        val volumePercent = (volume * 100).toInt().coerceIn(0, 100)
                        playerEngine.setVolume(volumePercent)
                        playerState.value = playerState.value.copy(
                            volume = volume
                        )
                        println("✓ Volume set successfully to: $volumePercent%")
                    } catch (e: Exception) {
                        val errorMsg = "Volume set failed: ${e.message ?: "Unknown error"}"
                        println("✗ Error in setVolume: $errorMsg")
                        playerState.value = playerState.value.copy(
                            errorMessage = errorMsg
                        )
                        onError(errorMsg)
                    }
                }
                
                override fun toggleFullscreen() {
                    if (isReleased.value || isReleasing.value) {
                        println("Cannot toggle fullscreen: player is released or releasing")
                        return
                    }
                    try {
                        playerState.value = playerState.value.copy(
                            isFullscreen = !playerState.value.isFullscreen
                        )
                        println("✓ Fullscreen toggled successfully")
                    } catch (e: Exception) {
                        val errorMsg = "Fullscreen toggle failed: ${e.message ?: "Unknown error"}"
                        println("✗ Error in toggleFullscreen: $errorMsg")
                        playerState.value = playerState.value.copy(
                            errorMessage = errorMsg
                        )
                        onError(errorMsg)
                    }
                }
                
                override fun release() {
                    if (isReleased.value || isReleasing.value) {
                        println("Player already released or releasing")
                        return
                    }
                    
                    isReleasing.value = true
                    try {
                        println("Releasing libmpv player...")
                        playerEngine.destroy()
                        isReleased.value = true
                        println("✓ Player released successfully")
                    } catch (e: Exception) {
                        println("✗ Error releasing player: ${e.message}")
                    } finally {
                        isReleasing.value = false
                    }
                }
            }
        }
    }
    
    // Setup player controls callback and initialization
    DisposableEffect(playerEngine) {
        if (playerEngine == null) {
            println("libmpv player engine is null, cannot initialize")
            return@DisposableEffect onDispose {}
        }
        
        // Validate URL before proceeding
        if (url.isBlank()) {
            val errorMsg = "Invalid playback URL"
            onError(errorMsg)
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            return@DisposableEffect onDispose {}
        }
        
        try {
            println("=== libmpv VideoPlayer Initialization ===")
            println("URL: $url")
            println("Fullscreen: $isFullscreen")
            
            // Initialize the engine
            val initialized = playerEngine.initialize()
            if (!initialized) {
                val errorMsg = "Failed to initialize libmpv engine"
                println(errorMsg)
                onPlayerInitFailed()
                onError(errorMsg)
                return@DisposableEffect onDispose {}
            }
            
            // Set up event callback
            playerEngine.setEventCallback { event ->
                when (event) {
                    is LibmpvEvent.StartFile -> {
                        println("Event: Start file")
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.BUFFERING
                        )
                    }
                    is LibmpvEvent.FileLoaded -> {
                        println("Event: File loaded")
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.PLAYING
                        )
                    }
                    is LibmpvEvent.EndFile -> {
                        println("Event: End file - reason: ${event.reason}, error code: ${event.error}")
                        if (event.reason == EndFileReason.ERROR) {
                            val errorMsg = if (event.error != 0) {
                                val errorStr = try {
                                    playerEngine.getBindings()?.mpv_error_string(event.error) ?: "Unknown error"
                                } catch (e: Exception) {
                                    "Unknown error"
                                }
                                "Playback failed: $errorStr (code: ${event.error})"
                            } else {
                                "Playback failed with unknown error"
                            }
                            println("Error details: $errorMsg")
                            playerState.value = playerState.value.copy(
                                playbackState = PlaybackState.ERROR,
                                errorMessage = errorMsg
                            )
                            onError(errorMsg)
                        } else {
                            playerState.value = playerState.value.copy(
                                playbackState = PlaybackState.ENDED
                            )
                        }
                    }
                    is LibmpvEvent.Error -> {
                        println("Event: Error - ${event.message}")
                        playerState.value = playerState.value.copy(
                            playbackState = PlaybackState.ERROR,
                            errorMessage = event.message
                        )
                        onError(event.message)
                    }
                    is LibmpvEvent.LogMessage -> {
                        // 只打印错误和警告级别的日志
                        if (event.level in listOf("error", "warn", "fatal")) {
                            println("libmpv [${event.level}] ${event.prefix}: ${event.message.trim()}")
                        }
                    }
                    else -> {
                        // Other events
                    }
                }
            }
            
            // Set up error callback
            playerEngine.setErrorCallback { exception ->
                val errorMsg = when (exception) {
                    is LibmpvException.PlaybackError -> "Playback error: ${exception.message}"
                    is LibmpvException.InitializationError -> "Initialization error: ${exception.message}"
                    is LibmpvException.ConfigurationError -> "Configuration error: ${exception.message}"
                    else -> "Error: ${exception.message}"
                }
                println("Error callback: $errorMsg")
                playerState.value = playerState.value.copy(
                    playbackState = PlaybackState.ERROR,
                    errorMessage = errorMsg
                )
                onError(errorMsg)
            }
            
            // TODO: Initialize frame renderer after engine is ready
            // Currently disabled because we're using gpu video output (separate window)
            // frameRenderer?.let { renderer ->
            //     try {
            //         if (renderer.initialize()) {
            //             println("✓ Frame renderer initialized successfully")
            //         } else {
            //             println("✗ Frame renderer initialization returned false")
            //         }
            //     } catch (e: Exception) {
            //         println("✗ Failed to initialize frame renderer: ${e.message}")
            //         e.printStackTrace()
            //     }
            // }
            
            // Provide controls to caller
            controls?.let { onPlayerControls(it) }
            println("✓ Player controls provided")
            println("==========================================")
            
        } catch (e: Exception) {
            val errorMsg = "Error setting up libmpv player: ${e.message}"
            println(errorMsg)
            e.printStackTrace()
            onPlayerInitFailed()
            onError(errorMsg)
        }
        
        onDispose {
            println("DisposableEffect onDispose called - cleaning up resources")
            if (!isReleased.value && !isReleasing.value) {
                isReleasing.value = true
                try {
                    playerEngine.destroy()
                    isReleased.value = true
                } catch (e: Exception) {
                    println("Error in dispose: ${e.message}")
                } finally {
                    isReleasing.value = false
                }
            }
        }
    }
    
    // Handle URL changes and start playback
    LaunchedEffect(url, playerEngine) {
        if (playerEngine == null || !playerEngine.isInitialized()) {
            println("Player not initialized, cannot load URL")
            return@LaunchedEffect
        }
        
        // Validate URL first
        if (url.isBlank()) {
            val errorMsg = "Invalid playback URL"
            onError(errorMsg)
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            return@LaunchedEffect
        }
        
        if (isReleased.value) {
            val errorMsg = "Player not initialized"
            println("Cannot load URL: player is released")
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            onError(errorMsg)
            return@LaunchedEffect
        }
        
        try {
            println("Loading new URL: $url")
            
            // Load and play the file
            playerEngine.loadFile(url)
            
            println("Media loaded successfully: $url")
            
        } catch (e: Exception) {
            // Only handle exceptions if the coroutine is still active
            if (!isActive) {
                println("Coroutine cancelled during media loading, ignoring error")
                return@LaunchedEffect
            }
            
            val errorMsg = "Cannot play media: ${e.message ?: "Unknown error"}"
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
    LaunchedEffect(playerEngine) {
        if (playerEngine == null) return@LaunchedEffect
        
        while (isActive && !isReleased.value) {
            try {
                if (playerEngine.isInitialized()) {
                    val position = playerEngine.getPosition()
                    val duration = playerEngine.getDuration()
                    val isPaused = playerEngine.isPaused()
                    
                    playerState.value = playerState.value.copy(
                        position = (position * 1000).toLong(),
                        duration = (duration * 1000).toLong(),
                        playbackState = if (isPaused) PlaybackState.PAUSED else playerState.value.playbackState
                    )
                }
            } catch (e: Exception) {
                // Ignore errors during state updates
            }
            
            delay(100) // Update every 100ms
        }
    }
    
    // Frame rendering loop
    LaunchedEffect(playerEngine, frameRenderer) {
        if (playerEngine == null || frameRenderer == null) return@LaunchedEffect
        
        while (isActive && !isReleased.value) {
            try {
                if (playerEngine.isInitialized()) {
                    val frame = frameRenderer.acquireFrame()
                    if (frame != null) {
                        currentFrame = frame
                    }
                }
            } catch (e: Exception) {
                // Ignore frame rendering errors
            }
            
            delay(16) // ~60 FPS
        }
    }
    
    // Render the video player
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (playerEngine == null) {
            // Show error message if player engine couldn't be created
            Text(
                text = "libmpv not available\nPlease install libmpv",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            // Render video frame
            currentFrame?.let { frame ->
                Image(
                    bitmap = frame,
                    contentDescription = "Video frame",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } ?: run {
                // Show loading message if no frame yet
                if (playerState.value.playbackState == PlaybackState.BUFFERING) {
                    Text(
                        text = "Loading...",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
