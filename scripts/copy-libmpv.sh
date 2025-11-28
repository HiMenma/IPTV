#!/bin/bash

set -e

RESOURCES_DIR="composeApp/src/desktopMain/resources/native"

# 检测操作系统和架构
OS=$(uname -s)
ARCH=$(uname -m)

echo "================================================"
echo "libmpv 库文件复制脚本"
echo "================================================"
echo ""
echo "检测到系统: $OS $ARCH"
echo ""

if [[ "$OS" == "Darwin" ]]; then
    # macOS
    if [[ "$ARCH" == "arm64" ]]; then
        # Apple Silicon
        TARGET_DIR="$RESOURCES_DIR/macos-aarch64"
        SOURCE_DIR="/opt/homebrew/lib"
        PLATFORM="macOS Apple Silicon (M1/M2/M3)"
    else
        # Intel
        TARGET_DIR="$RESOURCES_DIR/macos-x86_64"
        SOURCE_DIR="/usr/local/lib"
        PLATFORM="macOS Intel"
    fi
    
    LIB_FILE="libmpv.2.dylib"
    SOURCE_PATH="$SOURCE_DIR/$LIB_FILE"
    
    echo "平台: $PLATFORM"
    echo "源文件: $SOURCE_PATH"
    echo "目标目录: $TARGET_DIR"
    echo ""
    
    # 检查源文件是否存在
    if [ ! -f "$SOURCE_PATH" ]; then
        echo "❌ 错误: 找不到 libmpv 库文件"
        echo ""
        echo "请先安装 mpv:"
        echo "  brew install mpv"
        echo ""
        exit 1
    fi
    
    # 创建目标目录
    echo "创建目标目录..."
    mkdir -p "$TARGET_DIR"
    
    # 复制库文件
    echo "复制 $LIB_FILE..."
    cp "$SOURCE_PATH" "$TARGET_DIR/"
    
    # 验证复制
    if [ -f "$TARGET_DIR/$LIB_FILE" ]; then
        echo "✅ $LIB_FILE 复制成功"
        
        # 显示文件信息
        echo ""
        echo "文件信息:"
        ls -lh "$TARGET_DIR/$LIB_FILE"
        echo ""
        echo "架构信息:"
        file "$TARGET_DIR/$LIB_FILE"
    else
        echo "❌ 复制失败"
        exit 1
    fi
    
elif [[ "$OS" == "Linux" ]]; then
    # Linux
    TARGET_DIR="$RESOURCES_DIR/linux-x86_64"
    LIB_FILE="libmpv.so.2"
    
    echo "平台: Linux x86_64"
    echo "目标目录: $TARGET_DIR"
    echo ""
    
    # 查找库文件
    SOURCE_PATH=""
    if [ -f "/usr/lib/x86_64-linux-gnu/$LIB_FILE" ]; then
        SOURCE_PATH="/usr/lib/x86_64-linux-gnu/$LIB_FILE"
    elif [ -f "/usr/lib64/$LIB_FILE" ]; then
        SOURCE_PATH="/usr/lib64/$LIB_FILE"
    elif [ -f "/usr/lib/$LIB_FILE" ]; then
        SOURCE_PATH="/usr/lib/$LIB_FILE"
    fi
    
    if [ -z "$SOURCE_PATH" ]; then
        echo "❌ 错误: 找不到 libmpv 库文件"
        echo ""
        echo "请先安装 libmpv:"
        echo "  Ubuntu/Debian: sudo apt-get install libmpv-dev libmpv2"
        echo "  Fedora: sudo dnf install mpv-libs"
        echo "  Arch: sudo pacman -S mpv"
        echo ""
        exit 1
    fi
    
    echo "源文件: $SOURCE_PATH"
    echo ""
    
    # 创建目标目录
    echo "创建目标目录..."
    mkdir -p "$TARGET_DIR"
    
    # 复制库文件
    echo "复制 $LIB_FILE..."
    cp "$SOURCE_PATH" "$TARGET_DIR/"
    
    # 验证复制
    if [ -f "$TARGET_DIR/$LIB_FILE" ]; then
        echo "✅ $LIB_FILE 复制成功"
        
        # 显示文件信息
        echo ""
        echo "文件信息:"
        ls -lh "$TARGET_DIR/$LIB_FILE"
        echo ""
        echo "架构信息:"
        file "$TARGET_DIR/$LIB_FILE"
    else
        echo "❌ 复制失败"
        exit 1
    fi
    
else
    echo "❌ 不支持的操作系统: $OS"
    echo ""
    echo "支持的系统:"
    echo "  - macOS (Intel 和 Apple Silicon)"
    echo "  - Linux (x86_64)"
    echo ""
    echo "Windows 用户请使用 PowerShell 脚本: scripts/copy-libmpv.ps1"
    exit 1
fi

echo ""
echo "================================================"
echo "✅ 库文件已成功复制到项目中"
echo "================================================"
echo ""
echo "下一步:"
echo "  1. 编译项目: ./gradlew :composeApp:desktopJar"
echo "  2. 运行应用: ./gradlew :composeApp:run"
echo "  3. 打包应用: ./gradlew :composeApp:packageDistributionForCurrentOS"
echo ""
