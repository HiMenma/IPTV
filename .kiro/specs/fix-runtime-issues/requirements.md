# Requirements Document

## Introduction

本需求文档旨在识别并修复IPTV播放器项目中的潜在运行时问题。通过分析错误日志和代码结构，我们发现了几个关键问题：VLC播放器崩溃、数据库驱动缺失、资源管理不当等。这些问题会导致应用程序崩溃或功能无法正常工作。

## Requirements

### Requirement 1: 修复VLC播放器资源释放崩溃

**User Story:** 作为开发者，我希望修复Desktop版本视频播放器在释放资源时的崩溃问题，以便用户可以正常切换频道和关闭播放器而不会导致应用崩溃。

#### Acceptance Criteria

1. WHEN 用户切换频道或关闭播放器 THEN 系统 SHALL 安全地释放VLC资源而不触发SIGSEGV错误
2. WHEN VideoPlayer组件被dispose THEN 系统 SHALL 在正确的线程上下文中释放媒体播放器资源
3. WHEN 释放资源失败 THEN 系统 SHALL 捕获异常并记录错误而不是崩溃
4. IF 媒体播放器正在播放 THEN 系统 SHALL 先停止播放再释放资源

### Requirement 2: 实现缺失的数据库驱动

**User Story:** 作为开发者，我希望为所有平台实现数据库驱动，以便应用程序可以正常存储和检索播放列表、收藏等数据。

#### Acceptance Criteria

1. WHEN 应用启动 THEN 系统 SHALL 成功创建平台特定的数据库驱动
2. WHEN 在Android平台运行 THEN 系统 SHALL 使用Android SQLite驱动
3. WHEN 在Desktop平台运行 THEN 系统 SHALL 使用JVM SQLite驱动
4. WHEN 数据库初始化失败 THEN 系统 SHALL 提供清晰的错误信息

### Requirement 3: 优化视频播放器生命周期管理

**User Story:** 作为用户，我希望视频播放器能够正确管理资源生命周期，以便在切换频道或退出播放时不会出现内存泄漏或崩溃。

#### Acceptance Criteria

1. WHEN 播放新的URL THEN 系统 SHALL 先停止当前播放再加载新媒体
2. WHEN 组件被销毁 THEN 系统 SHALL 确保所有事件监听器被移除
3. WHEN 释放资源 THEN 系统 SHALL 使用try-catch包裹以防止异常传播
4. IF 播放器已经被释放 THEN 系统 SHALL 不再尝试访问播放器实例

### Requirement 4: 改进错误处理和日志记录

**User Story:** 作为开发者，我希望有更好的错误处理和日志记录机制，以便快速诊断和修复运行时问题。

#### Acceptance Criteria

1. WHEN 发生运行时错误 THEN 系统 SHALL 记录详细的错误信息包括堆栈跟踪
2. WHEN VLC初始化失败 THEN 系统 SHALL 显示友好的错误消息提示用户安装VLC
3. WHEN 数据库操作失败 THEN 系统 SHALL 捕获异常并提供恢复选项
4. WHEN 网络请求失败 THEN 系统 SHALL 提供重试机制

### Requirement 5: 添加VLC可用性检查

**User Story:** 作为用户，我希望在VLC未安装时收到明确提示，以便我知道需要安装VLC才能使用Desktop版本。

#### Acceptance Criteria

1. WHEN Desktop应用启动 THEN 系统 SHALL 检查VLC是否已安装
2. IF VLC未找到 THEN 系统 SHALL 显示安装指引对话框
3. WHEN VLC检测失败 THEN 系统 SHALL 提供手动指定VLC路径的选项
4. WHEN VLC可用 THEN 系统 SHALL 正常初始化视频播放器组件

### Requirement 6: 修复Android ExoPlayer流媒体格式支持

**User Story:** 作为Android用户，我希望能够播放M3U播放列表中的各种流媒体格式（HLS、DASH等），以便正常观看直播频道。

#### Acceptance Criteria

1. WHEN 播放HLS流媒体 THEN 系统 SHALL 成功加载并播放内容
2. WHEN 播放DASH流媒体 THEN 系统 SHALL 成功加载并播放内容
3. WHEN 播放SmoothStreaming流媒体 THEN 系统 SHALL 成功加载并播放内容
4. WHEN ExoPlayer遇到不支持的格式 THEN 系统 SHALL 显示清晰的错误消息而不是技术错误代码

### Requirement 7: 修复数据库Schema迁移问题

**User Story:** 作为开发者，我希望数据库能够自动迁移到新的schema版本，以便用户升级应用时不会遇到"列不存在"的错误。

#### Acceptance Criteria

1. WHEN 应用启动时检测到旧版本数据库 THEN 系统 SHALL 自动执行迁移
2. WHEN 数据库缺少categoryId列 THEN 系统 SHALL 添加该列并设置默认值
3. WHEN 迁移完成 THEN 系统 SHALL 记录迁移日志
4. IF 迁移失败 THEN 系统 SHALL 提供清晰的错误信息并允许用户选择重置数据库
