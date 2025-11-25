# IPTV Player 打包指南

本文档介绍如何为 Android、macOS 和 Windows 平台打包 IPTV Player 应用程序。

## 目录

- [前置要求](#前置要求)
- [Android 打包](#android-打包)
- [macOS 打包](#macos-打包)
- [Windows 打包](#windows-打包)
- [常见问题](#常见问题)

---

## 前置要求

### 通用要求

- **JDK**: Java Development Kit 17 或更高版本
- **Gradle**: 项目已包含 Gradle Wrapper，无需单独安装
- **Git**: 用于版本控制

验证 Java 版本：
```bash
java -version
```

### Android 特定要求

- **Android SDK**: Android SDK 34 (通过 Android Studio 安装)
- **Android Studio**: 推荐使用最新稳定版
- **签名密钥**: 用于发布版本的密钥库文件

### macOS 特定要求

- **操作系统**: macOS 10.14 或更高版本
- **Xcode Command Line Tools**: 用于代码签名
  ```bash
  xcode-select --install
  ```

### Windows 特定要求

- **WiX Toolset**: 用于创建 MSI 安装包
  - 下载地址: https://wixtoolset.org/releases/
  - 安装后需要将 WiX 添加到系统 PATH

---

## Android 打包

### 1. 调试版本 (Debug APK)

调试版本用于开发和测试，无需签名配置。

```bash
# 构建调试 APK
./gradlew :composeApp:assembleDebug

# 输出位置
# composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### 2. 发布版本 (Release APK)

#### 2.1 创建签名密钥

如果还没有签名密钥，需要先创建：

```bash
keytool -genkey -v -keystore iptv-release-key.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias iptv-key
```

按提示输入密钥库密码和密钥信息。

#### 2.2 配置签名信息

在项目根目录创建 `keystore.properties` 文件（不要提交到 Git）：

```properties
storePassword=你的密钥库密码
keyPassword=你的密钥密码
keyAlias=iptv-key
storeFile=../iptv-release-key.jks
```

#### 2.3 更新 build.gradle.kts

在 `composeApp/build.gradle.kts` 的 `android` 块中添加签名配置：

```kotlin
android {
    // ... 现有配置 ...
    
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = java.util.Properties()
                keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

#### 2.4 构建发布 APK

```bash
# 构建发布 APK
./gradlew :composeApp:assembleRelease

# 输出位置
# composeApp/build/outputs/apk/release/composeApp-release.apk
```

### 3. Android App Bundle (AAB)

用于 Google Play 发布：

```bash
# 构建 AAB
./gradlew :composeApp:bundleRelease

# 输出位置
# composeApp/build/outputs/bundle/release/composeApp-release.aab
```

### 4. 安装和测试

```bash
# 通过 ADB 安装到设备
adb install composeApp/build/outputs/apk/release/composeApp-release.apk

# 或使用 Gradle 直接安装
./gradlew :composeApp:installRelease
```

---

## macOS 打包

### 1. 构建 DMG 安装包

```bash
# 构建 macOS DMG 包
./gradlew :composeApp:packageDmg

# 输出位置
# composeApp/build/compose/binaries/main/dmg/IPTV-Player-1.0.dmg
```

### 2. 配置应用信息

在 `composeApp/build.gradle.kts` 中自定义 macOS 配置：

```kotlin
compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "IPTV-Player"
            packageVersion = "1.0.0"
            description = "IPTV 播放器应用"
            copyright = "© 2024 MenmaPro. All rights reserved."
            vendor = "MenmaPro"
            
            macOS {
                bundleID = "com.menmapro.iptv"
                iconFile.set(project.file("src/desktopMain/resources/icon.icns"))
                
                // 最低系统版本
                minimumSystemVersion = "10.14"
                
                // 应用分类
                appCategory = "public.app-category.entertainment"
                
                // 代码签名（可选，用于分发）
                // signing {
                //     sign.set(true)
                //     identity.set("Developer ID Application: Your Name")
                // }
            }
        }
    }
}
```

### 3. 代码签名（可选）

如果需要在 macOS 上分发应用，需要进行代码签名：

```bash
# 查看可用的签名身份
security find-identity -v -p codesigning

# 手动签名应用
codesign --force --deep --sign "Developer ID Application: Your Name" \
  composeApp/build/compose/binaries/main/app/IPTV-Player.app

# 验证签名
codesign --verify --verbose composeApp/build/compose/binaries/main/app/IPTV-Player.app
```

### 4. 公证（Notarization）

对于 macOS 10.15+ 的分发，需要进行公证：

```bash
# 创建 DMG 后进行公证
xcrun notarytool submit composeApp/build/compose/binaries/main/dmg/IPTV-Player-1.0.dmg \
  --apple-id "your-apple-id@example.com" \
  --team-id "YOUR_TEAM_ID" \
  --password "app-specific-password" \
  --wait

# 装订公证票据
xcrun stapler staple composeApp/build/compose/binaries/main/dmg/IPTV-Player-1.0.dmg
```

---

## Windows 打包

### 1. 安装 WiX Toolset

下载并安装 WiX Toolset 3.11 或更高版本：
- 下载地址: https://wixtoolset.org/releases/
- 安装后确保 WiX 的 bin 目录在系统 PATH 中

验证安装：
```bash
candle -?
```

### 2. 构建 MSI 安装包

```bash
# 构建 Windows MSI 包
./gradlew :composeApp:packageMsi

# 输出位置
# composeApp/build/compose/binaries/main/msi/IPTV-Player-1.0.msi
```

### 3. 配置 Windows 特定选项

在 `composeApp/build.gradle.kts` 中添加 Windows 配置：

```kotlin
compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "IPTV-Player"
            packageVersion = "1.0.0"
            description = "IPTV 播放器应用"
            copyright = "© 2024 MenmaPro. All rights reserved."
            vendor = "MenmaPro"
            
            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                
                // 菜单组
                menuGroup = "IPTV Player"
                
                // 升级 UUID（保持不变以支持升级）
                upgradeUuid = "YOUR-UNIQUE-UUID-HERE"
                
                // 快捷方式
                shortcut = true
                dirChooser = true
                
                // 每用户安装
                perUserInstall = true
            }
        }
    }
}
```

### 4. 构建 EXE 包（可选）

如果需要便携式 EXE 而不是安装程序：

```bash
# 构建可执行文件
./gradlew :composeApp:createDistributable

# 输出位置
# composeApp/build/compose/binaries/main/app/
```

### 5. 代码签名（可选）

使用 SignTool 对 MSI 进行签名：

```bash
# 使用证书签名
signtool sign /f "your-certificate.pfx" /p "password" /t http://timestamp.digicert.com \
  composeApp/build/compose/binaries/main/msi/IPTV-Player-1.0.msi
```

---

## 一次性构建所有平台

### 构建所有桌面平台

```bash
# 构建所有配置的平台
./gradlew :composeApp:packageDistributionForCurrentOS

# 或构建所有平台（需要在对应系统上运行）
./gradlew :composeApp:package
```

### 完整构建脚本

创建 `build-all.sh` 脚本：

```bash
#!/bin/bash

echo "开始构建 IPTV Player..."

# 清理之前的构建
./gradlew clean

# 构建 Android
echo "构建 Android APK..."
./gradlew :composeApp:assembleRelease

# 构建 Android AAB
echo "构建 Android AAB..."
./gradlew :composeApp:bundleRelease

# 构建桌面平台（根据当前操作系统）
echo "构建桌面应用..."
./gradlew :composeApp:packageDistributionForCurrentOS

echo "构建完成！"
echo "输出文件位置："
echo "  Android APK: composeApp/build/outputs/apk/release/"
echo "  Android AAB: composeApp/build/outputs/bundle/release/"
echo "  桌面应用: composeApp/build/compose/binaries/main/"
```

赋予执行权限：
```bash
chmod +x build-all.sh
./build-all.sh
```

---

## 常见问题

### 1. VLC 依赖问题（桌面版）

桌面版使用 VLCJ 播放视频，用户需要安装 VLC：

**macOS:**
```bash
brew install --cask vlc
```

**Windows:**
- 下载并安装 VLC: https://www.videolan.org/vlc/

**在安装包中包含说明:**
在应用首次启动时检测 VLC 并提示用户安装。

### 2. 构建失败：内存不足

增加 Gradle 内存：

编辑 `gradle.properties`：
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

### 3. Android 签名错误

确保 `keystore.properties` 文件路径正确，且密钥库文件存在。

### 4. macOS 无法打开应用（安全限制）

用户首次打开时可能遇到安全提示：
```bash
# 用户可以通过以下命令允许应用运行
xattr -cr /Applications/IPTV-Player.app
```

或在"系统偏好设置 > 安全性与隐私"中允许。

### 5. Windows Defender 误报

未签名的应用可能被 Windows Defender 标记。建议：
- 对应用进行代码签名
- 在发布说明中告知用户这是误报

### 6. 构建速度慢

启用 Gradle 构建缓存和并行构建：

编辑 `gradle.properties`：
```properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

---

## 版本管理

### 更新版本号

在 `composeApp/build.gradle.kts` 中更新：

```kotlin
android {
    defaultConfig {
        versionCode = 2  // 每次发布递增
        versionName = "1.1.0"  // 语义化版本
    }
}

compose.desktop {
    application {
        nativeDistributions {
            packageVersion = "1.1.0"
        }
    }
}
```

### 版本命名规范

遵循语义化版本 (Semantic Versioning)：
- **主版本号**: 不兼容的 API 变更
- **次版本号**: 向后兼容的功能新增
- **修订号**: 向后兼容的问题修正

例如: `1.2.3`

---

## 发布检查清单

### Android
- [ ] 更新版本号 (versionCode 和 versionName)
- [ ] 测试发布版本 APK
- [ ] 检查 ProGuard 规则
- [ ] 准备应用商店截图和描述
- [ ] 生成签名的 APK/AAB

### macOS
- [ ] 更新版本号
- [ ] 测试 DMG 安装
- [ ] 验证应用图标
- [ ] 代码签名（如需分发）
- [ ] 公证（如需分发）
- [ ] 测试在不同 macOS 版本上运行

### Windows
- [ ] 更新版本号
- [ ] 测试 MSI 安装和卸载
- [ ] 验证应用图标
- [ ] 代码签名（推荐）
- [ ] 测试在不同 Windows 版本上运行
- [ ] 检查防病毒软件兼容性

---

## 技术支持

如遇到构建问题，请检查：
1. Java 和 Gradle 版本是否符合要求
2. 所有依赖是否正确下载
3. 构建日志中的错误信息
4. 项目 GitHub Issues 中是否有类似问题

---

**最后更新**: 2024-11-26
