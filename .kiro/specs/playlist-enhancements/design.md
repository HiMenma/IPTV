# Design Document

## Overview

本设计文档描述了IPTV播放器播放列表增强功能的技术实现方案。主要包括三个核心功能：
1. **播放列表重命名**：允许用户自定义播放列表名称，并在UI中显示播放列表类型（M3U或Xtream）
2. **M3U播放崩溃修复**：解决打开M3U直播链接时应用崩溃的问题，确保播放器正确初始化和错误处理
3. **Xtream分类显示**：改进Xtream播放列表的展示方式，从一次性显示所有频道改为按分类分级展示

## Architecture

### 1. 播放列表重命名架构

```
PlaylistScreen
    ├── PlaylistRow (显示播放列表)
    │   ├── 名称显示
    │   ├── 类型标识 (M3U/Xtream图标)
    │   └── 长按/点击编辑按钮
    ├── RenameDialog (重命名对话框)
    │   ├── 输入框（预填充当前名称）
    │   ├── 类型标签（只读）
    │   └── 确认/取消按钮
    └── PlaylistScreenModel
        └── renamePlaylist(id, newName)
            ├── 验证名称
            ├── 更新数据库
            └── 刷新UI
```

### 2. M3U播放修复架构

```
ChannelListScreen
    └── onChannelClick
        ├── 验证频道URL
        ├── 创建PlayerScreen
        └── 导航到播放器

PlayerScreen
    ├── 初始化检查
    │   ├── 验证URL有效性
    │   └── 检查播放器可用性
    ├── VideoPlayer组件
    │   ├── 错误边界处理
    │   ├── try-catch包裹初始化
    │   └── 错误状态显示
    └── 错误恢复
        ├── 显示错误消息
        ├── 提供重试按钮
        └── 返回频道列表选项
```

### 3. Xtream分类显示架构

```
Xtream播放列表流程
    ├── PlaylistScreen
    │   └── 点击Xtream播放列表
    │       └── 导航到CategoryListScreen (新)
    │
    ├── CategoryListScreen (新)
    │   ├── 显示分类列表
    │   ├── 显示每个分类的频道数量
    │   └── 点击分类
    │       └── 导航到ChannelListScreen
    │
    └── ChannelListScreen
        ├── 接收分类参数
        ├── 只显示该分类的频道
        └── 提供返回分类列表的导航
```

## Components and Interfaces

### 1. 数据库Schema扩展

需要在Playlist表中添加对分类信息的支持，并确保名称字段可更新。

```sql
-- 已存在的Playlist表，无需修改结构
-- 但需要添加新的查询

-- 更新播放列表名称
updatePlaylistName:
UPDATE Playlist SET name = ?, updatedAt = ? WHERE id = ?;

-- 新增Category表用于存储Xtream分类
CREATE TABLE IF NOT EXISTS Category (
    id TEXT PRIMARY KEY NOT NULL,
    playlistId TEXT NOT NULL,
    name TEXT NOT NULL,
    parentId TEXT,
    FOREIGN KEY (playlistId) REFERENCES Playlist(id) ON DELETE CASCADE
);

-- 修改Channel表，添加categoryId字段
-- 需要通过迁移添加
ALTER TABLE Channel ADD COLUMN categoryId TEXT;

-- 查询分类
selectCategoriesByPlaylistId:
SELECT * FROM Category WHERE playlistId = ? ORDER BY name ASC;

-- 插入分类
insertCategory:
INSERT OR REPLACE INTO Category(id, playlistId, name, parentId)
VALUES (?, ?, ?, ?);

-- 按分类查询频道
selectChannelsByCategoryId:
SELECT * FROM Channel WHERE playlistId = ? AND categoryId = ? ORDER BY name ASC;

-- 统计分类频道数
countChannelsByCategory:
SELECT categoryId, COUNT(*) as count 
FROM Channel 
WHERE playlistId = ? 
GROUP BY categoryId;
```

### 2. Repository接口扩展

```kotlin
interface PlaylistRepository {
    // 现有方法
    fun getAllPlaylists(): Flow<List<Playlist>>
    suspend fun addM3uUrl(name: String, url: String)
    suspend fun addXtreamAccount(account: XtreamAccount)
    
    // 新增方法
    suspend fun renamePlaylist(playlistId: String, newName: String)
    suspend fun getCategories(playlistId: String): List<Category>
    suspend fun getChannelsByCategory(playlistId: String, categoryId: String): List<Channel>
    suspend fun getCategoryChannelCount(playlistId: String): Map<String, Int>
}
```

### 3. 新增UI组件

#### RenamePlaylistDialog

```kotlin
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
                // 类型标签
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(playlist.type) {
                            PlaylistType.M3U_URL, PlaylistType.M3U_FILE -> Icons.Default.Link
                            PlaylistType.XTREAM -> Icons.Default.Cloud
                        },
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(playlist.type) {
                            PlaylistType.M3U_URL -> "M3U (URL)"
                            PlaylistType.M3U_FILE -> "M3U (文件)"
                            PlaylistType.XTREAM -> "Xtream"
                        },
                        style = MaterialTheme.typography.caption
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 名称输入框
                OutlinedTextField(
                    value = newName,
                    onValueChange = { 
                        newName = it
                        error = if (it.isBlank()) "名称不能为空" else null
                    },
                    label = { Text("播放列表名称") },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank() && newName != playlist.name
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
```

#### CategoryListScreen

```kotlin
data class CategoryListScreen(val playlist: Playlist) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = remember { CategoryListScreenModel(playlist) }
        val categories by screenModel.categories.collectAsState()
        val channelCounts by screenModel.channelCounts.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val error by screenModel.error.collectAsState()
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(playlist.name) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { padding ->
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    ErrorView(
                        message = error!!,
                        onRetry = { screenModel.loadCategories() }
                    )
                }
                categories.isEmpty() -> {
                    EmptyView(message = "暂无分类")
                }
                else -> {
                    LazyColumn(modifier = Modifier.padding(padding)) {
                        items(categories) { category ->
                            CategoryRow(
                                category = category,
                                channelCount = channelCounts[category.id] ?: 0,
                                onClick = {
                                    navigator.push(
                                        ChannelListScreen(
                                            playlist = playlist,
                                            categoryId = category.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRow(
    category: Category,
    channelCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = "$channelCount 个频道",
                    style = MaterialTheme.typography.caption
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}
```

### 4. ScreenModel扩展

```kotlin
class PlaylistScreenModel(private val repository: PlaylistRepository) : ScreenModel {
    val playlists = repository.getAllPlaylists()
    
    // 新增：重命名播放列表
    fun renamePlaylist(playlistId: String, newName: String) {
        screenModelScope.launch {
            try {
                repository.renamePlaylist(playlistId, newName)
            } catch (e: Exception) {
                // 错误处理
                println("Error renaming playlist: ${e.message}")
            }
        }
    }
    
    // 现有方法...
}

class CategoryListScreenModel(
    private val playlist: Playlist,
    private val repository: PlaylistRepository
) : ScreenModel {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories
    
    private val _channelCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val channelCounts: StateFlow<Map<String, Int>> = _channelCounts
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadCategories()
    }
    
    fun loadCategories() {
        screenModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _categories.value = repository.getCategories(playlist.id)
                _channelCounts.value = repository.getCategoryChannelCount(playlist.id)
            } catch (e: Exception) {
                _error.value = "加载分类失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria, I identified several areas where properties can be consolidated:
- Properties 1.1 and 5.1 both test playlist type display - can be combined
- Properties 2.1, 2.2, and 2.5 all relate to playback error handling - can be consolidated into a comprehensive error handling property
- Properties 3.5 and 5.5 both test loading indicators - can be combined
- Properties 4.1 and 4.3 both test data completeness - can be combined

### Core Properties

**Property 1: Playlist display completeness**
*For any* playlist, when rendered in the UI, the display should include both the playlist name and a type indicator (icon or label) that correctly identifies whether it is M3U or Xtream type.
**Validates: Requirements 1.1, 5.1**

**Property 2: Rename dialog preserves type**
*For any* playlist, when the rename dialog is opened, the dialog should display the playlist type as a read-only field while allowing the name to be edited.
**Validates: Requirements 1.2**

**Property 3: Rename persistence**
*For any* playlist and any valid (non-empty) new name, when the user confirms the rename operation, the new name should be persisted to the database and the playlist's other fields should remain unchanged.
**Validates: Requirements 1.3, 4.5**

**Property 4: Empty name rejection**
*For any* playlist and any string composed entirely of whitespace, attempting to rename the playlist should be rejected and the original name should remain unchanged.
**Validates: Requirements 1.4**

**Property 5: Reactive UI updates**
*For any* playlist rename operation that succeeds, the playlist list UI should immediately reflect the new name without requiring a manual refresh.
**Validates: Requirements 1.5**

**Property 6: M3U playback safety**
*For any* M3U channel, clicking to play should either successfully load the stream or display an error message, but should never cause the application to crash.
**Validates: Requirements 2.1, 2.2, 2.5**

**Property 7: Resource cleanup on channel switch**
*For any* two M3U channels, when switching from one to the other, the system should release all resources associated with the first channel before loading the second channel.
**Validates: Requirements 2.4**

**Property 8: Xtream category-first navigation**
*For any* Xtream playlist, when opened, the system should navigate to a category list view rather than directly showing all channels.
**Validates: Requirements 3.1**

**Property 9: Category filtering**
*For any* category in an Xtream playlist, when selected, the channel list should contain only channels that belong to that category.
**Validates: Requirements 3.2**

**Property 10: Category navigation availability**
*For any* category view in an Xtream playlist, the UI should provide a navigation option to return to the category list.
**Validates: Requirements 3.4**

**Property 11: Loading state visibility**
*For any* long-running operation (category loading, channel loading, playlist loading), the system should display a loading indicator while the operation is in progress.
**Validates: Requirements 3.5, 5.5**

**Property 12: Error recovery options**
*For any* failed data loading operation (categories or channels), the system should display an error message and provide a retry option.
**Validates: Requirements 3.6**

**Property 13: Playlist data completeness**
*For any* playlist stored in or retrieved from the database, the data should include all required fields: id, name, type, and timestamps.
**Validates: Requirements 4.1, 4.3**

**Property 14: Xtream channel category association**
*For any* Xtream channel stored in the database, the channel data should include both categoryId and category name fields.
**Validates: Requirements 4.2**

**Property 15: Category-based channel filtering**
*For any* category ID query on Xtream channels, the returned channel list should contain only channels where the categoryId matches the query parameter.
**Validates: Requirements 4.4**

**Property 16: Category display completeness**
*For any* category in an Xtream playlist, when displayed in the category list, the UI should show both the category name and the count of channels in that category.
**Validates: Requirements 5.2**

## Data Models

### 1. 扩展Playlist模型

Playlist模型已经包含了必要的字段，无需修改：
- `id`: 唯一标识
- `name`: 可编辑的名称
- `type`: 播放列表类型（M3U_URL, M3U_FILE, XTREAM）
- `categories`: 分类列表（用于Xtream）

### 2. 扩展Channel模型

需要添加categoryId字段来关联分类：

```kotlin
@Serializable
data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val logoUrl: String? = null,
    val group: String? = null,
    val categoryId: String? = null,  // 新增
    val headers: Map<String, String> = emptyMap()
)
```

### 3. Category模型

已存在，无需修改：

```kotlin
@Serializable
data class Category(
    val id: String,
    val name: String,
    val parentId: String? = null
)
```

## Error Handling

### 1. M3U播放崩溃错误处理

**问题分析**：
- 可能的原因：URL无效、网络问题、播放器初始化失败、编解码器不支持
- 当前代码缺少错误边界和异常捕获

**解决方案**：

```kotlin
// PlayerScreen中添加错误处理
@Composable
fun PlayerScreen(channel: Channel) : Screen {
    var error by remember { mutableStateOf<String?>(null) }
    var isRetrying by remember { mutableStateOf(false) }
    
    if (error != null) {
        ErrorScreen(
            message = error!!,
            onRetry = {
                error = null
                isRetrying = true
            },
            onBack = { navigator.pop() }
        )
    } else {
        try {
            VideoPlayer(
                url = channel.url,
                onError = { errorMessage ->
                    error = errorMessage
                },
                onPlayerInitFailed = {
                    error = "播放器初始化失败，请检查VLC是否已安装"
                }
            )
        } catch (e: Exception) {
            LaunchedEffect(e) {
                error = "播放失败: ${e.message}"
            }
        }
    }
}
```

**VideoPlayer组件改进**：

```kotlin
@Composable
fun VideoPlayer(
    url: String,
    onError: (String) -> Unit = {},
    onPlayerInitFailed: () -> Unit = {}
) {
    var playerState by remember { mutableStateOf<PlayerState>(PlayerState.Initializing) }
    
    DisposableEffect(Unit) {
        try {
            // 验证URL
            if (url.isBlank()) {
                onError("无效的播放地址")
                return@DisposableEffect onDispose {}
            }
            
            // 初始化播放器
            val mediaPlayerComponent = try {
                EmbeddedMediaPlayerComponent()
            } catch (e: Exception) {
                onPlayerInitFailed()
                return@DisposableEffect onDispose {}
            }
            
            // 添加错误监听器
            mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(
                object : MediaPlayerEventAdapter() {
                    override fun error(mediaPlayer: MediaPlayer) {
                        onError("播放出错，请检查网络连接或尝试其他频道")
                    }
                    
                    override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                        playerState = PlayerState.Ready
                    }
                }
            )
            
            // 加载媒体
            try {
                mediaPlayerComponent.mediaPlayer().media().play(url)
                playerState = PlayerState.Playing
            } catch (e: Exception) {
                onError("无法加载媒体: ${e.message}")
            }
            
            onDispose {
                safeReleasePlayer(mediaPlayerComponent)
            }
        } catch (e: Exception) {
            onError("初始化失败: ${e.message}")
            onDispose {}
        }
    }
    
    // UI渲染...
}

private fun safeReleasePlayer(component: EmbeddedMediaPlayerComponent) {
    try {
        component.mediaPlayer()?.controls()?.stop()
        component.mediaPlayer()?.events()?.removeAllListeners()
        component.release()
    } catch (e: Exception) {
        println("Error releasing player: ${e.message}")
    }
}
```

### 2. 播放列表重命名错误处理

```kotlin
suspend fun renamePlaylist(playlistId: String, newName: String) {
    if (newName.isBlank()) {
        throw IllegalArgumentException("播放列表名称不能为空")
    }
    
    if (newName.length > 100) {
        throw IllegalArgumentException("播放列表名称过长")
    }
    
    try {
        playlistDao.updatePlaylistName(
            id = playlistId,
            name = newName.trim(),
            updatedAt = System.currentTimeMillis()
        )
    } catch (e: Exception) {
        throw Exception("更新播放列表名称失败: ${e.message}")
    }
}
```

### 3. 分类加载错误处理

```kotlin
suspend fun getCategories(playlistId: String): List<Category> {
    return try {
        when (val playlist = getPlaylistById(playlistId)) {
            null -> throw Exception("播放列表不存在")
            else -> when (playlist.type) {
                PlaylistType.XTREAM -> {
                    // 从数据库加载分类
                    categoryDao.selectCategoriesByPlaylistId(playlistId)
                }
                else -> emptyList()
            }
        }
    } catch (e: Exception) {
        throw Exception("加载分类失败: ${e.message}")
    }
}
```

## Testing Strategy

### 1. 单元测试

**播放列表重命名测试**：
- 测试正常重命名流程
- 测试空名称验证
- 测试名称长度限制
- 测试数据库更新

**分类加载测试**：
- 测试Xtream播放列表分类加载
- 测试M3U播放列表返回空分类
- 测试分类频道数统计
- 测试错误场景

**错误处理测试**：
- 测试无效URL处理
- 测试播放器初始化失败
- 测试网络错误恢复

### 2. 集成测试

**端到端播放测试**：
- 测试M3U频道播放流程
- 测试Xtream频道播放流程
- 测试频道切换
- 测试错误恢复

**分类导航测试**：
- 测试从播放列表到分类列表的导航
- 测试从分类列表到频道列表的导航
- 测试返回导航
- 测试状态保持

### 3. UI测试

**重命名对话框测试**：
- 测试对话框显示
- 测试输入验证
- 测试确认和取消操作

**分类列表测试**：
- 测试分类显示
- 测试频道数显示
- 测试点击导航
- 测试加载状态
- 测试错误状态

## Implementation Notes

### 1. 数据库迁移

需要添加Category表和修改Channel表：

```kotlin
// 在DatabaseDriver中添加迁移逻辑
object Schema : SqlDriver.Schema {
    override val version: Int = 2
    
    override fun create(driver: SqlDriver) {
        // 创建所有表
        IptvDatabase.Schema.create(driver)
    }
    
    override fun migrate(
        driver: SqlDriver,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            // 添加Category表
            driver.execute(null, """
                CREATE TABLE IF NOT EXISTS Category (
                    id TEXT PRIMARY KEY NOT NULL,
                    playlistId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    parentId TEXT,
                    FOREIGN KEY (playlistId) REFERENCES Playlist(id) ON DELETE CASCADE
                )
            """.trimIndent(), 0)
            
            // 添加categoryId到Channel表
            driver.execute(null, """
                ALTER TABLE Channel ADD COLUMN categoryId TEXT
            """.trimIndent(), 0)
        }
    }
}
```

### 2. Xtream API分类获取

在XtreamClient中添加获取分类的方法：

```kotlin
suspend fun getLiveCategories(): List<Category> {
    val response = client.get("$baseUrl/player_api.php") {
        parameter("username", username)
        parameter("password", password)
        parameter("action", "get_live_categories")
    }
    return response.body()
}
```

### 3. 导航流程改进

修改PlaylistScreen中的点击处理：

```kotlin
PlaylistRow(playlist) {
    when (playlist.type) {
        PlaylistType.XTREAM -> {
            // 导航到分类列表
            navigator.push(CategoryListScreen(playlist))
        }
        else -> {
            // 直接导航到频道列表
            navigator.push(ChannelListScreen(playlist))
        }
    }
}
```

### 4. 播放列表类型图标

定义类型图标映射：

```kotlin
fun PlaylistType.getIcon(): ImageVector {
    return when (this) {
        PlaylistType.M3U_URL -> Icons.Default.Link
        PlaylistType.M3U_FILE -> Icons.Default.InsertDriveFile
        PlaylistType.XTREAM -> Icons.Default.Cloud
    }
}

fun PlaylistType.getDisplayName(): String {
    return when (this) {
        PlaylistType.M3U_URL -> "M3U (URL)"
        PlaylistType.M3U_FILE -> "M3U (文件)"
        PlaylistType.XTREAM -> "Xtream"
    }
}
```

## Performance Considerations

### 1. 分类和频道加载优化

- 使用分页加载大量频道
- 缓存分类列表避免重复请求
- 使用Flow实现响应式更新

### 2. 播放器资源管理

- 确保播放器资源及时释放
- 避免同时创建多个播放器实例
- 使用对象池管理播放器

### 3. 数据库查询优化

- 为categoryId添加索引
- 使用批量查询减少数据库访问
- 缓存频道数统计结果

## Security Considerations

### 1. 输入验证

- 验证播放列表名称长度和字符
- 验证URL格式
- 防止SQL注入

### 2. 错误信息

- 不暴露敏感的系统信息
- 提供用户友好的错误消息
- 记录详细错误日志用于调试

## Migration Strategy

### 1. 向后兼容

- 保持现有API不变
- 新功能作为可选增强
- 支持旧版本数据库

### 2. 数据迁移

- 自动迁移数据库schema
- 为现有播放列表生成默认名称
- 为Xtream播放列表加载分类信息

### 3. 用户体验

- 首次使用时显示功能介绍
- 提供平滑的过渡动画
- 保持用户习惯的操作流程
