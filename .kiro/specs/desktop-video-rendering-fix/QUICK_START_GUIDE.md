# Desktop 视频播放快速入门指南

本指南帮助您快速开始使用 Desktop IPTV 播放器,并确保视频播放正常工作。

## 前置要求

### 1. 安装 VLC Media Player

Desktop 版本需要 VLC Media Player 才能播放视频。

**macOS**:
```bash
brew install --cask vlc
```

**Linux (Ubuntu/Debian)**:
```bash
sudo apt-get update
sudo apt-get install vlc
```

**Windows**:
从 [VLC 官网](https://www.videolan.org/vlc/) 下载并安装

### 2. 验证 VLC 安装

```bash
vlc --version
```

确保版本 >= 3.0.18

## 快速开始

### 步骤 1: 运行应用

```bash
cd /path/to/IPTV
./gradlew :composeApp:run
```

### 步骤 2: 添加播放列表

1. 点击右下角的 "+" 按钮
2. 选择 "M3U URL" 或 "Xtream" 标签
3. 输入播放列表信息
4. 点击 "Add"

### 步骤 3: 播放视频

1. 在播放列表中选择一个列表
2. 选择要播放的频道
3. 视频将自动开始播放

## 视频播放特性

### 自动配置

应用会自动:
- ✅ 检测您的操作系统
- ✅ 选择最佳视频输出模块
- ✅ 启用硬件加速 (如果可用)
- ✅ 优化缓存设置
- ✅ 配置格式特定选项

### 支持的格式

**视频编解码器**:
- H.264 (AVC) ⭐ 推荐
- H.265 (HEVC) ⭐ 推荐
- VP8/VP9
- MPEG-2/MPEG-4

**流媒体协议**:
- HTTP/HTTPS
- HLS (.m3u8)
- RTSP
- RTMP

### 智能恢复

如果视频播放出现问题,应用会自动:
1. 尝试备用视频输出模块
2. 调整缓存设置
3. 禁用硬件加速 (如果需要)
4. 提供详细的错误信息

## 常见问题

### Q: 黑屏但有声音?

**A**: 这通常是视频输出配置问题。应用会自动尝试修复:
1. 检查日志中的诊断信息
2. 应用会自动尝试备用视频输出
3. 如果问题持续,重启应用

### Q: 视频卡顿?

**A**: 可能是网络或性能问题:
1. 检查网络连接
2. 尝试降低视频质量
3. 确保硬件加速已启用
4. 关闭其他占用资源的应用

### Q: 无法播放某些格式?

**A**: 检查格式支持:
1. 确认 VLC 版本 >= 3.0.18
2. 查看支持的格式列表
3. 在 VLC 播放器中测试 URL
4. 查看应用日志获取详细信息

### Q: 应用崩溃?

**A**: 检查 VLC 安装:
1. 确认 VLC 已正确安装
2. 验证 VLC 版本
3. 重新安装 VLC
4. 查看系统日志

## 性能优化建议

### 1. 启用硬件加速

应用会自动检测并启用硬件加速。您可以验证:
- 查看应用日志
- 检查 CPU 使用率 (应该较低)

### 2. 优化网络

- 使用有线连接代替 WiFi
- 关闭其他占用带宽的应用
- 选择较低分辨率的流 (如果可用)

### 3. 系统优化

- 更新显卡驱动
- 关闭不必要的后台应用
- 确保系统有足够的可用内存

## 诊断信息

### 查看日志

应用会自动记录详细的诊断信息:

```
=== Video Rendering Diagnostic Report ===
Timestamp: [时间]
OS: [操作系统]
VLC Version: [版本]

Video Output Configuration:
  - Primary: [主要输出]
  - Fallback: [备用输出]
  - Hardware Acceleration: [状态]

Video Surface:
  - Initialized: [是否初始化]
  - Visible: [是否可见]
  - Size: [尺寸]

Media Information:
  - URL: [URL]
  - Codec: [编解码器]
  - Resolution: [分辨率]
  - FPS: [帧率]

Rendering Statistics:
  - Frames Displayed: [显示帧数]
  - Frames Dropped: [丢帧数]

Issues Detected:
  - [问题列表]

Suggestions:
  - [建议列表]
==========================================
```

### 理解诊断信息

**正常播放**:
- ✅ Video Surface Initialized: true
- ✅ Hardware Acceleration: enabled
- ✅ Frames Displayed: > 0
- ✅ Issues Detected: None

**有问题**:
- ❌ Video Surface Initialized: false
- ⚠️ Hardware Acceleration: disabled
- ❌ Frames Displayed: 0
- ⚠️ Issues Detected: [问题列表]

## 测试视频源

### 公开测试流

您可以使用这些公开的测试流来验证播放功能:

**HLS 流**:
```
https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
```

**RTSP 流**:
```
rtsp://wowzaec2demo.streamlock.net/vod/mp4:BigBuckBunny_115k.mp4
```

**HTTP 流**:
```
http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
```

### 测试步骤

1. 添加测试流到播放列表
2. 播放视频
3. 检查:
   - 视频画面是否显示
   - 音频是否播放
   - 是否有卡顿
   - CPU 使用率

## 获取帮助

### 文档资源

- [故障排除指南](VIDEO_TROUBLESHOOTING.md) - 详细的问题解决方案
- [VLC 配置指南](VLC_CONFIGURATION_GUIDE.md) - VLC 安装和配置
- [技术文档](TECHNICAL_DOCUMENTATION.md) - 开发者技术文档
- [项目 README](../../../README.md) - 项目概述

### 报告问题

如果遇到问题,请提供以下信息:

1. **系统信息**:
   - 操作系统和版本
   - VLC 版本
   - 应用版本

2. **问题描述**:
   - 具体症状
   - 重现步骤
   - 预期行为

3. **诊断信息**:
   - 应用日志
   - 诊断报告
   - 错误消息

4. **视频信息** (如果可以分享):
   - 视频 URL
   - 视频格式
   - 流协议

### 提交 Issue

在 GitHub 上提交 Issue:
1. 访问项目 GitHub 页面
2. 点击 "Issues" 标签
3. 点击 "New Issue"
4. 填写问题模板
5. 附加诊断信息

## 最佳实践

### 播放列表管理

1. **使用可靠的源**:
   - 选择稳定的 M3U 源
   - 验证 Xtream 账户有效性

2. **定期更新**:
   - 刷新播放列表
   - 删除失效的频道

3. **分类管理**:
   - 使用有意义的播放列表名称
   - 按类型或来源分组

### 播放优化

1. **选择合适的质量**:
   - 根据网络速度选择分辨率
   - 平衡质量和流畅度

2. **监控性能**:
   - 注意 CPU 使用率
   - 检查网络带宽
   - 观察内存使用

3. **及时反馈**:
   - 报告问题
   - 分享使用经验
   - 提出改进建议

## 系统要求

### 最低要求

- **CPU**: 双核 2.0 GHz
- **RAM**: 4 GB
- **GPU**: 支持 OpenGL 2.0
- **网络**: 5 Mbps (标清)
- **VLC**: 3.0.18 或更高

### 推荐配置

- **CPU**: 四核 2.5 GHz
- **RAM**: 8 GB
- **GPU**: 支持硬件视频解码
- **网络**: 25 Mbps (高清)
- **VLC**: 最新稳定版

### 支持的操作系统

- **macOS**: 10.15 (Catalina) 或更高
- **Linux**: Ubuntu 20.04 或更高,Fedora 33 或更高
- **Windows**: Windows 10 或更高

## 更新和维护

### 更新应用

```bash
cd /path/to/IPTV
git pull
./gradlew clean
./gradlew :composeApp:run
```

### 更新 VLC

**macOS**:
```bash
brew upgrade vlc
```

**Linux**:
```bash
sudo apt-get update
sudo apt-get upgrade vlc
```

**Windows**:
从 VLC 官网下载最新版本

### 清理缓存

如果遇到问题,可以清理应用缓存:

**macOS**:
```bash
rm -rf ~/Library/Caches/IPTV
```

**Linux**:
```bash
rm -rf ~/.cache/IPTV
```

**Windows**:
```powershell
Remove-Item -Recurse -Force $env:LOCALAPPDATA\IPTV\cache
```

## 下一步

现在您已经了解了基础知识,可以:

1. **探索功能**:
   - 添加多个播放列表
   - 尝试不同的视频源
   - 测试各种格式

2. **优化体验**:
   - 调整播放设置
   - 优化网络配置
   - 启用硬件加速

3. **深入学习**:
   - 阅读技术文档
   - 了解视频渲染原理
   - 参与社区讨论

## 反馈和贡献

我们欢迎您的反馈和贡献:

- 报告 Bug
- 提出功能建议
- 改进文档
- 提交代码

感谢使用 Desktop IPTV 播放器!

---

**最后更新**: 2025-11-26
**版本**: 1.0.0
