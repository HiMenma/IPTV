# VLC 配置指南

本指南提供 VLC Media Player 的安装、配置和优化建议,以确保 Desktop IPTV 播放器的最佳性能。

## 目录

1. [安装 VLC](#安装-vlc)
2. [验证安装](#验证安装)
3. [推荐配置](#推荐配置)
4. [性能优化](#性能优化)
5. [故障排除](#故障排除)
6. [高级配置](#高级配置)

## 安装 VLC

### macOS

**方法 1: 使用 Homebrew (推荐)**

```bash
# 安装 Homebrew (如果尚未安装)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 安装 VLC
brew install --cask vlc
```

**方法 2: 手动下载**

1. 访问 [VLC 官网](https://www.videolan.org/vlc/download-macos.html)
2. 下载 macOS 版本
3. 打开 DMG 文件
4. 将 VLC.app 拖到 Applications 文件夹

**验证安装**:
```bash
ls -la /Applications/VLC.app
vlc --version
```

### Linux

**Ubuntu/Debian**:

```bash
# 更新包列表
sudo apt-get update

# 安装 VLC 和开发库
sudo apt-get install vlc
sudo apt-get install libvlc-dev libvlccore-dev
sudo apt-get install vlc-plugin-base vlc-plugin-video-output
```

**Fedora/RHEL**:

```bash
# 启用 RPM Fusion 仓库
sudo dnf install https://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm

# 安装 VLC
sudo dnf install vlc
sudo dnf install vlc-devel
```

**Arch Linux**:

```bash
sudo pacman -S vlc
```

**验证安装**:
```bash
which vlc
vlc --version
```

### Windows

**方法 1: 使用 Chocolatey**

```powershell
# 安装 Chocolatey (如果尚未安装)
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# 安装 VLC
choco install vlc
```

**方法 2: 手动下载**

1. 访问 [VLC 官网](https://www.videolan.org/vlc/download-windows.html)
2. 下载 Windows 版本 (64-bit 推荐)
3. 运行安装程序
4. 按照向导完成安装

**验证安装**:
```powershell
# 在 PowerShell 中
Get-Command vlc
vlc --version
```

## 验证安装

### 检查 VLC 版本

**推荐版本**: 3.0.18 或更高

```bash
vlc --version
```

输出示例:
```
VLC media player 3.0.18 Vetinari (revision 3.0.18-0-gf60c0e8)
VLC version 3.0.18 Vetinari (3.0.18-0-gf60c0e8)
```

### 检查视频输出模块

**macOS**:
```bash
vlc --list | grep vout
```

应该看到:
- `macosx` - macOS 原生视频输出
- `opengl` - OpenGL 视频输出

**Linux**:
```bash
vlc --list | grep vout
```

应该看到:
- `xcb_x11` - X11 视频输出
- `opengl` - OpenGL 视频输出

**Windows**:
```powershell
vlc --list | findstr vout
```

应该看到:
- `directdraw` - DirectDraw 视频输出
- `opengl` - OpenGL 视频输出

### 检查硬件加速支持

**macOS**:
```bash
# 检查 VideoToolbox 支持
system_profiler SPDisplaysDataType
```

**Linux**:
```bash
# 检查 VA-API (Intel)
vainfo

# 检查 VDPAU (NVIDIA)
vdpauinfo
```

**Windows**:
```powershell
# 检查 DirectX
dxdiag
```

## 推荐配置

### 基本配置

应用会自动配置以下选项,无需手动设置:

**视频输出**:
- macOS: `--vout=macosx` (备用: `--vout=opengl`)
- Linux: `--vout=xcb_x11` (备用: `--vout=opengl`)
- Windows: `--vout=directdraw` (备用: `--vout=opengl`)

**硬件加速**:
- `--avcodec-hw=any` - 使用任何可用的硬件加速
- `--ffmpeg-hw` - 启用 FFmpeg 硬件加速

**界面选项**:
- `--no-video-title-show` - 不显示视频标题
- `--no-osd` - 禁用屏幕显示
- `--no-video-deco` - 禁用窗口装饰

### 网络配置

**缓存设置**:
- 标准流: `:network-caching=1000` (1 秒)
- 直播流: `:live-caching=300` (300 毫秒)

**网络选项**:
- `:http-reconnect` - 自动重连
- `:http-continuous` - 连续 HTTP

### 音频配置

**音频选项**:
- `:no-audio-time-stretch` - 禁用音频时间拉伸
- `:audio-desync=0` - 音频同步

## 性能优化

### 启用硬件加速

**macOS (VideoToolbox)**:

应用会自动检测并启用。手动测试:
```bash
vlc --avcodec-hw=videotoolbox <video_url>
```

**Linux (VA-API)**:

安装 VA-API 支持:
```bash
# Intel GPU
sudo apt-get install vainfo libva-drm2 i965-va-driver

# AMD GPU
sudo apt-get install vainfo libva-drm2 mesa-va-drivers
```

测试:
```bash
vlc --avcodec-hw=vaapi <video_url>
```

**Linux (VDPAU)**:

安装 VDPAU 支持:
```bash
# NVIDIA GPU
sudo apt-get install vdpauinfo libvdpau1 nvidia-vdpau-driver
```

测试:
```bash
vlc --avcodec-hw=vdpau <video_url>
```

**Windows (DXVA2/D3D11)**:

应用会自动检测并启用。手动测试:
```powershell
vlc --avcodec-hw=dxva2 <video_url>
```

### 优化缓存

**根据网络条件调整**:

| 场景 | 网络缓存 | 直播缓存 | 说明 |
|------|---------|---------|------|
| 本地文件 | 300ms | N/A | 最小延迟 |
| 局域网 | 1000ms | 500ms | 平衡 |
| 互联网 (好) | 1500ms | 800ms | 流畅播放 |
| 互联网 (差) | 3000ms | 1500ms | 防止卡顿 |
| 低延迟直播 | 1000ms | 300ms | 最小延迟 |

### 降低 CPU 使用

1. **启用硬件加速** (最重要)
2. **降低视频分辨率**
3. **关闭不必要的视频效果**
4. **使用高效的视频输出模块**

### 降低内存使用

1. **减少缓存大小**
2. **限制并发流数量**
3. **及时释放资源**

## 故障排除

### 问题 1: VLC 未找到

**症状**: 应用报告 "VLC not found"

**解决方案**:

**macOS**:
```bash
# 检查 VLC 是否在标准位置
ls -la /Applications/VLC.app

# 如果不在,创建符号链接
sudo ln -s /path/to/VLC.app /Applications/VLC.app
```

**Linux**:
```bash
# 检查 VLC 是否在 PATH 中
which vlc

# 如果不在,添加到 PATH
export PATH=$PATH:/usr/bin
```

**Windows**:
```powershell
# 检查 VLC 安装路径
Get-Command vlc

# 添加到 PATH
$env:Path += ";C:\Program Files\VideoLAN\VLC"
```

### 问题 2: 硬件加速不可用

**症状**: 日志显示 "Hardware acceleration not available"

**解决方案**:

**macOS**:
1. 检查 macOS 版本 (推荐 10.15+)
2. 更新系统
3. 检查显卡驱动

**Linux**:
1. 安装硬件加速库 (见上方)
2. 验证硬件加速:
   ```bash
   vainfo  # 或 vdpauinfo
   ```
3. 检查用户权限:
   ```bash
   sudo usermod -a -G video $USER
   ```

**Windows**:
1. 更新显卡驱动
2. 启用 DirectX 硬件加速
3. 检查 Windows 图形设置

### 问题 3: 视频输出模块失败

**症状**: 日志显示 "Video output initialization failed"

**解决方案**:

1. **尝试备用输出**:
   应用会自动尝试备用输出模块。

2. **手动测试输出模块**:
   ```bash
   # 测试 OpenGL
   vlc --vout=opengl <video_url>
   
   # 测试平台特定输出
   vlc --vout=macosx <video_url>  # macOS
   vlc --vout=xcb_x11 <video_url>  # Linux
   vlc --vout=directdraw <video_url>  # Windows
   ```

3. **检查显示服务器**:
   - Linux: 确保 X11 或 Wayland 正常运行
   - macOS: 检查窗口服务器
   - Windows: 检查 DWM (Desktop Window Manager)

### 问题 4: 插件缺失

**症状**: 某些格式无法播放

**解决方案**:

**Linux**:
```bash
# 安装所有 VLC 插件
sudo apt-get install vlc-plugin-*

# 或安装特定插件
sudo apt-get install vlc-plugin-base
sudo apt-get install vlc-plugin-video-output
sudo apt-get install vlc-plugin-access-extra
```

**macOS/Windows**:
重新安装 VLC,确保选择"完整安装"。

## 高级配置

### 自定义 VLC 选项

如果需要自定义 VLC 配置,可以修改应用代码:

**位置**: `composeApp/src/desktopMain/kotlin/com/menmapro/iptv/ui/components/VideoOutputConfiguration.kt`

**示例**:
```kotlin
fun getCustomVideoOptions(): Array<String> {
    return arrayOf(
        "--vout=opengl",
        "--avcodec-hw=any",
        "--no-video-title-show",
        "--no-osd",
        // 添加自定义选项
        "--verbose=2",              // 详细日志
        "--file-logging",           // 文件日志
        "--logfile=vlc.log"         // 日志文件
    )
}
```

### VLC 配置文件

**位置**:
- macOS: `~/Library/Preferences/org.videolan.vlc/vlcrc`
- Linux: `~/.config/vlc/vlcrc`
- Windows: `%APPDATA%\vlc\vlcrc`

**常用配置**:
```ini
# 硬件加速
avcodec-hw=any

# 视频输出
vout=auto

# 缓存
network-caching=1000
live-caching=300

# 性能
avcodec-threads=0  # 自动检测 CPU 核心数
```

### 环境变量

**VLC_PLUGIN_PATH**:
指定 VLC 插件目录
```bash
export VLC_PLUGIN_PATH=/usr/lib/vlc/plugins
```

**VLC_VERBOSE**:
设置日志级别 (0-3)
```bash
export VLC_VERBOSE=2
```

### 命令行测试

**测试视频播放**:
```bash
vlc --vout=macosx --avcodec-hw=any \
    --network-caching=1000 \
    --live-caching=300 \
    <video_url>
```

**测试硬件加速**:
```bash
vlc --avcodec-hw=any --verbose=2 <video_url>
```

**生成日志**:
```bash
vlc --file-logging --logfile=vlc.log <video_url>
```

## 推荐的 VLC 版本

| 平台 | 推荐版本 | 最低版本 | 说明 |
|------|---------|---------|------|
| macOS | 3.0.20 | 3.0.18 | 最新稳定版 |
| Linux | 3.0.20 | 3.0.18 | 最新稳定版 |
| Windows | 3.0.20 | 3.0.18 | 最新稳定版 |

**注意**: 避免使用 4.0 开发版本,可能不稳定。

## 支持的编解码器

### 视频编解码器

| 编解码器 | 硬件加速 | 说明 |
|---------|---------|------|
| H.264 (AVC) | ✅ | 最常用,推荐 |
| H.265 (HEVC) | ✅ | 高效,推荐 |
| VP8 | ⚠️ | 部分支持 |
| VP9 | ⚠️ | 部分支持 |
| MPEG-2 | ✅ | 传统格式 |
| MPEG-4 | ✅ | 传统格式 |

### 音频编解码器

| 编解码器 | 说明 |
|---------|------|
| AAC | 最常用 |
| MP3 | 传统格式 |
| Opus | 高质量 |
| Vorbis | 开源格式 |
| AC3 | 多声道 |

### 容器格式

| 格式 | 说明 |
|------|------|
| MP4 | 最常用 |
| MKV | 高级功能 |
| TS | 流媒体 |
| WebM | Web 优化 |
| AVI | 传统格式 |
| MOV | Apple 格式 |

## 性能基准

### 硬件加速性能对比

| 场景 | 软件解码 CPU | 硬件解码 CPU | 提升 |
|------|-------------|-------------|------|
| 1080p H.264 | 60-80% | 10-20% | 70% |
| 4K H.264 | 100%+ | 20-30% | 75% |
| 1080p H.265 | 80-100% | 15-25% | 70% |
| 4K H.265 | 100%+ | 25-35% | 70% |

### 缓存对延迟的影响

| 缓存大小 | 延迟 | 流畅度 | 适用场景 |
|---------|------|--------|---------|
| 300ms | 低 | 中 | 低延迟直播 |
| 1000ms | 中 | 高 | 标准直播 |
| 3000ms | 高 | 很高 | 不稳定网络 |

## 相关资源

### 官方文档

- [VLC 官网](https://www.videolan.org/)
- [VLC 用户指南](https://wiki.videolan.org/Documentation:Documentation/)
- [VLC 命令行帮助](https://wiki.videolan.org/VLC_command-line_help/)
- [VLC 硬件加速](https://wiki.videolan.org/Hardware_acceleration/)

### 社区资源

- [VLC 论坛](https://forum.videolan.org/)
- [VLC Reddit](https://www.reddit.com/r/VLC/)
- [VLCJ GitHub](https://github.com/caprica/vlcj)

### 相关项目文档

- [项目 README](../../../README.md)
- [故障排除指南](VIDEO_TROUBLESHOOTING.md)
- [技术文档](TECHNICAL_DOCUMENTATION.md)

## 更新日志

- **2025-11-26**: 初始版本
  - VLC 安装指南
  - 配置建议
  - 性能优化
  - 故障排除
  - 高级配置
