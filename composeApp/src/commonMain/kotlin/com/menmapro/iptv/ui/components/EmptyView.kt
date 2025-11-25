package com.menmapro.iptv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Common empty state view component that displays a message when no content is available.
 * Used across multiple screens for consistent empty state UI.
 * 
 * @param message The message to display in the empty state
 * @param modifier Optional modifier for the container
 */
@Composable
fun EmptyView(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}
