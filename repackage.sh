#!/bin/bash

# 桌面平台重新打包脚本
# 使用方法: ./repackage.sh [dmg|msi|exe|deb]

set -e

echo "================================"
echo "IPTV Player 桌面平台重新打包"
echo "================================"
echo ""

# 清理旧构建
echo "🧹 清理旧构建..."
./gradlew clean
echo "✅ 清理完成"
echo ""

# 根据参数选择打包类型
PACKAGE_TYPE=${1:-dmg}

case $PACKAGE_TYPE in
  dmg)
    echo "📦 开始打包 macOS DMG..."
    ./gradlew packageDmg
    echo "✅ DMG 打包完成"
    echo "📍 文件位置: composeApp/build/compose/binaries/main/dmg/"
    ;;
  msi)
    echo "📦 开始打包 Windows MSI..."
    ./gradlew packageMsi
    echo "✅ MSI 打包完成"
    echo "📍 文件位置: composeApp/build/compose/binaries/main/msi/"
    ;;
  exe)
    echo "📦 开始打包 Windows EXE..."
    ./gradlew packageExe
    echo "✅ EXE 打包完成"
    echo "📍 文件位置: composeApp/build/compose/binaries/main/exe/"
    ;;
  deb)
    echo "📦 开始打包 Linux DEB..."
    ./gradlew packageDeb
    echo "✅ DEB 打包完成"
    echo "📍 文件位置: composeApp/build/compose/binaries/main/deb/"
    ;;
  *)
    echo "❌ 未知的打包类型: $PACKAGE_TYPE"
    echo "使用方法: ./repackage.sh [dmg|msi|exe|deb]"
    exit 1
    ;;
esac

echo ""
echo "================================"
echo "✅ 打包完成！"
echo "================================"
echo ""
echo "修复内容："
echo "  ✅ 添加了 java.sql 模块支持"
echo "  ✅ 修正了图标文件路径"
echo "  ✅ 启用了完整运行时模块"
echo ""
echo "请安装并测试新打包的应用："
echo "  1. 检查图标是否正确显示"
echo "  2. 确认没有 DriverManager 错误"
echo "  3. 测试添加播放列表功能"
echo ""
