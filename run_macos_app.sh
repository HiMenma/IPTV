#!/bin/bash

# 运行 macOS 应用并显示日志

APP_PATH="build/macos/Build/Products/Release/IPTV Player.app"

if [ ! -d "$APP_PATH" ]; then
    echo "❌ 应用不存在，正在构建..."
    flutter build macos --release
fi

echo "🚀 启动 IPTV Player..."
echo "📝 日志输出："
echo "----------------------------------------"

# 运行应用并捕获输出
"$APP_PATH/Contents/MacOS/IPTV Player" 2>&1

echo "----------------------------------------"
echo "✅ 应用已退出"
