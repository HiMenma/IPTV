package com.menmapro.iptv.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 全屏播放控制组件
 * 包含进度条、音量控制、亮度控制
 */
@Composable
fun FullscreenControls(
    playerState: PlayerState,
    playerControls: PlayerControls?,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    // 音量和亮度状态
    var showVolumeIndicator by remember { mutableStateOf(false) }
    var showBrightnessIndicator by remember { mutableStateOf(false) }
    var currentVolume by remember { mutableStateOf(0f) }
    var currentBrightness by remember { mutableStateOf(0f) }
    
    // 获取音量管理器
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    
    // 初始化音量和亮度
    LaunchedEffect(Unit) {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
        activity?.window?.attributes?.let { attrs ->
            currentBrightness = if (attrs.screenBrightness < 0) {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
            } else {
                attrs.screenBrightness
            }
        }
    }
    
    // 隐藏指示器的定时器
    LaunchedEffect(showVolumeIndicator) {
        if (showVolumeIndicator) {
            kotlinx.coroutines.delay(1000)
            showVolumeIndicator = false
        }
    }
    
    LaunchedEffect(showBrightnessIndicator) {
        if (showBrightnessIndicator) {
            kotlinx.coroutines.delay(1000)
            showBrightnessIndicator = false
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 左侧：亮度控制区域
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .align(Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        activity?.window?.let { window ->
                            val delta = -dragAmount / size.height
                            currentBrightness = (currentBrightness + delta).coerceIn(0f, 1f)
                            
                            val layoutParams = window.attributes
                            layoutParams.screenBrightness = currentBrightness
                            window.attributes = layoutParams
                            
                            showBrightnessIndicator = true
                        }
                    }
                }
        )
        
        // 中间：点击显示/隐藏控制
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.4f)
                .align(Alignment.Center)
        )
        
        // 右侧：音量控制区域
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val delta = -dragAmount / size.height
                        currentVolume = (currentVolume + delta).coerceIn(0f, 1f)
                        
                        val newVolume = (currentVolume * maxVolume).roundToInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                        playerControls?.setVolume(currentVolume)
                        
                        showVolumeIndicator = true
                    }
                }
        )
        
        // 音量指示器
        if (showVolumeIndicator) {
            VolumeIndicator(
                volume = currentVolume,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        
        // 亮度指示器
        if (showBrightnessIndicator) {
            BrightnessIndicator(
                brightness = currentBrightness,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        
        // 播放控制和进度条
        if (showControls) {
            PlaybackControls(
                playerState = playerState,
                playerControls = playerControls,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * 音量指示器
 */
@Composable
fun VolumeIndicator(
    volume: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(32.dp)
            .background(Color.Black.copy(alpha = 0.7f), shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow, // 使用通用图标
                contentDescription = "音量",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "${(volume * 100).roundToInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

/**
 * 亮度指示器
 */
@Composable
fun BrightnessIndicator(
    brightness: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(32.dp)
            .background(Color.Black.copy(alpha = 0.7f), shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow, // 使用通用图标
                contentDescription = "亮度",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = "${(brightness * 100).roundToInt()}%",
                color = Color.White,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

/**
 * 播放控制和进度条
 */
@Composable
fun PlaybackControls(
    playerState: PlayerState,
    playerControls: PlayerControls?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        // 进度条
        if (playerState.duration > 0) {
            val progress = if (playerState.duration > 0) {
                playerState.position.toFloat() / playerState.duration.toFloat()
            } else {
                0f
            }
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = { newProgress ->
                        val newPosition = (newProgress * playerState.duration).toLong()
                        playerControls?.seekTo(newPosition)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colors.primary,
                        activeTrackColor = MaterialTheme.colors.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(playerState.position),
                        color = Color.White,
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = formatTime(playerState.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
        
        // 播放/暂停按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (playerState.playbackState == PlaybackState.PLAYING) {
                        playerControls?.pause()
                    } else {
                        playerControls?.play()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (playerState.playbackState == PlaybackState.PLAYING) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

/**
 * 格式化时间显示
 */
private fun formatTime(milliseconds: Long): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = milliseconds / (1000 * 60 * 60)
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * 防止屏幕休眠
 */
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
