package com.menmapro.iptv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.menmapro.iptv.data.model.Channel
import com.menmapro.iptv.ui.components.*

data class PlayerScreen(val channel: Channel) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val playerState = remember { mutableStateOf(PlayerState()) }
        var playerControls by remember { mutableStateOf<PlayerControls?>(null) }
        var showErrorScreen by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var retryCount by remember { mutableStateOf(0) }
        var isFullscreen by remember { mutableStateOf(false) }

        // Validate URL before attempting to play
        val urlValidationError = remember(channel.url) {
            when {
                channel.url.isBlank() -> "播放地址为空"
                !channel.url.startsWith("http://") && 
                !channel.url.startsWith("https://") && 
                !channel.url.startsWith("rtsp://") &&
                !channel.url.startsWith("rtmp://") -> "不支持的播放地址格式"
                else -> null
            }
        }

        // Monitor player state for errors
        LaunchedEffect(playerState.value.playbackState, playerState.value.errorMessage) {
            if (playerState.value.playbackState == PlaybackState.ERROR) {
                showErrorScreen = true
                errorMessage = playerState.value.errorMessage ?: "播放出错"
            }
        }

        // Show error if URL validation fails
        LaunchedEffect(urlValidationError) {
            if (urlValidationError != null) {
                showErrorScreen = true
                errorMessage = urlValidationError
            }
        }

        Scaffold(
            topBar = {
                if (!isFullscreen) {
                    TopAppBar(
                        title = { Text(channel.name) },
                        navigationIcon = {
                            IconButton(onClick = { 
                                playerControls?.release()
                                navigator.pop()
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                            }
                        }
                    )
                }
            }
        ) { padding ->
            if (showErrorScreen) {
                ErrorScreen(
                    message = errorMessage ?: "未知错误",
                    onRetry = {
                        // Reset error state and retry
                        showErrorScreen = false
                        errorMessage = null
                        retryCount++
                        playerState.value = PlayerState()
                    },
                    onBack = {
                        playerControls?.release()
                        navigator.pop()
                    },
                    modifier = Modifier.padding(padding).fillMaxSize()
                )
            } else if (urlValidationError == null) {
                // Only show VideoPlayer if URL is valid
                // The VideoPlayer component itself handles errors through playerState
                Box(
                    modifier = if (isFullscreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.padding(padding).fillMaxSize()
                    }
                ) {
                    VideoPlayer(
                        url = channel.url,
                        modifier = Modifier.fillMaxSize(),
                        playerState = playerState,
                        onPlayerControls = { controls ->
                            playerControls = controls
                        },
                        onError = { error ->
                            // Handle error callback
                            showErrorScreen = true
                            errorMessage = error
                        },
                        onPlayerInitFailed = {
                            // Handle player initialization failure
                            showErrorScreen = true
                            errorMessage = errorMessage ?: "播放器初始化失败"
                        },
                        isFullscreen = isFullscreen
                    )
                    
                    // Loading overlay
                    if (playerState.value.playbackState == PlaybackState.BUFFERING) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = Color.White)
                                Text(
                                    text = "正在加载...",
                                    color = Color.White,
                                    style = MaterialTheme.typography.body1
                                )
                            }
                        }
                    }
                    
                    // Fullscreen controls
                    if (!showErrorScreen) {
                        var showControls by remember { mutableStateOf(true) }
                        var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
                        
                        // Auto-hide controls after 3 seconds in fullscreen
                        LaunchedEffect(isFullscreen, lastInteractionTime) {
                            if (isFullscreen) {
                                kotlinx.coroutines.delay(3000)
                                if (System.currentTimeMillis() - lastInteractionTime >= 3000) {
                                    showControls = false
                                }
                            } else {
                                showControls = true
                            }
                        }
                        
                        if (isFullscreen) {
                            // 使用增强的全屏控制（仅Android）
                            FullscreenControlsWrapper(
                                playerState = playerState.value,
                                playerControls = playerControls,
                                showControls = showControls,
                                onToggleControls = {
                                    showControls = true
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onExitFullscreen = {
                                    playerControls?.toggleFullscreen()
                                    isFullscreen = false
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // 非全屏模式：显示简单的全屏按钮
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) {
                                        // 点击切换控制显示
                                    }
                            ) {
                                Button(
                                    onClick = { 
                                        playerControls?.toggleFullscreen()
                                        isFullscreen = true
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .alpha(0.7f),
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color.Black.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        text = "全屏",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Error screen component that displays error messages and action buttons
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            // Error icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colors.error
            )
            
            // Error title
            Text(
                text = "播放失败",
                style = MaterialTheme.typography.h5,
                color = MaterialTheme.colors.onBackground,
                textAlign = TextAlign.Center
            )
            
            // Error message
            Text(
                text = message,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Retry button
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重试")
                }
                
                // Back button
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("返回频道列表")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Help text
            Text(
                text = "提示：请检查网络连接或尝试其他频道",
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}
