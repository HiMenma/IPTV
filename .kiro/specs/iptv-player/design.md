# Design Document

## Overview

IPTV Player是一个使用Flutter框架开发的跨平台IPTV播放器应用。该应用支持Android、iOS、Web、Windows、macOS和Linux六大平台，集成Chewie开源播放器组件提供稳定的视频播放能力。

应用采用清晰的分层架构，将数据层、业务逻辑层和UI层分离，确保代码的可维护性和可测试性。核心功能包括：
- 多源支持：Xtream Codes API、M3U/M3U8本地文件和网络链接
- 配置管理：创建、命名、重命名、删除和刷新配置
- 播放功能：使用Chewie播放器播放IPTV流
- 用户功能：频道收藏、浏览历史记录
- 主题支持：深色模式和浅色模式自动切换
- 数据持久化：使用本地存储保存配置、收藏和历史

## Architecture

### 架构模式

应用采用**MVVM (Model-View-ViewModel)** 架构模式，结合**Repository模式**进行数据访问：

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer (View)                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Config Screen│  │Channel Screen│  │ Player Screen│  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└────────────┬────────────────┬────────────────┬──────────┘
             │                │                │
┌────────────┴────────────────┴────────────────┴──────────┐
│                  ViewModel Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ConfigViewModel│ │ChannelViewModel│ │PlayerViewModel│ │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└────────────┬────────────────┬────────────────┬──────────┘
             │                │                │
┌────────────┴────────────────┴────────────────┴──────────┐
│                  Service Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │XtreamService │  │  M3UService  │  │ PlayerService│  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└────────────┬────────────────┬────────────────┬──────────┘
             │                │                │
┌────────────┴────────────────┴────────────────┴──────────┐
│                 Repository Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ConfigRepo    │  │ FavoriteRepo │  │  HistoryRepo │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└────────────┬────────────────┬────────────────┬──────────┘
             │                │                │
┌────────────┴────────────────┴────────────────┴──────────┐
│                   Data Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │Local Storage │  │  HTTP Client │  │Aliyun Player │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└──────────────────────────────────────────────────────────┘
```

### 关键设计决策

1. **跨平台支持**：使用Flutter的平台抽象层，确保核心业务逻辑在所有平台上一致
2. **状态管理**：使用Provider进行状态管理，实现响应式UI更新
3. **数据持久化**：使用shared_preferences进行轻量级数据存储
4. **网络请求**：使用dio包处理HTTP请求
5. **文件操作**：使用file_picker选择本地文件，使用path_provider获取存储路径
6. **视频播放**：使用Chewie（基于video_player）提供跨平台视频播放能力
7. **主题管理**：使用Flutter的ThemeData和MediaQuery实现深色/浅色模式自动切换

## Components and Interfaces

### 1. Data Models

#### Configuration Model
```dart
enum ConfigType {
  xtream,
  m3uLocal,
  m3uNetwork
}

class Configuration {
  final String id;
  final String name;
  final ConfigType type;
  final Map<String, dynamic> credentials; // Xtream: url, username, password; M3U: filePath or url
  final DateTime createdAt;
  final DateTime? lastRefreshed;
  
  Configuration({
    required this.id,
    required this.name,
    required this.type,
    required this.credentials,
    required this.createdAt,
    this.lastRefreshed,
  });
  
  Map<String, dynamic> toJson();
  factory Configuration.fromJson(Map<String, dynamic> json);
}
```

#### Channel Model
```dart
class Channel {
  final String id;
  final String name;
  final String streamUrl;
  final String? logoUrl;
  final String? category;
  final String configId; // 关联的配置ID
  
  Channel({
    required this.id,
    required this.name,
    required this.streamUrl,
    this.logoUrl,
    this.category,
    required this.configId,
  });
  
  Map<String, dynamic> toJson();
  factory Channel.fromJson(Map<String, dynamic> json);
}
```

#### Favorite Model
```dart
class Favorite {
  final String channelId;
  final DateTime addedAt;
  
  Favorite({
    required this.channelId,
    required this.addedAt,
  });
  
  Map<String, dynamic> toJson();
  factory Favorite.fromJson(Map<String, dynamic> json);
}
```

#### BrowseHistory Model
```dart
class BrowseHistory {
  final String channelId;
  final DateTime watchedAt;
  
  BrowseHistory({
    required this.channelId,
    required this.watchedAt,
  });
  
  Map<String, dynamic> toJson();
  factory BrowseHistory.fromJson(Map<String, dynamic> json);
}
```

### 2. Service Layer

#### XtreamService
负责与Xtream Codes API交互：
```dart
class XtreamService {
  Future<List<Channel>> authenticate(String serverUrl, String username, String password);
  Future<List<Channel>> getChannels(String serverUrl, String username, String password);
  Future<List<String>> getCategories(String serverUrl, String username, String password);
}
```

#### M3UService
负责解析M3U/M3U8文件：
```dart
class M3UService {
  Future<List<Channel>> parseLocalFile(String filePath);
  Future<List<Channel>> parseNetworkFile(String url);
  String exportToM3U(List<Channel> channels);
}
```

#### PlayerService
封装Chewie播放器操作：
```dart
class PlayerService {
  Future<void> initialize();
  Future<void> play(String streamUrl);
  Future<void> pause();
  Future<void> resume();
  Future<void> stop();
  Future<void> setVolume(double volume);
  Future<void> dispose();
  ChewieController? get chewieController;
  VideoPlayerController? get videoController;
  Stream<PlayerState> get stateStream;
  Stream<String?> get errorStream;
}
```

### 3. Repository Layer

#### ConfigurationRepository
```dart
class ConfigurationRepository {
  Future<List<Configuration>> getAll();
  Future<Configuration?> getById(String id);
  Future<void> save(Configuration config);
  Future<void> update(Configuration config);
  Future<void> delete(String id);
}
```

#### FavoriteRepository
```dart
class FavoriteRepository {
  Future<List<Favorite>> getAll();
  Future<bool> isFavorite(String channelId);
  Future<void> add(String channelId);
  Future<void> remove(String channelId);
}
```

#### HistoryRepository
```dart
class HistoryRepository {
  Future<List<BrowseHistory>> getAll();
  Future<void> add(String channelId);
  Future<void> clear();
}
```

### 4. Theme Management

#### ThemeProvider
管理应用主题状态：
```dart
class ThemeProvider extends ChangeNotifier {
  ThemeMode _themeMode = ThemeMode.system;
  
  ThemeMode get themeMode => _themeMode;
  
  void setThemeMode(ThemeMode mode) {
    _themeMode = mode;
    notifyListeners();
  }
  
  ThemeData get lightTheme => ThemeData(
    brightness: Brightness.light,
    colorScheme: ColorScheme.fromSeed(
      seedColor: Colors.blue,
      brightness: Brightness.light,
    ),
    useMaterial3: true,
  );
  
  ThemeData get darkTheme => ThemeData(
    brightness: Brightness.dark,
    colorScheme: ColorScheme.fromSeed(
      seedColor: Colors.blue,
      brightness: Brightness.dark,
    ),
    useMaterial3: true,
  );
}
```

### 5. ViewModel Layer

#### ConfigurationViewModel
```dart
class ConfigurationViewModel extends ChangeNotifier {
  List<Configuration> configurations;
  
  Future<void> loadConfigurations();
  Future<void> createConfiguration(String name, ConfigType type, Map<String, dynamic> credentials);
  Future<void> renameConfiguration(String id, String newName);
  Future<void> deleteConfiguration(String id);
  Future<void> refreshConfiguration(String id);
  Future<void> exportToM3U(String id, String savePath);
}
```

#### ChannelViewModel
```dart
class ChannelViewModel extends ChangeNotifier {
  List<Channel> channels;
  List<Channel> favorites;
  List<Channel> history;
  
  Future<void> loadChannels(String configId);
  Future<void> loadFavorites();
  Future<void> loadHistory();
  Future<void> toggleFavorite(String channelId);
  Future<void> clearHistory();
}
```

#### PlayerViewModel
```dart
class PlayerViewModel extends ChangeNotifier {
  PlayerState state;
  String? error;
  
  Future<void> playChannel(Channel channel);
  Future<void> pause();
  Future<void> resume();
  Future<void> stop();
  Future<void> setVolume(double volume);
  Future<void> toggleFullscreen();
}
```

## Data Models

### 数据存储结构

使用JSON格式存储在本地：

**configurations.json**
```json
{
  "configurations": [
    {
      "id": "uuid",
      "name": "My IPTV",
      "type": "xtream",
      "credentials": {
        "serverUrl": "http://example.com",
        "username": "user",
        "password": "pass"
      },
      "createdAt": "2025-01-01T00:00:00Z",
      "lastRefreshed": "2025-01-02T00:00:00Z"
    }
  ]
}
```

**favorites.json**
```json
{
  "favorites": [
    {
      "channelId": "channel-uuid",
      "addedAt": "2025-01-01T00:00:00Z"
    }
  ]
}
```

**history.json**
```json
{
  "history": [
    {
      "channelId": "channel-uuid",
      "watchedAt": "2025-01-01T12:00:00Z"
    }
  ]
}
```

### M3U文件格式

导出的M3U文件格式：
```
#EXTM3U
#EXTINF:-1 tvg-id="" tvg-name="Channel Name" tvg-logo="http://logo.url" group-title="Category",Channel Name
http://stream.url
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Xtream authentication with valid credentials succeeds
*For any* valid Xtream server URL, username, and password, authentication should succeed and return a non-empty list of channels.
**Validates: Requirements 2.1**

### Property 2: Successful Xtream authentication persists configuration
*For any* successful Xtream authentication, the configuration should be saved to storage and be retrievable by its ID.
**Validates: Requirements 2.2**

### Property 3: Invalid Xtream credentials are rejected
*For any* invalid Xtream credentials (wrong password, invalid URL, etc.), authentication should fail and no configuration should be created.
**Validates: Requirements 2.3**

### Property 4: Xtream configuration refresh updates channel list
*For any* existing Xtream configuration, refreshing should fetch the latest channel list from the API and update the lastRefreshed timestamp.
**Validates: Requirements 2.4**

### Property 5: Xtream to M3U export round-trip preserves channels
*For any* Xtream configuration with channels, exporting to M3U format and then parsing the M3U file should produce an equivalent list of channels (same names and stream URLs).
**Validates: Requirements 2.5**

### Property 6: M3U/M3U8 parsing extracts all channels
*For any* valid M3U or M3U8 file (local or network), parsing should extract all channel entries with their names, stream URLs, and metadata.
**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

### Property 7: Invalid M3U content produces error
*For any* malformed M3U content (invalid format, missing required fields), parsing should fail gracefully and return a descriptive error message.
**Validates: Requirements 3.5**

### Property 8: M3U network configuration refresh fetches latest content
*For any* M3U network configuration, refreshing should fetch the remote file and update the channel list.
**Validates: Requirements 3.6**

### Property 9: Configuration creation persists data
*For any* valid configuration (name, type, and credentials), creating it should save it to storage and make it retrievable.
**Validates: Requirements 4.1**

### Property 10: Configuration rename preserves all other fields
*For any* configuration and new name, renaming should only change the name field while all other fields (id, type, credentials, timestamps) remain unchanged.
**Validates: Requirements 4.2**

### Property 11: Configuration deletion removes from storage
*For any* configuration, deleting it should remove it from storage such that it cannot be retrieved by its ID.
**Validates: Requirements 4.3**

### Property 12: Data persistence round-trip preserves content
*For any* data object (configuration, favorite, or history entry), saving it to storage and then loading it should produce an equivalent object.
**Validates: Requirements 4.4, 6.3, 7.2**

### Property 13: Application startup loads all saved data
*For any* set of saved configurations, favorites, and history entries, starting the application should load all of them correctly.
**Validates: Requirements 4.5, 6.4, 7.3**

### Property 14: Channel selection initiates playback
*For any* valid channel with a stream URL, selecting it should initialize the player and change the player state to playing or loading.
**Validates: Requirements 5.1**

### Property 15: Playback displays controls
*For any* channel being played, the UI should display playback controls (pause, volume, fullscreen).
**Validates: Requirements 5.2, 5.5**

### Property 16: Invalid stream URLs are handled gracefully
*For any* invalid or unavailable stream URL, attempting to play it should produce an error message without crashing the application.
**Validates: Requirements 5.3**

### Property 17: Stopping playback releases resources
*For any* active playback, stopping it should release player resources and change the player state to stopped or idle.
**Validates: Requirements 5.4, 9.4**

### Property 18: Marking channel as favorite adds to favorites list
*For any* channel, marking it as favorite should add it to the favorites list such that it appears when querying favorites.
**Validates: Requirements 6.1**

### Property 19: Unmarking favorite removes from favorites list
*For any* favorited channel, unmarking it should remove it from the favorites list such that it no longer appears when querying favorites.
**Validates: Requirements 6.2**

### Property 20: Favorites display shows all favorited channels
*For any* set of favorited channels, viewing the favorites section should display all of them.
**Validates: Requirements 6.5**

### Property 21: Playing channel records history with timestamp
*For any* channel played, it should be recorded in browse history with a timestamp indicating when it was played.
**Validates: Requirements 7.1**

### Property 22: Browse history displays in reverse chronological order
*For any* set of history entries, viewing browse history should display them sorted by timestamp in descending order (most recent first).
**Validates: Requirements 7.4**

### Property 23: Clearing history removes all entries
*For any* browse history state, clearing it should remove all history entries such that the history list is empty.
**Validates: Requirements 7.5**

### Property 24: Chewie player initializes correctly
*For any* stream URL, the player should use Chewie APIs to initialize and control playback.
**Validates: Requirements 9.2**

### Property 25: Playback errors are handled appropriately
*For any* playback error from Chewie, the application should handle the error callback and display appropriate error information to the user.
**Validates: Requirements 9.3**

### Property 26: Dark mode theme applies correctly
*For any* system theme setting (light or dark), the application should display the corresponding theme with appropriate colors and contrast.
**Validates: Requirements 10.1, 10.2**

### Property 27: Theme changes update all UI components
*For any* theme change, all UI components should update to reflect the new theme without requiring app restart.
**Validates: Requirements 10.3**

### Property 28: Player controls maintain consistent styling
*For any* theme (light or dark), player controls should maintain consistent styling and remain clearly visible.
**Validates: Requirements 10.4**

### Property 29: Text and icons have sufficient contrast
*For any* theme, all text and icons should have sufficient contrast ratios to meet accessibility standards.
**Validates: Requirements 10.5**

## Error Handling

### Network Errors
- **Xtream API failures**: Timeout, connection refused, invalid response
  - Strategy: Retry with exponential backoff, display user-friendly error messages
- **M3U network fetch failures**: URL unreachable, timeout
  - Strategy: Cache last successful fetch, allow offline access to cached data

### Parsing Errors
- **Invalid M3U format**: Missing headers, malformed entries
  - Strategy: Skip invalid entries, log warnings, continue parsing valid entries
- **Invalid Xtream API response**: Unexpected JSON structure
  - Strategy: Validate response schema, provide fallback empty channel list

### Playback Errors
- **Stream unavailable**: 404, 403, timeout
  - Strategy: Display error overlay, allow user to retry or select different channel
- **Codec not supported**: Unsupported video/audio format
  - Strategy: Display codec error message, suggest alternative stream if available
- **Player initialization failure**: Chewie or video_player fails to initialize
  - Strategy: Display error message with retry option

### Storage Errors
- **Disk full**: Cannot save configuration or favorites
  - Strategy: Display storage error, suggest clearing old data
- **Corrupted data**: Invalid JSON in storage
  - Strategy: Reset to default state, backup corrupted data for recovery

### Input Validation Errors
- **Invalid configuration name**: Empty or too long
  - Strategy: Display validation error, prevent save until corrected
- **Invalid Xtream credentials**: Missing required fields
  - Strategy: Highlight missing fields, prevent authentication attempt

## Testing Strategy

### Unit Testing

单元测试将验证各个组件的具体行为和边界情况：

**Data Models**
- JSON序列化和反序列化的正确性
- 模型验证逻辑（如空字段检查）

**Services**
- M3U解析器处理各种格式变体
- Xtream API响应解析
- 错误情况处理（网络失败、无效数据）

**Repositories**
- CRUD操作的正确性
- 数据持久化和检索

**ViewModels**
- 状态管理逻辑
- 用户操作响应

### Property-Based Testing

属性测试将使用**test**包（Dart/Flutter的标准测试框架）结合**faker**包生成随机测试数据，验证系统的通用属性。每个属性测试将运行至少100次迭代。

**测试库选择**: Dart的test包配合自定义生成器

**关键属性测试**:

1. **配置管理属性**
   - 配置的创建、重命名、删除操作的正确性
   - 数据持久化round-trip测试

2. **M3U解析属性**
   - 各种格式的M3U文件解析
   - 导出和重新导入的一致性

3. **收藏和历史属性**
   - 添加/删除操作的幂等性
   - 数据持久化的一致性
   - 历史排序的正确性

4. **播放器状态属性**
   - 状态转换的正确性
   - 资源释放的完整性

**属性测试标注格式**:
每个属性测试必须使用以下格式标注：
```dart
// Feature: iptv-player, Property 5: Xtream to M3U export round-trip preserves channels
test('xtream export and import preserves channels', () {
  // Test implementation
});
```

### Integration Testing

集成测试将验证组件之间的交互：

- 完整的配置创建流程（UI → ViewModel → Service → Repository）
- 播放流程（频道选择 → 播放器初始化 → 播放控制）
- 数据同步（配置更新 → UI刷新）

### Platform-Specific Testing

由于应用支持多平台，需要在各平台上进行测试：

- **Android/iOS**: 使用Flutter的集成测试框架
- **Web**: 使用浏览器自动化测试
- **Desktop (Windows/macOS/Linux)**: 使用Flutter的桌面测试工具

### Test Data Generation

使用faker包生成测试数据：
- 随机配置名称、URL、凭证
- 随机M3U内容（不同数量的频道、不同的元数据）
- 随机时间戳用于历史记录测试

### Continuous Testing

- 所有测试应在CI/CD管道中自动运行
- 属性测试应使用固定的随机种子以确保可重现性
- 失败的属性测试应记录导致失败的具体输入

## Implementation Notes

### Flutter Dependencies

```yaml
dependencies:
  flutter:
    sdk: flutter
  # 状态管理
  provider: ^6.0.0
  # 网络请求
  dio: ^5.0.0
  # 本地存储
  shared_preferences: ^2.0.0
  # 文件选择
  file_picker: ^5.0.0
  # 路径管理
  path_provider: ^2.0.0
  # UUID生成
  uuid: ^3.0.0
  # 视频播放器
  video_player: ^2.9.0
  chewie: ^1.8.0
  # 屏幕常亮
  wakelock_plus: ^1.2.0
  # 图片缓存
  cached_network_image: ^3.4.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  # 测试数据生成
  faker: ^2.0.0
  # 集成测试
  integration_test:
    sdk: flutter
```

### Project Structure

```
lib/
├── main.dart
├── models/
│   ├── configuration.dart
│   ├── channel.dart
│   ├── favorite.dart
│   └── browse_history.dart
├── services/
│   ├── xtream_service.dart
│   ├── m3u_service.dart
│   └── player_service.dart
├── repositories/
│   ├── configuration_repository.dart
│   ├── favorite_repository.dart
│   └── history_repository.dart
├── providers/
│   └── theme_provider.dart
├── viewmodels/
│   ├── configuration_viewmodel.dart
│   ├── channel_viewmodel.dart
│   └── player_viewmodel.dart
├── views/
│   ├── screens/
│   │   ├── home_screen.dart
│   │   ├── configuration_screen.dart
│   │   ├── channel_list_screen.dart
│   │   ├── player_screen.dart
│   │   ├── favorites_screen.dart
│   │   └── history_screen.dart
│   └── widgets/
│       ├── configuration_card.dart
│       ├── channel_item.dart
│       └── player_controls.dart
└── utils/
    ├── constants.dart
    └── validators.dart

test/
├── unit/
│   ├── models/
│   ├── services/
│   ├── repositories/
│   └── viewmodels/
├── property/
│   ├── configuration_properties_test.dart
│   ├── m3u_properties_test.dart
│   ├── favorite_properties_test.dart
│   └── player_properties_test.dart
└── integration/
    └── app_flow_test.dart
```

### Platform-Specific Considerations

**Android**
- 需要网络权限：`<uses-permission android:name="android.permission.INTERNET"/>`
- 需要存储权限（如果访问本地文件）

**iOS**
- 需要在Info.plist中声明网络使用
- 需要文件访问权限声明

**Web**
- CORS问题：某些M3U URL可能需要代理
- 播放器可能需要特定的Web配置

**Desktop (Windows/macOS/Linux)**
- 文件系统访问权限
- 窗口管理和全屏支持

### Theme Implementation

**主题配置**
- 使用Material 3设计规范
- 支持系统主题自动切换（ThemeMode.system）
- 提供手动切换选项（可选功能）

**颜色方案**
- 浅色主题：使用ColorScheme.fromSeed with Brightness.light
- 深色主题：使用ColorScheme.fromSeed with Brightness.dark
- 播放器控制：在两种主题下保持一致的半透明黑色背景

**主题应用**
- 在MaterialApp中配置theme和darkTheme
- 使用ThemeProvider管理主题状态
- 所有Widget使用Theme.of(context)获取当前主题颜色

### Performance Considerations

- **频道列表缓存**: 避免重复解析大型M3U文件
- **懒加载**: 大型频道列表使用虚拟滚动
- **图片缓存**: 频道Logo使用缓存避免重复下载
- **后台刷新**: 配置刷新在后台线程执行，不阻塞UI
- **视频播放优化**: 使用Chewie的缓存和预加载功能

### Security Considerations

- **凭证存储**: Xtream凭证使用flutter_secure_storage加密存储
- **HTTPS**: 优先使用HTTPS连接
- **输入验证**: 所有用户输入进行验证，防止注入攻击
