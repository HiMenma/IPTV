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

/**
 * 跨平台全屏控制包装器
 * Android使用增强的控制，Desktop使用简单的退出按钮
 */
@Composable
expect fun FullscreenControlsWrapper(
    playerState: PlayerState,
    playerControls: PlayerControls?,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier
)
