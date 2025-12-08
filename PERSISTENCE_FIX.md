# 收藏和历史记录持久化问题修复

## 问题描述
应用完全退出后重新打开，收藏和历史记录列表清空了。

## 根本原因分析

### 1. 数据存储层正常
测试证明`FavoriteRepository`和`HistoryRepository`的数据持久化是正常的：
- ✅ 数据正确保存到SharedPreferences
- ✅ 应用重启后数据仍然存在
- ✅ 可以正确读取保存的数据

### 2. 问题在于数据加载层
`ChannelViewModel.loadFavorites()`和`loadHistory()`的逻辑：

```dart
1. 从FavoriteRepository获取收藏的channelId列表
2. 从ConfigurationRepository获取所有配置
3. 遍历每个配置，加载所有频道
4. 过滤出收藏/历史中的频道
```

**问题点**：
- 如果配置加载失败（网络问题、服务器不可达等）
- 如果频道加载失败
- 就会导致`allChannels`为空
- 最终收藏/历史列表也为空

## 解决方案

### 方案1: 添加调试日志（已实施）
在关键位置添加调试日志，帮助定位问题：

```dart
debugPrint('Loading favorites: ${favoriteRecords.length} favorite records found');
debugPrint('Favorite IDs: $_favoriteIds');
debugPrint('Found ${configs.length} configurations');
```

### 方案2: 改进错误处理（已实施）
- 在空状态页面添加刷新按钮
- 添加try-catch捕获加载错误
- 显示详细的错误信息

### 方案3: 缓存频道数据（推荐）
将频道数据也缓存到本地，避免每次都需要从网络加载：

```dart
// 保存频道数据到本地
Future<void> _cacheChannels(String configId, List<Channel> channels) async {
  final prefs = await SharedPreferences.getInstance();
  final data = {
    'channels': channels.map((c) => c.toJson()).toList(),
    'cachedAt': DateTime.now().toIso8601String(),
  };
  await prefs.setString('channels_$configId', json.encode(data));
}

// 从缓存加载频道
Future<List<Channel>?> _loadCachedChannels(String configId) async {
  final prefs = await SharedPreferences.getInstance();
  final cached = prefs.getString('channels_$configId');
  if (cached == null) return null;
  
  final data = json.decode(cached);
  final cachedAt = DateTime.parse(data['cachedAt']);
  
  // 缓存有效期24小时
  if (DateTime.now().difference(cachedAt).inHours > 24) {
    return null;
  }
  
  return (data['channels'] as List)
      .map((item) => Channel.fromJson(item))
      .toList();
}
```

## 测试步骤

### 1. 验证数据持久化
```bash
flutter test test/debug/persistence_test.dart
```

### 2. 检查实际存储的数据
在应用中添加调试代码：

```dart
import 'package:iptv_player/debug/storage_inspector.dart';

// 在某个按钮的onPressed中
await StorageInspector.printAllData();
```

### 3. 查看日志
```bash
flutter run --release
# 或
adb logcat | grep -i "flutter\|favorites\|history"
```

### 4. 手动测试
1. 添加几个收藏
2. 播放几个频道（记录历史）
3. 完全关闭应用（从最近任务中清除）
4. 重新打开应用
5. 切换到Favorites标签
6. 切换到History标签
7. 检查数据是否还在

## 调试工具

### StorageInspector
```dart
// 查看所有存储的数据
await StorageInspector.printAllData();

// 查看收藏数据
final favData = await StorageInspector.getFavoritesData();
print('Favorites: $favData');

// 查看历史数据
final histData = await StorageInspector.getHistoryData();
print('History: $histData');
```

### 手动检查SharedPreferences
```bash
# Android
adb shell
run-as com.example.iptv_player
cd shared_prefs
cat com.example.iptv_player_preferences.xml
```

## 临时解决方案

如果问题仍然存在，用户可以：

1. **手动刷新**
   - 在Favorites页面点击刷新按钮
   - 在History页面点击刷新按钮

2. **检查网络连接**
   - 确保设备有网络连接
   - 确保配置的服务器可访问

3. **重新添加配置**
   - 如果配置有问题，删除并重新添加

## 长期解决方案

### 实现频道缓存
创建`ChannelCacheRepository`：

```dart
class ChannelCacheRepository {
  static const String _prefix = 'channels_cache_';
  static const Duration _cacheValidity = Duration(hours: 24);
  
  Future<void> saveChannels(String configId, List<Channel> channels) async {
    // 保存频道到本地
  }
  
  Future<List<Channel>?> loadChannels(String configId) async {
    // 从本地加载频道
    // 检查缓存是否过期
  }
  
  Future<void> clearCache(String configId) async {
    // 清除特定配置的缓存
  }
}
```

### 修改loadFavorites逻辑
```dart
Future<void> loadFavorites() async {
  try {
    final favoriteRecords = await _favoriteRepository.getAll();
    final configs = await _configRepository.getAll();
    final allChannels = <Channel>[];
    
    for (final config in configs) {
      // 先尝试从缓存加载
      var channels = await _channelCache.loadChannels(config.id);
      
      if (channels == null) {
        // 缓存不存在或过期，从网络加载
        channels = await _loadChannelsFromNetwork(config);
        // 保存到缓存
        await _channelCache.saveChannels(config.id, channels);
      }
      
      allChannels.addAll(channels);
    }
    
    // 过滤收藏的频道
    _favorites = allChannels
        .where((channel) => _favoriteIds.contains(channel.id))
        .toList();
  } catch (e) {
    _error = 'Failed to load favorites: $e';
  }
}
```

## 预期行为

### 正常情况
1. ✅ 添加收藏后，数据保存到SharedPreferences
2. ✅ 应用重启后，从SharedPreferences读取收藏ID
3. ✅ 从配置加载频道数据
4. ✅ 过滤出收藏的频道并显示

### 异常情况
1. ⚠️ 网络不可用 → 显示错误，提供刷新按钮
2. ⚠️ 配置加载失败 → 显示错误，提供刷新按钮
3. ⚠️ 频道加载失败 → 显示错误，提供刷新按钮

## 下一步

1. ✅ 添加调试日志
2. ✅ 改进错误处理
3. ✅ 添加刷新按钮
4. ⏳ 实现频道缓存（可选，但强烈推荐）
5. ⏳ 添加离线模式支持

---

**当前版本已添加调试日志和改进的错误处理** ✅
