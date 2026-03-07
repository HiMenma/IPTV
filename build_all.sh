#!/bin/bash

# IPTV Player Build Script (Native Only)
# Usage: ./build_all.sh [--apk] [--mac] [--all]

VERSION="1.0.0+1"
DIST_DIR="./dist"
mkdir -p $DIST_DIR

echo ">>> 构建版本: $VERSION"

build_android() {
    echo ">>> 构建 Android..."
    flutter build apk --release
    cp build/app/outputs/flutter-apk/app-release.apk "$DIST_DIR/iptv_player_android_v$VERSION.apk"
    echo "[成功] Android 产物已归档"
}

build_macos() {
    echo ">>> 构建 macOS..."
    flutter build macos --release
    cd build/macos/Build/Products/Release/
    zip -r "../../../../../$DIST_DIR/iptv_player_macos_v$VERSION.app.zip" "IPTV Player.app"
    cd - > /dev/null
    echo "[成功] macOS 产物已归档"
}

if [[ "$1" == "--apk" ]]; then
    build_android
elif [[ "$1" == "--mac" ]]; then
    build_macos
elif [[ "$1" == "--all" ]]; then
    build_android
    build_macos
else
    echo "请指定构建目标: --apk, --mac, 或 --all"
fi

echo "=========================================="
echo "构建任务结束。产物位置: $(pwd)/dist/"
ls -lh $DIST_DIR
echo "=========================================="
