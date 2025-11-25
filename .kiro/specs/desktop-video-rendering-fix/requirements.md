# Requirements Document

## Introduction

本需求文档旨在修复桌面版（desktopMain）IPTV播放器中的视频渲染问题。用户报告在打开直播链接时出现黑屏，只有声音没有画面。这表明VLC媒体播放器能够解码音频流，但视频渲染组件未能正确显示视频画面。

## Glossary

- **VLC Media Player**: 开源的跨平台多媒体播放器和框架
- **VLCJ**: VLC的Java绑定库，用于在Java/Kotlin应用中集成VLC功能
- **EmbeddedMediaPlayerComponent**: VLCJ提供的嵌入式媒体播放器组件
- **SwingPanel**: Compose for Desktop中用于嵌入Swing组件的容器
- **Video Surface**: 视频渲染表面，用于显示视频帧
- **Hardware Acceleration**: 硬件加速，使用GPU加速视频解码和渲染

## Requirements

### Requirement 1: 修复视频渲染黑屏问题

**User Story:** 作为桌面版用户，我希望在播放直播链接时能够看到视频画面，以便正常观看直播内容。

#### Acceptance Criteria

1. WHEN 用户播放直播链接 THEN 系统 SHALL 正确显示视频画面和播放音频
2. WHEN 视频开始播放 THEN 系统 SHALL 在视频表面上渲染视频帧
3. WHEN 视频格式改变 THEN 系统 SHALL 自动调整渲染参数以正确显示
4. IF 视频渲染失败 THEN 系统 SHALL 记录详细错误信息并尝试备用渲染方案

### Requirement 2: 配置VLC视频输出选项

**User Story:** 作为开发者，我希望正确配置VLC的视频输出选项，以便确保视频能够在嵌入式组件中正确渲染。

#### Acceptance Criteria

1. WHEN 初始化媒体播放器 THEN 系统 SHALL 配置适当的视频输出模块
2. WHEN 在不同操作系统上运行 THEN 系统 SHALL 使用平台特定的最佳视频输出选项
3. WHEN 硬件加速可用 THEN 系统 SHALL 启用硬件加速以提高性能
4. IF 默认视频输出失败 THEN 系统 SHALL 尝试备用视频输出模块

### Requirement 3: 验证视频表面初始化

**User Story:** 作为开发者，我希望确保视频渲染表面正确初始化，以便VLC能够将视频帧绘制到正确的位置。

#### Acceptance Criteria

1. WHEN 创建EmbeddedMediaPlayerComponent THEN 系统 SHALL 确保视频表面已正确初始化
2. WHEN 视频表面创建 THEN 系统 SHALL 验证其尺寸和可见性
3. WHEN 组件布局改变 THEN 系统 SHALL 更新视频表面以匹配新尺寸
4. IF 视频表面初始化失败 THEN 系统 SHALL 提供清晰的错误消息

### Requirement 4: 添加VLC媒体选项配置

**User Story:** 作为开发者，我希望为媒体播放添加适当的VLC选项，以便优化直播流的播放和渲染。

#### Acceptance Criteria

1. WHEN 播放媒体 THEN 系统 SHALL 应用适当的网络缓存选项
2. WHEN 播放直播流 THEN 系统 SHALL 配置低延迟选项
3. WHEN 播放不同格式 THEN 系统 SHALL 根据格式调整解码选项
4. IF 媒体选项配置失败 THEN 系统 SHALL 使用默认选项并记录警告

### Requirement 5: 改进视频播放诊断和日志

**User Story:** 作为开发者，我希望有详细的诊断信息，以便快速识别视频渲染问题的根本原因。

#### Acceptance Criteria

1. WHEN 视频播放开始 THEN 系统 SHALL 记录视频编解码器和格式信息
2. WHEN 视频帧渲染 THEN 系统 SHALL 记录渲染统计信息
3. WHEN 检测到黑屏 THEN 系统 SHALL 记录可能的原因和建议的解决方案
4. WHEN 播放错误发生 THEN 系统 SHALL 记录VLC内部状态和错误代码

### Requirement 6: 支持多种视频输出后端

**User Story:** 作为用户，我希望应用能够在不同的系统配置下工作，即使某些视频输出方法不可用。

#### Acceptance Criteria

1. WHEN 主要视频输出不可用 THEN 系统 SHALL 自动尝试备用输出方法
2. WHEN 在macOS上运行 THEN 系统 SHALL 优先使用适合macOS的视频输出
3. WHEN 在Linux上运行 THEN 系统 SHALL 优先使用适合Linux的视频输出
4. WHEN 在Windows上运行 THEN 系统 SHALL 优先使用适合Windows的视频输出
