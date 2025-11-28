# libmpv 库文件打包指南

本指南说明如何将 libmpv 库文件集成到项目中，使应用可以独立运行，无需用户安装 libmpv。

## 目录结构

```
composeApp/src/desktopMain/resources/native/
├── macos-x86_64/          # macOS Intel 芯片
│   └── libmpv.2.dylib
├── macos-aarch64/         # macOS Apple Silicon (M1/M2/M3)
│   └── libmpv.2.dylib
├── linux-x86_64/          # Linux 64位
│   └── libmpv.so.2
└── windows-x86_64/        # Windows 64位
    ├── libmpv-2.dll
    └── mpv-2.dll          # 备用名称
```

## 获取 libmpv 库文件

### macOS

#### Apple Silicon (M1/M2/M3)
```bash
# 安装 mpv（如果还没安装）
brew install mpv

# 复制库文件到项目
cp /opt/homebrew/lib/libmpv.2.dylib \
   composeApp/src/desktopMain/resources/native/macos-aarch64/

# 复制依赖库（可选，如果需要）
cp /opt/homebrew/lib/libavcodec*.dylib \
   composeApp/src/desktopMain/resources/native/macos-aarch64/
cp /opt/homebrew/lib/libavformat*.dylib \
   composeApp/src/desktopMain/resources/native/macos-aarch64/
cp /opt/homebrew/lib/libavutil*.dylib \
   composeApp/src/desktopMain/resources/native/macos-aarch64/
cp /opt/homebrew/lib/libswscale*.dylib \
   composeApp/src/desktopMain/resources/native/macos-aarch64/
```

#### Intel Mac
```bash
# 安装 mpv（如果还没安装）
brew install mpv

# 复制库文件到项目
cp /usr/local/lib/libmpv.2.dylib \
   composeApp/src/desktopMain/resources/native/macos-x86_64/

# 复制依赖库（可选）
cp /usr/local/lib/libavcodec*.dylib \
   composeApp/src/desktopMain/resources/native/macos-x86_64/
cp /usr/local/lib/libavformat*.dylib \
   composeApp/src/desktopMain/resources/native/macos-x86_64/
cp /usr/local/lib/libavutil*.dylib \
   composeApp/src/desktopMain/resources/native/macos-x86_64/
cp /usr/local/lib/libswscale*.dylib \
   composeApp/src/desktopMain/resources/native/macos-x86_64/
```

### Linux

#### Ubuntu/Debian
```bash
# 安装 libmpv
sudo apt-get install libmpv-dev libmpv2

# 复制库文件到项目
cp /usr/lib/x86_64-linux-gnu/libmpv.so.2 \
   composeApp/src/desktopMain/resources/native/linux-x86_64/

# 复制依赖库（可选）
cp /usr/lib/x86_64-linux-gnu/libavcodec.so.* \
   composeApp/src/desktopMain/resources/native/linux-x86_64/
cp /usr/lib/x86_64-linux-gnu/libavformat.so.* \
   composeApp/src/desktopMain/resources/native/linux-x86_64/
cp /usr/lib/x86_64-linux-gnu/libavutil.so.* \
   composeApp/src/desktopMain/resources/native/linux-x86_64/
cp /usr/lib/x86_64-linux-gnu/libswscale.so.* \
   composeApp/src/desktopMain/resources/native/linux-x86_64/
```

#### Fedora/RHEL
```bash
# 安装 mpv
sudo dnf install mpv mpv-libs

# 复制库文件到项目
cp /usr/lib64/libmpv.so.2 \
   composeApp/src/desktopMain/resources/native/linux-x86_64/
```

### Windows

#### 下载预编译版本
1. 访问 [shinchiro builds](https://github.com/shinchiro/mpv-winbuild-cmake/releases)
2. 下载最新的 `mpv-x86_64-*.7z` 文件
3. 解压文件
4. 复制以下文件到项目：

```powershell
# 复制主库文件
Copy-Item "mpv-x86_64-*/libmpv-2.dll" `
   "composeApp/src/desktopMain/resources/native/windows-x86_64/"

# 或者如果文件名是 mpv-2.dll
Copy-Item "mpv-x86_64-*/mpv-2.dll" `
   "composeApp/src/desktopMain/resources/native/windows-x86_64/"

# 复制依赖库（可选）
Copy-Item "mpv-x86_64-*/*.dll" `
   "composeApp/src/desktopMain/resources/native/windows-x86_64/"
```

## 自动化脚本

### macOS/Linux 一键复制脚本

创建 `scripts/copy-libmpv.sh`:

```bash
#!/bin/bash

set -e

RESOURCES_DIR="composeApp/src/desktopMain/resources/native"

# 检测操作系统和架构
OS=$(uname -s)
ARCH=$(uname -m)

echo "检测到系统: $OS $ARCH"

if [[ "$OS" == "Darwin" ]]; then
    # macOS
    if [[ "$ARCH" == "arm64" ]]; then
        # Apple Silicon
        TARGET_DIR="$RESOURCES_DIR/macos-aarch64"
        SOURCE_DIR="/opt/homebrew/lib"
    else
        # Intel
        TARGET_DIR="$RESOURCES_DIR/macos-x86_64"
        SOURCE_DIR="/usr/local/lib"
    fi
    
    echo "复制 libmpv 到 $TARGET_DIR"
    mkdir -p "$TARGET_DIR"
    cp "$SOURCE_DIR/libmpv.2.dylib" "$TARGET_DIR/"
    
    echo "✓ libmpv 复制成功"
    
elif [[ "$OS" == "Linux" ]]; then
    # Linux
    TARGET_DIR="$RESOURCES_DIR/linux-x86_64"
    
    echo "复制 libmpv 到 $TARGET_DIR"
    mkdir -p "$TARGET_DIR"
    
    if [ -f "/usr/lib/x86_64-linux-gnu/libmpv.so.2" ]; then
        cp "/usr/lib/x86_64-linux-gnu/libmpv.so.2" "$TARGET_DIR/"
    elif [ -f "/usr/lib64/libmpv.so.2" ]; then
        cp "/usr/lib64/libmpv.so.2" "$TARGET_DIR/"
    else
        echo "错误: 找不到 libmpv.so.2"
        exit 1
    fi
    
    echo "✓ libmpv 复制成功"
else
    echo "不支持的操作系统: $OS"
    exit 1
fi

echo ""
echo "库文件已复制到项目中"
echo "现在可以构建应用了: ./gradlew :composeApp:packageDistributionForCurrentOS"
```

使用方法：
```bash
chmod +x scripts/copy-libmpv.sh
./scripts/copy-libmpv.sh
```

### Windows PowerShell 脚本

创建 `scripts/copy-libmpv.ps1`:

```powershell
$ErrorActionPreference = "Stop"

$ResourcesDir = "composeApp/src/desktopMain/resources/native/windows-x86_64"

Write-Host "请确保已下载 MPV Windows 构建版本"
Write-Host "下载地址: https://github.com/shinchiro/mpv-winbuild-cmake/releases"
Write-Host ""

$MpvDir = Read-Host "请输入 MPV 解压目录路径"

if (-not (Test-Path $MpvDir)) {
    Write-Host "错误: 目录不存在: $MpvDir" -ForegroundColor Red
    exit 1
}

Write-Host "创建目标目录..."
New-Item -ItemType Directory -Force -Path $ResourcesDir | Out-Null

Write-Host "复制 libmpv-2.dll..."
if (Test-Path "$MpvDir/libmpv-2.dll") {
    Copy-Item "$MpvDir/libmpv-2.dll" $ResourcesDir
    Write-Host "✓ libmpv-2.dll 复制成功" -ForegroundColor Green
} elseif (Test-Path "$MpvDir/mpv-2.dll") {
    Copy-Item "$MpvDir/mpv-2.dll" "$ResourcesDir/libmpv-2.dll"
    Write-Host "✓ mpv-2.dll 复制成功（重命名为 libmpv-2.dll）" -ForegroundColor Green
} else {
    Write-Host "错误: 找不到 libmpv-2.dll 或 mpv-2.dll" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "库文件已复制到项目中" -ForegroundColor Green
Write-Host "现在可以构建应用了: .\gradlew.bat :composeApp:packageDistributionForCurrentOS"
```

使用方法：
```powershell
.\scripts\copy-libmpv.ps1
```

## 验证库文件

### 检查文件是否存在
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

### 检查库文件信息

#### macOS
```bash
# 查看架构
file composeApp/src/desktopMain/resources/native/macos-aarch64/libmpv.2.dylib

# 查看依赖
otool -L composeApp/src/desktopMain/resources/native/macos-aarch64/libmpv.2.dylib
```

#### Linux
```bash
# 查看架构
file composeApp/src/desktopMain/resources/native/linux-x86_64/libmpv.so.2

# 查看依赖
ldd composeApp/src/desktopMain/resources/native/linux-x86_64/libmpv.so.2
```

#### Windows
```powershell
# 查看文件信息
Get-Item composeApp\src\desktopMain\resources\native\windows-x86_64\libmpv-2.dll | Format-List
```

## 构建配置

库文件会自动包含在构建输出中。构建时，Gradle 会：

1. 将 `resources/native/` 目录复制到输出目录
2. 应用会根据运行平台自动加载对应的库文件
3. 打包时会包含所有平台的库文件（或仅当前平台）

## 文件大小参考

- **libmpv.2.dylib** (macOS): ~2-5 MB
- **libmpv.so.2** (Linux): ~2-5 MB  
- **libmpv-2.dll** (Windows): ~5-10 MB

如果包含所有依赖库，总大小可能达到 50-100 MB。

## 许可证注意事项

libmpv 使用 LGPL 2.1+ 许可证。打包 libmpv 时需要：

1. 在应用的 LICENSE 或 NOTICE 文件中声明使用了 libmpv
2. 提供 libmpv 的源代码链接或包含源代码
3. 如果修改了 libmpv，需要提供修改后的源代码

示例声明：
```
This application uses libmpv (https://mpv.io/), which is licensed under 
the GNU Lesser General Public License v2.1 or later.

Source code: https://github.com/mpv-player/mpv
```

## 故障排除

### 库文件找不到
- 确认文件已复制到正确的目录
- 检查文件名是否正确（区分大小写）
- 验证文件权限（应该可读可执行）

### 架构不匹配
- 确保复制的是正确架构的库文件
- macOS: 使用 `file` 命令检查架构
- Linux: 使用 `file` 命令检查架构

### 依赖库缺失
- 某些系统可能需要额外的依赖库
- 使用 `otool -L` (macOS) 或 `ldd` (Linux) 查看依赖
- 将缺失的依赖库也复制到项目中

## 下一步

完成库文件复制后：

1. 运行 `./gradlew :composeApp:run` 测试应用
2. 运行 `./gradlew :composeApp:packageDistributionForCurrentOS` 打包应用
3. 测试打包后的应用是否能独立运行

## 参考资源

- [MPV 官网](https://mpv.io/)
- [MPV GitHub](https://github.com/mpv-player/mpv)
- [MPV Windows 构建](https://github.com/shinchiro/mpv-winbuild-cmake/releases)
- [LGPL 许可证](https://www.gnu.org/licenses/lgpl-2.1.html)
