# Design Document

## Overview

本设计文档描述了修复IPTV播放器运行时问题的技术方案。主要关注点包括：
- 改进VLC播放器的资源管理以防止崩溃
- 修复Android平台的数据库驱动初始化问题
- 增强错误处理和日志记录机制
- 添加VLC可用性检查和用户友好的错误提示

## Architecture

### 1. 视频播放器资源管理架构

```
VideoPlayer (Composable)
    ├── DisposableEffect (生命周期管理)
    │   ├── 初始化: 创建MediaPlayerComponent
    │   ├── 配置: 添加事件监听器
    │   └── 清理: 安全释放资源
    ├── LaunchedEffect (状态更新)
    │   └── 定期更新播放位置和状态
    └── LaunchedEffect (URL变化)
        ├── 停止当前播放
        └── 加载新媒体
```

### 2. 数据库驱动架构

```
Platform-Specific Implementation
    ├── Android
    │   ├── 需要Context参数
    │   ├── 通过MainActivity传递
    │   └── 使用AndroidSqliteDriver
    └── Desktop
        ├── 无需额外参数
        ├── 使用用户主目录
        └── 使用JdbcSqliteDriver
```

### 3. 错误处理架构

```
Error Handling Layer
    ├── VLC初始化错误
    │   ├── 检测VLC可用性
    │   └── 显示安装指引
    ├── 数据库错误
    │   ├── 捕获SQL异常
    │   └── 提供恢复选项
    └── 网络错误
        ├── 超时处理
        └── 重试机制
```

## Components and Interfaces

### 1. VideoPlayerManager (新增)

负责管理视频播放器的生命周期和资源释放。

```kotlin
class VideoPlayerManager {
    private var mediaPlayerComponent: EmbeddedMediaPlayerComponent? = null
    private var isReleased = false
    
    fun initialize(): EmbeddedMediaPlayerComponent
    fun release()
    fun isInitialized(): Boolean
}
```

### 2. DatabaseDriverFactory (改进)

改进数据库驱动创建逻辑，支持Android的Context传递。

```kotlin
// Android
class AndroidDatabaseDriverFactory(private val context: Context) {
    fun createDriver(): SqlDriver
}

// Desktop
object DesktopDatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
```

### 3. VlcAvailabilityChecker (新增)

检查VLC是否可用并提供用户友好的错误提示。

```kotlin
object VlcAvailabilityChecker {
    fun isVlcAvailable(): Boolean
    fun getInstallationInstructions(): String
}
```

### 4. ErrorHandler (新增)

统一的错误处理接口。

```kotlin
interface ErrorHandler {
    fun handleError(error: Throwable, context: String)
    fun logError(message: String, error: Throwable?)
}
```

## Data Models

### 1. PlayerReleaseState (新增)

跟踪播放器释放状态以防止重复释放。

```kotlin
data class PlayerReleaseState(
    val isReleasing: Boolean = false,
    val isReleased: Boolean = false,
    val lastError: String? = null
)
```

### 2. DatabaseInitResult (新增)

数据库初始化结果。

```kotlin
sealed class DatabaseInitResult {
    data class Success(val driver: SqlDriver) : DatabaseInitResult()
    data class Failure(val error: String) : DatabaseInitResult()
}
```

## Error Handling

### 1. VLC资源释放错误处理

**问题**: VLCJ在dispose时可能触发SIGSEGV崩溃

**解决方案**:
- 在释放前检查播放器状态
- 先停止播放再释放资源
- 使用try-catch包裹所有VLC调用
- 在后台线程执行释放操作
- 添加释放状态标志防止重复释放

```kotlin
fun safeRelease() {
    if (isReleased) return
    isReleased = true
    
    try {
        // 1. 停止播放
        mediaPlayer?.controls()?.stop()
        
        // 2. 移除事件监听器
        mediaPlayer?.events()?.removeAllListeners()
        
        // 3. 释放资源
        mediaPlayerComponent?.release()
    } catch (e: Exception) {
        // 记录错误但不抛出
        println("Error releasing VLC player: ${e.message}")
    }
}
```

### 2. 数据库初始化错误处理

**问题**: Android平台需要Context但当前实现会抛出异常

**解决方案**:
- 在MainActivity中初始化Koin时传递Context
- 使用androidContext()提供Context
- 在Desktop平台使用无参数版本

```kotlin
// Android MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initKoin {
            androidContext(this@MainActivity)
        }
        
        setContent {
            App()
        }
    }
}
```

### 3. 网络请求错误处理

**问题**: 网络请求失败时缺少重试机制

**解决方案**:
- 添加超时配置
- 实现指数退避重试
- 提供用户友好的错误消息

```kotlin
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxRetries - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block() // 最后一次尝试
}
```

## Testing Strategy

### 1. 单元测试

**VideoPlayerManager测试**:
- 测试初始化和释放流程
- 测试重复释放不会崩溃
- 测试错误处理

**DatabaseDriverFactory测试**:
- 测试Android和Desktop平台的驱动创建
- 测试数据库文件创建
- 测试错误场景

### 2. 集成测试

**播放器生命周期测试**:
- 测试频道切换场景
- 测试快速切换不会崩溃
- 测试内存泄漏

**数据库操作测试**:
- 测试播放列表CRUD操作
- 测试并发访问
- 测试数据持久化

### 3. 平台特定测试

**Android测试**:
- 测试Context传递
- 测试Activity生命周期
- 测试ExoPlayer集成

**Desktop测试**:
- 测试VLC可用性检查
- 测试文件系统权限
- 测试跨平台路径处理

## Implementation Notes

### 1. VLC资源释放顺序

关键是按正确顺序释放资源：
1. 停止播放 (stop)
2. 移除事件监听器 (removeAllListeners)
3. 释放媒体播放器组件 (release)

### 2. Android Context传递

使用Koin的androidContext()功能：
```kotlin
startKoin {
    androidContext(context)
    modules(appModule)
}
```

然后在module中使用：
```kotlin
single { 
    val driver = AndroidSqliteDriver(
        IptvDatabase.Schema, 
        androidContext(), 
        "iptv.db"
    )
    IptvDatabase(driver)
}
```

### 3. Desktop数据库路径

使用用户主目录下的隐藏文件夹：
```kotlin
val databasePath = File(System.getProperty("user.home"), ".iptv/iptv.db")
```

### 4. VLC检测

在VideoPlayer初始化前检查VLC：
```kotlin
val vlcAvailable = remember {
    NativeDiscovery().discover()
}

if (!vlcAvailable) {
    // 显示错误消息和安装指引
}
```

## Performance Considerations

### 1. 播放器初始化

- 延迟初始化MediaPlayerComponent直到真正需要播放
- 缓存播放器实例避免重复创建
- 使用对象池管理播放器资源

### 2. 数据库操作

- 使用事务批量操作
- 添加索引优化查询
- 实现分页加载大量频道

### 3. 内存管理

- 及时释放不再使用的资源
- 使用WeakReference避免内存泄漏
- 监控内存使用情况

## Security Considerations

### 1. 数据库安全

- 不存储明文密码
- 使用加密存储敏感信息
- 验证SQL输入防止注入

### 2. 网络安全

- 验证HTTPS证书
- 超时保护防止DoS
- 验证M3U内容防止恶意代码

## Migration Strategy

### 1. 向后兼容

- 保持现有API不变
- 添加新功能作为可选项
- 提供迁移指南

### 2. 数据迁移

- 检测旧版本数据库
- 自动迁移到新schema
- 备份用户数据

## Monitoring and Logging

### 1. 日志级别

- ERROR: 崩溃和严重错误
- WARN: 可恢复的错误
- INFO: 重要操作
- DEBUG: 详细调试信息

### 2. 关键指标

- 播放器初始化成功率
- 资源释放失败次数
- 数据库操作延迟
- 网络请求成功率

### 3. 崩溃报告

- 捕获未处理异常
- 记录堆栈跟踪
- 收集设备信息
- 提供用户反馈渠道


## Android ExoPlayer流媒体支持

### 问题分析

ExoPlayer错误 "No suitable media source factory found for content type: 22" 表示缺少对特定流媒体格式的支持。Content type 22 通常对应 HLS (HTTP Live Streaming) 或其他自适应流媒体格式。

### 解决方案

需要添加ExoPlayer的额外依赖来支持各种流媒体格式：

```kotlin
// build.gradle.kts - androidMain dependencies
implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
implementation("androidx.media3:media3-exoplayer-dash:1.3.1")
implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.3.1")
```

这些库提供：
- **media3-exoplayer-hls**: HLS (HTTP Live Streaming) 支持
- **media3-exoplayer-dash**: DASH (Dynamic Adaptive Streaming over HTTP) 支持
- **media3-exoplayer-smoothstreaming**: Microsoft Smooth Streaming 支持

### 实现细节

ExoPlayer会自动检测这些库并注册相应的MediaSource工厂。不需要修改VideoPlayer代码，只需添加依赖即可。

### 错误消息改进

在VideoPlayer.android.kt中改进错误处理，将技术错误代码转换为用户友好的消息：

```kotlin
override fun onPlayerError(error: PlaybackException) {
    val errorMsg = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "网络连接超时"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "HTTP错误: ${error.message}"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "媒体格式错误"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "清单文件格式错误"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "解码器初始化失败"
        PlaybackException.ERROR_CODE_UNSPECIFIED -> "播放错误: 不支持的媒体格式或编码"
        else -> "播放错误: ${error.message ?: "未知错误 (代码: ${error.errorCode})"}"
    }
    onError(errorMsg)
}
```

## 数据库Schema迁移

### 问题分析

错误 "no such column: Channel.categoryId" 表示：
1. 旧版本数据库没有 `categoryId` 列
2. 新代码尝试访问该列导致SQL错误
3. SQLDelight没有配置自动迁移

### 解决方案架构

```
Database Migration System
    ├── Version Detection
    │   └── 检查当前schema版本
    ├── Migration Scripts
    │   ├── Version 1 → 2: 添加categoryId列
    │   └── 未来迁移...
    └── Error Recovery
        ├── 记录迁移日志
        └── 提供重置选项
```

### 实现方案

#### 1. 添加Schema版本管理

在SQLDelight配置中添加版本号：

```kotlin
// IptvDatabase.sq
-- Schema Version: 2

-- Migration from version 1 to 2
-- ALTER TABLE Channel ADD COLUMN categoryId TEXT;
```

#### 2. 创建迁移管理器

```kotlin
object DatabaseMigration {
    private const val CURRENT_VERSION = 2
    private const val VERSION_KEY = "db_version"
    
    suspend fun migrate(driver: SqlDriver, dataStore: DataStore<Preferences>) {
        val currentVersion = dataStore.data.first()[intPreferencesKey(VERSION_KEY)] ?: 1
        
        if (currentVersion < CURRENT_VERSION) {
            performMigration(driver, currentVersion, CURRENT_VERSION)
            dataStore.edit { it[intPreferencesKey(VERSION_KEY)] = CURRENT_VERSION }
        }
    }
    
    private fun performMigration(driver: SqlDriver, from: Int, to: Int) {
        when {
            from == 1 && to >= 2 -> migrateV1ToV2(driver)
        }
    }
    
    private fun migrateV1ToV2(driver: SqlDriver) {
        try {
            driver.execute(null, "ALTER TABLE Channel ADD COLUMN categoryId TEXT", 0)
            println("✓ Database migrated from v1 to v2: added categoryId column")
        } catch (e: Exception) {
            // Column might already exist
            println("Migration v1→v2: ${e.message}")
        }
    }
}
```

#### 3. 在应用启动时执行迁移

```kotlin
// Koin.kt
single {
    val driver = createDatabaseDriver(androidContext())
    
    // 执行迁移
    runBlocking {
        DatabaseMigration.migrate(driver, get())
    }
    
    IptvDatabase(driver)
}
```

### 备用方案：安全的列访问

如果迁移失败，在DAO中添加安全检查：

```kotlin
private fun hasColumn(tableName: String, columnName: String): Boolean {
    return try {
        database.iptvDatabaseQueries.executeQuery(
            "SELECT $columnName FROM $tableName LIMIT 0"
        )
        true
    } catch (e: Exception) {
        false
    }
}
```

### 用户数据保护

在迁移前备份数据：

```kotlin
suspend fun backupDatabase(context: Context) {
    val dbFile = context.getDatabasePath("iptv.db")
    val backupFile = File(context.filesDir, "iptv_backup_${System.currentTimeMillis()}.db")
    dbFile.copyTo(backupFile, overwrite = true)
}
```

### 错误恢复

如果迁移失败，提供重置选项：

```kotlin
suspend fun resetDatabase(driver: SqlDriver) {
    try {
        // 删除所有表
        driver.execute(null, "DROP TABLE IF EXISTS Channel", 0)
        driver.execute(null, "DROP TABLE IF EXISTS Playlist", 0)
        driver.execute(null, "DROP TABLE IF EXISTS Category", 0)
        driver.execute(null, "DROP TABLE IF EXISTS Favorite", 0)
        driver.execute(null, "DROP TABLE IF EXISTS EpgProgram", 0)
        
        // 重新创建schema
        IptvDatabase.Schema.create(driver)
        
        println("✓ Database reset successfully")
    } catch (e: Exception) {
        println("✗ Failed to reset database: ${e.message}")
        throw e
    }
}
```

## 测试策略更新

### ExoPlayer流媒体测试

1. **单元测试**：
   - 测试不同流媒体URL的格式检测
   - 测试错误消息转换逻辑

2. **集成测试**：
   - 使用测试HLS流验证播放
   - 测试格式切换（从普通URL到HLS）
   - 测试错误场景（无效URL、网络错误）

### 数据库迁移测试

1. **单元测试**：
   - 测试版本检测逻辑
   - 测试迁移脚本执行
   - 测试备份和恢复功能

2. **集成测试**：
   - 创建v1数据库并测试迁移到v2
   - 测试迁移后数据完整性
   - 测试迁移失败时的错误处理

3. **属性测试**：
   - 迁移应该是幂等的（多次执行结果相同）
   - 迁移不应该丢失现有数据
   - 迁移后所有查询应该正常工作
