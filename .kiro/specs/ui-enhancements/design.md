# UI增强功能设计

## 1. 播放列表名称编辑

### 设计方案
修改`AddPlaylistDialog`组件，添加名称输入字段：

```kotlin
// 状态变量
var playlistName by remember { mutableStateOf("") }

// UI布局
OutlinedTextField(
    value = playlistName,
    onValueChange = { playlistName = it },
    label = { Text("播放列表名称（可选）") },
    modifier = Modifier.fillMaxWidth()
)
```

### 数据流
1. 用户输入名称（可选）
2. 用户输入URL/Xtream信息
3. 点击添加按钮
4. 如果名称为空，使用默认名称
5. 调用repository添加播放列表

## 2. 添加播放列表加载提示

### 设计方案
创建`LoadingDialog`组件：

```kotlin
@Composable
fun LoadingDialog(
    message: String,
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("请稍候") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(message)
            }
        },
        confirmButton = {}
    )
}
```

### 状态管理
在`PlaylistScreenModel`中添加：
```kotlin
var isLoading by mutableStateOf(false)
var loadingMessage by mutableStateOf("")
var errorMessage by mutableStateOf<String?>(null)
```

## 3. 播放器加载状态提示

### 设计方案
在`PlayerScreen`中添加加载覆盖层：

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    VideoPlayer(...)
    
    // 加载覆盖层
    if (playerState.value.playbackState == PlaybackState.BUFFERING) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text("正在加载...", color = Color.White)
            }
        }
    }
}
```

## 4. 手动刷新播放列表

### 设计方案
在`PlaylistRow`中添加刷新按钮：

```kotlin
IconButton(onClick = onRefresh) {
    Icon(
        imageVector = Icons.Default.Refresh,
        contentDescription = "刷新播放列表"
    )
}
```

### Repository方法
添加`refreshPlaylist`方法：

```kotlin
suspend fun refreshPlaylist(playlistId: String) {
    val playlist = getPlaylistById(playlistId) ?: return
    
    when (playlist.type) {
        PlaylistType.M3U_URL -> {
            // 重新下载M3U
            addM3uUrl(playlist.name, playlist.url ?: "")
        }
        PlaylistType.XTREAM -> {
            // 重新获取Xtream数据
            // 需要从数据库获取账户信息
        }
        else -> {
            // M3U_FILE不支持刷新
        }
    }
}
```

## 5. 全屏播放支持

### Desktop实现
使用VLC的全屏API（已在VideoPlayer.desktop.kt中实现）：

```kotlin
override fun toggleFullscreen() {
    mediaPlayerComponent.mediaPlayer().fullScreen().toggle()
}
```

### Android实现
需要实现系统UI控制：

```kotlin
override fun toggleFullscreen() {
    // 切换全屏状态
    isFullscreen = !isFullscreen
    
    if (isFullscreen) {
        // 隐藏系统UI
        // 使用WindowInsetsController
    } else {
        // 显示系统UI
    }
}
```

### PlayerScreen修改
添加全屏状态管理：

```kotlin
var isFullscreen by remember { mutableStateOf(false) }

Scaffold(
    topBar = {
        if (!isFullscreen) {
            TopAppBar(...)
        }
    }
) { padding ->
    Box(modifier = Modifier.fillMaxSize()) {
        VideoPlayer(...)
        
        // 全屏按钮
        IconButton(
            onClick = { 
                playerControls?.toggleFullscreen()
                isFullscreen = !isFullscreen
            },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = if (isFullscreen) 
                    Icons.Default.FullscreenExit 
                else 
                    Icons.Default.Fullscreen,
                contentDescription = "全屏"
            )
        }
    }
}
```

## 数据库扩展

为支持刷新功能，需要存储Xtream账户信息：

```sql
CREATE TABLE IF NOT EXISTS xtream_accounts (
    playlist_id TEXT PRIMARY KEY,
    server_url TEXT NOT NULL,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE
);
```

## 组件层次结构

```
PlaylistScreen
├── LoadingDialog (新增)
├── AddPlaylistDialog (修改)
│   └── 名称输入字段 (新增)
└── PlaylistRow (修改)
    └── 刷新按钮 (新增)

PlayerScreen (修改)
├── TopAppBar (条件显示)
├── VideoPlayer
├── LoadingOverlay (新增)
└── FullscreenButton (新增)
```
