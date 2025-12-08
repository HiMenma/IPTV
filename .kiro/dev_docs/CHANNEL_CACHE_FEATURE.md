# 频道缓存功能

## 功能说明

所有配置的频道数据现在都会**永久缓存**在本地，无论来源是：
- ✅ Xtream服务器
- ✅ M3U网络URL
- ✅ M3U本地文件

### 工作原理

1. **首次加载**
   - 从源（Xtream/M3U URL/本地文件）加载频道
   - 自动保存到本地缓存
   - 显示频道列表

2. **后续加载**
   - 直接从本地缓存加载
   - 即时显示，无需等待网络
   - 即使离线也能查看频道列表

3. **手动刷新**
   - 点击刷新按钮
   - 从源重新加载频道
   - 更新本地缓存

## 优势

### 1. 性能提升
- ⚡ **即时加载**：缓存加载速度极快
- ⚡ **减少网络请求**：不需要每次都从网络加载
- ⚡ **节省流量**：减少数据使用

### 2. 离线支持
- 📱 **离线查看**：即使没有网络也能查看频道列表
- 📱 **收藏和历史**：即使离线也能正常显示
- 📱 **更好的体验**：不依赖网络连接

### 3. 数据持久化
- 💾 **永久保存**：缓存不会过期
- 💾 **应用重启**：数据不会丢失
- 💾 **手动控制**：只在需要时刷新

## 使用方法

### 查看频道列表
```
1. 打开应用
2. 选择一个配置
3. 频道列表立即显示（从缓存加载）
```

### 刷新频道数据
```
1. 在频道列表页面
2. 点击AppBar右上角的刷新按钮
3. 等待从源重新加载
4. 缓存自动更新
```

### 查看收藏和历史
```
1. 切换到Favorites或History标签
2. 数据从缓存加载，即时显示
3. 即使离线也能正常工作
```

## 技术实现

### ChannelCacheRepository

```dart
class ChannelCacheRepository {
  // 保存频道到缓存
  Future<void> saveChannels(String configId, List<Channel> channels);
  
  // 从缓存加载频道
  Future<List<Channel>?> loadChannels(String configId);
  
  // 检查是否有缓存
  Future<bool> hasCache(String configId);
  
  // 获取缓存时间戳
  Future<DateTime?> getCacheTimestamp(String configId);
  
  // 清除特定配置的缓存
  Future<void> clearCache(String configId);
  
  // 清除所有缓存
  Future<void> clearAllCaches();
}
```

### 加载流程

```dart
Future<void> loadChannels(String configId, {bool forceRefresh = false}) async {
  if (!forceRefresh) {
    // 尝试从缓存加载
    final cached = await cacheRepository.loadChannels(configId);
    if (cached != null) {
      return cached; // 使用缓存
    }
  }
  
  // 从源加载
  final channels = await loadFromSource(configId);
  
  // 保存到缓存
  await cacheRepository.saveChannels(configId, channels);
  
  return channels;
}
```

## 缓存管理

### 查看缓存信息
```dart
final info = await cacheRepository.getCacheInfo('config_id');
print('Has cache: ${info['hasCache']}');
print('Channel count: ${info['channelCount']}');
print('Cached at: ${info['timestamp']}');
```

### 清除缓存
```dart
// 清除特定配置的缓存
await cacheRepository.clearCache('config_id');

// 清除所有缓存
await cacheRepository.clearAllCaches();
```

## 存储位置

缓存数据保存在SharedPreferences中：
- **键格式**：`channels_cache_{configId}`
- **时间戳**：`channels_timestamp_{configId}`
- **数据格式**：JSON

## 测试

### 单元测试
```bash
flutter test test/unit/repositories/channel_cache_test.dart
```

测试覆盖：
- ✅ 保存和加载
- ✅ 缓存检查
- ✅ 时间戳
- ✅ 清除缓存
- ✅ 缓存信息

### 手动测试

#### 测试缓存加载
1. 打开应用，选择一个配置
2. 等待频道加载完成
3. 完全关闭应用
4. 重新打开应用
5. 选择同一个配置
6. 频道应该立即显示（从缓存）

#### 测试刷新功能
1. 在频道列表页面
2. 点击刷新按钮
3. 应该看到加载指示器
4. 频道列表更新

#### 测试离线模式
1. 打开应用，加载频道（建立缓存）
2. 关闭网络连接
3. 重新打开应用
4. 频道列表应该正常显示
5. 收藏和历史也应该正常显示

## 日志输出

### 缓存命中
```
I/flutter: Loaded 150 channels from cache for config config_123
```

### 缓存未命中
```
I/flutter: No cache found for config config_123
I/flutter: Loading channels from source for config_123
I/flutter: Saved 150 channels to cache for config_123
```

### 强制刷新
```
I/flutter: Loading channels from source for config_123 (forceRefresh: true)
I/flutter: Saved 150 channels to cache for config_123
```

## 常见问题

### Q: 缓存会占用多少空间？
A: 取决于频道数量，通常每个频道约1-2KB。1000个频道约1-2MB。

### Q: 缓存会过期吗？
A: 不会。缓存永久保存，除非手动刷新或清除。

### Q: 如何更新频道数据？
A: 点击频道列表页面的刷新按钮。

### Q: 删除配置会删除缓存吗？
A: 目前不会自动删除。可以手动清除或等待后续版本实现自动清理。

### Q: 缓存损坏怎么办？
A: 如果缓存损坏，系统会自动从源重新加载。

## 性能对比

### 无缓存（之前）
```
首次加载: 2-5秒（网络加载）
后续加载: 2-5秒（每次都从网络加载）
离线: 无法使用
```

### 有缓存（现在）
```
首次加载: 2-5秒（网络加载 + 缓存保存）
后续加载: <100ms（从缓存加载）
离线: 完全可用（查看频道列表、收藏、历史）
```

## 未来改进

### 可能的增强功能
1. **自动清理**：删除配置时自动清除缓存
2. **缓存大小限制**：限制总缓存大小
3. **后台更新**：定期在后台更新缓存
4. **增量更新**：只更新变化的频道
5. **压缩存储**：压缩缓存数据节省空间

## 相关文件

- `lib/repositories/channel_cache_repository.dart` - 缓存仓库
- `lib/viewmodels/channel_viewmodel.dart` - 使用缓存的ViewModel
- `test/unit/repositories/channel_cache_test.dart` - 单元测试

---

**频道缓存功能已实现！** ✅

**APK路径**: `build/app/outputs/flutter-apk/app-release.apk`
**大小**: 53.2MB
**测试**: ✅ 所有测试通过
