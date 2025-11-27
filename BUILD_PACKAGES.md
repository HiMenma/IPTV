# 打包指南

## 支持的平台

本应用支持以下平台的打包：

- **macOS**: DMG安装包
- **Windows**: MSI和EXE安装包
- **Linux**: DEB安装包

## 打包命令

### 打包当前操作系统
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

### 打包所有平台（需要在对应平台上执行）
```bash
./gradlew :composeApp:packageDistribution
```

### 打包特定格式

#### macOS DMG
```bash
./gradlew :composeApp:packageDmg
```

#### Windows MSI
```bash
./gradlew :composeApp:packageMsi
```

#### Windows EXE
```bash
./gradlew :composeApp:packageExe
```

#### Linux DEB
```bash
./gradlew :composeApp:packageDeb
```

## 输出位置

打包后的文件位于：
```
composeApp/build/compose/binaries/main/
├── dmg/IPTV-Player-1.0.0.dmg      (macOS)
├── msi/IPTV-Player-1.0.0.msi      (Windows)
├── exe/IPTV-Player-1.0.0.exe      (Windows)
└── deb/iptv-player_1.0.0_amd64.deb (Linux)
```

## 应用图标

应用已配置自定义图标：

- **macOS**: 使用 `app_icon.icns` (ICNS格式)
- **Windows**: 使用 `app_icon_windows.png` (PNG格式)
- **Linux**: 使用 `app_icon.png` (PNG格式)

图标文件位于：`composeApp/src/desktopMain/resources/`

## 应用信息

- **应用名称**: IPTV Player
- **包名**: IPTV-Player
- **版本**: 1.0.0
- **Bundle ID** (macOS): com.menmapro.iptv
- **描述**: 支持M3U和Xtream的跨平台IPTV播放器

## 注意事项

### Windows打包
- 在macOS或Linux上打包Windows安装包需要安装WiX工具集
- 建议在Windows系统上进行Windows打包以获得最佳结果

### macOS打包
- 需要在macOS系统上进行
- 如需签名和公证，需要Apple Developer账户

### Linux打包
- 可以在任何平台上打包
- 建议在Linux系统上测试打包结果

## 依赖要求

### macOS
- VLC Media Player (用户需要自行安装)

### Windows
- VLC Media Player (用户需要自行安装)

### Linux
- VLC Media Player (用户需要自行安装)
- 可通过包管理器安装：`sudo apt install vlc`

## 版本更新

修改版本号请编辑 `composeApp/build.gradle.kts`：

```kotlin
nativeDistributions {
    packageVersion = "1.0.0"  // 修改这里
    // ...
}
```

## Android APK

构建Android APK：
```bash
./gradlew :composeApp:assembleDebug
```

输出位置：
```
composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

发布版本：
```bash
./gradlew :composeApp:assembleRelease
```

## 清理构建

```bash
./gradlew clean
```

## 故障排除

### 打包失败
1. 确保已安装所有必需的工具
2. 检查磁盘空间是否充足
3. 尝试清理后重新构建：`./gradlew clean`

### 图标未显示
1. 确认图标文件存在于 `composeApp/src/desktopMain/resources/`
2. 检查 `build.gradle.kts` 中的图标路径配置
3. 重新构建应用

### VLC依赖问题
应用运行时需要VLC Media Player，请确保：
1. 已安装VLC
2. VLC在系统PATH中
3. VLC版本 >= 3.0

## 发布清单

发布前检查：
- [ ] 更新版本号
- [ ] 测试所有平台
- [ ] 验证图标显示正确
- [ ] 测试视频播放功能
- [ ] 准备发布说明
- [ ] 创建GitHub Release
- [ ] 上传所有平台的安装包
