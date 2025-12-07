# IPTV Player 打包指南

本文档说明如何在 macOS ARM 环境下为各个平台打包 IPTV Player 应用的安装包。

## 环境要求

- macOS ARM (Apple Silicon)
- Flutter SDK (已安装)
- Android Studio (已安装)
- Xcode (用于 iOS/macOS 打包)

## 打包前准备

### 1. 检查 Flutter 环境

```bash
flutter doctor -v
```

确保所有必需的工具链都已正确安装。

### 2. 清理项目

```bash
flutter clean
flutter pub get
```

### 3. 生成应用图标和启动画面

```bash
# 生成应用图标
dart run flutter_launcher_icons

# 生成启动画面
dart run flutter_native_splash:create
```

## 各平台打包说明

### 📱 Android 打包

#### APK 打包（用于测试和直接分发）

```bash
# 构建 Release APK
flutter build apk --release

# 构建分架构的 APK（体积更小）
flutter build apk --split-per-abi --release
```

生成的文件位置：
- 通用 APK: `build/app/outputs/flutter-apk/app-release.apk`
- 分架构 APK: `build/app/outputs/flutter-apk/app-armeabi-v7a-release.apk`、`app-arm64-v8a-release.apk`、`app-x86_64-release.apk`

#### AAB 打包（用于 Google Play 商店）

```bash
# 构建 Android App Bundle
flutter build appbundle --release
```

生成的文件位置：`build/app/outputs/bundle/release/app-release.aab`

#### 签名说明

项目已配置签名密钥（`release.keystore`），打包时会自动签名。如需修改签名配置，编辑 `android/app/build.gradle.kts` 文件。

### 🍎 iOS 打包

**注意：** 需要 Apple Developer 账号和证书。

#### 1. 在 Xcode 中配置

```bash
# 打开 iOS 项目
open ios/Runner.xcworkspace
```

在 Xcode 中：
1. 选择 Runner 项目
2. 在 Signing & Capabilities 中配置 Team 和 Bundle Identifier
3. 确保选择正确的 Provisioning Profile

#### 2. 构建 iOS 应用

```bash
# 构建 iOS Release 版本
flutter build ios --release --no-codesign

# 或者直接构建 IPA（需要配置好签名）
flutter build ipa --release
```

生成的文件位置：`build/ios/ipa/iptv_player.ipa`

#### 3. 通过 Xcode 打包

1. 在 Xcode 中选择 Product > Archive
2. 等待归档完成
3. 在 Organizer 中选择归档，点击 Distribute App
4. 选择分发方式（App Store、Ad Hoc、Enterprise 等）

### 🖥️ macOS 打包

#### 1. 构建 macOS 应用

```bash
# 构建 macOS Release 版本
flutter build macos --release
```

生成的文件位置：`build/macos/Build/Products/Release/iptv_player.app`

#### 2. 创建 DMG 安装包（可选）

可以使用 `create-dmg` 工具创建 DMG 安装包：

```bash
# 安装 create-dmg
brew install create-dmg

# 创建 DMG
create-dmg \
  --volname "IPTV Player" \
  --window-pos 200 120 \
  --window-size 800 400 \
  --icon-size 100 \
  --icon "iptv_player.app" 200 190 \
  --hide-extension "iptv_player.app" \
  --app-drop-link 600 185 \
  "IPTV-Player-1.0.0.dmg" \
  "build/macos/Build/Products/Release/iptv_player.app"
```

#### 3. 代码签名（可选，用于分发）

```bash
# 签名应用
codesign --deep --force --verify --verbose --sign "Developer ID Application: Your Name" \
  build/macos/Build/Products/Release/iptv_player.app

# 公证应用（需要 Apple Developer 账号）
xcrun notarytool submit IPTV-Player-1.0.0.dmg \
  --apple-id "your-email@example.com" \
  --team-id "YOUR_TEAM_ID" \
  --password "app-specific-password"
```

### 🐧 Linux 打包

#### 1. 构建 Linux 应用

```bash
# 构建 Linux Release 版本
flutter build linux --release
```

生成的文件位置：`build/linux/x64/release/bundle/`

#### 2. 创建可分发的压缩包

```bash
# 进入构建目录
cd build/linux/x64/release/

# 创建 tar.gz 压缩包
tar -czf iptv-player-1.0.0-linux-x64.tar.gz bundle/

# 或创建 zip 压缩包
zip -r iptv-player-1.0.0-linux-x64.zip bundle/
```

#### 3. 创建 AppImage（推荐）

需要在 Linux 环境下使用 `appimagetool`：

```bash
# 创建 AppDir 结构
mkdir -p AppDir/usr/bin
cp -r build/linux/x64/release/bundle/* AppDir/usr/bin/

# 创建 desktop 文件
cat > AppDir/iptv_player.desktop << EOF
[Desktop Entry]
Name=IPTV Player
Exec=iptv_player
Icon=iptv_player
Type=Application
Categories=AudioVideo;Player;
EOF

# 复制图标
cp icon/tv.png AppDir/iptv_player.png

# 使用 appimagetool 创建 AppImage
appimagetool AppDir iptv-player-1.0.0-x86_64.AppImage
```

### 🌐 Web 打包

#### 1. 构建 Web 应用

```bash
# 构建 Web Release 版本
flutter build web --release
```

生成的文件位置：`build/web/`

#### 2. 部署到服务器

将 `build/web/` 目录下的所有文件上传到 Web 服务器即可。

#### 3. 本地测试

```bash
# 使用 Python 启动本地服务器
cd build/web
python3 -m http.server 8000

# 或使用 Node.js
npx serve
```

然后在浏览器中访问 `http://localhost:8000`

### 🪟 Windows 打包

**注意：** 在 macOS 上无法直接构建 Windows 应用，需要在 Windows 环境下进行。

如果你有 Windows 环境，可以执行：

```bash
# 构建 Windows Release 版本
flutter build windows --release
```

生成的文件位置：`build/windows/x64/runner/Release/`

可以使用 Inno Setup 或 NSIS 创建安装程序。

## 版本管理

### 修改版本号

编辑 `pubspec.yaml` 文件中的 `version` 字段：

```yaml
version: 1.0.0+1
```

格式：`主版本.次版本.修订号+构建号`

### 使用命令行指定版本

```bash
flutter build apk --build-name=1.0.1 --build-number=2
```

## 快速打包脚本

你可以创建一个脚本来自动化打包流程：

```bash
#!/bin/bash
# build_all.sh

echo "🧹 清理项目..."
flutter clean
flutter pub get

echo "🎨 生成图标和启动画面..."
dart run flutter_launcher_icons
dart run flutter_native_splash:create

echo "📱 构建 Android APK..."
flutter build apk --split-per-abi --release

echo "🖥️  构建 macOS 应用..."
flutter build macos --release

echo "🌐 构建 Web 应用..."
flutter build web --release

echo "✅ 打包完成！"
echo "Android APK: build/app/outputs/flutter-apk/"
echo "macOS App: build/macos/Build/Products/Release/"
echo "Web: build/web/"
```

使用方法：

```bash
chmod +x build_all.sh
./build_all.sh
```

## 常见问题

### 1. Android R8 混淆错误（Missing classes）

**错误信息：** `Missing class com.aliyun.** / com.google.android.play.core.**`

**解决方案：** 项目已配置 ProGuard 规则（`android/app/proguard-rules.pro`），如果仍有问题：

```bash
# 清理项目
flutter clean
cd android && ./gradlew clean && cd ..

# 重新获取依赖
flutter pub get

# 重新构建
flutter build apk --release
```

### 2. Java 版本警告

**警告信息：** `源值 8 已过时`

这是正常的警告，不影响构建。项目已配置使用 Java 17。

### 3. Android 构建失败

- 检查 Java 版本：`java -version`（需要 Java 17）
- 清理 Gradle 缓存：`cd android && ./gradlew clean`
- 删除 `.gradle` 文件夹：`rm -rf ~/.gradle/caches/`

### 4. iOS/macOS 构建失败

- 更新 CocoaPods：`sudo gem install cocoapods`
- 清理 Pods：`cd ios && rm -rf Pods Podfile.lock && pod install`

### 5. 构建体积过大

- 使用 `--split-per-abi` 为 Android 构建分架构 APK（推荐）
- 启用代码混淆：`flutter build apk --obfuscate --split-debug-info=build/debug-info`

### 6. 阿里云播放器插件问题

如果遇到播放器相关的构建问题，确保：
- 网络连接正常（需要从 GitHub 拉取插件）
- 已正确配置各平台的原生依赖
- ProGuard 规则已正确配置

## 测试打包结果

### Android

```bash
# 安装到连接的设备
flutter install

# 或使用 adb
adb install build/app/outputs/flutter-apk/app-release.apk
```

### macOS

直接双击 `build/macos/Build/Products/Release/iptv_player.app` 运行。

### Web

使用本地服务器测试（见上文 Web 打包部分）。

## 发布清单

在发布前确保：

- [ ] 更新版本号
- [ ] 测试所有核心功能
- [ ] 检查应用图标和启动画面
- [ ] 准备应用商店截图和描述
- [ ] 配置正确的签名证书
- [ ] 测试打包后的应用
- [ ] 准备更新日志

## 相关资源

- [Flutter 官方打包文档](https://docs.flutter.dev/deployment)
- [Android 发布指南](https://docs.flutter.dev/deployment/android)
- [iOS 发布指南](https://docs.flutter.dev/deployment/ios)
- [macOS 发布指南](https://docs.flutter.dev/deployment/macos)
- [Web 发布指南](https://docs.flutter.dev/deployment/web)

---

**提示：** 在你的 macOS ARM 环境下，可以直接打包 Android、macOS 和 Web 版本。iOS 需要 Apple Developer 账号，Linux 建议在 Linux 环境下打包，Windows 需要在 Windows 环境下打包。
