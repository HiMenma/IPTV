#!/bin/bash

# macOS 网络连接测试脚本

echo "🔍 检查 macOS 应用网络配置..."
echo ""

APP_PATH="build/macos/Build/Products/Release/IPTV Player.app"

if [ ! -d "$APP_PATH" ]; then
    echo "❌ 应用不存在: $APP_PATH"
    echo "请先运行: flutter build macos --release"
    exit 1
fi

echo "✅ 应用存在"
echo ""

echo "📋 检查 Info.plist 中的 NSAppTransportSecurity..."
plutil -p "$APP_PATH/Contents/Info.plist" | grep -A 5 NSAppTransportSecurity
echo ""

echo "🔐 检查代码签名..."
codesign --display --verbose=2 "$APP_PATH" 2>&1 | grep -E "(Identifier|Signature|Format)"
echo ""

echo "📝 检查 entitlements..."
codesign -d --entitlements /dev/stdout "$APP_PATH" 2>/dev/null | plutil -p - 2>/dev/null || echo "无法读取 entitlements"
echo ""

echo "🔥 检查防火墙状态..."
FIREWALL_STATE=$(sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate 2>/dev/null)
echo "$FIREWALL_STATE"
echo ""

echo "🌐 测试网络连接..."
echo "测试 HTTP 连接..."
curl -I -s -o /dev/null -w "%{http_code}" http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
echo " - HTTP 测试"

echo "测试 HTTPS 连接..."
curl -I -s -o /dev/null -w "%{http_code}" https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
echo " - HTTPS 测试"
echo ""

echo "💡 建议："
echo "1. 如果防火墙已启用，请在系统设置中允许 IPTV Player"
echo "2. 尝试从命令行运行应用查看日志："
echo "   \"$APP_PATH/Contents/MacOS/IPTV Player\""
echo "3. 查看控制台日志："
echo "   log stream --predicate 'processImagePath contains \"IPTV Player\"' --level debug"
echo ""

echo "🔧 如果仍有问题，尝试重新签名："
echo "   codesign --force --deep --sign - --entitlements macos/Runner/Release.entitlements \"$APP_PATH\""
echo ""
