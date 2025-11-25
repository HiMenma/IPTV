package com.menmapro.iptv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.menmapro.iptv.data.model.Playlist
import com.menmapro.iptv.data.model.getDisplayName
import com.menmapro.iptv.data.model.getIcon

/**
 * Dialog for renaming a playlist
 * 
 * @param playlist The playlist to rename
 * @param onDismiss Callback when dialog is dismissed
 * @param onConfirm Callback when rename is confirmed with the new name
 */
@Composable
fun RenamePlaylistDialog(
    playlist: Playlist,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(playlist.name) }
    var error by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名播放列表") },
        text = {
            Column {
                // Type label (read-only)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = playlist.type.getIcon(),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = playlist.type.getDisplayName(),
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Name input field
                OutlinedTextField(
                    value = newName,
                    onValueChange = { 
                        newName = it
                        error = when {
                            it.isBlank() -> "名称不能为空"
                            it.length > 100 -> "名称过长（最多100个字符）"
                            else -> null
                        }
                    },
                    label = { Text("播放列表名称") },
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Error message
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName.trim()) },
                enabled = newName.isNotBlank() && newName.trim() != playlist.name && error == null
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
