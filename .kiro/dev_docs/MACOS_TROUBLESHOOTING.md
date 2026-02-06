# macOS 播放问题完整故障排除指南

## 已完成的配置

✅ **Info.plist** - 添加了 NSAppTransportSecurity 允许 HTTP
✅ **Entitlements** - 禁用沙盒，添加网络权限
✅ **重新构建** - 使用最新配置构建应用
✅ **重新签名** - 确保 entitlements 正确应用

## 快速诊断

### 1. 运行测试脚本
```bash
./test_macos_network.sh
```

### 2. 直接运行应用查看日志
```bash
./run_macos_app.sh
```

或者：
```bash
"build/macos/Build/Products/Release/IPTV Player.app/Contents/MacOS/IPTV Player"
```

### 3. 查看系统日志
在另一个终端窗口运行：
```bash
log stream --predicate 'processImagePath contains "IPTV Player"' --level debug
```

然后启动应用，查看实时日志输出。

## 常见问题和解决方案

### 问题 1: "无法连接到服务器"

**可能原因：**
- 防火墙阻止
- URL 格式错误
- 网络权限未生效

**解决步骤：**

1. 检查防火墙：
```bash
# 查看防火墙状态
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate

# 如果启用，添加应用到允许列表
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add "build/macos/Build/Products/Release/IPTV Player.app"
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblockapp "build/macos/Build/Products/Release/IPTV Player.app"
```

2. 测试简单的 HTTP URL：
   - 使用这个测试流：`http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4`

3. 完全重新构建：
```bash
flutter clean
rm -rf build/
flutter pub get
cd macos && pod install && cd ..
flutter build macos --release
codesign --force --deep --sign - --entitlements macos/Runner/Release.entitlements "build/macos/Build/Products/Release/IPTV Player.app"
```

### 问题 2: "播放器初始化失败"

**可能原因：**
- video_player 插件问题
- 媒体格式不支持

**解决步骤：**

1. 检查 video_player 版本：
```bash
flutter pub outdated
```

2. 尝试使用 Debug 模式（有更多日志）：
```bash
flutter run -d macos --verbose
```

3. 查看是否有 AVFoundation 错误：
```bash
log stream --predicate 'subsystem contains "com.apple.AVFoundation"' --level debug
```

### 问题 3: "权限被拒绝"

**可能原因：**
- Entitlements 未正确应用
- 需要用户授权

**解决步骤：**

1. 验证 entitlements：
```bash
codesign -d --entitlements /dev/stdout "build/macos/Build/Products/Release/IPTV Player.app" 2>/dev/null | plutil -p -
```

应该看到：
```
{
  "com.apple.security.app-sandbox" => false
  "com.apple.security.network.client" => true
  "com.apple.security.network.server" => true
  ...
}
```

2. 如果 entitlements 不正确，重新签名：
```bash
codesign --force --deep --sign - --entitlements macos/Runner/Release.entitlements "build/macos/Build/Products/Release/IPTV Player.app"
```

### 问题 4: "HTTPS 可以但 HTTP 不行"

**可能原因：**
- NSAppTransportSecurity 配置未生效

**解决步骤：**

1. 验证 Info.plist：
```bash
plutil -p "build/macos/Build/Products/Release/IPTV Player.app/Contents/Info.plist" | grep -A 5 NSAppTransportSecurity
```

应该看到：
```
"NSAppTransportSecurity" => {
  "NSAllowsArbitraryLoads" => true
  "NSAllowsLocalNetworking" => true
}
```

2. 如果配置不正确，检查源文件：
```bash
cat macos/Runner/Info.plist | grep -A 10 NSAppTransportSecurity
```

3. 重新构建：
```bash
flutter clean
flutter build macos --release
```

### 问题 5: "本地网络 IPTV 无法访问"

**可能原因：**
- 本地网络权限未启用
- 路由器防火墙

**解决步骤：**

1. 确认 NSAllowsLocalNetworking 已启用（见问题 4）

2. 测试本地网络连接：
```bash
# 假设你的 IPTV 服务器在 192.168.1.100:8080
curl -I http://192.168.1.100:8080
```

3. 检查 macOS 防火墙是否阻止本地连接

## 高级调试

### 使用 Xcode 调试

1. 打开项目：
```bash
open macos/Runner.xcworkspace
```

2. 在 Xcode 中：
   - 选择 Runner scheme
   - 点击 Product > Run
   - 查看控制台输出

### 使用 lldb 调试

```bash
lldb "build/macos/Build/Products/Release/IPTV Player.app/Contents/MacOS/IPTV Player"
(lldb) run
```

### 网络抓包

使用 Wireshark 或 tcpdump 查看网络请求：
```bash
sudo tcpdump -i any -n host <your-iptv-server-ip>
```

## 替代方案

### 方案 1: 使用 Debug 构建
Debug 构建有更宽松的限制：
```bash
flutter build macos --debug
```

### 方案 2: 使用 Profile 构建
```bash
flutter build macos --profile
```

### 方案 3: 从源码运行
```bash
flutter run -d macos
```
这样可以看到实时日志输出。

## 验证配置

运行以下命令验证所有配置：

```bash
echo "=== 检查 Info.plist ==="
plutil -p "build/macos/Build/Products/Release/IPTV Player.app/Contents/Info.plist" | grep -A 5 NSAppTransportSecurity

echo ""
echo "=== 检查 Entitlements ==="
codesign -d --entitlements /dev/stdout "build/macos/Build/Products/Release/IPTV Player.app" 2>/dev/null | plutil -p -

echo ""
echo "=== 检查签名 ==="
codesign --verify --verbose "build/macos/Build/Products/Release/IPTV Player.app"

echo ""
echo "=== 检查防火墙 ==="
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate
```

## 需要提供的调试信息

如果问题仍然存在，请提供：

1. **错误信息**：应用显示的具体错误
2. **系统日志**：运行 `log stream` 时的输出
3. **URL 格式**：你尝试播放的 IPTV URL（可以脱敏）
4. **系统信息**：
   ```bash
   sw_vers
   ```
5. **网络测试**：
   ```bash
   curl -v <your-iptv-url>
   ```

## 最后的手段

如果所有方法都失败，尝试完全移除安全限制（仅用于测试）：

1. 编辑 `macos/Runner/Release.entitlements`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- 完全禁用沙盒 -->
</dict>
</plist>
```

2. 重新构建：
```bash
flutter clean
flutter build macos --release
```

3. 不签名直接运行：
```bash
"build/macos/Build/Products/Release/IPTV Player.app/Contents/MacOS/IPTV Player"
```

## 相关文件

- `macos/Runner/Info.plist` - 应用配置
- `macos/Runner/Release.entitlements` - 权限配置
- `test_macos_network.sh` - 网络测试脚本
- `run_macos_app.sh` - 运行应用脚本

## 参考资料

- [Apple App Sandbox](https://developer.apple.com/documentation/security/app_sandbox)
- [App Transport Security](https://developer.apple.com/documentation/security/preventing_insecure_network_connections)
- [Flutter macOS Deployment](https://docs.flutter.dev/platform-integration/macos/building)
- [video_player Plugin](https://pub.dev/packages/video_player)
