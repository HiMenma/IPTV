# Android APK 构建指南

本文档说明如何构建只包含 ARM 架构的 Android APK，适用于手机设备。

## 快速开始

### macOS/Linux 用户

```bash
./build_android.sh
```

### Windows 用户

```cmd
build_android.bat
```

## 手动构建

如果你想手动执行构建步骤：

```bash
# 1. 清理之前的构建
flutter clean

# 2. 获取依赖
flutter pub get

# 3. 构建 APK（只包含 ARM 架构）
flutter build apk --release --target-platform android-arm,android-arm64
```

## 架构说明

### 包含的架构
- **arm64-v8a**: 64位 ARM 架构（现代手机）
- **armeabi-v7a**: 32位 ARM 架构（旧手机）

### 不包含的架构
- **x86**: 模拟器使用（不适用于真实手机）
- **x86_64**: 64位模拟器使用（不适用于真实手机）

通过只包含 ARM 架构，APK 文件大小会显著减小（通常减少 30-40%）。

## 输出文件

构建完成后，APK 文件位于：
```
build/app/outputs/flutter-apk/app-release.apk
```

## 安装到手机

### 方法 1: 使用 ADB

```bash
adb install build/app/outputs/flutter-apk/app-release.apk
```

### 方法 2: 直接传输

1. 将 APK 文件传输到手机
2. 在手机上打开文件管理器
3. 点击 APK 文件进行安装
4. 如果提示"未知来源"，需要在设置中允许安装未知来源的应用

## 签名配置

当前使用的是 debug 签名。如果需要发布到应用商店或正式分发，需要配置正式签名：

### 1. 创建密钥库

```bash
keytool -genkey -v -keystore ~/release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias release
```

### 2. 配置签名

在 `android/app/build.gradle.kts` 中添加签名配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("/path/to/release.keystore")
            storePassword = "your-password"
            keyAlias = "release"
            keyPassword = "your-password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3. 使用环境变量（推荐）

创建 `android/key.properties` 文件：

```properties
storePassword=your-store-password
keyPassword=your-key-password
keyAlias=release
storeFile=/path/to/release.keystore
```

然后在 `build.gradle.kts` 中引用：

```kotlin
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}
```

## 构建其他变体

### 构建分离的 APK（每个架构一个文件）

```bash
flutter build apk --release --split-per-abi --target-platform android-arm,android-arm64
```

这会生成两个文件：
- `app-armeabi-v7a-release.apk` (32位)
- `app-arm64-v8a-release.apk` (64位)

### 构建 App Bundle（推荐用于 Google Play）

```bash
flutter build appbundle --release --target-platform android-arm,android-arm64
```

输出文件：`build/app/outputs/bundle/release/app-release.aab`

## 优化建议

### 1. 启用代码混淆

在 `android/app/build.gradle.kts` 中：

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 2. 减小 APK 大小

```bash
# 使用 --split-per-abi 为每个架构生成单独的 APK
flutter build apk --release --split-per-abi --target-platform android-arm,android-arm64

# 使用 --obfuscate 混淆 Dart 代码
flutter build apk --release --obfuscate --split-debug-info=./debug-info
```

## 故障排除

### 问题：构建失败

1. 确保 Flutter SDK 已正确安装：
   ```bash
   flutter doctor
   ```

2. 清理并重新构建：
   ```bash
   flutter clean
   flutter pub get
   flutter build apk --release
   ```

### 问题：APK 无法安装

1. 检查手机是否允许安装未知来源的应用
2. 确保手机架构是 ARM（几乎所有手机都是）
3. 检查 Android 版本是否满足最低要求（在 `android/app/build.gradle.kts` 中的 `minSdk`）

### 问题：APK 文件太大

1. 使用 `--split-per-abi` 为每个架构生成单独的 APK
2. 启用代码混淆和资源压缩
3. 移除未使用的资源和依赖

## 版本管理

在 `pubspec.yaml` 中更新版本号：

```yaml
version: 1.0.0+1
```

格式：`主版本.次版本.修订版本+构建号`

## 相关命令

```bash
# 查看 APK 内容
unzip -l build/app/outputs/flutter-apk/app-release.apk

# 分析 APK 大小
flutter build apk --analyze-size

# 查看构建详细信息
flutter build apk --release -v
```

## 参考资料

- [Flutter Android 构建文档](https://docs.flutter.dev/deployment/android)
- [Android 应用签名](https://developer.android.com/studio/publish/app-signing)
- [减小 APK 大小](https://docs.flutter.dev/perf/app-size)
