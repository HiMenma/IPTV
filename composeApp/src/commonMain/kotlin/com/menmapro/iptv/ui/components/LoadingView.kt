package com.menmapro.iptv.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Common loading view component that displays a circular progress indicator.
 * Used across multiple screens for consistent loading state UI.
 * 
 * @param modifier Optional modifier for the container
 */
@Composable
fun LoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
