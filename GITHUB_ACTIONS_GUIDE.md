# GitHub Actions 自动构建指南

本项目使用 GitHub Actions 自动构建所有平台的安装包，包括 Windows、macOS、Linux 和 Android。

## 📋 目录

- [功能特性](#功能特性)
- [触发构建](#触发构建)
- [构建产物](#构建产物)
- [配置说明](#配置说明)
- [常见问题](#常见问题)

## ✨ 功能特性

自动构建工作流支持以下功能：

- **多平台构建**：同时构建 Windows、macOS、Linux 和 Android 版本
- **自动发布**：标签推送时自动创建 GitHub Release
- **手动触发**：支持通过 GitHub 界面手动触发构建
- **构建缓存**：使用 Gradle 缓存加速构建过程
- **产物保留**：构建产物保留 30 天供下载

## 🚀 触发构建

### 方式一：推送标签（推荐）

这是最常用的方式，适合正式发布版本：

```bash
# 1. 创建标签
git tag v1.0.0

# 2. 推送标签到 GitHub
git push origin v1.0.0
```

标签命名规范：
- 必须以 `v` 开头
- 建议使用语义化版本号，如 `v1.0.0`、`v1.2.3`
- 推送标签后会自动触发构建并创建 GitHub Release

### 方式二：手动触发

适合测试或临时构建：

1. 打开 GitHub 仓库页面
2. 点击 **Actions** 标签
3. 选择 **构建发布包** 工作流
4. 点击右侧 **Run workflow** 按钮
5. 输入版本号（例如：1.0.0）
6. 点击 **Run workflow** 确认

手动触发的构建不会创建 GitHub Release，但会上传构建产物供下载。

## 📦 构建产物

### 桌面应用

| 平台 | 格式 | 文件名示例 | 说明 |
|------|------|-----------|------|
| Windows | MSI | `iptv-player-1.0.0.msi` | Windows 安装程序 |
| macOS | DMG | `iptv-player-1.0.0.dmg` | macOS 磁盘映像 |
| Linux | DEB | `iptv-player_1.0.0_amd64.deb` | Debian/Ubuntu 安装包 |

### Android 应用

| 类型 | 文件名 | 说明 |
|------|--------|------|
| Debug | `composeApp-debug.apk` | 调试版本，包含调试信息 |
| Release | `composeApp-release.apk` | 发布版本，需要签名 |

### 下载构建产物

#### 从 GitHub Actions 下载

1. 进入 **Actions** 标签
2. 选择对应的工作流运行记录
3. 滚动到页面底部的 **Artifacts** 部分
4. 点击对应平台的产物名称下载

产物名称：
- `iptv-player-windows`
- `iptv-player-macos`
- `iptv-player-linux`
- `iptv-player-android-debug`
- `iptv-player-android-release`

#### 从 GitHub Release 下载

如果是通过标签触发的构建：

1. 进入仓库的 **Releases** 页面
2. 找到对应版本的 Release
3. 在 **Assets** 部分下载所需平台的安装包

## ⚙️ 配置说明

### 工作流配置文件

配置文件位置：`.github/workflows/build-release.yml`

### 关键配置项

#### 1. 触发条件

```yaml
on:
  push:
    tags:
      - 'v*'  # 匹配所有 v 开头的标签
  workflow_dispatch:  # 允许手动触发
```

#### 2. 构建矩阵

```yaml
strategy:
  matrix:
    os: [ubuntu-latest, macos-latest, windows-latest]
```

这会在三个操作系统上并行构建，大大缩短总构建时间。

#### 3. Java 版本

```yaml
java-version: '17'
```

项目使用 JDK 17，确保与本地开发环境一致。

#### 4. 产物保留时间

```yaml
retention-days: 30
```

构建产物默认保留 30 天，可根据需要调整。

### 修改版本号

版本号在 `composeApp/build.gradle.kts` 中配置：

```kotlin
compose.desktop {
    application {
        nativeDistributions {
            packageVersion = "1.0.0"  // 修改这里
        }
    }
}

android {
    defaultConfig {
        versionCode = 1      // Android 版本号
        versionName = "1.0"  // Android 版本名称
    }
}
```

### Android 签名配置（可选）

如果需要发布到 Google Play，需要配置签名：

1. 在 GitHub 仓库设置中添加 Secrets：
   - `KEYSTORE_FILE`：Base64 编码的 keystore 文件
   - `KEYSTORE_PASSWORD`：keystore 密码
   - `KEY_ALIAS`：密钥别名
   - `KEY_PASSWORD`：密钥密码

2. 修改工作流文件，添加签名步骤：

```yaml
- name: 签名 APK
  run: |
    echo "${{ secrets.KEYSTORE_FILE }}" | base64 -d > keystore.jks
    ./gradlew assembleRelease \
      -Pandroid.injected.signing.store.file=keystore.jks \
      -Pandroid.injected.signing.store.password=${{ secrets.KEYSTORE_PASSWORD }} \
      -Pandroid.injected.signing.key.alias=${{ secrets.KEY_ALIAS }} \
      -Pandroid.injected.signing.key.password=${{ secrets.KEY_PASSWORD }}
```

## 🔧 常见问题

### Q1: 构建失败怎么办？

**A:** 检查以下几点：

1. 查看 Actions 日志，找到具体错误信息
2. 确保本地能够成功构建：`./gradlew packageDistributionForCurrentOS`
3. 检查 Gradle 配置文件是否有语法错误
4. 确认所有依赖都能正常下载

### Q2: macOS 构建产物无法安装

**A:** macOS 应用需要签名和公证：

1. 需要 Apple Developer 账号
2. 配置代码签名证书
3. 添加公证步骤到工作流

参考 [Compose Desktop 签名文档](https://github.com/JetBrains/compose-multiplatform/tree/master/tutorials/Signing_and_notarization_on_macOS)

### Q3: Android APK 无法安装

**A:** 可能的原因：

1. **Debug APK**：需要在设备上启用"允许安装未知来源应用"
2. **Release APK**：未签名的 APK 无法安装，需要配置签名
3. **架构不匹配**：确保 APK 包含设备对应的架构（ARM/x86）

### Q4: 如何加速构建？

**A:** 已实现的优化：

- ✅ 使用 Gradle 缓存
- ✅ 并行构建多个平台
- ✅ 使用最新的 Actions runner

额外优化建议：

- 使用自托管 runner（如果有服务器）
- 减少不必要的依赖
- 使用增量编译

### Q5: 如何构建特定平台？

**A:** 修改工作流的 matrix 配置，只保留需要的平台：

```yaml
strategy:
  matrix:
    os: [ubuntu-latest]  # 只构建 Linux
```

或者本地构建：

```bash
# Windows
./gradlew packageMsi

# macOS
./gradlew packageDmg

# Linux
./gradlew packageDeb

# Android
./gradlew assembleRelease
```

### Q6: 构建时间太长

**A:** 典型构建时间：

- Android：5-10 分钟
- 桌面应用（每个平台）：10-15 分钟
- 总计（并行）：15-20 分钟

如果超过这个时间，检查：

1. 网络连接是否正常
2. 依赖下载是否缓慢
3. 是否有不必要的构建步骤

## 📚 相关文档

- [BUILD_PACKAGES.md](BUILD_PACKAGES.md) - 本地构建指南
- [BUILD_GUIDE.md](BUILD_GUIDE.md) - 详细构建说明
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Compose Multiplatform 文档](https://github.com/JetBrains/compose-multiplatform)

## 🎯 最佳实践

1. **版本管理**
   - 使用语义化版本号
   - 每次发布前更新 CHANGELOG
   - 标签与版本号保持一致

2. **测试流程**
   - 先手动触发测试构建
   - 确认所有平台都能正常构建
   - 再推送标签创建正式发布

3. **发布流程**
   ```bash
   # 1. 更新版本号
   # 编辑 composeApp/build.gradle.kts
   
   # 2. 提交更改
   git add .
   git commit -m "chore: bump version to 1.0.0"
   
   # 3. 创建标签
   git tag v1.0.0
   
   # 4. 推送
   git push origin main
   git push origin v1.0.0
   
   # 5. 等待构建完成
   # 6. 在 GitHub Release 中添加更新说明
   ```

4. **安全建议**
   - 不要在代码中硬编码密钥
   - 使用 GitHub Secrets 存储敏感信息
   - 定期更新依赖版本
   - 审查第三方 Actions 的安全性

## 💡 提示

- 构建过程完全自动化，无需手动干预
- 所有平台的构建是并行的，节省时间
- 构建产物会自动上传，方便下载和分发
- 标签推送会自动创建 Release，简化发布流程

---

如有问题或建议，欢迎提交 Issue！
