# 运行时问题修复总结

## 修复的问题

### 1. Android ExoPlayer 流媒体格式支持问题

**问题描述：**
- 错误信息：`Error initializing ExoPlayer: No suitable media source factory found for content type: 22`
- 原因：ExoPlayer 缺少对 HLS、DASH 等流媒体格式的支持库

**解决方案：**
- 添加了三个 ExoPlayer 扩展库：
  - `androidx.media3:media3-exoplayer-hls` - HLS 支持
  - `androidx.media3:media3-exoplayer-dash` - DASH 支持
  - `androidx.media3:media3-exoplayer-smoothstreaming` - SmoothStreaming 支持

**修改的文件：**
- `gradle/libs.versions.toml` - 添加了新的依赖定义
- `composeApp/build.gradle.kts` - 在 androidMain 中添加了依赖
- `composeApp/src/androidMain/kotlin/com/menmapro/iptv/ui/components/VideoPlayer.android.kt` - 改进了错误消息

**改进的错误处理：**
```kotlin
PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "解码器初始化失败，设备可能不支持此格式"
PlaybackException.ERROR_CODE_UNSPECIFIED -> "播放错误: 不支持的媒体格式或编码"
else -> "播放错误: ${error.message ?: "未知错误"} (代码: ${error.errorCode})"
```

### 2. Desktop 数据库 Schema 迁移问题

**问题描述：**
- 错误信息：`no such column: Channel.categoryId`
- 原因：旧版本数据库没有 `categoryId` 列，新代码尝试访问该列导致 SQL 错误

**解决方案：**
实现了完整的数据库迁移系统：

1. **DatabaseMigration.kt** - 迁移管理器
   - 版本检测和管理
   - 自动执行迁移脚本
   - 幂等性保证（多次执行安全）
   - 错误处理和日志记录

2. **DataStore 集成** - 版本存储
   - 使用 DataStore 存储数据库版本号
   - 跨平台支持（Android 和 Desktop）

3. **Koin 集成** - 启动时自动迁移
   - 在数据库初始化前执行迁移
   - 错误处理不会阻止应用启动

**创建的文件：**
- `composeApp/src/commonMain/kotlin/com/menmapro/iptv/data/database/DatabaseMigration.kt`
- `composeApp/src/commonMain/kotlin/com/menmapro/iptv/data/database/DataStoreFactory.kt`
- `composeApp/src/androidMain/kotlin/com/menmapro/iptv/data/database/DataStoreFactory.android.kt`
- `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/data/database/DataStoreFactory.desktop.kt`
- `composeApp/src/desktopTest/kotlin/com/menmapro/iptv/data/database/DatabaseMigrationTest.kt`

**修改的文件：**
- `composeApp/src/commonMain/kotlin/com/menmapro/iptv/di/Koin.kt` - 集成迁移逻辑

## 迁移系统特性

### 版本管理
- 当前版本：v2
- v1 → v2：添加 `categoryId` 列到 Channel 表

### 安全特性
1. **幂等性**：多次执行迁移不会出错
2. **列存在检查**：迁移前检查列是否已存在
3. **错误恢复**：提供 `resetDatabase` 函数用于紧急恢复
4. **详细日志**：记录所有迁移步骤和错误

### 测试覆盖
创建了 4 个测试用例：
1. ✅ 测试从 v1 迁移到 v2 添加 categoryId 列
2. ✅ 测试迁移的幂等性（多次执行安全）
3. ✅ 测试跳过已是最新版本的迁移
4. ✅ 测试处理列已存在的情况

## 验证结果

### 编译测试
```bash
./gradlew :composeApp:desktopTest
BUILD SUCCESSFUL
```

### 单元测试
所有测试通过：
- DatabaseMigrationTest: 4/4 通过
- 其他现有测试：全部通过

## 使用说明

### Android 用户
1. 更新应用后，ExoPlayer 将自动支持 HLS/DASH 流媒体
2. 数据库会在首次启动时自动迁移
3. 如果遇到播放问题，错误消息会更加清晰

### Desktop 用户
1. 首次启动时会自动检测并迁移数据库
2. 迁移过程完全自动，无需用户干预
3. 数据库文件位置：`~/.iptv/iptv.db`
4. 版本信息存储在：`~/.iptv/iptv_preferences.preferences_pb`

### 开发者
添加新的迁移脚本：
```kotlin
// 在 DatabaseMigration.kt 中
private fun migrateV2ToV3(driver: SqlDriver) {
    // 执行 v2 → v3 的迁移逻辑
}

// 在 performMigration 中添加
when (version) {
    2 -> migrateV1ToV2(driver)
    3 -> migrateV2ToV3(driver)  // 新增
}
```

## 未来改进建议

1. **备份功能**：在迁移前自动备份数据库
2. **迁移回滚**：支持迁移失败时回滚
3. **迁移通知**：向用户显示迁移进度
4. **性能优化**：对大型数据库使用批量操作

## 相关文档

- Requirements: `.kiro/specs/fix-runtime-issues/requirements.md`
- Design: `.kiro/specs/fix-runtime-issues/design.md`
- Tasks: `.kiro/specs/fix-runtime-issues/tasks.md`
