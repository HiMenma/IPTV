# IPTV Player 项目代码结构与核心逻辑分析报告

> 分析日期：2026-10-14 ｜ 分析对象：`/Users/menmapro/Documents/GitHub/IPTV`（分支 `dev_flutter`，HEAD `698b552`）
> 分析方式：软件团队协作产出（架构师结构分析 + QA 质量分析），由交付总监对全部结论逐条核验（含 `flutter analyze` / `flutter test` 实证）。
> 代码规模：`lib/` 36 个 Dart 文件约 4,912 行；`test/` 20 个测试文件；依赖 57 个包。

---

## 一、项目定位与技术栈概览

### 1.1 项目定位

一个**跨平台 IPTV 电视直播播放器**：用户添加多种来源的频道配置（Xtream Codes API / M3U 网络播放列表 / 本地 M3U 文件 / 单条直链），应用拉取频道列表并提供搜索、分类筛选、收藏、观看历史与视频播放（含断流自动重连）。

实际落地平台为 **Android + macOS**（`android/`、`macos/` 目录齐全，构建脚本覆盖）。但存在文档与代码不一致：`lib/README.md:45-51` 宣称支持 Web/Windows/Linux 且依赖 `aliplayer_widget`（阿里云播放器）——该依赖不在 `pubspec.yaml` 中；而 `DEV_NOTES.md:60` 又称"已剥离所有 Web 相关冗余代码"，代码中却仍保留 `kIsWeb` 分支（`channel_list_screen.dart:5,285-287`）。**结论：仅 Android/macOS 是真实目标平台。**

### 1.2 技术栈

| 层 | 选型 | 依据 |
|---|---|---|
| 框架 | Flutter（Dart SDK `>=3.4.0 <4.0.0`） | `pubspec.yaml:6-7` |
| 状态管理 | Provider 6.x（`ChangeNotifier` + `Consumer`） | `pubspec.yaml:12`；`lib/main.dart:30-36` |
| 架构模式 | MVVM + Service/Repository 分层 | 全库结构，`ARCHITECTURE.md:36` |
| 网络 | dio 5.x（唯一实际使用的 HTTP 客户端） | `xtream_service.dart:2`、`m3u_service.dart:2` |
| 播放 | video_player 2.9 + chewie 1.8（`isLive: true`） | `pubspec.yaml:23-24`；`player_service.dart:39-68` |
| 持久化 | sqflite（4 张表：configurations/favorites/history/channel_cache） | `pubspec.yaml:35`；`database_helper.dart:84-133` |
| 其他 | shared_preferences（仅主题）、file_picker、path_provider、cached_network_image、shimmer、wakelock_plus、uuid、intl | `pubspec.yaml` |

**冗余依赖**：`http: ^1.3.0`（`pubspec.yaml:14`）在 `lib/` 中零 import（已 grep 验证），与 dio 功能重复，建议移除。

### 1.3 目录职责

| 目录/文件 | 职责 | 备注 |
|---|---|---|
| `lib/main.dart` | 入口：初始化 Logger → `runApp` → `MultiProvider` 装配 4 个 Provider → `MainNavigationScreen`（3 Tab：首页/收藏/历史） | 无路由表，导航用 `Navigator.push` |
| `lib/models/` | 纯数据实体：Channel、Configuration、Favorite、BrowseHistory（手写 toJson/fromMap） | 无 json_serializable/copyWith 完整性（Configuration.copyWith 缺 id/createdAt） |
| `lib/viewmodels/` | 3 个 `ChangeNotifier` 业务状态机：ConfigurationViewModel、ChannelViewModel、PlayerViewModel | 直接 new 具体 Repository/Service 实现，耦合单例 |
| `lib/repositories/` | SQLite CRUD。**实际生效**：`configuration_repository.dart`、`channel_cache_repository_sqlite.dart`、`favorite_repository_sqlite.dart`、`history_repository_sqlite.dart` | **另有 3 个 SharedPreferences 版本共约 381 行，全库零 import（死代码）** |
| `lib/services/` | XtreamService（API+解析）、M3UService（下载+isolate 解析+导出）、PlayerService（video_player/chewie 封装+看门狗） | |
| `lib/database/` | DatabaseHelper 单例：开库、`_ensureSchemaConsistency` 自愈、vacuumer | |
| `lib/providers/` | ThemeProvider（主题，shared_preferences 持久化） | |
| `lib/utils/` | AppLogger（文件日志）、ErrorHandler（重试+友好错误）、Validators | **Validators 与 getUserFriendlyMessage 生产代码零调用** |
| `lib/views/` | screens（home/channel_list/player/configuration/favorites/history）+ widgets | |
| `lib/debug/` | StorageInspector | 死代码，零引用 |
| `test/` | unit/integration/property/debug 20 个文件 | **全部无法加载（见问题 H9）** |

### 1.4 依赖规则（实际）

```
View ──► ViewModel ──► Repository ──► DatabaseHelper(单例)
                └────► Service（网络/播放）
```
实际破坏点：`channel_list_screen.dart:282` 在 UI 层直接 `new M3UService()`；ViewModel 构造函数直接 new 具体 SQLite Repository（`channel_viewmodel.dart:35-40`），未依赖抽象。

---

## 二、核心业务流程（按执行顺序）

### 2.1 应用启动与全局状态装配

```
main() (lib/main.dart:13)
 ├─ WidgetsFlutterBinding.ensureInitialized()
 ├─ AppLogger.init() → 删除并重建 iptv_debug.log（每次启动清空，app_logger.dart:15-18）
 ├─ DatabaseHelper.initPlatformFactory() → 仅 debugPrint，空实现（database_helper.dart:16-18）
 └─ runApp(MyApp)
      └─ MultiProvider（main.dart:30-36）：ThemeProvider / ConfigurationViewModel / ChannelViewModel / PlayerViewModel
           └─ MaterialApp(home: MainNavigationScreen)
                └─ IndexedStack[HomeScreen, FavoritesScreen, HistoryScreen] + NavigationBar
```
数据库是**懒加载**：首次访问 `DatabaseHelper.database` getter 时才 `openDatabase` + `_ensureSchemaConsistency`（`database_helper.dart:20-25`）。注意 `initPlatformFactory` 是空操作——桌面端 sqflite 需要 `sqflite_common_ffi`，当前未引入，macOS 构建的数据库可用性存疑。

### 2.2 配置 CRUD 与排序

```
HomeScreen.initState → ConfigurationViewModel.loadConfigurations()（home_screen.dart:21-23）
  → ConfigurationRepository.getAll()（configuration_repository.dart:14-29）
      ORDER BY order_index ASC, created_at DESC
新增/编辑：ConfigurationScreen._saveConfiguration()（configuration_screen.dart:177-220）
  → addConfiguration / updateConfiguration → repository.add/update → loadConfigurations 全量刷新
拖拽排序：ReorderableListView.onReorder → reorderConfigurations（configuration_viewmodel.dart:41-60）
  ① 内存 List 重排 + notifyListeners（乐观更新）
  ② repository.updateOrder(items)（batch update order_index）
删除：_deleteConfiguration → deleteConfiguration → repository.delete + cacheRepository.clearCache
     （configuration_viewmodel.dart:87-98；不清 favorites/history → 孤儿数据，见问题 M25）
刷新按钮：refreshConfiguration → 仅清 channel_cache（DB 层）→ home_screen 弹"Refreshed"
     （Service 内存缓存未清 → 刷新实际无效，见问题 H3）
```

### 2.3 频道加载（缓存优先 + 回写）

```
ChannelListScreen.initState → ChannelViewModel.loadChannels(configId)（channel_list_screen.dart:39-41）
 ① ConfigurationRepository.getById(configId)
 ② !forceRefresh → ChannelCacheRepositorySQLite.loadChannels()（查 SQLite channel_cache）
     命中且非空 → 直接返回（channel_viewmodel.dart:60-72）
 ③ 未命中/forceRefresh → 按 config.type 分派：
     xtream    → XtreamService.getChannels()（2 次 dio：get_live_categories + get_live_streams，主线程组装）
     m3uNetwork→ M3UService.parseNetworkFile()（dio 下载 + compute isolate 解析）
     m3uLocal  → M3UService.parseLocalFile()（读文件 + compute isolate 解析）
     directLink→ 内存构造单频道 Channel(id=config.id)
 ④ saveChannels(configId, channels) 全量回写 DB（先 delete 后 batch insert）
 ⑤ _loadFavoriteIds() 全量加载收藏 ID 集合
 catch：_error = 'Failed to load channels: $e' 且 _channels = []（清空已加载列表，见问题 H5）
```
**已知缺陷**：③ 的四个分支都没有把 `forceRefresh` 透传给 Service（`channel_viewmodel.dart:83,93,98`），Service 内存缓存（Xtream 10 分钟 / M3U 5 分钟）会返回旧数据——"刷新"功能对用户不可见地失效（问题 H3）。另外 Xtream 分支的 JSON 组装在主线程（`xtream_service.dart:118-142`，无 compute），万级频道时阻塞 UI（问题 M17）。

### 2.4 播放与自动重连（看门狗状态机）

```
PlayerScreen.initState → postFrameCallback → PlayerViewModel.playChannel(channel)（player_screen.dart:24-29）
  → PlayerService.play(streamUrl)（player_service.dart:31-90）
      ① _updateState(preparing) → _disposeControllers() + _stopStallTimer()
      ② VideoPlayerController.networkUrl(httpHeaders: VLC UA)
      ③ initialize().timeout(35s)
      ④ new ChewieController(isLive: true, autoPlay: true)
      ⑤ setVolume → macOS 补 1s 延迟 + play() → WakelockPlus.enable()
      ⑥ _updateState(playing)
  → HistoryRepositorySQLite.add(channel.id)（与 play 同一 try，写失败会误报 error，见问题 H6）

错误/停顿通道：
  视频控制器 listener（player_service.dart:94-113）
    value.hasError        → error + errorStream
    value.isBuffering && playing → _startStallTimer()（15s 阈值）
    停顿超时（:118-122）   → error + errorStream('Playback stalled')
  PlayerViewModel._handlePlaybackError（player_viewmodel.dart:68-86）
    retryCount < 3 → _error='Retrying (n/3)' + Timer(3s) → _performReconnect() → play(isRetry: true)
    else           → _state = error（UI 显示错误遮罩：Retry / Back）
  stateStream listener（:52-59）：playing → retryCount=0 且清空 _error
```
**状态枚举**：`PlayerState { idle, preparing, prepared, playing, paused, stopped, error, retrying }`（`player_service.dart:10`）。`prepared` 永不到达（死状态）。UI 消费方：`player_screen.dart:66-75`（preparing/idle 转圈；retrying 橙色遮罩；error 全屏遮罩）。全屏状态 `_isFullscreen`/`toggleFullscreen` 无任何 `SystemChrome` 调用，功能实际不存在（问题 L29）。

### 2.5 收藏与历史

```
收藏：channel_list_screen onFavoriteToggle → ChannelViewModel.toggleFavorite(channelId)
      （channel_viewmodel.dart:209-235）
      → FavoriteRepositorySQLite.add/remove（insert/delete，先查重）
      → _favoriteIds 内存集合同步；_favorites.removeWhere 即时移除
收藏页：FavoritesScreen → loadFavorites → favoriteRepository.getAll()（added_at DESC）
      → cacheRepository.getChannelsByIds(ids)（IN 查询，500/批，无 ORDER BY）
      → 未按收藏时间重排（顺序乱跳，见问题 M15）
历史：playChannel → historyRepository.add(channelId)（replace，更新 watched_at）
历史页：loadHistory → historyRepository.getAll()（watched_at DESC）
      → getChannelsByIds → channelMap 按 ID 重排（顺序正确，与收藏不对称）
```
**结构性耦合**：`_favorites`/`_history` 持有的是从 `channel_cache` 重建的 Channel 对象，缓存被清（刷新/换配置）后收藏页只剩 channelId、显示为空（架构师标注：本项目的根本设计约束）。

### 2.6 主题切换

```
HomeScreen AppBar PopupMenu → ThemeProvider.setThemeMode(mode)（home_screen.dart:37）
  → _saveThemeMode（shared_preferences 'theme_mode' 存 index）+ notifyListeners
启动时 _loadThemeMode() 异步读回（构造函数中调用，不 await，theme_provider.dart:20-27）
```

---

## 三、存在的问题与改进点（按严重程度分级）

> 实证依据：`flutter analyze` 共 199 个问题（lib/ 86 个，多为 avoid_print；test/ 113 个含大量编译错误）；`flutter test` 全部 20 个测试文件加载失败。以下条目均已对照源码逐条核验。

### 3.1 高危（P0 立即处理 / P1 本迭代修复）

| # | 问题 | 位置（文件:行号） | 优先级 |
|---|---|---|---|
| H1 | **发布签名密钥被 git 跟踪**：`release.keystore` 与 `release.keystore.base64` 均在版本库中，`.gitignore` 无 keystore 条目。clone 即获得签名私钥 | 仓库根目录两个文件；`.gitignore` | **P0** |
| H2 | **账号密码明文写入持久化日志**：Xtream streamUrl 形如 `/live/{user}/{pass}/{id}.m3u8`，被 `AppLogger.log` 原样写入 `iptv_debug.log` 落盘 | `lib/services/player_service.dart:33`；`lib/utils/app_logger.dart:13,34` | P1 |
| H3 | **"刷新配置"功能无效**：只清 DB 缓存；Service 内存缓存未清且 `loadChannels` 不透传 `forceRefresh`，10 分钟（Xtream）/5 分钟（M3U）内仍返回旧列表，UI 却提示"Refreshed" | `lib/viewmodels/configuration_viewmodel.dart:100-107`、`lib/viewmodels/channel_viewmodel.dart:60-73,83,93,98`、`lib/services/xtream_service.dart:77-81,155`、`lib/services/m3u_service.dart:33-37,57-61`、`lib/views/screens/home_screen.dart:181-189` | P1 |
| H4 | **数据库文件路径不一致**：初始化用 `getApplicationDocumentsDirectory()`，删除/统计用 `getDatabasesPath()`（Android 上是两个不同目录）→ `deleteDatabase()` 静默无效、`getDatabaseSize()` 恒为 0 | `lib/database/database_helper.dart:28-29` vs `:141,150` | P1 |
| H5 | **加载频道失败会清空已加载列表**：catch 中 `_channels = []`，弱网下一次刷新失败即整列表丢失 | `lib/viewmodels/channel_viewmodel.dart:121-124` | P1 |
| H6 | **写历史失败误报播放错误**：`_historyRepository.add()` 与 `play()` 同一 try，DB 写失败把 `_state=error`，错误遮罩盖住正常播放画面 | `lib/viewmodels/player_viewmodel.dart:127-134` | P1 |
| H7 | **重连竞态**：手动 Retry（`playChannel`）不取消 `_reconnectTimer`，自动重连与手动重试并发进入 `play()`，互相 dispose 控制器，导致状态错乱/黑屏 | `lib/viewmodels/player_viewmodel.dart:76-80,116-135`；`lib/services/player_service.dart:31-36` | P1 |
| H8 | **凭证明文存储**：Xtream 用户名/密码明文存 SQLite `configurations.credentials`；`stream_url` 内嵌 user/password 落库 `channel_cache.stream_url`；数据库无加密 | `lib/models/configuration.dart:42`、`lib/services/xtream_service.dart:123`、`lib/repositories/channel_cache_repository_sqlite.dart:29` | P1 |
| H9 | **测试套件完全失效**：`test/` 引用了 `lib/` 中不存在的 API（`PlayerService.closeStreams`、`XtreamService.authenticate`、`ConfigurationRepository.save`、`Configuration` 缺 `updatedAt` 命名参数、`sqflite_common_ffi` 未声明依赖等），flutter analyze 在 test/ 检出 113 个问题（大量编译错误），`flutter test` 20 个文件全部加载失败 | `test/property/player_properties_test.dart:62`、`test/property/xtream_properties_test.dart:46`、`test/property/startup_properties_test.dart:60`、`test/unit/repositories/sqlite_repositories_test.dart:2` 等 | P1 |
| H10 | **CI 工作流与本项目技术栈不符**：使用 Compose Multiplatform 模板（`composeApp/build/compose/binaries/...`）、无 setup-flutter 与 `flutter build`、`chmod +x ./gradlew` 排在首次 `./gradlew` 之后、连续构建两次 release、产物路径写成 composeApp（实际 Flutter 为 `android/app/build/outputs/apk/`、`build/app/outputs/flutter-apk/`） | `.github/workflows/build-release.yml:48,55-59,86-103,109,116` | P1 |
| H11 | **Release 实际用 debug 密钥签名**，而 BUILD_GUIDE 声称"已配置签名密钥，打包时自动签名"——产出的 release APK 无法上架 | `android/app/build.gradle.kts:36-38` vs `BUILD_GUIDE.md:68` | P1 |
| H12 | **缺少全局错误捕获**：无 `runZonedGuarded` / `FlutterError.onError`，ViewModel 的 `rethrow` 与未 await 异步错误无归宿，release 包异常无日志 | `lib/main.dart:13-23` | P1 |

### 3.2 中危（边界情况 / 性能 / 错误处理缺失）

| # | 问题 | 位置（文件:行号） | 优先级 |
|---|---|---|---|
| M13 | `ErrorHandler.getUserFriendlyMessage` 全仓库无调用方，UI 直接展示原始异常字符串（含内部路径/URL） | `lib/utils/error_handler.dart:51`；`lib/viewmodels/channel_viewmodel.dart:122,159,195,231`、`lib/views/screens/configuration_screen.dart:216` | P1 |
| M14 | 大列表每次 build 全量过滤 + 分类集合计算，每次击键 setState 全量重建（万级频道卡顿；与 DEV_NOTES"50,000+ 频道 120fps"宣称矛盾） | `lib/views/screens/channel_list_screen.dart:54-55,135,252-269` | P1 |
| M15 | 收藏列表丢失"最近收藏优先"排序：history 有 channelMap 重排，favorites 没有 | `lib/viewmodels/channel_viewmodel.dart:146-155`（对比 `:180-191`） | P1 |
| M16 | 拖拽排序乐观更新失败无回滚，且 `updateOrder` catch 不 rethrow，内存与 DB 静默不一致 | `lib/viewmodels/configuration_viewmodel.dart:41-60`、`lib/repositories/configuration_repository.dart:107-125` | P1 |
| M17 | Xtream 频道 JSON 组装在主线程（无 isolate），万级频道阻塞 UI | `lib/services/xtream_service.dart:118-142`（对比 `m3u_service.dart:47,73` 用了 compute） | P2 |
| M18 | M3U 解析对 UTF-8 BOM/前导空行误判为 "Invalid M3U format" | `lib/services/m3u_service.dart:87-90` | P2 |
| M19 | 不安全类型强转：`credentials['serverUrl'] as String` 缺键即 TypeError；`ConfigType.values.byName` 遇未知枚举名抛 ArgumentError | `lib/viewmodels/channel_viewmodel.dart:80-82,92,97,101`、`lib/models/configuration.dart:56` | P2 |
| M20 | `Configuration.fromMap` 单条解析失败被 `getAll` 的 catch 吞成 `return []` → 一条坏数据导致全部配置从 UI 消失（静默数据丢失） | `lib/models/configuration.dart:52-64`、`lib/repositories/configuration_repository.dart:22-28` | P2 |
| M21 | `_ensureSchemaConsistency` 的 catch 吞掉所有异常，schema 修复失败后带病运行；且自愈清单不含 `channel_cache` 表（旧版本 DB 缺该表时查询全失败被吞成空） | `lib/database/database_helper.dart:39-82`（清单仅 history/configurations/favorites） | P2 |
| M22 | Service 内存缓存只增不汰：`clearCache`/`clearAllCache` 生产代码零调用，TTL 到期不淘汰，多配置万级频道内存持续增长 | `lib/services/xtream_service.dart:24-28,144-145`、`lib/services/m3u_service.dart:23-27,180-193` | P2 |
| M23 | `_performReconnect` 的"刷新 token"是假动作：只打日志不重新获取 URL，token 过期场景重连必失败（与 README"token expiry 自动重连"宣称不符） | `lib/viewmodels/player_viewmodel.dart:94-107` | P2 |
| M24 | Android 全局允许明文流量且信任用户安装的 CA（`certificates src="user"`），公共 WiFi 下可被中间人窃取凭证 | `android/app/src/main/AndroidManifest.xml`、`android/app/src/main/res/xml/network_security_config.xml` | P2 |
| M25 | 删除配置不清除其频道的收藏/历史，留下孤儿数据；重新添加同配置后 UUIDv5 同 ID 使孤儿记录"复活" | `lib/viewmodels/configuration_viewmodel.dart:87-98` | P2 |
| M26 | `AppLogger.log` 每次写盘 `writeAsStringSync + flush:true`，同步 IO 压平台线程，播放期日志密集时掉帧 | `lib/utils/app_logger.dart:28-39` | P2 |
| M27 | `BuildContext` 跨异步间隙使用（`use_build_context_synchronously`，flutter analyze 实证） | `lib/views/screens/home_screen.dart:22,177,185`、`lib/views/screens/channel_list_screen.dart:40` | P2 |

### 3.3 低危（死代码 / 重复代码 / 可维护性 / 文档不一致）

| # | 问题 | 位置（文件:行号） | 优先级 |
|---|---|---|---|
| L28 | 3 个 SharedPreferences 版 Repository 共约 381 行，全库零 import（死代码） | `lib/repositories/channel_cache_repository.dart`、`history_repository.dart`、`favorite_repository.dart` | P2 |
| L29 | `PlayerControls`（171 行）零使用；`toggleFullscreen`/`isFullscreen` 无 `SystemChrome` 调用，全屏功能实际不存在 | `lib/views/widgets/player_controls.dart`、`lib/viewmodels/player_viewmodel.dart:23,149-152` | P2 |
| L30 | `StorageInspector`（78 行）零引用，且 `clearAll()` 会 `prefs.clear()` 误伤主题设置 | `lib/debug/storage_inspector.dart:73-77` | P2 |
| L31 | `Validators`（134 行）生产代码零使用，配置表单用内联 `'Required'` 占位校验，URL 格式无校验 | `lib/utils/validators.dart`（仅 test 引用）、`lib/views/screens/configuration_screen.dart:112,134,140,147,157` | P2 |
| L32 | Xtream 账号状态是死功能：`getAccountInfo` 无人调用，`accountStatus`/`expirationDate`/`lastRefreshed` 从未被写入，`ConfigurationCard` 对应 UI 永不显示 | `lib/services/xtream_service.dart:32-75`、`lib/views/widgets/configuration_card.dart:49-101` | P2 |
| L33 | `http` 依赖未使用（lib/ 零 import） | `pubspec.yaml:14` | P2 |
| L34 | `channel_grid_item.dart` 存在死类 `BorderAlphaAnalysis` 和无意义三元（两支相同） | `lib/views/widgets/channel_grid_item.dart:34,136-138` | P2 |
| L35 | 未使用字段：`M3UService._uuid`、`PlayerViewModel._xtreamService/_m3uService`（每次构造多建 2 个 Dio） | `lib/services/m3u_service.dart:16`、`lib/viewmodels/player_viewmodel.dart:17-18` | P2 |
| L36 | `print`/`debugPrint`/`AppLogger` 三套日志混用，30+ 处 `print`（flutter analyze：lib/ 86 个 info 中绝大多数是 avoid_print） | 各 `*_repository_sqlite.dart`、`debug/` 等 | P2 |
| L37 | UI 层直接 `new M3UService()` 做导出，违反分层 | `lib/views/screens/channel_list_screen.dart:282` | P2 |
| L38 | 导出 M3U 先写临时文件却只用剪贴板，临时文件从不使用也从不清理 | `lib/views/screens/channel_list_screen.dart:289-299` | P2 |
| L39 | 文档与代码不一致（一组）：①DEV_NOTES 称 Web 已剥离但代码有 `kIsWeb` 分支；②README"Glassmorphism UI"无实现；③DEV_NOTES"50,000+ 频道 120fps"与 M14 矛盾；④README"token expiry 自动重连"与 M23 矛盾；⑤`lib/README.md` 宣称 `aliplayer_widget` 依赖与 Web/Windows/Linux 支持；⑥`docs/DEV_NOTES.md` 与根目录 `DEV_NOTES.md` 是内容不同的重复副本 | `DEV_NOTES.md:12,60`、`README.md`、`lib/README.md:41,45-51`、`docs/DEV_NOTES.md` | P2 |
| L40 | history 表无上限，随观看频道数无限增长，无清理策略 | `lib/repositories/history_repository_sqlite.dart:26-44` | P2 |
| L41 | `executeWithRetry` 固定 2s 重试无指数退避；重试期间无取消机制 | `lib/utils/error_handler.dart:28-49` | P3 |
| L42 | Xtream 认证判断 `userInfo['auth'] == 0` 严格相等，服务端返回字符串 `"0"` 时被判为已认证 | `lib/services/xtream_service.dart:53` | P3 |
| L43 | `WRITE_EXTERNAL_STORAGE` 未设 `maxSdkVersion`，现代 Android 上为无操作权限（file_picker 不需要它） | `android/app/src/main/AndroidManifest.xml` | P3 |
| L44 | `getCacheInfo` 为拿计数执行 3 次查询且 `loadChannels` 全量拉取只为 `.length` | `lib/repositories/channel_cache_repository_sqlite.dart:178-195` | P3 |
| L45 | `exportToM3U` 把双引号转义为 `&quot;`，但解析端正则不解转义，导出再导入后名称含字面 `&quot;` | `lib/services/m3u_service.dart:140-153` vs `:108-109` | P3 |
| L46 | Windows 上 `_localFilePath.split('/').last` 无法提取文件名（反斜杠路径） | `lib/views/screens/configuration_screen.dart:118` | P3 |
| L47 | `toggleFavorite` 无并发/防重复点击保护；添加收藏后 `_favorites` 列表不更新（代码注释自认） | `lib/viewmodels/channel_viewmodel.dart:209-235` | P3 |
| L48 | `database` getter 并发首次访问会双跑 `_initDatabase` + `_ensureSchemaConsistency` | `lib/database/database_helper.dart:20-25` | P3 |
| L49 | `ThemeProvider._loadThemeMode` 无 try/catch，SharedPreferences 读取异常会成为未捕获异步错误 | `lib/providers/theme_provider.dart:20-27` | P3 |
| L50 | 缓存加载无 `ORDER BY`，`loadChannels`/`getChannelsByIds` 返回顺序即 SQLite 行序 | `lib/repositories/channel_cache_repository_sqlite.dart:49-53,155-159` | P3 |
| L51 | 已弃用 API：`withOpacity`、`surfaceVariant`（flutter analyze 实证，Flutter 3.27+ 警告） | `lib/views/screens/player_screen.dart:109`、`lib/views/widgets/channel_grid_item.dart:95,112,128`、`lib/views/widgets/channel_list_item.dart:71,91`、`lib/views/widgets/configuration_card.dart:62,180` | P3 |

**统计**：高危 12 项（P0×1、P1×11），中危 15 项，低危 24 项，合计 51 项。

---

## 四、改进建议（按优先级排序的可执行修复清单）

### P0 — 立即处理

**H1 签名密钥入库**
- `git rm --cached release.keystore release.keystore.base64`，`.gitignore` 增加 `*.keystore`、`*.jks`、`release.keystore.base64`。
- 该私钥已进入 git 历史，**必须视为已泄露**：在 Google Play Console 重置上传密钥并重新签发；不要只是删除文件。
- CI 已从 `secrets.KEYSTORE_FILE` 解码（`.github/workflows/build-release.yml:87`），仓库内文件本就多余，确认无引用后删除。

### P1 — 本迭代修复（高危 12 项）

- **H2 日志脱敏**：`player_service.dart:33` 改为记录脱敏 URL（把 `/live/{user}/{pass}/` 替换为 `/live/***/***/`）；或在 `AppLogger` 增加敏感字段过滤。同时排查其他打印 streamUrl 的位置。
- **H3 修复刷新链路**：① `channel_viewmodel.dart:83,93,98` 三个调用点把 `forceRefresh` 透传给 Service；② `ConfigurationViewModel.refreshConfiguration` 一并清理 Service 内存缓存（注入 XtreamService/M3UService 或引入统一 CacheManager）；③ `home_screen.dart:186` 提示改为"已清除缓存，下次打开重新加载"或等真实刷新完成再提示。
- **H4 统一数据库路径**：`database_helper.dart` 抽取 `Future<String> get _dbPath`（基于 `getApplicationDocumentsDirectory()`），`_initDatabase`/`deleteDatabase`/`getDatabaseSize` 三处共用；`test/unit/repositories/sqlite_repositories_test.dart:18` 依赖 deleteDatabase 做清理，修复后需复跑。
- **H5 失败不清空列表**：`channel_viewmodel.dart:121-124` catch 中删除 `_channels = []`；`channel_list_screen.dart:167-169` 改为"错误提示条 + 保留旧列表"。
- **H6 历史写入与播放解耦**：把 `player_viewmodel.dart:128` 的 `_historyRepository.add` 移出主 try（单独 try/catch，失败仅记日志）。
- **H7 消除重连竞态**：`playChannel` 开头 `_reconnectTimer?.cancel(); _reconnectTimer = null;`；`PlayerService.play` 加在途保护（新 play 到达时取消/等待旧请求）。
- **H8 凭证加密**：短期方案——DB 启用 SQLCipher（sqflite 支持）或 credentials 迁 `flutter_secure_storage`；`channel_cache.stream_url` 只存 streamId、播放时动态拼接；并清理历史明文数据。
- **H9 修复测试套件**：先删除/重写引用不存在 API 的断言（`closeStreams`/`authenticate`/`save`/`updatedAt`/`sqflite_common_ffi`——后者需在 pubspec dev_dependencies 声明），以 `flutter analyze` 清零 test/ 错误、`flutter test` 全绿为完成标准；建议优先为 H3/H4/H5 的修复补回归测试。
- **H10 重写 CI**：desktop job 改用 `subosito/flutter-action` + `flutter config --enable-*-desktop` + `flutter build macos/linux/windows`；android job 先 setup-flutter 再 `flutter build apk --release`，产物路径改 `build/app/outputs/flutter-apk/*.apk`；修正 chmod 顺序，去掉重复构建。
- **H11 配置真签名**：`android/app/build.gradle.kts` 增加从 `key.properties`（已被 .gitignore 忽略）读取的 signingConfig，release 指向它；同步修正 `BUILD_GUIDE.md:68` 的描述。
- **H12 全局错误兜底**：`main.dart` 改 `runZonedGuarded(() { ... runApp(...); }, (e, s) => AppLogger.log('Uncaught: $e\n$s'))`，并设置 `FlutterError.onError`。

### P2 — 排入后续迭代（中危 + 重要低危）

- M13：ViewModel 的 `_error` 赋值统一走 `ErrorHandler.getUserFriendlyMessage(e)`（技术细节仍记 AppLogger），UI 零改动生效。
- M14：分类列表改为仅在 `viewModel.channels` 变化时计算；搜索加 200-300ms debounce；过滤逻辑可下沉 ViewModel。
- M15：仿照 `channel_viewmodel.dart:186-191` 的 channelMap 重排恢复收藏顺序。
- M16：`configuration_repository.dart:107-125` catch 中 rethrow；VM catch 恢复拖拽前快照再 notifyListeners。
- M17：Xtream 解析参照 M3U 的 `compute` 模式移入 isolate。
- M18：解析前 `content.replaceFirst('\uFEFF', '')` 并跳过前导空行再匹配 `#EXTM3U`。
- M19：credentials 读取改 `as String?` + null 检查；`ConfigType.values.byName` 包 try/catch，坏数据跳过并记日志。
- M20：`getAll` 改逐条 try/catch，坏行跳过 + 日志；仅数据库级错误才返回空并报错。
- M21：`_ensureSchemaConsistency` 失败时向上抛（或在 `database` getter 标记致命错误）；自愈清单补 `channel_cache` 表。
- M22：缓存引入 LRU/总量上限，或 TTL 过期即从 map 移除；`refreshConfiguration` 真正调用 clearCache（与 H3 合并修复）。
- M23：要么实现（xtream 类型重连时重新 `get_live_streams` 找新 URL），要么删除假代码并修正 README。
- M24：`network_security_config.xml` 去掉 `<certificates src="user" />`；如可能按域名限定 cleartext。
- M25：删除配置时按该 config 的 channel_id 批量清理 favorites/history（给现有 Repository 加批量 remove）。
- M26：改异步 `writeAsString`（去 flush）或内存缓冲批量落盘；release 可只 debugPrint。
- M27：`home_screen.dart:22`、`channel_list_screen.dart:40` 的 `Future.microtask` 中加 `mounted` 检查后再用 context（analyze 已给出确切行号）。
- L28-L32：删除 3 个 SharedPreferences 死仓库、`player_controls.dart`（或补真全屏）、`storage_inspector.dart`；把 `Validators.validateUrl` 接进 `configuration_screen.dart:134,157`；Xtream 账号状态要么接线（加载频道后调 `getAccountInfo` 回写）要么整链删除。
- L33：移除 `http` 依赖。L34：删死类、直接 `clipBehavior: Clip.antiAlias`。L35：删未使用字段。L36：`print` 统一替换 `AppLogger.log`（analyze 告警可清零）。L37：导出逻辑移入 ViewModel。L38：删临时文件写入或改 `FilePicker.saveFile`。L39：修订 README/DEV_NOTES/lib/README 五处不实宣称，删除 `docs/DEV_NOTES.md` 重复副本。L40：history 加上限（如最近 500 条）。

### P3 — 择机处理（低危剩余）

L41 指数退避 + 取消令牌；L42 `int.tryParse(userInfo['auth'].toString()) == 0`；L43 删 `WRITE_EXTERNAL_STORAGE` 或加 `maxSdkVersion="29"`；L44 用 `SELECT COUNT(*)`；L45 导出/导入统一转义策略；L46 用 `p.basename`；L47 toggleFavorite 加防重入 + 同步 `_favorites`；L48 database getter 记忆在途初始化 Future；L49 `_loadThemeMode` 包 try/catch；L50 查询补 ORDER BY；L51 替换 `withOpacity`/`surfaceVariant`。

---

## 附：本次分析的客观实证记录

| 检查 | 命令 | 结果 |
|---|---|---|
| 静态分析 | `flutter analyze` | 199 issues：lib/ 86（绝大多数 `avoid_print` info；另有 unused_field/unused_import/use_build_context_synchronously/deprecated_member_use），test/ 113（大量编译错误：undefined method `closeStreams`/`authenticate`/`save`、missing required param `updatedAt`、uri_does_not_exist `sqflite_common_ffi` 等） |
| 测试运行 | `flutter test` | 20 个测试文件全部加载失败（编译错误 + 部分 flutter_tester 环境异常）；抽跑 `test/integration/favorites_flow_test.dart` 确认编译错误（`favoriteRepository` 参数不存在） |
| git 敏感文件 | `git ls-files` | `release.keystore`、`release.keystore.base64` 被跟踪 |
| 死代码验证 | grep import | `favorite_repository.dart`/`history_repository.dart`/`channel_cache_repository.dart`/`player_controls.dart`/`storage_inspector.dart` 零 import；`Validators`/`getUserFriendlyMessage`/`getAccountInfo` 零调用 |
