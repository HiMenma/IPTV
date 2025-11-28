# Requirements Document

## Introduction

本需求文档旨在将桌面版 IPTV 播放器的播放引擎从 VLC Media Player 迁移到 FFmpeg/libav。当前应用使用 VLCJ 库集成 VLC 播放器，但 FFmpeg/libav 提供了更灵活的集成方式、更好的性能控制和更广泛的格式支持。此迁移将使用 JavaCV 库（FFmpeg 的 Java 绑定）来实现视频播放功能。

## Glossary

- **FFmpeg**: 开源的跨平台多媒体处理框架，支持音视频编解码、转换和流处理
- **libav**: FFmpeg 的核心库集合，包括 libavcodec（编解码）、libavformat（格式处理）、libavutil（工具函数）等
- **JavaCV**: FFmpeg、OpenCV 等库的 Java 绑定，提供 Java/Kotlin 访问接口
- **FFmpegFrameGrabber**: JavaCV 提供的帧抓取器，用于从媒体源读取音视频帧
- **Frame**: 音视频帧数据结构，包含图像或音频样本
- **FrameRecorder**: JavaCV 提供的帧记录器，用于编码和输出音视频
- **Canvas**: Java AWT 组件，用于渲染视频帧
- **BufferedImage**: Java 图像数据结构，用于存储和处理视频帧
- **Hardware Acceleration**: 硬件加速，使用 GPU 加速视频解码和渲染（如 NVDEC、VideoToolbox、VAAPI）
- **Compose for Desktop**: Kotlin 的桌面 UI 框架，基于 Jetpack Compose

## Requirements

### Requirement 1: 实现基于 FFmpeg 的视频播放器

**User Story:** 作为桌面版用户，我希望使用基于 FFmpeg 的播放器播放直播和点播内容，以便获得稳定可靠的播放体验。

#### Acceptance Criteria

1. WHEN 用户播放媒体 URL THEN 系统 SHALL 使用 FFmpeg 解码并渲染音视频内容
2. WHEN 视频帧解码完成 THEN 系统 SHALL 在 Canvas 组件上渲染视频帧
3. WHEN 音频帧解码完成 THEN 系统 SHALL 通过音频输出设备播放音频
4. WHEN 播放过程中发生错误 THEN 系统 SHALL 记录详细错误信息并通知用户
5. WHEN 用户停止播放 THEN 系统 SHALL 正确释放所有 FFmpeg 资源

### Requirement 2: 支持播放控制功能

**User Story:** 作为用户，我希望能够控制视频播放（播放、暂停、跳转、音量），以便按需观看内容。

#### Acceptance Criteria

1. WHEN 用户点击播放按钮 THEN 系统 SHALL 开始或恢复视频播放
2. WHEN 用户点击暂停按钮 THEN 系统 SHALL 暂停视频播放并保持当前位置
3. WHEN 用户拖动进度条 THEN 系统 SHALL 跳转到指定时间位置
4. WHEN 用户调整音量滑块 THEN 系统 SHALL 调整音频输出音量
5. WHEN 播放控制命令执行失败 THEN 系统 SHALL 记录错误并保持播放器状态一致

### Requirement 3: 支持多种媒体格式和协议

**User Story:** 作为用户，我希望播放器支持常见的视频格式和流媒体协议，以便播放各种来源的内容。

#### Acceptance Criteria

1. WHEN 用户播放 HTTP/HTTPS 流 THEN 系统 SHALL 正确处理网络流媒体
2. WHEN 用户播放 RTSP 流 THEN 系统 SHALL 正确处理实时流协议
3. WHEN 用户播放 HLS (m3u8) 流 THEN 系统 SHALL 正确处理自适应流媒体
4. WHEN 用户播放本地文件 THEN 系统 SHALL 正确处理本地媒体文件
5. WHEN 遇到不支持的格式 THEN 系统 SHALL 提供清晰的错误消息

### Requirement 4: 实现硬件加速支持

**User Story:** 作为用户，我希望播放器能够利用硬件加速，以便获得更好的性能和更低的 CPU 使用率。

#### Acceptance Criteria

1. WHEN 系统支持硬件加速 THEN 系统 SHALL 自动检测并启用硬件解码
2. WHEN 在 macOS 上运行 THEN 系统 SHALL 优先使用 VideoToolbox 硬件加速
3. WHEN 在 Linux 上运行 THEN 系统 SHALL 优先使用 VAAPI 或 VDPAU 硬件加速
4. WHEN 在 Windows 上运行 THEN 系统 SHALL 优先使用 DXVA2 或 D3D11VA 硬件加速
5. IF 硬件加速不可用或失败 THEN 系统 SHALL 自动回退到软件解码

### Requirement 5: 优化直播流播放

**User Story:** 作为用户，我希望直播流能够低延迟播放，以便获得接近实时的观看体验。

#### Acceptance Criteria

1. WHEN 播放直播流 THEN 系统 SHALL 使用低延迟缓冲策略
2. WHEN 检测到网络抖动 THEN 系统 SHALL 动态调整缓冲区大小
3. WHEN 直播流出现延迟累积 THEN 系统 SHALL 自动跳帧以追赶实时进度
4. WHEN 直播流中断 THEN 系统 SHALL 自动尝试重连
5. WHEN 直播流恢复 THEN 系统 SHALL 从最新位置继续播放

### Requirement 6: 实现音视频同步

**User Story:** 作为用户，我希望音频和视频保持同步，以便获得良好的观看体验。

#### Acceptance Criteria

1. WHEN 播放音视频内容 THEN 系统 SHALL 保持音视频时间戳同步
2. WHEN 检测到音视频不同步 THEN 系统 SHALL 自动调整播放速度以恢复同步
3. WHEN 音频播放速度快于视频 THEN 系统 SHALL 丢弃视频帧以追赶音频
4. WHEN 视频播放速度快于音频 THEN 系统 SHALL 延迟视频渲染以等待音频
5. WHEN 同步误差超过阈值 THEN 系统 SHALL 记录警告信息

### Requirement 7: 提供详细的播放状态和诊断信息

**User Story:** 作为开发者，我希望获得详细的播放状态和诊断信息，以便快速定位和解决问题。

#### Acceptance Criteria

1. WHEN 播放开始 THEN 系统 SHALL 记录媒体格式、编解码器和流信息
2. WHEN 播放过程中 THEN 系统 SHALL 定期更新帧率、比特率和缓冲状态
3. WHEN 发生解码错误 THEN 系统 SHALL 记录错误类型、时间戳和上下文信息
4. WHEN 性能出现问题 THEN 系统 SHALL 记录 CPU 使用率和内存占用
5. WHEN 用户请求诊断报告 THEN 系统 SHALL 生成包含所有关键指标的报告

### Requirement 8: 确保资源正确释放

**User Story:** 作为用户，我希望播放器能够正确管理资源，以便避免内存泄漏和系统资源耗尽。

#### Acceptance Criteria

1. WHEN 停止播放 THEN 系统 SHALL 释放所有 FFmpeg 解码器资源
2. WHEN 切换视频 THEN 系统 SHALL 先释放旧资源再分配新资源
3. WHEN 应用退出 THEN 系统 SHALL 确保所有播放器资源已释放
4. WHEN 播放器空闲超过阈值 THEN 系统 SHALL 释放非必要资源
5. IF 资源释放失败 THEN 系统 SHALL 记录错误并尝试强制清理

### Requirement 9: 保持与现有 API 的兼容性

**User Story:** 作为开发者，我希望新的播放器实现能够与现有代码兼容，以便最小化迁移成本。

#### Acceptance Criteria

1. WHEN 调用 VideoPlayer Composable THEN 系统 SHALL 使用相同的函数签名
2. WHEN 使用 PlayerControls 接口 THEN 系统 SHALL 提供相同的控制方法
3. WHEN 监听 PlayerState THEN 系统 SHALL 提供相同的状态更新
4. WHEN 处理错误回调 THEN 系统 SHALL 使用相同的回调接口
5. WHEN 现有代码调用播放器 THEN 系统 SHALL 无需修改即可工作

### Requirement 10: 支持全屏播放

**User Story:** 作为用户，我希望能够全屏播放视频，以便获得沉浸式观看体验。

#### Acceptance Criteria

1. WHEN 用户切换到全屏模式 THEN 系统 SHALL 将视频渲染到全屏窗口
2. WHEN 在全屏模式下 THEN 系统 SHALL 保持视频宽高比
3. WHEN 退出全屏模式 THEN 系统 SHALL 恢复到窗口模式并继续播放
4. WHEN 全屏模式下调整窗口大小 THEN 系统 SHALL 自动调整视频渲染尺寸
5. WHEN 全屏切换失败 THEN 系统 SHALL 记录错误并保持当前模式

