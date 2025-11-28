# libmpv 安装和配置指南

本指南提供了在不同平台上安装和配置 libmpv 的详细说明。

## 目录

- [什么是 libmpv](#什么是-libmpv)
- [系统要求](#系统要求)
- [平台特定安装](#平台特定安装)
  - [macOS 安装](#macos-安装)
  - [Linux 安装](#linux-安装)
  - [Windows 安装](#windows-安装)
- [验证安装](#验证安装)
- [配置选项](#配置选项)
- [故障排除](#故障排除)
- [常见问题](#常见问题)

---

## 什么是 libmpv

**libmpv** 是 MPV 媒体播放器的库形式,提供了强大的视频播放功能:

- 🎯 **广泛的格式支持**: 支持几乎所有主流视频格式和编解码器
- ⚡ **优秀的性能**: 高效的解码和渲染,低资源占用
- 🚀 **硬件加速**: 支持平台特定的硬件加速 (VideoToolbox, VAAPI, D3D11VA)
- 📺 **流媒体支持**: 完整支持 HLS, RTSP, HTTP 等流媒体协议
- 🔧 **灵活配置**: 丰富的配置选项,可针对不同场景优化

**MPV** 是一个免费、开源、跨平台的媒体播放器,以其简洁、高效和强大而闻名。libmpv 将 MPV 的核心功能封装为 C 库,可以嵌入到其他应用程序中。

---

## 系统要求

### 最低要求

- **操作系统**:
  - macOS 10.14 (Mojave) 或更高版本
  - Linux: Ubuntu 18.04, Debian 10, Fedora 30 或更高版本
  - Windows 10 或更高版本

- **硬件**:
  - CPU: 双核 1.5GHz 或更高
  - 内存: 2GB RAM 或更高
  - 显卡: 支持 OpenGL 2.1 或更高版本

### 推荐配置

- **硬件**:
  - CPU: 四核 2.0GHz 或更高
  - 内存: 4GB RAM 或更高
  - 显卡: 支持硬件视频解码 (H.264/HEVC)

- **网络**: 5Mbps 或更高 (用于高清流媒体)

---

## 平台特定安装

### macOS 安装

#### 方法 1: 使用 Homebrew (推荐)

Homebrew 是 macOS 上最流行的包管理器,可以轻松安装 libmpv。

**1. 安装 Homebrew** (如果尚未安装):

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

**2. 安装 MPV** (包含 libmpv):

```bash
brew install mpv
```

**3. 验证安装**:

```bash
# 检查 mpv 命令
mpv --version

# 检查 libmpv 库文件
# Apple Silicon (M1/M2/M3)
ls -l /opt/homebrew/lib/libmpv.dylib

# Intel Mac
ls -l /usr/local/lib/libmpv.dylib
```

#### 方法 2: 从源代码编译

如果需要自定义编译选项:

```bash
# 安装构建依赖
brew install pkg-config ffmpeg

# 克隆 MPV 源代码
git clone https://github.com/mpv-player/mpv.git
cd mpv

# 配置和编译
./bootstrap.py
./waf configure --enable-libmpv-shared
./waf build
sudo ./waf install
```

#### 库文件位置

- **Apple Silicon**: `/opt/homebrew/lib/libmpv.dylib`
- **Intel Mac**: `/usr/local/lib/libmpv.dylib`
- **头文件**: `/opt/homebrew/include/mpv/` 或 `/usr/local/include/mpv/`

---

### Linux 安装

#### Ubuntu / Debian

**1. 更新包列表**:

```bash
sudo apt-get update
```

**2. 安装 libmpv 开发包**:

```bash
sudo apt-get install libmpv-dev mpv
```

**3. 验证安装**:

```bash
# 检查 mpv 命令
mpv --version

# 检查 libmpv 库文件
ls -l /usr/lib/x86_64-linux-gnu/libmpv.so
```

#### Fedora / RHEL / CentOS

**1. 启用 RPM Fusion 仓库** (如果尚未启用):

```bash
sudo dnf install https://download1.rpmfusion.org/free/fedora/rpmfusion-free-release-$(rpm -E %fedora).noarch.rpm
```

**2. 安装 MPV**:

```bash
sudo dnf install mpv mpv-libs-devel
```

**3. 验证安装**:

```bash
mpv --version
ls -l /usr/lib64/libmpv.so
```

#### Arch Linux

```bash
sudo pacman -S mpv
```

#### 从源代码编译

```bash
# 安装构建依赖
sudo apt-get install build-essential pkg-config python3 \
    libavcodec-dev libavformat-dev libswscale-dev \
    libass-dev libasound2-dev libpulse-dev

# 克隆并编译
git clone https://github.com/mpv-player/mpv.git
cd mpv
./bootstrap.py
./waf configure --enable-libmpv-shared
./waf build
sudo ./waf install
```

#### 库文件位置

- **64位系统**: `/usr/lib/x86_64-linux-gnu/libmpv.so` (Debian/Ubuntu)
- **64位系统**: `/usr/lib64/libmpv.so` (Fedora/RHEL)
- **头文件**: `/usr/include/mpv/`

---

### Windows 安装

Windows 上安装 libmpv 稍微复杂一些,因为没有官方的包管理器。

#### 方法 1: 下载预编译二进制文件 (推荐)

**1. 下载 MPV**:

访问 [MPV Windows 构建](https://sourceforge.net/projects/mpv-player-windows/files/) 或 [shinchiro 构建](https://github.com/shinchiro/mpv-winbuild-cmake/releases)

下载最新的 `mpv-x86_64-*.7z` 文件。

**2. 解压文件**:

将下载的文件解压到一个目录,例如 `C:\mpv\`

**3. 配置环境变量**:

将 libmpv 所在目录添加到系统 PATH:

- 打开"系统属性" > "高级" > "环境变量"
- 在"系统变量"中找到 `Path`
- 添加 `C:\mpv\` (或您解压的目录)
- 点击"确定"保存

**4. 验证安装**:

打开命令提示符或 PowerShell:

```powershell
# 检查 mpv 命令
mpv --version

# 检查 libmpv-2.dll
dir C:\mpv\libmpv-2.dll
```

#### 方法 2: 使用 Scoop 包管理器

Scoop 是 Windows 上的命令行包管理器。

**1. 安装 Scoop** (如果尚未安装):

```powershell
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex
```

**2. 安装 MPV**:

```powershell
scoop install mpv
```

**3. 验证安装**:

```powershell
mpv --version
```

#### 方法 3: 手动放置 DLL 文件

如果不想修改系统 PATH,可以将 `libmpv-2.dll` 复制到应用程序目录:

1. 下载并解压 MPV
2. 找到 `libmpv-2.dll`
3. 将其复制到 IPTV Player 的可执行文件所在目录

#### 库文件位置

- **DLL 文件**: `libmpv-2.dll`
- **头文件**: `include/mpv/`

---

## 验证安装

### 使用 mpv 命令行

最简单的验证方法是使用 mpv 命令行播放一个测试视频:

```bash
# 播放在线测试视频
mpv https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8

# 播放本地文件
mpv /path/to/video.mp4

# 显示详细信息
mpv --version
```

### 检查库文件

**macOS**:
```bash
# Apple Silicon
file /opt/homebrew/lib/libmpv.dylib

# Intel
file /usr/local/lib/libmpv.dylib
```

**Linux**:
```bash
file /usr/lib/x86_64-linux-gnu/libmpv.so
ldd /usr/lib/x86_64-linux-gnu/libmpv.so
```

**Windows**:
```powershell
dir C:\mpv\libmpv-2.dll
```

### 在应用中测试

运行 IPTV Player 应用:

```bash
./gradlew :composeApp:run
```

如果 libmpv 正确安装,应用应该能够:
1. 成功启动
2. 显示播放器界面
3. 播放视频流

如果 libmpv 未找到,应用会显示错误消息并提供安装指南。

---

## 配置选项

### 默认配置

IPTV Player 使用以下默认配置:

```kotlin
val DEFAULT_LIBMPV_CONFIG = LibmpvConfiguration(
    hardwareAcceleration = true,
    hwdecMethod = "auto",      // 自动选择最佳硬件加速方法
    videoOutput = "gpu",       // GPU 渲染
    audioOutput = "auto",      // 自动选择音频输出
    cacheSize = 150000,        // 150 MB 缓存
    cacheSecs = 10,            // 10 秒缓冲
    demuxerReadahead = 5,      // 5 秒预读取
    networkTimeout = 30,       // 30 秒网络超时
    userAgent = "IPTV-Player/1.0"
)
```

### 硬件加速配置

#### macOS

```kotlin
// 使用 VideoToolbox (推荐)
engine.setOption("hwdec", "videotoolbox")
engine.setOption("hwdec-codecs", "h264,hevc")
```

支持的硬件加速方法:
- `videotoolbox`: Apple VideoToolbox (推荐)
- `videotoolbox-copy`: VideoToolbox with copy-back
- `auto`: 自动选择

#### Linux

```kotlin
// Intel/AMD GPU - 使用 VAAPI
engine.setOption("hwdec", "vaapi")

// NVIDIA GPU - 使用 VDPAU
engine.setOption("hwdec", "vdpau")

// 自动选择
engine.setOption("hwdec", "auto")
```

支持的硬件加速方法:
- `vaapi`: VA-API (Intel/AMD)
- `vdpau`: VDPAU (NVIDIA)
- `auto`: 自动选择

#### Windows

```kotlin
// 使用 D3D11VA (推荐)
engine.setOption("hwdec", "d3d11va")

// 或使用 DXVA2
engine.setOption("hwdec", "dxva2")

// 自动选择
engine.setOption("hwdec", "auto")
```

支持的硬件加速方法:
- `d3d11va`: Direct3D 11 (推荐)
- `dxva2`: DirectX Video Acceleration 2
- `auto`: 自动选择

### 视频输出配置

```kotlin
// GPU 渲染 (推荐)
engine.setOption("vo", "gpu")

// X11 输出 (Linux)
engine.setOption("vo", "x11")

// 自动选择
engine.setOption("vo", "auto")
```

### 缓存和缓冲配置

```kotlin
// 启用缓存
engine.setOption("cache", "yes")

// 缓存大小 (KB)
engine.setOption("cache-secs", "10")

// 预读取时间 (秒)
engine.setOption("demuxer-readahead-secs", "5")

// 网络超时 (秒)
engine.setOption("network-timeout", "30")
```

### 音频配置

```kotlin
// 自动选择音频输出
engine.setOption("ao", "auto")

// 音频缓冲 (秒)
engine.setOption("audio-buffer", "0.2")

// 音量 (0-100)
engine.setVolume(50)
```

### 低延迟配置 (直播流)

```kotlin
// 使用低延迟配置文件
engine.setOption("profile", "low-latency")

// 或手动配置
engine.setOption("cache", "no")
engine.setOption("cache-secs", "5")
engine.setOption("demuxer-readahead-secs", "2")
engine.setOption("video-sync", "audio")
```

### 高质量配置 (点播内容)

```kotlin
// 增大缓存
engine.setOption("cache-secs", "30")
engine.setOption("demuxer-readahead-secs", "10")

// 启用高质量缩放
engine.setOption("scale", "ewa_lanczossharp")
engine.setOption("cscale", "ewa_lanczossharp")
```

---

## 故障排除

### 问题 1: libmpv 未找到

**症状**: 应用提示 "libmpv not found" 或 "Failed to load libmpv"

**解决方案**:

**macOS**:
```bash
# 检查 libmpv 是否存在
ls /opt/homebrew/lib/libmpv.dylib  # Apple Silicon
ls /usr/local/lib/libmpv.dylib     # Intel

# 如果不存在,安装 mpv
brew install mpv

# 检查 Homebrew 路径是否在 PATH 中
echo $PATH | grep homebrew
```

**Linux**:
```bash
# 检查 libmpv 是否存在
ls /usr/lib/x86_64-linux-gnu/libmpv.so

# 如果不存在,安装 libmpv
sudo apt-get install libmpv-dev mpv

# 更新动态链接器缓存
sudo ldconfig
```

**Windows**:
```powershell
# 检查 libmpv-2.dll 是否在 PATH 中
where libmpv-2.dll

# 如果找不到,添加到 PATH 或复制到应用目录
```

### 问题 2: 版本不兼容

**症状**: 应用启动但播放失败,日志显示版本错误

**解决方案**:

```bash
# 检查 mpv 版本
mpv --version

# 需要 mpv 0.33.0 或更高版本
# 如果版本过旧,更新 mpv

# macOS
brew upgrade mpv

# Linux
sudo apt-get update
sudo apt-get upgrade mpv

# Windows
# 下载最新版本并替换旧文件
```

### 问题 3: 硬件加速不工作

**症状**: 视频播放但 CPU 使用率很高,或视频黑屏

**解决方案**:

```bash
# 测试硬件加速
mpv --hwdec=auto --vo=gpu https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8

# 如果失败,尝试禁用硬件加速
mpv --hwdec=no https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
```

在应用中:
```kotlin
// 禁用硬件加速
engine.setOption("hwdec", "no")

// 或尝试不同的方法
engine.setOption("hwdec", "auto-copy")
```

可能原因:
- 显卡驱动过旧
- 不支持的硬件
- 编解码器不支持硬件解码

### 问题 4: 视频黑屏

**症状**: 音频正常但视频不显示

**解决方案**:

```kotlin
// 尝试不同的视频输出
engine.setOption("vo", "x11")      // Linux
engine.setOption("vo", "gpu")      // 通用
engine.setOption("vo", "direct3d") // Windows

// 禁用硬件加速
engine.setOption("hwdec", "no")

// 检查像素格式
engine.setOption("vf", "format=yuv420p")
```

### 问题 5: 音频问题

**症状**: 无声音或音频断断续续

**解决方案**:

```kotlin
// 尝试不同的音频输出
engine.setOption("ao", "pulse")    // Linux PulseAudio
engine.setOption("ao", "alsa")     // Linux ALSA
engine.setOption("ao", "coreaudio") // macOS
engine.setOption("ao", "wasapi")   // Windows

// 增加音频缓冲
engine.setOption("audio-buffer", "0.5")

// 检查音频设备
engine.setOption("audio-device", "auto")
```

### 问题 6: 网络流播放失败

**症状**: 本地文件可以播放,但网络流失败

**解决方案**:

```bash
# 测试网络连接
curl -I "YOUR_STREAM_URL"

# 使用 mpv 测试
mpv --log-file=mpv.log "YOUR_STREAM_URL"
```

在应用中:
```kotlin
// 增加网络超时
engine.setOption("network-timeout", "60")

// 设置 User-Agent
engine.setOption("user-agent", "Mozilla/5.0")

// 启用重连
engine.setOption("stream-lavf-o", "reconnect=1,reconnect_streamed=1")

// 增加缓存
engine.setOption("cache-secs", "20")
```

### 问题 7: 内存泄漏

**症状**: 应用运行一段时间后内存占用持续增长

**解决方案**:

```kotlin
// 确保正确释放资源
override fun onDispose() {
    // 停止播放
    engine.stop()
    
    // 销毁引擎
    engine.destroy()
    
    // 释放渲染上下文
    renderer.release()
}

// 检查事件线程是否停止
// 检查是否有未释放的帧缓冲
```

### 问题 8: 性能问题

**症状**: CPU 使用率过高或播放卡顿

**解决方案**:

```kotlin
// 启用硬件加速
engine.setOption("hwdec", "auto")

// 降低视频质量
engine.setOption("vd-lavc-threads", "2")

// 减小缓存
engine.setOption("cache-secs", "5")

// 禁用高级缩放
engine.setOption("scale", "bilinear")

// 降低音频质量
engine.setOption("audio-samplerate", "44100")
```

---

## 常见问题

### Q1: libmpv 和 mpv 有什么区别?

**A**: mpv 是一个完整的媒体播放器应用程序,而 libmpv 是 mpv 的库形式,可以嵌入到其他应用程序中。安装 mpv 通常会同时安装 libmpv。

### Q2: 需要什么版本的 libmpv?

**A**: IPTV Player 需要 libmpv 0.33.0 或更高版本。推荐使用最新稳定版本。

### Q3: libmpv 支持哪些视频格式?

**A**: libmpv 支持几乎所有主流视频格式,包括:
- 容器: MP4, MKV, AVI, MOV, TS, WebM, FLV
- 编解码器: H.264, H.265, VP8, VP9, AV1, MPEG-2, MPEG-4
- 流媒体: HLS, RTSP, RTMP, HTTP, UDP/RTP

### Q4: 硬件加速是否必需?

**A**: 不是必需的,但强烈推荐。硬件加速可以显著降低 CPU 使用率,提高播放性能,特别是对于高分辨率视频。

### Q5: 如何检查硬件加速是否工作?

**A**: 使用 mpv 命令行测试:

```bash
# 启用硬件加速
mpv --hwdec=auto --vo=gpu YOUR_VIDEO

# 查看日志
mpv --log-file=mpv.log --hwdec=auto YOUR_VIDEO
# 检查日志中是否有 "Using hardware decoding" 消息
```

### Q6: libmpv 是否免费?

**A**: 是的,libmpv 是开源软件,使用 GPL 和 LGPL 许可证。可以免费使用和分发。

### Q7: 如何更新 libmpv?

**A**:

**macOS**:
```bash
brew upgrade mpv
```

**Linux**:
```bash
sudo apt-get update
sudo apt-get upgrade mpv
```

**Windows**:
下载最新版本并替换旧文件。

### Q8: libmpv 占用多少磁盘空间?

**A**: 
- macOS/Linux: 约 10-20 MB (库文件)
- Windows: 约 30-50 MB (包含依赖)

### Q9: 可以同时使用多个 libmpv 实例吗?

**A**: 可以,libmpv 支持多实例。每个实例都是独立的,可以同时播放不同的视频。

### Q10: libmpv 是否支持字幕?

**A**: 是的,libmpv 完全支持字幕,包括:
- 内嵌字幕 (MKV, MP4)
- 外部字幕文件 (SRT, ASS, SSA)
- 在线字幕

---

## 其他资源

### 官方文档

- **MPV 官网**: https://mpv.io/
- **MPV 手册**: https://mpv.io/manual/stable/
- **libmpv 文档**: https://github.com/mpv-player/mpv/blob/master/libmpv/client.h
- **MPV GitHub**: https://github.com/mpv-player/mpv

### 社区支持

- **MPV 论坛**: https://github.com/mpv-player/mpv/discussions
- **IRC**: #mpv on irc.libera.chat
- **Reddit**: r/mpv

### 相关项目

- **mpv.js**: JavaScript bindings for libmpv
- **python-mpv**: Python bindings for libmpv
- **node-mpv**: Node.js bindings for libmpv

---

## 贡献

如果您在使用 libmpv 时遇到问题或有改进建议,请:

1. 查看本指南的故障排除部分
2. 搜索 [GitHub Issues](https://github.com/YOUR_USERNAME/IPTV/issues)
3. 提交新的 Issue,包含:
   - 操作系统和版本
   - libmpv 版本
   - 详细的问题描述
   - 错误日志
   - 复现步骤

---

**最后更新**: 2024-11-28
