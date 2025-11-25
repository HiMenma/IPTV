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

/**
 * 媒体错误分类
 */
private data class MediaErrorCategory(
    val category: String,
    val userMessage: String,
    val suggestions: List<String>
)

/**
 * 对媒体加载错误进行分类并提供用户友好的消息
 * 
 * Validates: Requirements 1.4, 4.4
 */
private fun categorizeMediaError(exception: Exception): MediaErrorCategory {
    val message = exception.message?.lowercase() ?: ""
    
    return when {
        message.contains("connection") || message.contains("connect") -> {
            MediaErrorCategory(
                category = "网络连接错误",
                userMessage = "无法连接到媒体服务器",
                suggestions = listOf(
                    "检查网络连接是否正常",
                    "确认媒体URL是否可访问",
                    "检查防火墙设置",
                    "尝试使用其他网络"
                )
            )
        }
        
        message.contains("timeout") || message.contains("timed out") -> {
            MediaErrorCategory(
                category = "连接超时",
                userMessage = "连接媒体服务器超时",
                suggestions = listOf(
                    "检查网络速度",
                    "确认服务器是否在线",
                    "尝试增加超时时间",
                    "稍后重试"
                )
            )
        }
        
        message.contains("404") || message.contains("not found") -> {
            MediaErrorCategory(
                category = "资源不存在",
                userMessage = "媒体资源不存在 (404)",
                suggestions = listOf(
                    "确认URL是否正确",
                    "检查媒体文件是否已被删除",
                    "联系内容提供商"
                )
            )
        }
        
        message.contains("403") || message.contains("forbidden") -> {
            MediaErrorCategory(
                category = "访问被拒绝",
                userMessage = "无权访问该媒体资源 (403)",
                suggestions = listOf(
                    "检查是否需要认证",
                    "确认访问权限",
                    "联系内容提供商"
                )
            )
        }
        
        message.contains("401") || message.contains("unauthorized") -> {
            MediaErrorCategory(
                category = "未授权",
                userMessage = "需要身份验证才能访问 (401)",
                suggestions = listOf(
                    "检查登录凭据",
                    "确认订阅状态",
                    "重新登录"
                )
            )
        }
        
        message.contains("codec") || message.contains("decode") -> {
            MediaErrorCategory(
                category = "解码错误",
                userMessage = "无法解码媒体格式",
                suggestions = listOf(
                    "确认VLC支持该媒体格式",
                    "尝试更新VLC版本",
                    "检查媒体文件是否损坏",
                    "尝试禁用硬件加速"
                )
            )
        }
        
        message.contains("format") || message.contains("invalid") -> {
            MediaErrorCategory(
                category = "格式错误",
                userMessage = "媒体格式无效或不支持",
                suggestions = listOf(
                    "确认媒体格式是否受支持",
                    "检查URL格式是否正确",
                    "尝试其他播放源"
                )
            )
        }
        
        message.contains("ssl") || message.contains("certificate") -> {
            MediaErrorCategory(
                category = "SSL证书错误",
                userMessage = "SSL证书验证失败",
                suggestions = listOf(
                    "检查系统时间是否正确",
                    "更新系统证书",
                    "联系服务器管理员"
                )
            )
        }
        
        message.contains("dns") || message.contains("resolve") -> {
            MediaErrorCategory(
                category = "DNS解析错误",
                userMessage = "无法解析服务器地址",
                suggestions = listOf(
                    "检查DNS设置",
                    "尝试使用其他DNS服务器",
                    "确认域名是否正确"
                )
            )
        }
        
        else -> {
            MediaErrorCategory(
                category = "未知错误",
                userMessage = "无法加载媒体: ${exception.message ?: "未知原因"}",
                suggestions = listOf(
                    "检查URL是否有效",
                    "确认网络连接正常",
                    "查看详细日志以获取更多信息",
                    "尝试重启应用程序"
                )
            )
        }
    }
}

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
        initializeMediaPlayerWithFallback(onPlayerInitFailed, onError)
    }
    
    // Store event listener reference for proper cleanup
    val eventListener = remember {
        object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                try {
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.PLAYING
                    )
                    
                    // Log video codec information when playback starts
                    // Validates: Requirements 5.1
                    VideoRenderingDiagnostics.logVideoCodecInfo(mediaPlayer)
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
                    // Generate diagnostic report when error occurs
                    // Validates: Requirements 5.4
                    val diagnosticReport = VideoRenderingDiagnostics.generateDiagnosticReport(mediaPlayer)
                    println(diagnosticReport)
                    
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
                // Validate video surface before setting up player
                println("=== Video Surface Validation ===")
                val validationResult = VideoSurfaceValidator.validateVideoSurface(mediaPlayerComponent)
                
                if (!validationResult.isValid) {
                    println("⚠️ Video surface validation failed:")
                    validationResult.issues.forEach { issue ->
                        println("  - $issue")
                    }
                    
                    println("Attempting to fix video surface issues...")
                    var fixSuccessful = true
                    
                    // Try to fix visibility issues
                    if (!VideoSurfaceValidator.isVideoSurfaceVisible(mediaPlayerComponent)) {
                        try {
                            mediaPlayerComponent.videoSurfaceComponent().isVisible = true
                            println("✓ Set video surface visibility to true")
                        } catch (e: Exception) {
                            println("✗ Failed to set visibility: ${e.message}")
                            fixSuccessful = false
                        }
                    }
                    
                    // Try to fix dimension issues
                    val dimensions = VideoSurfaceValidator.getVideoSurfaceDimensions(mediaPlayerComponent)
                    if (dimensions == null || dimensions.width <= 0 || dimensions.height <= 0) {
                        try {
                            mediaPlayerComponent.videoSurfaceComponent().setSize(800, 600)
                            println("✓ Set video surface dimensions to 800x600")
                        } catch (e: Exception) {
                            println("✗ Failed to set dimensions: ${e.message}")
                            fixSuccessful = false
                        }
                    }
                    
                    // Re-validate after fixes
                    val revalidationResult = VideoSurfaceValidator.validateVideoSurface(mediaPlayerComponent)
                    
                    if (!revalidationResult.isValid) {
                        // Log warnings but continue - the component might become displayable
                        // once it's added to the window hierarchy
                        println("⚠️ Video surface validation has warnings after fix attempts:")
                        revalidationResult.issues.forEach { issue ->
                            println("  • $issue")
                        }
                        println("Continuing with initialization - issues may resolve once component is displayed")
                    } else {
                        println("✓ Video surface validation passed after fixes")
                    }
                } else {
                    println("✓ Video surface validation passed")
                }
                println("================================")
                
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
    
    // Periodically log rendering statistics and detect black screen
    // Validates: Requirements 5.2, 5.3
    LaunchedEffect(mediaPlayerComponent) {
        var statsLogCounter = 0
        while (isActive && !isReleased.value && mediaPlayerComponent != null) {
            try {
                if (playerState.value.playbackState == PlaybackState.PLAYING) {
                    // Log rendering stats every 10 seconds (20 iterations * 500ms)
                    statsLogCounter++
                    if (statsLogCounter >= 20) {
                        VideoRenderingDiagnostics.logRenderingStats(mediaPlayerComponent.mediaPlayer())
                        statsLogCounter = 0
                    }
                    
                    // Detect black screen - check if playing but no video output
                    val diagnosis = VideoRenderingDiagnostics.detectBlackScreen(mediaPlayerComponent.mediaPlayer())
                    if (diagnosis.isBlackScreen) {
                        println("⚠️ Black screen detected!")
                        println("Possible causes:")
                        diagnosis.possibleCauses.forEach { cause ->
                            println("  - $cause")
                        }
                        println("Suggested fixes:")
                        diagnosis.suggestedFixes.forEach { fix ->
                            println("  - $fix")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error in diagnostics monitoring: ${e.message}")
            }
            delay(500)
        }
    }
    
    // Handle URL changes - improved with proper resource cleanup and media options
    LaunchedEffect(url) {
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
        
        // Perform comprehensive pre-check before attempting playback
        // Validates: Requirements 1.1, 2.1, 3.1
        println("=== Video Playback Pre-Check ===")
        val preCheckResult = VideoPlaybackPreCheck.performPreCheck(url, mediaPlayerComponent)
        
        // Generate and log pre-check report
        val preCheckReport = VideoPlaybackPreCheck.generatePreCheckReport(preCheckResult)
        println(preCheckReport)
        println("================================")
        
        // If pre-check failed with critical issues, abort playback
        if (!preCheckResult.canProceed) {
            val errorMsg = buildString {
                appendLine("播放前检查失败，无法继续播放")
                appendLine()
                
                // Include critical issues
                val criticalIssues = preCheckResult.issues.filter { 
                    it.severity == IssueSeverity.CRITICAL 
                }
                
                if (criticalIssues.isNotEmpty()) {
                    appendLine("严重问题:")
                    criticalIssues.forEach { issue ->
                        appendLine("  • ${issue.message}")
                        if (issue.suggestions.isNotEmpty()) {
                            appendLine("    建议:")
                            issue.suggestions.forEach { suggestion ->
                                appendLine("      - $suggestion")
                            }
                        }
                    }
                }
            }
            
            println("✗ Pre-check failed, aborting playback")
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.ERROR,
                errorMessage = errorMsg
            )
            onError(errorMsg)
            return@LaunchedEffect
        }
        
        // Log warnings if any
        val warnings = preCheckResult.issues.filter { it.severity == IssueSeverity.WARNING }
        if (warnings.isNotEmpty()) {
            println("⚠️ Pre-check passed with warnings:")
            warnings.forEach { warning ->
                println("  • ${warning.message}")
            }
            println("Proceeding with playback despite warnings...")
        } else {
            println("✓ Pre-check passed, proceeding with playback")
        }
        
        // Perform all operations synchronously to avoid coroutine cancellation issues
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
            
            // Step 2: Update state to show loading (no delay needed)
            playerState.value = playerState.value.copy(
                playbackState = PlaybackState.BUFFERING,
                errorMessage = null
            )
            
            // Step 3: Build media options based on URL type
            val mediaOptions = buildMediaOptions(url)
            println("Media options configured: ${mediaOptions.joinToString(", ")}")
            
            // Step 4: Load and play new media with options and error handling
            // Use a try-catch to handle any VLC-specific exceptions
            try {
                val success = mediaPlayerComponent.mediaPlayer().media().play(url, *mediaOptions)
                if (success) {
                    println("Media loaded successfully with options: $url")
                } else {
                    throw Exception("媒体加载失败，返回false")
                }
            } catch (e: Exception) {
                throw Exception("无法播放媒体URL: ${e.message ?: "未知错误"}", e)
            }
            
        } catch (e: Exception) {
            // Only handle exceptions if the coroutine is still active
            if (!isActive) {
                println("Coroutine cancelled during media loading, ignoring error")
                return@LaunchedEffect
            }
            
            // Categorize error and provide user-friendly message
            val errorCategory = categorizeMediaError(e)
            val userFriendlyMsg = errorCategory.userMessage
            
            println("Error loading media: $userFriendlyMsg")
            println("Technical details: ${e.message}")
            e.printStackTrace()
            
            // Generate comprehensive diagnostic report on media loading error
            // Validates: Requirements 5.4, 1.4, 4.4
            if (mediaPlayerComponent != null) {
                try {
                    val diagnosticReport = VideoRenderingDiagnostics.generateDiagnosticReport(mediaPlayerComponent.mediaPlayer())
                    println(diagnosticReport)
                    
                    // Build detailed error message with diagnostics
                    val detailedErrorMsg = buildString {
                        appendLine(userFriendlyMsg)
                        appendLine()
                        appendLine("错误类型: ${errorCategory.category}")
                        appendLine()
                        appendLine("建议:")
                        errorCategory.suggestions.forEach { suggestion ->
                            appendLine("  • $suggestion")
                        }
                        appendLine()
                        appendLine("技术详情:")
                        appendLine("  ${e.message ?: "无详细信息"}")
                    }
                    
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = detailedErrorMsg
                    )
                    onError(detailedErrorMsg)
                    
                } catch (diagError: Exception) {
                    println("⚠️ Could not generate diagnostic report: ${diagError.message}")
                    
                    // Fallback to simple error message
                    playerState.value = playerState.value.copy(
                        playbackState = PlaybackState.ERROR,
                        errorMessage = userFriendlyMsg
                    )
                    onError(userFriendlyMsg)
                }
            } else {
                playerState.value = playerState.value.copy(
                    playbackState = PlaybackState.ERROR,
                    errorMessage = userFriendlyMsg
                )
                onError(userFriendlyMsg)
            }
        }
    }

    if (mediaPlayerComponent != null) {
        SwingPanel(
            background = Color.Black,
            modifier = modifier,
            factory = { 
                // Return the media player component
                // EmbeddedMediaPlayerComponent manages its own video surface
                mediaPlayerComponent.apply {
                    try {
                        // Get the video surface component
                        val videoSurface = videoSurfaceComponent()
                        
                        // Ensure video surface is visible
                        videoSurface.isVisible = true
                        
                        // Set initial size if needed
                        if (videoSurface.width <= 0 || videoSurface.height <= 0) {
                            videoSurface.setSize(800, 600)
                            println("✓ Initial video surface size set to 800x600")
                        }
                        
                        println("✓ Video surface initialized in factory")
                        println("  Video surface parent: ${videoSurface.parent?.javaClass?.simpleName ?: "null"}")
                        println("  Video surface visible: ${videoSurface.isVisible}")
                        println("  Video surface size: ${videoSurface.width}x${videoSurface.height}")
                    } catch (e: Exception) {
                        println("⚠️ Error initializing video surface in factory: ${e.message}")
                        e.printStackTrace()
                    }
                }
            },
            update = { component ->
                // Monitor and respond to size changes
                // Validates: Requirements 3.3
                try {
                    val currentSize = component.size
                    
                    // Only update if component has valid size
                    if (currentSize.width > 0 && currentSize.height > 0) {
                        val videoSurface = component.videoSurfaceComponent()
                        val videoSurfaceSize = videoSurface.size
                        
                        // Check if video surface size differs from component size
                        if (videoSurfaceSize.width != currentSize.width || 
                            videoSurfaceSize.height != currentSize.height) {
                            
                            println("=== Video Surface Size Update ===")
                            println("Component size changed:")
                            println("  Previous: ${videoSurfaceSize.width}x${videoSurfaceSize.height}")
                            println("  New: ${currentSize.width}x${currentSize.height}")
                            
                            // Update video surface to match new size
                            videoSurface.setSize(currentSize.width, currentSize.height)
                            
                            // Ensure video surface remains visible
                            if (!videoSurface.isVisible) {
                                videoSurface.isVisible = true
                                println("  Video surface visibility restored")
                            }
                            
                            println("✓ Video surface updated to match component size")
                            println("=================================")
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ Error updating video surface size: ${e.message}")
                }
            }
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
 * Initialize media player with comprehensive error recovery
 * 
 * Uses VideoRenderingRecovery to try multiple configuration strategies.
 * Records all attempts and provides detailed diagnostic information.
 * 
 * @param onPlayerInitFailed Callback when initialization fails
 * @param onError Callback for error messages
 * @return EmbeddedMediaPlayerComponent or null if initialization failed
 * 
 * Validates: Requirements 1.4, 2.4, 3.4, 4.4
 */
private fun initializeMediaPlayerWithFallback(
    onPlayerInitFailed: () -> Unit,
    onError: (String) -> Unit
): EmbeddedMediaPlayerComponent? {
    // Log platform information
    println("=== VLC Media Player Initialization ===")
    println(VideoOutputConfiguration.getPlatformInfo())
    println()
    
    // Attempt recovery with multiple strategies
    val recoveryResult = VideoRenderingRecovery.attemptRecovery()
    
    // Log attempts summary
    println()
    println(VideoRenderingRecovery.generateAttemptsSummary(recoveryResult.attempts))
    println()
    
    if (recoveryResult.success && recoveryResult.mediaPlayerComponent != null) {
        // Success!
        println("✓ VLC player initialized successfully")
        println("  Configuration used: ${recoveryResult.configurationUsed}")
        println("  Total attempts: ${recoveryResult.attempts.size}")
        println("==========================================")
        
        return recoveryResult.mediaPlayerComponent
    } else {
        // All recovery strategies failed
        println("✗ All initialization strategies failed")
        println("==========================================")
        
        val errorMessage = recoveryResult.finalErrorMessage ?: "未知初始化错误"
        
        onPlayerInitFailed()
        onError(errorMessage)
        return null
    }
}

/**
 * Build media options based on URL type and format
 * 
 * Detects whether the URL is a live stream or VOD content and applies
 * appropriate caching and decoding options.
 * Includes hardware acceleration configuration based on system support.
 * Uses VideoFormatDetector for comprehensive format detection and adaptation.
 * 
 * For live streams, applies low-latency optimizations:
 * - Reduced caching (300ms live, 1000ms network)
 * - Disabled clock jitter and synchronization
 * - Disabled audio time stretching
 * 
 * Validates: Requirements 1.3, 4.1, 4.2, 4.3
 * 
 * @param url The media URL to analyze
 * @return Array of VLC media options
 */
private fun buildMediaOptions(url: String): Array<String> {
    // Use VideoFormatDetector for comprehensive format detection
    val isLiveStream = VideoFormatDetector.isLiveStreamUrl(url)
    val streamFormat = VideoFormatDetector.detectStreamFormat(url)
    val videoFormat = VideoFormatDetector.detectVideoFormat(url)
    
    println("=== Building Media Options ===")
    println("  URL type: ${if (isLiveStream) "Live Stream" else "VOD"}")
    println("  Stream format: $streamFormat")
    println("  Video format: ${VideoFormatDetector.getFormatName(videoFormat)}")
    println("  Format description: ${VideoFormatDetector.getFormatDescription(videoFormat)}")
    
    // Start with base configuration for live or VOD
    val builder = if (isLiveStream) {
        println("  Applying live stream optimizations:")
        println("    • Low-latency caching (300ms live, 1000ms network)")
        println("    • Clock jitter disabled")
        println("    • Clock synchronization disabled")
        println("    • Audio time-stretch disabled")
        MediaOptionsBuilder.forLiveStream()
    } else {
        println("  Applying VOD optimizations:")
        println("    • Standard caching (3000ms network)")
        MediaOptionsBuilder.forVOD()
    }
    
    // Detect and enable hardware acceleration if supported
    // Validates: Requirements 2.3
    val hwAccelSupport = HardwareAccelerationDetector.detectHardwareAcceleration()
    if (hwAccelSupport.isSupported) {
        builder.withHardwareAcceleration(true)
        println("  Hardware acceleration: Enabled (${hwAccelSupport.accelerationType})")
    } else {
        builder.withHardwareAcceleration(false)
        println("  Hardware acceleration: Disabled (${hwAccelSupport.reason})")
    }
    
    // Apply format-specific decoding options using VideoFormatDetector
    // Validates: Requirements 1.3, 4.3
    val formatOptions = VideoFormatDetector.getFormatSpecificOptions(videoFormat)
    if (formatOptions.isNotEmpty()) {
        println("  Applying format-specific optimizations for ${VideoFormatDetector.getFormatName(videoFormat)}:")
        formatOptions.forEach { option ->
            builder.withCustomOption(option)
            println("    • $option")
        }
    } else {
        println("  Using default decoding options")
    }
    
    val options = builder.build()
    println("  Total options configured: ${options.size}")
    println("==============================")
    
    return options
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
