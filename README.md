# IPTV Player - 跨平台 IPTV 播放器

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

## 快速开始

### 前置要求

- **JDK 11+**: 确保已安装 Java 开发工具包
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

## 已知问题

### Desktop 版本

- **VLC 依赖**: Desktop 版本需要系统安装 VLC Media Player
  - macOS: `brew install --cask vlc`
  - Windows: 从 [VLC 官网](https://www.videolan.org/vlc/) 下载安装

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
