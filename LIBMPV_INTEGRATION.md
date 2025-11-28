# libmpv 集成指南

本文档说明如何将 libmpv 库集成到项目中，使应用可以独立运行，无需用户单独安装 libmpv。

## 概述

libmpv 现在已经集成到项目中，通过以下方式实现：

1. **资源打包**: 将平台特定的 libmpv 库文件放在 `resources/native/` 目录
2. **自动加载**: `LibmpvLoader` 自动检测平台并从资源中提取和加载库
3. **降级策略**: 如果资源中没有库文件，会尝试从系统路径加载

## 目录结构

```
composeApp/src/desktopMain/resources/native/
├── macos-x86_64/          # macOS Intel 芯片
│   └── libmpv.2.dylib     (3.6 MB)
├── macos-aarch64/         # macOS Apple Silicon (M1/M2/M3)
│   └── libmpv.2.dylib     (3.6 MB)
├── linux-x86_64/          # Linux 64位
│   └── libmpv.so.2        (~3-5 MB)
└── windows-x86_64/        # Windows 64位
    └── libmpv-2.dll       (~5-10 MB)
```

## 快速开始

### 1. 复制库文件到项目

#### macOS / Linux
```bash
# 运行自动复制脚本
./scripts/copy-libmpv.sh
```

#### Windows
```powershell
# 运行 PowerShell 脚本
.\scripts\copy-libmpv.ps1
```

### 2. 编译和运行

```bash
# 编译项目
./gradlew :composeApp:desktopJar

# 运行应用
./gradlew :composeApp:run

# 打包应用（包含所有依赖）
./gradlew :composeApp:packageDistributionForCurrentOS
```

## 工作原理

### 加载策略

`LibmpvLoader` 使用三层加载策略：

1. **优先级 1: 打包的库文件**
   - 从 `resources/native/{platform}/` 提取库文件到临时目录
   - 加载提取的库文件
   - ✅ 优点：应用完全独立，无需系统安装
   - ⚠️ 注意：首次运行时需要提取（几秒钟）

2. **优先级 2: 系统路径**
   - 在标准系统路径中查找 libmpv
   - macOS: `/opt/homebrew/lib`, `/usr/local/lib`
   - Linux: `/usr/lib`, `/usr/lib64`, `/usr/lib/x86_64-linux-gnu`
   - Windows: `C:\mpv`, `%ProgramFiles%\mpv`

3. **优先级 3: 系统库搜索**
   - 使用 JNA 的默认库搜索机制
   - 依赖系统的动态链接器

### 平台检测

```kotlin
private fun getPlatformResourceDir(): String {
    return when {
        Platform.isMac() -> {
            val arch = System.getProperty("os.arch")
            if (arch == "aarch64" || arch == "arm64") {
                "macos-aarch64"  // Apple Silicon
            } else {
                "macos-x86_64"   // Intel
            }
        }
        Platform.isLinux() -> "linux-x86_64"
        Platform.isWindows() -> "windows-x86_64"
        else -> "unknown"
    }
}
```

### 库文件提取

```kotlin
private fun extractBundledLibrary(): String? {
    val platformDir = getPlatformResourceDir()
    val libraryFileName = getLibraryFileName()
    val resourcePath = "/native/$platformDir/$libraryFileName"
    
    // 从资源加载
    val resourceStream = LibmpvLoader::class.java.getResourceAsStream(resourcePath)
    
    // 提取到临时目录
    val tempDir = Files.createTempDirectory("libmpv-native").toFile()
    val extractedFile = File(tempDir, libraryFileName)
    
    // 复制文件
    resourceStream.use { input ->
        FileOutputStream(extractedFile).use { output ->
            input.copyTo(output)
        }
    }
    
    return extractedFile.absolutePath
}
```

## 获取库文件

### macOS

```bash
# 安装 mpv（如果还没安装）
brew install mpv

# 运行复制脚本
./scripts/copy-libmpv.sh
```

脚本会自动：
- 检测 CPU 架构（Intel 或 Apple Silicon）
- 从正确的 Homebrew 路径复制库文件
- 验证文件完整性

### Linux

```bash
# Ubuntu/Debian
sudo apt-get install libmpv-dev libmpv2

# Fedora
sudo dnf install mpv-libs

# Arch
sudo pacman -S mpv

# 运行复制脚本
./scripts/copy-libmpv.sh
```

### Windows

1. 下载 MPV Windows 构建：
   - 访问 https://github.com/shinchiro/mpv-winbuild-cmake/releases
   - 下载最新的 `mpv-x86_64-*.7z`
   - 解压到任意目录

2. 运行复制脚本：
   ```powershell
   .\scripts\copy-libmpv.ps1
   ```

3. 输入解压目录路径

## 验证集成

### 检查库文件是否存在

```bash
# macOS Apple Silicon
ls -lh composeApp/src/desktopMain/resources/native/macos-aarch64/

# macOS Intel
ls -lh composeApp/src/desktopMain/resources/native/macos-x86_64/

# Linux
ls -lh composeApp/src/desktopMain/resources/native/linux-x86_64/

# Windows
dir composeApp\src\desktopMain\resources\native\windows-x86_64\
```

### 测试加载

运行应用并查看日志：

```bash
./gradlew :composeApp:run
```

成功的日志输出：
```
Attempting to load bundled libmpv from resources...
Looking for bundled library at: /native/macos-aarch64/libmpv.2.dylib
Extracting bundled library to: /tmp/libmpv-native.../libmpv.2.dylib
✓ Library extracted successfully
✓ Successfully loaded bundled libmpv from: /tmp/libmpv-native.../libmpv.2.dylib
```

## 打包分发

### 打包当前平台

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

输出位置：
- macOS: `composeApp/build/compose/binaries/main/dmg/`
- Linux: `composeApp/build/compose/binaries/main/deb/` 或 `rpm/`
- Windows: `composeApp/build/compose/binaries/main/msi/`

### 跨平台打包

要为所有平台打包，需要：

1. 收集所有平台的库文件
2. 放置到对应的 `resources/native/` 目录
3. 在各平台上分别运行打包命令

## 文件大小

### 单个库文件
- macOS: ~3.6 MB
- Linux: ~3-5 MB
- Windows: ~5-10 MB

### 打包后应用大小
- 基础应用: ~50 MB
- 包含 libmpv: ~55-60 MB
- 包含所有平台库: ~65-75 MB

## 许可证合规

libmpv 使用 **LGPL 2.1+** 许可证。集成时需要：

### 1. 添加许可证声明

在 `LICENSE` 或 `NOTICE` 文件中添加：

```
This application includes libmpv (https://mpv.io/), which is licensed 
under the GNU Lesser General Public License v2.1 or later.

libmpv source code: https://github.com/mpv-player/mpv
libmpv license: https://github.com/mpv-player/mpv/blob/master/LICENSE.LGPL

LGPL allows dynamic linking with proprietary software. This application
dynamically links to libmpv and does not modify the libmpv source code.
```

### 2. 提供源代码访问

在应用的"关于"页面或文档中提供：
- libmpv 源代码链接
- 使用的 libmpv 版本号
- LGPL 许可证文本链接

### 3. 动态链接声明

确保说明应用是通过动态链接使用 libmpv，而不是静态链接。

## 故障排除

### 问题 1: 库文件未找到

**症状**: 日志显示 "No bundled library found"

**解决方案**:
1. 检查文件是否存在于正确的目录
2. 验证文件名是否正确
3. 重新运行复制脚本

### 问题 2: 提取失败

**症状**: "Failed to extract bundled library"

**解决方案**:
1. 检查临时目录权限
2. 确保有足够的磁盘空间
3. 检查文件是否损坏

### 问题 3: 加载失败

**症状**: "Failed to load bundled library"

**解决方案**:
1. 验证库文件架构是否匹配
2. 检查是否缺少依赖库
3. 查看详细错误日志

### 问题 4: 架构不匹配

**症状**: macOS 上 "wrong architecture"

**解决方案**:
1. 确认使用正确的库文件（Intel vs Apple Silicon）
2. 重新复制对应架构的库文件

## 开发工作流

### 日常开发

```bash
# 1. 确保库文件已复制
./scripts/copy-libmpv.sh

# 2. 开发和测试
./gradlew :composeApp:run

# 3. 编译
./gradlew :composeApp:desktopJar
```

### 发布流程

```bash
# 1. 更新版本号
# 编辑 build.gradle.kts

# 2. 确保所有平台库文件都已复制
./scripts/copy-libmpv.sh

# 3. 运行测试
./gradlew :composeApp:desktopTest

# 4. 打包
./gradlew :composeApp:packageDistributionForCurrentOS

# 5. 测试打包后的应用
# 运行生成的安装包

# 6. 创建发布
# 上传到 GitHub Releases 或其他平台
```

## 优势

### ✅ 用户体验
- 无需手动安装 libmpv
- 一键安装即可使用
- 跨平台一致性

### ✅ 开发体验
- 简化部署流程
- 减少用户支持工作
- 版本控制更容易

### ✅ 分发
- 独立的应用包
- 无外部依赖
- 更容易分发

## 注意事项

### ⚠️ 文件大小
- 应用包会增加 3-10 MB
- 考虑是否需要所有平台的库

### ⚠️ 更新
- 需要手动更新库文件
- 定期检查 libmpv 新版本

### ⚠️ 许可证
- 遵守 LGPL 要求
- 提供源代码访问

## 相关文档

- [LIBMPV_BUNDLE_GUIDE.md](LIBMPV_BUNDLE_GUIDE.md) - 详细的打包指南
- [LIBMPV_SETUP_GUIDE.md](LIBMPV_SETUP_GUIDE.md) - 系统安装指南
- [LIBMPV_API_FIX.md](LIBMPV_API_FIX.md) - API 修复说明
- [LIBMPV_CURRENT_STATUS.md](LIBMPV_CURRENT_STATUS.md) - 当前状态

## 支持

如有问题，请：
1. 查看故障排除部分
2. 检查日志输出
3. 提交 GitHub Issue

---

**最后更新**: 2024-11-28
