# macOS 网络连接调试指南

## 当前配置

已完成以下配置来支持 HTTP 连接：

### 1. Info.plist 配置
- ✅ 添加了 `NSAppTransportSecurity` 允许任意 HTTP 加载
- ✅ 启用了本地网络支持

### 2. Entitlements 配置
- ✅ 网络客户端权限 (`com.apple.security.network.client`)
- ✅ 网络服务器权限 (`com.apple.security.network.server`)
- ⚠️ **已禁用沙盒** (`com.apple.security.app-sandbox` = false)

## 可能的问题和解决方案

### 问题 1: 防火墙阻止

macOS 防火墙可能阻止应用的网络连接。

**检查方法：**
1. 打开"系统设置" > "网络" > "防火墙"
2. 查看 IPTV Player 是否被阻止
3. 如果被阻止，点击"允许传入连接"

**命令行检查：**
```bash
# 查看防火墙状态
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate

# 查看应用是否被阻止
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --listapps | grep -i iptv
```

### 问题 2: 需要重新签名

构建后的应用可能需要重新签名才能使配置生效。

**解决方案：**
```bash
# 删除旧的签名
codesign --remove-signature "build/macos/Build/Products/Release/IPTV Player.app"

# 重新签名（ad-hoc 签名）
codesign --force --deep --sign - "build/macos/Build/Products/Release/IPTV Player.app"

# 验证签名
codesign --verify --verbose "build/macos/Build/Products/Release/IPTV Player.app"
```

### 问题 3: 缓存的应用版本

macOS 可能缓存了旧版本的应用。

**解决方案：**
```bash
# 完全删除应用
rm -rf "build/macos/Build/Products/Release/IPTV Player.app"

# 清理 Flutter 缓存
flutter clean

# 重新构建
flutter build macos --release

# 重置 Launch Services 数据库
/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -kill -r -domain local -domain system -domain user
```

### 问题 4: video_player 插件限制

video_player 插件在 macOS 上可能有特定限制。

**检查方法：**
1. 查看控制台日志：打开"控制台.app"
2. 过滤 "IPTV Player" 或 "video_player"
3. 查看是否有错误信息

**命令行查看日志：**
```bash
# 实时查看应用日志
log stream --predicate 'processImagePath contains "IPTV Player"' --level debug
```

### 问题 5: URL 格式问题

某些 IPTV URL 可能需要特殊处理。

**测试 URL：**
尝试使用这些公开的测试流：
- `http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4`
- `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`

### 问题 6: 需要额外的 entitlements

某些网络功能可能需要额外权限。

**尝试添加到 Release.entitlements：**
```xml
<!-- 音频输入（如果需要） -->
<key>com.apple.security.device.audio-input</key>
<false/>

<!-- 摄像头（如果需要） -->
<key>com.apple.security.device.camera</key>
<false/>

<!-- 临时异常允许 -->
<key>com.apple.security.temporary-exception.mach-lookup.global-name</key>
<array>
    <string>com.apple.audio.SystemSoundServer-OSX</string>
</array>
```

## 调试步骤

### 步骤 1: 检查应用权限
```bash
# 查看应用的 entitlements
codesign -d --entitlements - "build/macos/Build/Products/Release/IPTV Player.app"
```

### 步骤 2: 检查 Info.plist
```bash
# 查看 Info.plist 内容
plutil -p "build/macos/Build/Products/Release/IPTV Player.app/Contents/Info.plist" | grep -A 5 NSAppTransportSecurity
```

### 步骤 3: 运行应用并查看日志
```bash
# 从命令行运行应用（可以看到输出）
"build/macos/Build/Products/Release/IPTV Player.app/Contents/MacOS/IPTV Player"
```

### 步骤 4: 使用 Debug 模式
```bash
# 构建 debug 版本（有更多日志）
flutter run -d macos --verbose
```

## 替代方案

### 方案 1: 使用 Debug 构建
Debug 构建的沙盒限制较少：
```bash
flutter build macos --debug
```

### 方案 2: 完全移除沙盒
如果上述方法都不行，可以尝试完全移除沙盒相关配置：

编辑 `macos/Runner/Release.entitlements`，只保留：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
</dict>
</plist>
```

### 方案 3: 使用 Profile 构建
```bash
flutter build macos --profile
```

## 测试清单

- [ ] 检查防火墙设置
- [ ] 重新签名应用
- [ ] 清理缓存并重新构建
- [ ] 查看控制台日志
- [ ] 测试简单的 HTTP URL
- [ ] 尝试 Debug 模式运行
- [ ] 检查 entitlements 是否正确应用
- [ ] 验证 Info.plist 配置

## 获取更多信息

如果问题仍然存在，请提供：
1. 控制台日志中的错误信息
2. 使用的 IPTV URL 格式
3. 具体的错误提示（如果有）
4. macOS 版本

## 相关命令

```bash
# 完整的重新构建流程
flutter clean
rm -rf build/
flutter pub get
cd macos && pod install && cd ..
flutter build macos --release
codesign --force --deep --sign - "build/macos/Build/Products/Release/IPTV Player.app"

# 运行并查看日志
"build/macos/Build/Products/Release/IPTV Player.app/Contents/MacOS/IPTV Player" 2>&1 | tee app.log
```
