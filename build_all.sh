#!/bin/bash

# ==========================================
# IPTV 多平台参数化打包脚本 (Fixed Version)
# ==========================================

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

ROOT_DIR=$(pwd)
DIST_DIR="$ROOT_DIR/dist"
mkdir -p "$DIST_DIR"

# 初始化构建标志
BUILD_APK=false
BUILD_MAC=false
BUILD_WIN=false
BUILD_LINUX=false
BUILD_WEB=false

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --all)   BUILD_APK=true; BUILD_MAC=true; BUILD_WIN=true; BUILD_LINUX=true; BUILD_WEB=true; shift ;;
        --apk)   BUILD_APK=true; shift ;;
        --mac)   BUILD_MAC=true; shift ;;
        --win)   BUILD_WIN=true; shift ;;
        --linux) BUILD_LINUX=true; shift ;;
        --web)   BUILD_WEB=true; shift ;;
        *)       shift ;;
    esac
done

APP_VERSION=$(grep 'version: ' pubspec.yaml | sed 's/version: //')
echo -e "${BLUE}>>> 构建版本: $APP_VERSION${NC}"

# 1. Android
if [ "$BUILD_APK" = true ]; then
    echo -e "${BLUE}>>> 构建 Android...${NC}"
    if flutter build apk --release --target-platform android-arm,android-arm64; then
        cp build/app/outputs/flutter-apk/app-release.apk "$DIST_DIR/iptv_player_android_v$APP_VERSION.apk"
        echo -e "${GREEN}[成功] Android 产物已归档${NC}"
    fi
fi

# 2. macOS
if [ "$BUILD_MAC" = true ] && [[ "$OSTYPE" == "darwin"* ]]; then
    echo -e "${BLUE}>>> 构建 macOS...${NC}"
    if flutter build macos --release; then
        APP_PATH=$(find build/macos/Build/Products/Release -name "*.app" -maxdepth 1 | head -n 1)
        if [ ! -z "$APP_PATH" ]; then
            APP_NAME=$(basename "$APP_PATH")
            (cd "$(dirname "$APP_PATH")" && zip -r "$DIST_DIR/iptv_player_macos_v$APP_VERSION.app.zip" "$APP_NAME")
            echo -e "${GREEN}[成功] macOS 产物已归档${NC}"
        fi
    fi
fi

# 3. Web
if [ "$BUILD_WEB" = true ]; then
    echo -e "${BLUE}>>> 构建 Web...${NC}"
    if flutter build web --release; then
        (cd build/web && zip -r "$DIST_DIR/iptv_player_web_v$APP_VERSION.zip" .)
        echo -e "${GREEN}[成功] Web 产物已归档${NC}"
    fi
fi

echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}构建任务结束。产物位置: $DIST_DIR/${NC}"
ls -lh "$DIST_DIR"
echo -e "${GREEN}==========================================${NC}"
