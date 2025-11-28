# Requirements Document

## Introduction

本文档定义了将 IPTV 播放器桌面平台从 Kotlin Multiplatform 迁移到原生开发的需求。目标是为 macOS 和 Windows 平台分别使用原生技术栈（Swift/SwiftUI 和 C#/WPF 或 C++/Qt）重新实现桌面应用，同时保持与现有 Android 应用的功能一致性。

## Glossary

- **Native Desktop App**: 使用平台原生技术栈开发的桌面应用程序
- **macOS App**: 使用 Swift 和 SwiftUI 开发的 macOS 原生应用
- **Windows App**: 使用 C# 和 WPF 开发的 Windows 原生应用
- **Shared Backend**: 共享的后端逻辑，包括网络请求、数据解析、数据库操作等
- **M3U Parser**: 解析 M3U 播放列表格式的组件
- **Xtream Codes API**: IPTV 服务提供商使用的 API 协议
- **Video Player Engine**: 视频播放引擎，负责解码和渲染视频流
- **EPG**: Electronic Program Guide，电子节目指南
- **HLS**: HTTP Live Streaming，苹果开发的流媒体协议
- **RTSP**: Real Time Streaming Protocol，实时流媒体协议

## Requirements

### Requirement 1

**User Story:** 作为项目架构师，我希望为 macOS 和 Windows 平台选择合适的原生技术栈，以便实现高性能和良好的用户体验。

#### Acceptance Criteria

1. WHEN 选择 macOS 技术栈 THEN 系统应当使用 Swift 作为编程语言和 SwiftUI 作为 UI 框架
2. WHEN 选择 Windows 技术栈 THEN 系统应当使用 C# 作为编程语言和 WPF 作为 UI 框架
3. WHEN 评估技术栈 THEN 系统应当考虑视频播放性能、开发效率、维护成本和平台集成度
4. WHEN 确定技术栈 THEN 系统应当确保所选技术支持 HLS、RTSP、HTTP 等主流流媒体协议
5. WHEN 确定技术栈 THEN 系统应当确保所选技术支持硬件加速视频解码

### Requirement 2

**User Story:** 作为开发者，我希望设计清晰的架构边界，以便在原生 UI 和共享业务逻辑之间实现良好的分离。

#### Acceptance Criteria

1. WHEN 设计应用架构 THEN 系统应当将 UI 层、业务逻辑层和数据层明确分离
2. WHEN 实现业务逻辑 THEN 系统应当将网络请求、数据解析、数据库操作等逻辑封装为可复用的模块
3. WHEN 原生 UI 需要调用业务逻辑 THEN 系统应当通过定义良好的接口进行通信
4. WHEN 跨平台共享代码 THEN 系统应当考虑使用 Rust、C++ 或保留部分 Kotlin 代码作为共享库
5. WHEN 平台特定功能实现 THEN 系统应当在各平台的原生代码中实现，不强制共享

### Requirement 3

**User Story:** 作为 macOS 用户，我希望使用原生的 macOS 应用，以便获得流畅的用户体验和完整的系统集成。

#### Acceptance Criteria

1. WHEN macOS 应用启动 THEN 系统应当显示符合 macOS Human Interface Guidelines 的用户界面
2. WHEN 用户添加 M3U 播放列表 THEN macOS 应用应当支持通过 URL 或本地文件导入
3. WHEN 用户添加 Xtream Codes 账户 THEN macOS 应用应当支持输入服务器地址、用户名和密码
4. WHEN 用户浏览频道列表 THEN macOS 应用应当显示频道名称、分类和缩略图
5. WHEN 用户选择频道播放 THEN macOS 应用应当使用 AVPlayer 或 VLCKit 播放视频流
6. WHEN 视频播放 THEN macOS 应用应当支持播放控制（播放、暂停、音量、全屏）
7. WHEN 用户收藏频道 THEN macOS 应用应当将收藏信息保存到本地数据库
8. WHEN 应用需要持久化数据 THEN macOS 应用应当使用 Core Data 或 SQLite 存储数据

### Requirement 4

**User Story:** 作为 Windows 用户，我希望使用原生的 Windows 应用，以便获得流畅的用户体验和完整的系统集成。

#### Acceptance Criteria

1. WHEN Windows 应用启动 THEN 系统应当显示符合 Windows 设计规范的用户界面
2. WHEN 用户添加 M3U 播放列表 THEN Windows 应用应当支持通过 URL 或本地文件导入
3. WHEN 用户添加 Xtream Codes 账户 THEN Windows 应用应当支持输入服务器地址、用户名和密码
4. WHEN 用户浏览频道列表 THEN Windows 应用应当显示频道名称、分类和缩略图
5. WHEN 用户选择频道播放 THEN Windows 应用应当使用 MediaElement、LibVLC 或 FFmpeg 播放视频流
6. WHEN 视频播放 THEN Windows 应用应当支持播放控制（播放、暂停、音量、全屏）
7. WHEN 用户收藏频道 THEN Windows 应用应当将收藏信息保存到本地数据库
8. WHEN 应用需要持久化数据 THEN Windows 应用应当使用 SQLite 或 Entity Framework 存储数据

### Requirement 5

**User Story:** 作为开发者，我希望实现 M3U 播放列表解析功能，以便用户可以导入和使用 M3U 格式的频道列表。

#### Acceptance Criteria

1. WHEN 解析 M3U 文件 THEN 系统应当提取频道名称、URL、分组和其他元数据
2. WHEN M3U 文件包含 EXTINF 标签 THEN 系统应当正确解析频道信息
3. WHEN M3U 文件包含 tvg-logo 属性 THEN 系统应当提取频道图标 URL
4. WHEN M3U 文件包含 group-title 属性 THEN 系统应当提取频道分组信息
5. WHEN M3U 文件格式错误 THEN 系统应当返回明确的错误信息并继续解析有效条目

### Requirement 6

**User Story:** 作为开发者，我希望实现 Xtream Codes API 客户端，以便用户可以通过 Xtream Codes 协议访问 IPTV 服务。

#### Acceptance Criteria

1. WHEN 用户提供 Xtream Codes 凭证 THEN 系统应当向服务器发送认证请求
2. WHEN 认证成功 THEN 系统应当获取并存储用户信息和服务器配置
3. WHEN 获取直播频道列表 THEN 系统应当调用相应的 API 端点并解析 JSON 响应
4. WHEN 获取 VOD 内容列表 THEN 系统应当调用相应的 API 端点并解析 JSON 响应
5. WHEN 获取 EPG 数据 THEN 系统应当调用相应的 API 端点并解析 XML 或 JSON 响应
6. WHEN API 请求失败 THEN 系统应当返回明确的错误信息并支持重试机制

### Requirement 7

**User Story:** 作为用户，我希望应用能够流畅播放各种格式的视频流，以便观看 IPTV 频道。

#### Acceptance Criteria

1. WHEN 播放 HLS 流 THEN 系统应当正确解析 m3u8 播放列表并播放视频
2. WHEN 播放 RTSP 流 THEN 系统应当建立 RTSP 连接并播放实时视频
3. WHEN 播放 HTTP 流 THEN 系统应当通过 HTTP 协议获取并播放视频
4. WHEN 视频流支持硬件解码 THEN 系统应当优先使用硬件加速
5. WHEN 网络连接中断 THEN 系统应当显示错误信息并支持重新连接
6. WHEN 视频缓冲 THEN 系统应当显示加载指示器
7. WHEN 用户调整音量 THEN 系统应当实时更新音频输出音量

### Requirement 8

**User Story:** 作为用户，我希望应用能够保存我的播放列表和收藏，以便下次启动时自动加载。

#### Acceptance Criteria

1. WHEN 用户添加播放列表 THEN 系统应当将播放列表信息存储到本地数据库
2. WHEN 用户收藏频道 THEN 系统应当将收藏信息存储到本地数据库
3. WHEN 应用启动 THEN 系统应当从本地数据库加载所有播放列表和收藏
4. WHEN 用户删除播放列表 THEN 系统应当从数据库中删除相关数据
5. WHEN 数据库操作失败 THEN 系统应当返回明确的错误信息并保持数据一致性

### Requirement 9

**User Story:** 作为开发者，我希望建立清晰的项目结构和构建流程，以便团队成员可以高效地开发和维护代码。

#### Acceptance Criteria

1. WHEN 创建项目结构 THEN 系统应当为 macOS 和 Windows 应用分别创建独立的项目目录
2. WHEN 组织共享代码 THEN 系统应当将共享的业务逻辑放在独立的模块或库中
3. WHEN 配置构建系统 THEN macOS 应用应当使用 Xcode 和 Swift Package Manager
4. WHEN 配置构建系统 THEN Windows 应用应当使用 Visual Studio 和 NuGet 包管理器
5. WHEN 执行构建 THEN 系统应当能够为各平台生成可分发的应用程序包
6. WHEN 配置 CI/CD THEN 系统应当支持在 GitHub Actions 中自动构建和发布

### Requirement 10

**User Story:** 作为开发者，我希望保持 Android 应用的独立性，以便 Android 平台继续使用 Kotlin Multiplatform 开发。

#### Acceptance Criteria

1. WHEN 迁移桌面平台 THEN 系统应当保留 Android 应用的现有代码结构
2. WHEN Android 应用需要更新 THEN 系统应当能够独立于桌面应用进行开发和发布
3. WHEN 共享业务逻辑更新 THEN 系统应当确保 Android 应用可以访问更新后的共享代码
4. WHEN 构建 Android 应用 THEN 系统应当继续使用 Gradle 和 Kotlin Multiplatform 工具链
