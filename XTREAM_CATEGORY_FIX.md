# Xtream分类显示修复

## 问题描述
M3U配置可以看到分类，但Xtream配置没有显示分类信息。

## 根本原因
Xtream API的频道数据中，分类信息是通过`category_id`字段关联的，而不是直接包含`category_name`。需要：
1. 先调用`get_live_categories`接口获取分类列表
2. 建立category_id到category_name的映射
3. 在解析频道时，通过category_id查找对应的分类名称

## 修复内容

### 1. 新增分类映射方法
**文件**: `lib/services/xtream_service.dart`

**新增方法**: `_getCategoryMap()`
```dart
Future<Map<String, String>> _getCategoryMap(
  String serverUrl,
  String username,
  String password,
) async {
  // 调用 get_live_categories API
  // 返回 Map<category_id, category_name>
}
```

**功能**:
- 调用Xtream API的`get_live_categories`接口
- 解析返回的分类列表
- 构建category_id到category_name的映射表
- 错误时返回空Map，不影响频道加载

### 2. 修改authenticate方法
**修改前**:
```dart
Future<List<Channel>> authenticate(...) async {
  // 直接获取频道列表
  final response = await _dio.get(...);
  channels = _parseChannels(data, ...);
}
```

**修改后**:
```dart
Future<List<Channel>> authenticate(...) async {
  // 先获取分类映射
  final categoryMap = await _getCategoryMap(normalizedUrl, username, password);
  
  // 再获取频道列表，传入分类映射
  final response = await _dio.get(...);
  channels = _parseChannels(data, ..., categoryMap);
}
```

### 3. 更新_parseChannels方法
**修改前**:
```dart
List<Channel> _parseChannels(
  dynamic channelsData,
  String configId,
  String serverUrl,
  String username,
  String password,
) {
  // 直接从 category_name 字段获取分类
  final category = item['category_name']?.toString();
}
```

**修改后**:
```dart
List<Channel> _parseChannels(
  dynamic channelsData,
  String configId,
  String serverUrl,
  String username,
  String password,
  Map<String, String> categoryMap,  // 新增参数
) {
  // 优先从 category_id 查找分类名称
  String? category;
  final categoryId = item['category_id']?.toString();
  if (categoryId != null && categoryMap.containsKey(categoryId)) {
    category = categoryMap[categoryId];
  } else {
    // 降级：直接使用 category_name（如果有）
    category = item['category_name']?.toString();
  }
}
```

### 4. 优化getCategories方法
**修改前**:
```dart
Future<List<String>> getCategories(...) async {
  // 直接解析API响应
  final response = await _dio.get(...);
  // 提取 category_name
}
```

**修改后**:
```dart
Future<List<String>> getCategories(...) async {
  // 复用 _getCategoryMap 方法
  final categoryMap = await _getCategoryMap(normalizedUrl, username, password);
  return categoryMap.values.toList()..sort();
}
```

## 数据流程

### Xtream API调用顺序
1. **获取分类列表**
   ```
   GET /player_api.php?username=xxx&password=xxx&action=get_live_categories
   返回: [
     {"category_id": "1", "category_name": "Sports"},
     {"category_id": "2", "category_name": "News"},
     ...
   ]
   ```

2. **获取频道列表**
   ```
   GET /player_api.php?username=xxx&password=xxx&action=get_live_streams
   返回: [
     {"stream_id": "123", "name": "ESPN", "category_id": "1", ...},
     {"stream_id": "456", "name": "CNN", "category_id": "2", ...},
     ...
   ]
   ```

3. **映射分类**
   ```
   category_id "1" -> "Sports"
   category_id "2" -> "News"
   ```

### M3U vs Xtream 分类处理对比

| 特性 | M3U | Xtream |
|------|-----|--------|
| 分类字段 | `group-title` | `category_id` |
| 获取方式 | 直接从文件解析 | 需要额外API调用 |
| 分类名称 | 直接包含在频道数据中 | 需要通过ID映射 |
| 实现复杂度 | 简单 | 较复杂 |

## UI显示

### ChannelItem组件
**文件**: `lib/views/widgets/channel_item.dart`

已经支持显示分类信息：
```dart
subtitle: channel.category != null
    ? Text(
        channel.category!,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: const TextStyle(fontSize: 12),
      )
    : null,
```

### 分类筛选器
**文件**: `lib/views/screens/channel_list_screen.dart`

已经实现：
- 自动提取所有分类
- 横向滚动的FilterChip
- 点击筛选功能

## 错误处理

### 分类获取失败
- 不会中断频道加载
- 返回空的categoryMap
- 频道的category字段为null
- 分类筛选器不显示

### 降级策略
1. 优先使用category_id映射
2. 如果映射失败，尝试使用category_name字段
3. 如果都没有，category为null

## 性能优化

### 缓存策略
- 分类映射在内存中缓存
- 与频道数据一起缓存
- 5分钟缓存有效期

### API调用优化
- 每次加载频道时只调用一次分类API
- 使用批量操作减少数据库写入

## 测试建议

1. **Xtream配置测试**:
   - 添加Xtream配置
   - 加载频道列表
   - 验证分类是否正确显示
   - 测试分类筛选功能

2. **M3U配置测试**:
   - 确保M3U配置的分类仍然正常工作
   - 验证两种配置类型的分类显示一致

3. **边界情况**:
   - 分类API调用失败
   - 频道没有category_id
   - 空分类列表

## 相关文件
- `lib/services/xtream_service.dart` - Xtream服务和分类处理
- `lib/views/widgets/channel_item.dart` - 频道项显示
- `lib/views/screens/channel_list_screen.dart` - 分类筛选UI
