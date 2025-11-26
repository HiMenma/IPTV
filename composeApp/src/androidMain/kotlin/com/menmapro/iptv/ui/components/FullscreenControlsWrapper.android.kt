package com.menmapro.iptv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun FullscreenControlsWrapper(
    playerState: PlayerState,
    playerControls: PlayerControls?,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier
) {
    // 防止屏幕休眠
    KeepScreenOn()
    
    Box(modifier = modifier) {
        // 点击区域切换控制显示
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    onToggleControls()
                }
        ) {
            // 增强的全屏控制（音量、亮度、进度条）
            FullscreenControls(
                playerState = playerState,
                playerControls = playerControls,
                showControls = showControls,
                onToggleControls = onToggleControls,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // 退出全屏按钮
        if (showControls) {
            IconButton(
                onClick = onExitFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "退出全屏",
                    tint = Color.White
                )
            }
        }
    }
}
