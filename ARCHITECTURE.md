# 🏗️ IPTV Player 技术架构文档

本文档旨在帮助开发者理解项目的分层结构、核心逻辑流及关键文件职责。

---

## 1. 整体设计模式
项目采用 **MVVM (Model-View-ViewModel)** 架构，结合 **Service/Repository** 模式，确保 UI、业务逻辑与数据存储解耦。

*   **View**: Flutter Widgets，仅负责 UI 渲染。
*   **ViewModel**: 状态管理（Provider），驱动 UI 更新并持有业务状态。
*   **Repository**: 数据访问抽象层，负责 SQLite 增删改查。
*   **Service**: 纯功能模块，负责流解析、API 请求或播放器底层控制。

---

## 2. 目录结构与关键文件索引

### 📁 `lib/models/` (数据实体)
*   **`channel.dart`**: 定义单个直播频道的数据结构（名称、URL、Logo、分类等）。
*   **`configuration.dart`**: 定义播放源配置（M3U 订阅、Xtream 账号、本地文件）。

### 📁 `lib/services/` (核心功能引擎)
*   **`player_service.dart`**: **[关键]** 播放器底层封装。直接操作 `video_player` 和 `chewie`，管理硬件锁 (Wakelock) 和渲染 Texture 生命周期。
*   **`xtream_service.dart`**: 处理 Xtream Codes API 逻辑（登录、同步频道列表）。
*   **`m3u_service.dart`**: 高性能 M3U 文件解析引擎，包含对海量频道的正则表达式优化。

### 📁 `lib/repositories/` (持久化层)
*   **`configuration_repository.dart`**: 负责配置信息的增删改查及排序持久化。
*   **`history_repository_sqlite.dart`**: 处理播放历史的 SQLite 存取。
*   **`channel_cache_repository_sqlite.dart`**: 将海量频道列表缓存到本地数据库，避免每次打开都重新解析。

### 📁 `lib/viewmodels/` (业务逻辑/状态管理)
*   **`player_viewmodel.dart`**: **[大脑]** 控制播放状态机。处理自动重试（Watchdog）、错误捕获及 UI 状态分发。
*   **`configuration_viewmodel.dart`**: 管理主界面的配置列表状态（如拖拽排序的物理落地）。

### 📁 `lib/database/` (存储底层)
*   **`database_helper.dart`**: **[骨架]** SQLite 数据库管理。包含版本升级 (Migration) 和**自愈逻辑 (Self-healing)**，确保表结构与代码一致。

### 📁 `lib/views/` (UI 界面)
*   **`screens/player_screen.dart`**: **[核心视图]** 沉浸式播放页面。实现了逃生导航路径、自动隐藏工具栏及报错覆盖层。
*   **`widgets/channel_grid_item.dart`**: 高颜值网格布局组件，集成了 `shimmer` 加载动画。

---

## 3. 核心业务流程

### 播放自愈流程 (Playback Watchdog)
1.  `PlayerService` 监听流状态 -> 发现停顿 (Stall) 超过 15 秒。
2.  抛出 `Playback stalled` 错误。
3.  `PlayerViewModel` 捕获异常 -> 开始自动重试计数。
4.  UI 显示“自动连接中...”提示层。
5.  `PlayerViewModel` 调用 `PlayerService.play()` 重新拉流。

### 数据库自愈流程 (DB Self-healing)
1.  App 启动，调用 `DatabaseHelper.ensureSchemaConsistency()`。
2.  执行 `PRAGMA table_info` 扫描现有表结构。
3.  如果发现用户是通过旧版本升级安装，且表里缺少新增字段（如 `order_index`）。
4.  动态执行 `ALTER TABLE` 补齐字段，防止业务层 SQL 报错。

---

## 4. 关键构建脚本
*   **`build_all.sh`**: 统一构建入口。支持一键打包 Android APK 和 macOS App，并自动进行产物归档。

---
**Last Updated**: 2026-03-07
**Team**: Gemini OmG Team
