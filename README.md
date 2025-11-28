# IPTV Player - 跨平台 IPTV 播放器

[![构建状态](https://github.com/YOUR_USERNAME/IPTV/actions/workflows/build-release.yml/badge.svg)](https://github.com/YOUR_USERNAME/IPTV/actions/workflows/build-release.yml)
[![最新版本](https://img.shields.io/github/v/release/YOUR_USERNAME/IPTV)](https://github.com/YOUR_USERNAME/IPTV/releases/latest)
[![下载量](https://img.shields.io/github/downloads/YOUR_USERNAME/IPTV/total)](https://github.com/YOUR_USERNAME/IPTV/releases)

这是一个使用 Kotlin Multiplatform (KMP) 和 Compose Multiplatform 开发的跨平台 IPTV 播放器,支持 Android、macOS 和 Windows。

## 功能特性

- ✅ **多平台支持**: Android、macOS、Windows
- ✅ **M3U 播放列表**: 支持 M3U URL 和本地 M3U 文件
- ✅ **Xtream Codes**: 支持 Xtream Codes API 登录和流媒体播放
- ✅ **视频播放**:
  - Android: ExoPlayer (Media3)
  - Desktop: FFmpeg (推荐) 或 VLC (备选)
- ✅ **Material Design UI**: 现代化的用户界面

## 🛠️ 技术栈

### 核心框架

- **Kotlin Multiplatform (KMP)**: 跨平台共享业务逻辑
  - 共享代码比例: ~80%
  - 平台特定代码: ~20%
  
- **Compose Multiplatform**: 声明式 UI 框架
  - 统一的 UI 代码
  - Material Design 3
  - 响应式设计

### 网络和数据

- **Ktor Client**: 异步网络请求
  - HTTP/HTTPS 支持
  - JSON 序列化
  - 内容协商
  
- **SQLDelight**: 类型安全的 SQL 数据库
  - 跨平台数据持久化
  - 编译时 SQL 验证
  
- **DataStore**: 键值对存储
  - 用户偏好设置
  - 配置管理

### 依赖注入和导航

- **Koin**: 轻量级依赖注入
  - 简单易用
  - Compose 集成
  
- **Voyager**: 类型安全的导航
  - 屏幕管理
  - 状态保持

### 媒体播放

**Desktop 平台**:
- **libmpv**: MPV 媒体播放器库 (默认)
  - 强大的格式支持
  - 优秀的性能
  - 硬件加速支持
  - 低延迟直播流

**Android 平台**:
- **Media3 (ExoPlayer)**: Google 官方播放器
  - 自适应流媒体
  - HLS/DASH 支持
  - 硬件加速

### 图片加载

- **Coil 3**: 现代化的图片加载库
  - Compose 原生支持
  - 内存和磁盘缓存
  - 网络图片加载

### 测试框架

- **Kotlin Test**: 跨平台测试
- **JUnit 5**: JVM 测试
- **Kotest**: 属性测试和断言
- **MockK**: Kotlin 模拟框架

### 构建工具

- **Gradle 8.5**: 构建自动化
- **Kotlin 2.0.21**: 最新 Kotlin 版本
- **Compose Compiler**: Compose 编译器插件

### 依赖版本

主要依赖版本 (详见 `gradle/libs.versions.toml`):

```toml
[versions]
kotlin = "2.0.21"
compose = "1.7.1"
ktor = "3.0.1"
koin = "4.0.0"
media3 = "1.5.0"
jna = "5.13.0"
```

**Desktop 播放器依赖**:

libmpv 播放器 (默认):
```kotlin
// JNA 用于调用 libmpv C API
implementation("net.java.dev.jna:jna:5.13.0")
implementation("net.java.dev.jna:jna-platform:5.13.0")
```

**Android 播放器依赖**:
```kotlin
// Media3 (ExoPlayer)
implementation("androidx.media3:media3-exoplayer:1.5.0")
implementation("androidx.media3:media3-ui:1.5.0")
implementation("androidx.media3:media3-exoplayer-hls:1.5.0")
```

**共享依赖**:
```kotlin
// 网络请求
implementation("io.ktor:ktor-client-core:3.0.1")
implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")

// 依赖注入
implementation("io.insert-koin:koin-core:4.0.0")
implementation("io.insert-koin:koin-compose:4.0.0")

// 图片加载
implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc01")
implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.0-rc01")
```

## 项目结构

```
IPTV/
├── composeApp/
│   ├── src/
│   │   ├── commonMain/          # 共享代码
│   │   │   ├── kotlin/
│   │   │   │   └── com/menmapro/iptv/
│   │   │   │       ├── data/    # 数据层
│   │   │   │       │   ├── model/       # 数据模型
│   │   │   │       │   ├── parser/      # M3U 解析器
│   │   │   │       │   ├── network/     # Xtream API 客户端
│   │   │   │       │   └── repository/  # 数据仓库
│   │   │   │       ├── ui/      # UI 层
│   │   │   │       │   ├── screens/     # 页面
│   │   │   │       │   ├── components/  # 组件
│   │   │   │       │   └── theme/       # 主题
│   │   │   │       └── di/      # 依赖注入
│   │   ├── androidMain/         # Android 特定代码
│   │   └── desktopMain/         # Desktop 特定代码
│   └── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml       # 版本目录
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 📥 下载安装

### 从 GitHub Releases 下载

访问 [Releases 页面](https://github.com/YOUR_USERNAME/IPTV/releases/latest) 下载最新版本：

| 平台 | 文件类型 | 说明 |
|------|---------|------|
| 🪟 Windows | `.msi` | Windows 安装程序 |
| 🍎 macOS | `.dmg` | macOS 磁盘映像 |
| 🐧 Linux | `.deb` | Debian/Ubuntu 安装包 |
| 🤖 Android | `.apk` | Android 安装包 |

### 安装说明

**Windows**: 双击 `.msi` 文件，按照安装向导操作

**macOS**: 
1. 打开 `.dmg` 文件
2. 将应用拖到 Applications 文件夹
3. 首次运行需要在"系统偏好设置 > 安全性与隐私"中允许

**Linux**: 
```bash
sudo dpkg -i iptv-player_1.0.0_amd64.deb
```

**Android**: 
1. 下载 `.apk` 文件
2. 在设备上启用"允许安装未知来源应用"
3. 打开 APK 文件安装

## 🛠️ 开发构建

### 前置要求

**必需**:
- **JDK 17+**: 确保已安装 Java 开发工具包
  ```bash
  # 验证 Java 版本
  java -version
  ```

**可选** (根据目标平台):
- **Android SDK**: 用于 Android 构建
  - Android SDK 34 或更高版本
  - Android Build Tools 34.0.0 或更高版本
  
- **libmpv**: Desktop 视频播放所需 (推荐安装)
  - macOS: `brew install mpv`
  - Linux: `sudo apt-get install libmpv-dev`
  - Windows: 从 [MPV 官网](https://mpv.io/installation/) 下载

**注意**: libmpv 是外部依赖,需要在系统上安装。详细安装指南请参考 [LIBMPV_SETUP_GUIDE.md](LIBMPV_SETUP_GUIDE.md)。

### 克隆项目

```bash
git clone https://github.com/YOUR_USERNAME/IPTV.git
cd IPTV
```

### 运行 Desktop 版本

**前置要求**: 确保已安装 libmpv (参考 [LIBMPV_SETUP_GUIDE.md](LIBMPV_SETUP_GUIDE.md))

```bash
# 运行应用 (使用 libmpv 播放器)
./gradlew :composeApp:run
```

**验证 libmpv 安装**:

```bash
# macOS
ls /opt/homebrew/lib/libmpv.dylib  # Apple Silicon
ls /usr/local/lib/libmpv.dylib     # Intel

# Linux
ls /usr/lib/x86_64-linux-gnu/libmpv.so

# Windows
# 检查 libmpv-2.dll 是否在系统 PATH 中
```

如果 libmpv 未安装,应用会显示错误消息并提供安装指南。

### 运行 Android 版本

```bash
# 连接 Android 设备或启动模拟器

# 安装 Debug 版本
./gradlew :composeApp:installDebug

# 或者构建 APK
./gradlew :composeApp:assembleDebug

# APK 位置: composeApp/build/outputs/apk/debug/
```

### 构建发布版本

**Desktop 平台**:

```bash
# 为当前操作系统打包
./gradlew :composeApp:packageDistributionForCurrentOS

# 输出位置:
# - macOS: composeApp/build/compose/binaries/main/dmg/
# - Windows: composeApp/build/compose/binaries/main/msi/
# - Linux: composeApp/build/compose/binaries/main/deb/
```

**Android 平台**:

```bash
# 构建签名的 Release APK
./gradlew :composeApp:assembleRelease

# 输出位置: composeApp/build/outputs/apk/release/
```

**所有平台** (使用脚本):

```bash
# macOS/Linux
./repackage.sh

# Windows
repackage.bat
```

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行 Desktop 测试
./gradlew :composeApp:desktopTest

# 运行 Android 测试
./gradlew :composeApp:testDebugUnitTest

# 运行特定测试
./gradlew :composeApp:desktopTest --tests "FFmpegPlayerEngineTest"
```

### 清理构建

```bash
# 清理构建缓存
./gradlew clean

# 清理并重新构建
./gradlew clean build
```

### 详细构建文档

- **[本地构建指南](BUILD_PACKAGES.md)** - 本地打包所有平台的详细说明
- **[GitHub Actions 自动构建](GITHUB_ACTIONS_GUIDE.md)** - 自动化构建和发布配置
- **[快速发布指南](RELEASE_GUIDE.md)** - 一键发布新版本的完整流程

## 使用说明

### 添加 M3U 播放列表

1. 启动应用
2. 点击右下角的 "+" 按钮
3. 选择 "M3U URL" 标签
4. 输入 M3U 播放列表 URL (例如: `https://iptv-org.github.io/iptv/index.m3u`)
5. 点击 "Add"

### 添加 Xtream Codes 账户

1. 启动应用
2. 点击右下角的 "+" 按钮
3. 选择 "Xtream" 标签
4. 输入服务器 URL、用户名和密码
5. 点击 "Add"

### 播放频道

1. 在播放列表页面选择一个播放列表
2. 在频道列表中选择要播放的频道
3. 视频将自动开始播放

## 🎬 Desktop 播放器

Desktop 版本使用 **libmpv 播放器**作为默认播放引擎,提供强大的格式支持和优秀的性能。

### libmpv 播放器 (默认,推荐)

基于 MPV 媒体播放器的库形式,通过 JNA 绑定提供 Kotlin 集成。

#### 核心优势

| 特性 | 描述 |
|------|------|
| 🎯 **强大的格式支持** | 支持几乎所有主流视频格式和流媒体协议 |
| ⚡ **优秀的性能** | 高效的解码和渲染,低 CPU 占用 |
| 🚀 **硬件加速** | 自动检测并使用平台特定的硬件加速 |
| 📺 **低延迟直播** | 优化的直播流处理,延迟低至 0.5-1 秒 |
| 🔧 **灵活配置** | 丰富的配置选项,可针对不同场景优化 |
| 🌍 **跨平台** | 支持 macOS、Linux 和 Windows |

#### 主要特性

- ✅ **硬件加速**: 自动检测并使用平台特定的硬件加速
  - 🍎 macOS: VideoToolbox (hwdec=videotoolbox)
  - 🐧 Linux: VAAPI (hwdec=vaapi) 或 VDPAU (hwdec=vdpau)
  - 🪟 Windows: D3D11VA (hwdec=d3d11va)
  
- ✅ **智能缓冲**: 可配置的缓冲策略
  - 网络缓存: 150MB 默认
  - 缓冲时间: 10 秒默认
  - 预读取: 5 秒默认
  
- ✅ **音视频同步**: libmpv 内置的精确同步
  - 自动音视频同步
  - 帧精确的播放控制
  - 智能丢帧策略
  
- ✅ **错误处理**: 完善的错误处理和恢复
  - 详细的错误日志
  - 自动重试机制
  - 回退到安全配置
  
- ✅ **播放控制**: 完整的播放控制功能
  - 播放/暂停/停止
  - 精确跳转 (Seek)
  - 音量控制
  - 位置和时长查询

#### 架构设计

libmpv 播放器采用事件驱动架构:

```
┌─────────────────────────────────────────────────────────┐
│              LibmpvPlayerImplementation                  │
│  ┌────────────────────────────────────────────────────┐ │
│  │         LibmpvVideoPlayer (Compose)                │ │
│  │  - UI 集成                                         │ │
│  │  - 生命周期管理                                     │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                 LibmpvPlayerEngine                       │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ Event        │  │ Command      │  │ Property     │  │
│  │ Thread       │  │ Execution    │  │ Management   │  │
│  │              │  │              │  │              │  │
│  │ 处理 libmpv  │  │ 播放控制     │  │ 状态查询     │  │
│  │ 事件回调     │  │ 命令执行     │  │ 配置管理     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              LibmpvFrameRenderer                         │
│  - 视频帧获取                                            │
│  - 像素格式转换                                          │
│  - Compose 渲染集成                                      │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              libmpv C API (JNA Bindings)                 │
└─────────────────────────────────────────────────────────┘
```

### 支持的视频格式和协议

libmpv 播放器支持几乎所有主流的视频格式和协议:

**视频编解码器**:
- ✅ H.264 (AVC) - 推荐,硬件加速支持
- ✅ H.265 (HEVC) - 推荐,硬件加速支持
- ✅ VP8/VP9 - WebM 格式
- ✅ MPEG-2/MPEG-4
- ✅ AV1 - 新一代编解码器
- ✅ 其他 FFmpeg 支持的编解码器

**音频编解码器**:
- ✅ AAC, MP3, Opus
- ✅ AC3, EAC3 (Dolby Digital)
- ✅ FLAC, Vorbis
- ✅ PCM (未压缩音频)

**流媒体协议**:
- ✅ HTTP/HTTPS - 标准 HTTP 流
- ✅ HLS (HTTP Live Streaming) - .m3u8 播放列表,自适应流
- ✅ RTSP (Real Time Streaming Protocol) - 实时流
- ✅ RTMP (Real Time Messaging Protocol) - Flash 流
- ✅ UDP/RTP - 组播流
- ✅ FILE - 本地文件播放

**容器格式**:
- ✅ MP4, MKV, AVI, MOV
- ✅ TS (MPEG Transport Stream)
- ✅ WebM
- ✅ FLV
- ✅ OGG, 3GP

### 播放器配置

#### libmpv 播放器配置

libmpv 需要在系统上安装。默认配置已针对大多数场景优化,但您可以根据需要调整。

**默认配置**:

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

**配置选项说明**:

| 选项 | 说明 | 推荐值 |
|------|------|--------|
| `hardwareAcceleration` | 启用硬件加速 | `true` |
| `hwdecMethod` | 硬件解码方法 | `"auto"` (自动), `"videotoolbox"` (macOS), `"vaapi"` (Linux), `"d3d11va"` (Windows) |
| `videoOutput` | 视频输出方式 | `"gpu"` (GPU 渲染), `"x11"` (Linux X11) |
| `audioOutput` | 音频输出方式 | `"auto"` (自动选择) |
| `cacheSize` | 网络缓存大小 (KB) | `150000` (150 MB) |
| `cacheSecs` | 缓冲时间 (秒) | `10` (直播流), `30` (点播) |
| `demuxerReadahead` | 预读取时间 (秒) | `5` |
| `networkTimeout` | 网络超时 (秒) | `30` |

**自定义配置示例**:

```kotlin
// 低延迟直播流配置
val liveStreamConfig = LibmpvConfiguration(
    hardwareAcceleration = true,
    hwdecMethod = "auto",
    videoOutput = "gpu",
    cacheSize = 50000,         // 减小缓存
    cacheSecs = 5,             // 减少缓冲时间
    demuxerReadahead = 2,      // 减少预读取
    networkTimeout = 15
)

// 高质量点播配置
val vodConfig = LibmpvConfiguration(
    hardwareAcceleration = true,
    hwdecMethod = "auto",
    videoOutput = "gpu",
    cacheSize = 300000,        // 增大缓存
    cacheSecs = 30,            // 增加缓冲时间
    demuxerReadahead = 10,     // 增加预读取
    networkTimeout = 60
)
```

**运行时配置**:

```kotlin
// 在播放器引擎中设置选项
engine.setOption("hwdec", "auto")
engine.setOption("vo", "gpu")
engine.setOption("cache", "yes")
engine.setOption("cache-secs", "10")
```

#### 性能调优建议

**低延迟场景** (直播流):
- 减小缓存大小 (50-100 MB)
- 减少缓冲时间 (5-10 秒)
- 启用硬件加速
- 使用 GPU 视频输出

**高质量场景** (点播内容):
- 增大缓存大小 (200-300 MB)
- 增加缓冲时间 (20-30 秒)
- 启用硬件加速
- 使用 GPU 视频输出

**低性能设备**:
- 禁用硬件加速 (如果不稳定)
- 减小缓存大小
- 降低视频分辨率
- 使用软件解码

### 📚 文档资源

#### libmpv 播放器文档

**用户指南**:
- 🚀 **[libmpv 安装指南](LIBMPV_SETUP_GUIDE.md)** - 平台特定的 libmpv 安装说明
- 🔧 **[故障排除指南](LIBMPV_SETUP_GUIDE.md#故障排除)** - 常见问题解决方案
- ⚙️ **[配置选项](LIBMPV_SETUP_GUIDE.md#配置选项)** - libmpv 配置详解

**开发者文档**:
- 🏗️ **[设计文档](.kiro/specs/libmpv-player-migration/design.md)** - 架构设计和技术细节
- 📋 **[需求文档](.kiro/specs/libmpv-player-migration/requirements.md)** - 功能需求和验收标准
- 📝 **[实现计划](.kiro/specs/libmpv-player-migration/tasks.md)** - 实现任务列表

#### 构建和发布文档

- 🔨 **[本地构建指南](BUILD_GUIDE.md)** - 本地打包所有平台
- 🤖 **[GitHub Actions 指南](GITHUB_ACTIONS_GUIDE.md)** - 自动化构建和发布
- 🚀 **[发布指南](RELEASE_GUIDE.md)** - 一键发布新版本

### 🔧 故障排除

#### libmpv 播放器常见问题

详细的故障排除指南: [LIBMPV_SETUP_GUIDE.md](LIBMPV_SETUP_GUIDE.md#故障排除)

**1. libmpv 未找到**

症状: 应用提示 "libmpv not found" 或无法初始化

解决方案:
```bash
# macOS
brew install mpv

# 验证安装
ls /opt/homebrew/lib/libmpv.dylib  # Apple Silicon
ls /usr/local/lib/libmpv.dylib     # Intel

# Linux
sudo apt-get install libmpv-dev

# 验证安装
ls /usr/lib/x86_64-linux-gnu/libmpv.so

# Windows
# 从 https://mpv.io/installation/ 下载
# 将 libmpv-2.dll 放到系统 PATH 中
```

**2. 播放器初始化失败**

症状: 应用启动时崩溃或无法播放视频

解决方案:
```bash
# 检查 libmpv 版本
mpv --version

# 清理并重新构建
./gradlew clean build

# 查看详细日志
./gradlew :composeApp:run --info
```

可能原因:
- libmpv 未正确安装
- libmpv 版本过旧 (需要 0.33.0+)
- JNA 依赖问题
- 平台特定库路径问题

**3. 视频黑屏但有声音**

症状: 音频正常播放,但视频区域显示黑屏

解决方案:
```kotlin
// 禁用硬件加速测试
engine.setOption("hwdec", "no")

// 尝试不同的视频输出
engine.setOption("vo", "x11")  // Linux
engine.setOption("vo", "gpu")  // 通用
```

可能原因:
- 硬件加速不兼容
- 视频输出配置问题
- 渲染上下文创建失败
- 显卡驱动问题

**4. 音视频不同步**

症状: 音频和视频播放速度不一致

解决方案:
```kotlin
// libmpv 自动处理音视频同步
// 如果仍有问题,尝试:
engine.setOption("video-sync", "audio")
engine.setOption("audio-buffer", "0.2")
```

可能原因:
- 解码性能不足
- 系统资源紧张
- 音频设备延迟

**5. 直播流延迟过高**

症状: 直播流延迟超过 2-3 秒

解决方案:
```kotlin
// 减小缓存
engine.setOption("cache", "no")
engine.setOption("cache-secs", "5")
engine.setOption("demuxer-readahead-secs", "2")

// 启用低延迟模式
engine.setOption("profile", "low-latency")
```

可能原因:
- 缓存配置过大
- 网络带宽不足
- 服务器延迟

**6. 网络流播放失败**

症状: HTTP/HLS 流无法播放或频繁中断

解决方案:
```bash
# 测试网络连接
curl -I "YOUR_STREAM_URL"

# 使用 mpv 命令行测试
mpv "YOUR_STREAM_URL"
```

```kotlin
// 增加超时时间
engine.setOption("network-timeout", "60")

// 设置 User-Agent
engine.setOption("user-agent", "IPTV-Player/1.0")

// 启用重连
engine.setOption("stream-lavf-o", "reconnect=1,reconnect_streamed=1")
```

可能原因:
- 网络连接不稳定
- 防火墙阻止连接
- 流服务器问题
- URL 格式错误

**7. 内存泄漏或崩溃**

症状: 应用运行一段时间后内存占用持续增长或崩溃

解决方案:
```kotlin
// 确保正确释放资源
override fun onDispose() {
    engine.stop()
    engine.destroy()
}

// 检查事件线程是否正确停止
// 检查渲染上下文是否正确释放
```

可能原因:
- 未调用 `destroy()` 方法
- 事件线程未正确停止
- 渲染上下文未释放
- libmpv 内部错误

**8. 性能问题 (高 CPU/内存使用)**

症状: CPU 使用率过高或内存占用过大

解决方案:
```kotlin
// 禁用硬件加速 (如果反而更慢)
engine.setOption("hwdec", "no")

// 降低视频质量
engine.setOption("vd-lavc-threads", "2")

// 减小缓存
engine.setOption("cache-secs", "5")
```

可能原因:
- 硬件加速不稳定
- 视频分辨率过高
- 系统资源不足
- 解码器选择不当

#### 获取帮助

如果问题仍未解决:

1. **查看日志**: 应用日志包含详细的错误信息
2. **测试 mpv 命令行**: 使用 `mpv` 命令测试流是否可以播放
3. **检查 libmpv 版本**: 确保使用最新版本的 libmpv
4. **提交 Issue**: 在 [GitHub Issues](https://github.com/YOUR_USERNAME/IPTV/issues) 提交问题,附上:
   - 操作系统和版本
   - libmpv 版本 (`mpv --version`)
   - 应用版本
   - 错误日志
   - 复现步骤
   - 测试的流 URL (如果可以公开)

## ⚠️ 已知问题和限制

### Desktop 版本

**libmpv 播放器** (默认):
- ⚠️ 需要系统安装 libmpv (外部依赖)
- ⚠️ 首次播放可能需要 1-2 秒初始化
- ⚠️ 某些专有编解码器 (如 DRM 保护内容) 可能不支持
- ⚠️ 硬件加速在某些旧设备上可能不稳定
- ⚠️ Windows 上需要手动配置 libmpv 路径

### Android 版本

- ✅ 使用 ExoPlayer (Media3),性能稳定
- ⚠️ 某些设备可能不支持硬件解码
- ⚠️ 低端设备播放高分辨率视频可能卡顿

### 编译警告

以下警告不影响功能,可以忽略:
- `ArrowBack` 图标弃用警告 (Compose 版本升级导致)
- `kotlinOptions` 迁移到 `compilerOptions` (已处理)
- 某些依赖的传递依赖警告

### 性能建议

**推荐配置**:
- CPU: 双核 2.0GHz 或更高
- 内存: 4GB RAM 或更高
- 网络: 5Mbps 或更高 (用于高清流)

**最低配置**:
- CPU: 单核 1.5GHz
- 内存: 2GB RAM
- 网络: 2Mbps (用于标清流)

## 🗺️ 开发计划

### 已完成 ✅

- ✅ 跨平台支持 (Android, macOS, Windows)
- ✅ M3U 播放列表支持
- ✅ Xtream Codes API 支持
- ✅ FFmpeg 播放器实现 (Desktop)
- ✅ ExoPlayer 集成 (Android)
- ✅ 硬件加速支持
- ✅ 直播流优化
- ✅ 音视频同步
- ✅ 自动重连机制
- ✅ 性能监控和诊断

### 进行中 🚧

- 🚧 播放列表持久化 (本地数据库)
- 🚧 EPG (电子节目指南) 支持
- 🚧 收藏频道功能

### 计划中 📋

**核心功能**:
- [ ] 多语言支持 (中文、英文、西班牙语等)
- [ ] 设置页面 (播放器配置、主题、语言等)
- [ ] 字幕支持
- [ ] 音轨切换
- [ ] 播放历史记录

**播放器增强**:
- [ ] 画中画模式 (PiP)
- [ ] 播放速度控制
- [ ] 视频截图功能
- [ ] 录制功能
- [ ] 播放列表循环播放

**用户体验**:
- [ ] 搜索功能
- [ ] 频道分组和排序
- [ ] 自定义主题
- [ ] 键盘快捷键
- [ ] 触摸手势 (Android)

**性能优化**:
- [ ] 预加载和缓存优化
- [ ] 更智能的缓冲策略
- [ ] 低功耗模式
- [ ] 网络自适应播放

**平台特定**:
- [ ] iOS 支持
- [ ] Linux ARM 支持 (树莓派)
- [ ] Android TV 优化
- [ ] Web 版本 (Kotlin/JS)

## 🧪 测试

项目包含完整的测试套件,确保代码质量和功能正确性。

### 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行 Desktop 测试
./gradlew :composeApp:desktopTest

# 运行 Android 单元测试
./gradlew :composeApp:testDebugUnitTest

# 运行特定测试类
./gradlew :composeApp:desktopTest --tests "FFmpegPlayerEngineTest"

# 运行特定测试方法
./gradlew :composeApp:desktopTest --tests "FFmpegPlayerEngineTest.testPlaybackControl"
```

### 测试覆盖

**FFmpeg 播放器测试**:
- ✅ 单元测试: 核心组件和工具类
- ✅ 集成测试: HTTP/HLS/本地文件播放
- ✅ 性能测试: CPU、内存、延迟测试
- ✅ 硬件加速测试: 平台特定加速验证

**共享代码测试**:
- ✅ UI 组件测试
- ✅ 数据模型测试
- ✅ 数据库测试
- ✅ 网络客户端测试

### 测试报告

测试报告位于:
```
composeApp/build/reports/tests/
├── desktopTest/index.html
└── testDebugUnitTest/index.html
```

在浏览器中打开 `index.html` 查看详细的测试报告。

### 手动测试

**Desktop 播放器测试清单**:

1. **基础播放**
   - [ ] HTTP 流播放
   - [ ] HLS 流播放
   - [ ] 本地文件播放
   - [ ] RTSP 流播放

2. **播放控制**
   - [ ] 播放/暂停
   - [ ] 跳转 (Seek)
   - [ ] 音量调整
   - [ ] 停止播放

3. **硬件加速**
   - [ ] 自动检测硬件加速
   - [ ] 硬件加速启用
   - [ ] 软件解码回退

4. **直播流优化**
   - [ ] 低延迟播放
   - [ ] 自动重连
   - [ ] 延迟累积处理

5. **音视频同步**
   - [ ] 同步误差 < 40ms
   - [ ] 自动同步调整
   - [ ] 丢帧处理

6. **资源管理**
   - [ ] 正确释放资源
   - [ ] 无内存泄漏
   - [ ] 线程正确停止

详细测试指南: [CONFIGURATION_VERIFICATION.md](.kiro/specs/ffmpeg-player-migration/CONFIGURATION_VERIFICATION.md)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request!

### 贡献指南

1. **Fork 项目**
2. **创建特性分支**: `git checkout -b feature/amazing-feature`
3. **提交更改**: `git commit -m 'Add amazing feature'`
4. **推送到分支**: `git push origin feature/amazing-feature`
5. **提交 Pull Request**

### 代码规范

- 遵循 Kotlin 官方代码风格
- 为新功能添加测试
- 更新相关文档
- 确保所有测试通过

### 报告问题

提交 Issue 时请包含:
- 操作系统和版本
- 应用版本
- 详细的问题描述
- 复现步骤
- 错误日志或截图

## 许可证

MIT License

## 联系方式

如有问题或建议,请通过 GitHub Issues 联系。
