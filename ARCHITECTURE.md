# 🏗️ IPTV Player 技术架构文档

本文档旨在帮助开发者理解项目的分层结构、核心逻辑流及关键文件职责。

---

## 1. 目录结构地图 (Project Map)

```text
.
├── android/                # Android 原生配置与权限声明
├── macos/                  # macOS 原生工程与渲染设置
├── lib/                    # 核心业务代码 (Dart)
│   ├── database/           # SQLite 底层封装与自愈逻辑
│   ├── models/             # 频道与配置的数据实体
│   ├── providers/          # 全局状态管理 (主题等)
│   ├── repositories/       # 数据持久化层 (CRUD)
│   ├── services/           # 核心功能逻辑 (播放控制、流解析)
│   ├── utils/              # 通用工具类 (日志、错误处理)
│   ├── viewmodels/         # 业务状态机 (驱动 UI 更新)
│   └── views/              # UI 视图层
│       ├── screens/        # 全屏页面
│       └── widgets/        # 可复用 UI 组件
├── test/                   # 单元测试与集成测试
├── ARCHITECTURE.md         # [当前文档]
├── DEV_NOTES.md            # 开发笔记与故障排除记录
├── BUILD_GUIDE.md          # 构建与打包指南
├── build_all.sh            # 自动化构建脚本
└── pubspec.yaml            # 项目依赖管理
```

---

## 2. 整体设计模式
项目采用 **MVVM (Model-View-ViewModel)** 架构，结合 **Service/Repository** 模式，确保 UI、业务逻辑与数据存储解耦。

*   **View**: Flutter Widgets，仅负责 UI 渲染。
*   **ViewModel**: 状态管理（Provider），驱动 UI 更新并持有业务状态。
*   **Repository**: 数据访问抽象层，负责 SQLite 增删改查。
*   **Service**: 纯功能模块，负责流解析、API 请求或播放器底层控制。

---

## 3. 核心文件索引

### 📁 `lib/services/` (功能引擎)
*   **`player_service.dart`**: **[关键]** 播放器底层封装。直接操作 `video_player` 和 `chewie`，管理硬件锁 (Wakelock) 和渲染 Texture 生命周期。
*   **`m3u_service.dart`**: 高性能 M3U 文件解析引擎，包含对海量频道的正则表达式优化。

### 📁 `lib/viewmodels/` (大脑)
*   **`player_viewmodel.dart`**: 控制播放状态机。处理自动重试（Watchdog）、错误捕获及 UI 状态分发。

### 📁 `lib/database/` (骨架)
*   **`database_helper.dart`**: SQLite 数据库管理。包含版本升级 (Migration) 和**自愈逻辑 (Self-healing)**。

---

## 4. 核心业务流程

### 播放自愈流程 (Playback Watchdog)
1.  `PlayerService` 监听流状态 -> 发现停顿 (Stall) 超过 15 秒。
2.  抛出 `Playback stalled` 错误。
3.  `PlayerViewModel` 捕获异常 -> UI 显示“自动连接中...”提示。
4.  自动执行 `PlayerService.play()` 重新拉流。

### 数据库自愈流程 (DB Self-healing)
1.  App 启动，执行 `PRAGMA table_info` 扫描表结构。
2.  如果发现缺少新增字段（如 `order_index`），动态执行 `ALTER TABLE` 补齐。

---
**Last Updated**: 2026-03-07
**Team**: Gemini OmG Team
