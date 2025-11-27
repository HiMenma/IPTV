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
  - Desktop: VLCJ (需要安装 VLC Media Player)
- ✅ **Material Design UI**: 现代化的用户界面

## 技术栈

- **Kotlin Multiplatform**: 共享业务逻辑
- **Compose Multiplatform**: 跨平台 UI 框架
- **Ktor**: 网络请求
- **Koin**: 依赖注入
- **Voyager**: 导航框架
- **Coil 3**: 图片加载
- **ExoPlayer**: Android 视频播放
- **VLCJ**: Desktop 视频播放

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

- **JDK 17+**: 确保已安装 Java 开发工具包
- **Android SDK**: 用于 Android 构建 (可选,如果只运行 Desktop 版本则不需要)
- **VLC Media Player**: Desktop 版本需要安装 VLC (macOS/Windows)

### 运行 Desktop 版本

```bash
# 克隆项目
cd /Users/menmapro/Documents/GitHub/IPTV

# 运行 Desktop 应用
./gradlew :composeApp:run
```

### 运行 Android 版本

```bash
# 连接 Android 设备或启动模拟器

# 安装 Debug 版本
./gradlew :composeApp:installDebug

# 或者直接运行
./gradlew :composeApp:assembleDebug
```

### 构建发布版本

```bash
# Desktop 打包 (DMG for macOS, MSI for Windows)
./gradlew :composeApp:packageDistributionForCurrentOS

# Android APK
./gradlew :composeApp:assembleRelease
```

详细构建说明请参阅：
- **[本地构建指南](BUILD_PACKAGES.md)** - 本地打包所有平台
- **[GitHub Actions 自动构建](GITHUB_ACTIONS_GUIDE.md)** - 自动化构建和发布
- **[快速发布指南](RELEASE_GUIDE.md)** - 一键发布新版本

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

## 视频渲染改进 (Desktop)

Desktop 版本已经过全面优化,解决了视频渲染黑屏问题,并提供了更好的播放体验:

### 主要改进

- ✅ **平台特定视频输出**: 自动检测操作系统并使用最佳视频输出模块
- ✅ **硬件加速**: 自动启用硬件加速以提高性能和降低 CPU 使用率
- ✅ **智能备用机制**: 如果主要视频输出失败,自动尝试备用输出方法
- ✅ **视频表面验证**: 确保视频渲染表面正确初始化和配置
- ✅ **直播流优化**: 针对直播流的低延迟缓存配置
- ✅ **格式自适应**: 根据视频格式自动调整解码选项
- ✅ **详细诊断**: 提供详细的播放诊断信息,便于问题排查

### 支持的视频格式和协议

**视频编解码器**:
- H.264 (AVC) - 推荐,硬件加速支持
- H.265 (HEVC) - 推荐,硬件加速支持
- VP8/VP9 - WebM 格式
- MPEG-2/MPEG-4
- 其他 VLC 支持的编解码器

**流媒体协议**:
- HTTP/HTTPS - 标准 HTTP 流
- HLS (HTTP Live Streaming) - .m3u8 播放列表
- RTSP (Real Time Streaming Protocol)
- RTMP (Real Time Messaging Protocol)
- UDP/RTP - 组播流

**容器格式**:
- MP4, MKV, AVI, MOV
- TS (MPEG Transport Stream)
- WebM
- FLV

### VLC 配置建议

Desktop 版本需要系统安装 VLC Media Player:

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
从 [VLC 官网](https://www.videolan.org/vlc/) 下载安装

**推荐 VLC 版本**: 3.0.18 或更高版本

### 文档资源

完整的视频渲染文档:

- **[快速入门指南](.kiro/specs/desktop-video-rendering-fix/QUICK_START_GUIDE.md)** - 新用户快速开始
- **[故障排除指南](.kiro/specs/desktop-video-rendering-fix/VIDEO_TROUBLESHOOTING.md)** - 常见问题解决方案
- **[VLC 配置指南](.kiro/specs/desktop-video-rendering-fix/VLC_CONFIGURATION_GUIDE.md)** - VLC 安装和配置详解
- **[技术文档](.kiro/specs/desktop-video-rendering-fix/TECHNICAL_DOCUMENTATION.md)** - 开发者技术参考

### 故障排除

详细的故障排除指南请参阅: [VIDEO_TROUBLESHOOTING.md](.kiro/specs/desktop-video-rendering-fix/VIDEO_TROUBLESHOOTING.md)

常见问题快速解决:

1. **黑屏但有声音**
   - 确保已安装 VLC Media Player
   - 检查系统是否支持硬件加速
   - 查看应用日志中的诊断信息

2. **视频卡顿或延迟**
   - 检查网络连接
   - 尝试降低视频质量
   - 查看 CPU 使用率

3. **无法播放某些格式**
   - 确认 VLC 版本是否支持该格式
   - 查看错误日志获取详细信息

## 已知问题

### Desktop 版本

- **VLC 依赖**: Desktop 版本需要系统安装 VLC Media Player (见上方安装说明)
- **首次播放延迟**: 首次播放可能需要几秒钟初始化视频输出

### 弃用警告

- `ArrowBack` 图标有弃用警告,但不影响功能
- `kotlinOptions` 已迁移到 `compilerOptions`

## 开发计划

- [ ] 添加播放列表持久化 (本地数据库)
- [ ] 支持 EPG (电子节目指南)
- [ ] 添加收藏频道功能
- [ ] 支持多语言
- [ ] 优化视频播放控制
- [ ] 添加设置页面

## 贡献

欢迎提交 Issue 和 Pull Request!

## 许可证

MIT License

## 联系方式

如有问题或建议,请通过 GitHub Issues 联系。
