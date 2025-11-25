package com.menmapro.iptv.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Extension function to get the icon for a PlaylistType
 * @return ImageVector representing the playlist type
 */
fun PlaylistType.getIcon(): ImageVector {
    return when (this) {
        PlaylistType.M3U_URL -> Icons.Default.PlayArrow
        PlaylistType.M3U_FILE -> Icons.Default.Settings
        PlaylistType.XTREAM -> Icons.Default.Star
    }
}

/**
 * Extension function to get the display name for a PlaylistType
 * @return String representing the human-readable name of the playlist type
 */
fun PlaylistType.getDisplayName(): String {
    return when (this) {
        PlaylistType.M3U_URL -> "M3U (URL)"
        PlaylistType.M3U_FILE -> "M3U (文件)"
        PlaylistType.XTREAM -> "Xtream"
    }
}
