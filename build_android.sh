#!/bin/bash

# Android APK 打包脚本
# 只构建 ARM 架构（arm64-v8a 和 armeabi-v7a）用于手机设备

set -e  # 遇到错误立即退出

echo "=========================================="
echo "开始构建 Android APK"
echo "=========================================="

# 清理之前的构建
echo "清理之前的构建..."
flutter clean

# 获取依赖
echo "获取依赖..."
flutter pub get

# 构建 APK（只包含 ARM 架构）
echo "构建 APK（ARM 架构）..."
flutter build apk --release --target-platform android-arm,android-arm64

# 显示构建结果
echo ""
echo "=========================================="
echo "构建完成！"
echo "=========================================="
echo ""
echo "APK 文件位置："
echo "  $(pwd)/build/app/outputs/flutter-apk/app-release.apk"
echo ""

# 显示 APK 文件大小
if [ -f "build/app/outputs/flutter-apk/app-release.apk" ]; then
    SIZE=$(du -h "build/app/outputs/flutter-apk/app-release.apk" | cut -f1)
    echo "APK 文件大小: $SIZE"
    echo ""
    echo "可以使用以下命令安装到手机："
    echo "  adb install build/app/outputs/flutter-apk/app-release.apk"
fi

echo ""
echo "=========================================="
