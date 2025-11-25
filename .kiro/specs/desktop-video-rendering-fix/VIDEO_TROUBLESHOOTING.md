# Desktop 视频播放故障排除指南

本指南帮助您诊断和解决 Desktop 版本 IPTV 播放器的视频播放问题。

## 最新更新

**2025-11-26：生命周期修复**
- ✅ 修复了协程取消导致的错误：`The coroutine scope left the composition`
- ✅ 修复了视频表面初始化问题（尺寸 0x0）
- ✅ 改进了 SwingPanel 的 factory 初始化逻辑
- 详见：[LIFECYCLE_FIX.md](./LIFECYCLE_FIX.md) 和 [QUICK_TEST_LIFECYCLE_FIX.md](./QUICK_TEST_LIFECYCLE_FIX.md)

## 目录

1. [常见问题](#常见问题)
2. [诊断步骤](#诊断步骤)
3. [平台特定问题](#平台特定问题)
4. [高级配置](#高级配置)
5. [日志分析](#日志分析)

## 常见问题

### 问题 0: 协程取消错误（已修复）

**症状**: 看到错误消息 `The coroutine scope left the composition`

**状态**: ✅ 已在 2025-11-26 修复

**原因**: 
- URL 快速切换导致协程被取消
- 视频表面初始化时机问题

**解决方案**:
- 更新到最新版本即可
- 详见 [LIFECYCLE_FIX.md](./LIFECYCLE_FIX.md)

### 问题 1: 视频表面尺寸无效（已修复）

**症状**: 日志显示 `视频表面尺寸无效: 0x0`

**状态**: ✅ 已在 2025-11-26 修复

**原因**:
- SwingPanel 的 factory 没有正确初始化视频表面
- 视频表面可见性未设置

**解决方案**:
- 更新到最新版本即可
- 现在会在 factory 中自动设置初始尺寸为 800x600

### 问题 2: 黑屏但有声音

**症状**: 播放视频时只能听到声音,但屏幕显示黑色。

**可能原因**:
- 视频输出模块未正确初始化
- 视频表面配置问题
- 硬件加速不兼容
- VLC 版本过旧

**解决方案**:

1. **检查 VLC 安装**:
   ```bash
   # macOS
   vlc --version
   
   # Linux
   vlc --version
   
   # Windows (在 VLC 安装目录)
   vlc.exe --version
   ```
   确保版本 >= 3.0.18

2. **查看应用日志**:
   应用会自动记录诊断信息。查找包含以下内容的日志:
   ```
   Video Output Configuration:
     - Primary: vout=...
     - Fallback: vout=...
     - Hardware Acceleration: enabled/disabled
   ```

3. **尝试禁用硬件加速**:
   如果问题持续,可能是硬件加速导致。应用会自动尝试备用方案。

4. **重启应用**:
   有时视频输出初始化需要重新启动应用。

### 问题 2: 视频卡顿或延迟

**症状**: 视频播放不流畅,出现卡顿或延迟。

**可能原因**:
- 网络带宽不足
- CPU 性能不足
- 缓存配置不当
- 视频分辨率过高

**解决方案**:

1. **检查网络连接**:
   - 测试网络速度
   - 尝试使用有线连接代替 WiFi
   - 检查是否有其他应用占用带宽

2. **降低视频质量**:
   - 选择较低分辨率的流
   - 如果可用,选择较低比特率的选项

3. **检查 CPU 使用率**:
   - 打开系统监视器查看 CPU 使用情况
   - 关闭其他占用 CPU 的应用
   - 确保硬件加速已启用

4. **调整缓存设置**:
   应用会根据流类型自动配置缓存:
   - 直播流: 300-1000ms
   - 点播: 1000-3000ms

### 问题 3: 无法播放某些格式

**症状**: 某些视频格式或协议无法播放。

**可能原因**:
- VLC 不支持该编解码器
- 流协议不兼容
- URL 格式错误

**解决方案**:

1. **检查支持的格式**:
   参考 README.md 中的"支持的视频格式和协议"部分。

2. **验证 URL**:
   - 确保 URL 格式正确
   - 在浏览器或 VLC 播放器中测试 URL
   - 检查是否需要认证

3. **更新 VLC**:
   较新版本的 VLC 支持更多格式。

4. **查看错误日志**:
   应用会记录详细的错误信息,包括:
   - 媒体格式
   - 编解码器信息
   - 错误代码

### 问题 4: 应用启动时崩溃

**症状**: 应用在启动或播放视频时崩溃。

**可能原因**:
- VLC 未安装或路径不正确
- 系统库缺失
- 权限问题

**解决方案**:

1. **验证 VLC 安装**:
   ```bash
   # macOS
   ls -la /Applications/VLC.app
   
   # Linux
   which vlc
   
   # Windows
   # 检查 C:\Program Files\VideoLAN\VLC
   ```

2. **重新安装 VLC**:
   完全卸载后重新安装 VLC。

3. **检查系统日志**:
   - macOS: Console.app
   - Linux: journalctl 或 /var/log
   - Windows: Event Viewer

### 问题 5: 音视频不同步

**症状**: 音频和视频播放不同步。

**可能原因**:
- 网络延迟
- 解码性能问题
- 流本身的问题

**解决方案**:

1. **检查流质量**:
   在其他播放器中测试相同的流。

2. **启用硬件加速**:
   应用会自动启用硬件加速以提高解码性能。

3. **调整缓存**:
   应用会根据流类型自动调整缓存设置。

## 诊断步骤

### 步骤 1: 收集诊断信息

应用会自动生成详细的诊断报告。查找日志中的以下部分:

```
=== Video Rendering Diagnostic Report ===
Timestamp: [时间戳]
OS: [操作系统]
VLC Version: [VLC 版本]

Video Output Configuration:
  - Primary: [主要输出]
  - Fallback: [备用输出]
  - Hardware Acceleration: [状态]

Video Surface:
  - Initialized: [是否初始化]
  - Visible: [是否可见]
  - Size: [尺寸]

Media Information:
  - URL: [媒体 URL]
  - Codec: [编解码器]
  - Resolution: [分辨率]
  - FPS: [帧率]

Rendering Statistics:
  - Frames Displayed: [显示帧数]
  - Frames Dropped: [丢帧数]

Issues Detected:
  - [检测到的问题]

Suggestions:
  - [建议的解决方案]
==========================================
```

### 步骤 2: 验证系统要求

**最低要求**:
- CPU: 双核 2.0 GHz 或更高
- RAM: 4 GB
- GPU: 支持 OpenGL 2.0 或更高
- VLC: 3.0.18 或更高

**推荐配置**:
- CPU: 四核 2.5 GHz 或更高
- RAM: 8 GB
- GPU: 支持硬件视频解码
- VLC: 最新稳定版

### 步骤 3: 测试视频输出

应用会自动测试以下视频输出模块:

**macOS**:
1. 主要: `vout=macosx`
2. 备用: `vout=opengl`

**Linux**:
1. 主要: `vout=xcb_x11`
2. 备用: `vout=opengl`

**Windows**:
1. 主要: `vout=directdraw`
2. 备用: `vout=opengl`

如果主要输出失败,应用会自动尝试备用输出。

### 步骤 4: 检查硬件加速

应用会自动检测并启用硬件加速。查看日志中的:

```
Hardware Acceleration:
  - Available: [是/否]
  - Enabled: [是/否]
  - Method: [方法]
```

**支持的硬件加速方法**:
- macOS: VideoToolbox
- Linux: VA-API, VDPAU
- Windows: DXVA2, D3D11

## 平台特定问题

### macOS

**问题**: 视频输出失败

**解决方案**:
1. 确保 VLC 已安装在 `/Applications/VLC.app`
2. 检查系统完整性保护 (SIP) 设置
3. 授予应用必要的权限

**问题**: 硬件加速不可用

**解决方案**:
1. 检查 macOS 版本 (推荐 10.15 或更高)
2. 更新显卡驱动
3. 在"系统偏好设置 > 安全性与隐私"中检查权限

### Linux

**问题**: 视频输出失败

**解决方案**:
1. 安装必要的库:
   ```bash
   sudo apt-get install libvlc-dev libvlccore-dev
   sudo apt-get install vlc-plugin-base
   ```

2. 检查 X11 配置:
   ```bash
   echo $DISPLAY
   xdpyinfo
   ```

3. 尝试不同的视频输出:
   - `xcb_x11` (推荐)
   - `x11`
   - `opengl`

**问题**: 硬件加速不可用

**解决方案**:
1. 安装 VA-API 或 VDPAU:
   ```bash
   # VA-API (Intel)
   sudo apt-get install vainfo libva-drm2
   
   # VDPAU (NVIDIA)
   sudo apt-get install vdpauinfo libvdpau1
   ```

2. 验证硬件加速:
   ```bash
   vainfo  # 对于 VA-API
   vdpauinfo  # 对于 VDPAU
   ```

### Windows

**问题**: 视频输出失败

**解决方案**:
1. 确保 VLC 已正确安装
2. 检查 VLC 安装路径是否在系统 PATH 中
3. 以管理员身份运行应用

**问题**: 硬件加速不可用

**解决方案**:
1. 更新显卡驱动
2. 启用 DirectX 硬件加速
3. 检查 Windows 图形设置

## 高级配置

### 自定义 VLC 选项

应用会自动配置最佳的 VLC 选项,但您可以通过修改代码来自定义:

**网络缓存**:
```kotlin
// 默认值
networkCaching = 1000  // 1 秒
liveCaching = 300      // 300 毫秒
```

**硬件加速**:
```kotlin
// 自动检测并启用
hardwareAcceleration = true
```

**视频输出**:
```kotlin
// 平台特定
videoOutput = "auto"  // 自动选择
```

### 性能优化

**降低 CPU 使用率**:
1. 启用硬件加速
2. 降低视频分辨率
3. 调整缓存设置

**降低内存使用**:
1. 减少缓存大小
2. 限制并发流数量

**降低网络使用**:
1. 选择较低比特率的流
2. 使用本地缓存

## 日志分析

### 关键日志消息

**成功初始化**:
```
✅ Video output initialized successfully
✅ Video surface validated
✅ Hardware acceleration enabled
✅ Media playback started
```

**警告消息**:
```
⚠️ Primary video output failed, trying fallback
⚠️ Hardware acceleration not available
⚠️ Network caching increased due to buffering
```

**错误消息**:
```
❌ Video output initialization failed
❌ Video surface validation failed
❌ Media playback error: [错误代码]
❌ VLC not found or incompatible version
```

### 日志位置

应用日志会输出到标准输出。您可以重定向到文件:

```bash
# macOS/Linux
./gradlew :composeApp:run > app.log 2>&1

# Windows
gradlew.bat :composeApp:run > app.log 2>&1
```

### 启用详细日志

应用会自动记录详细的诊断信息,包括:
- 视频输出配置
- 视频表面状态
- 媒体信息
- 渲染统计
- 错误和警告

## 获取帮助

如果以上步骤无法解决您的问题:

1. **收集信息**:
   - 操作系统和版本
   - VLC 版本
   - 应用日志
   - 诊断报告
   - 视频 URL (如果可以分享)

2. **提交 Issue**:
   在 GitHub 上提交 Issue,包含以上信息。

3. **社区支持**:
   在项目讨论区寻求帮助。

## 相关资源

- [VLC 官方文档](https://wiki.videolan.org/Documentation:Documentation/)
- [VLC 命令行选项](https://wiki.videolan.org/VLC_command-line_help/)
- [VLCJ 文档](https://github.com/caprica/vlcj)
- [项目 README](../../../README.md)

## 更新日志

- **2025-11-26**: 初始版本
  - 添加视频渲染改进文档
  - 添加故障排除指南
  - 添加平台特定问题解决方案
