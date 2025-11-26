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
    Box(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        ) {
            onToggleControls()
        }
    ) {
        // Desktop版本：简单的退出全屏按钮
        if (showControls) {
            Button(
                onClick = onExitFullscreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "退出全屏",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "退出全屏",
                    color = Color.White
                )
            }
        }
    }
}
