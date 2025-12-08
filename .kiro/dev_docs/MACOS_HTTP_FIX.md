# macOS HTTP 播放支持修复

## 问题描述

macOS 应用无法播放 HTTP 链接的 IPTV 流，因为 macOS 默认的 App Transport Security (ATS) 策略阻止了非 HTTPS 连接。

## 解决方案

### 1. 更新 Info.plist

在 `macos/Runner/Info.plist` 中添加了 App Transport Security 配置：

```xml
<!-- Allow HTTP connections for IPTV streaming -->
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key>
    <true/>
    <key>NSAllowsLocalNetworking</key>
    <true/>
</dict>
```

这个配置：
- `NSAllowsArbitraryLoads`: 允许应用加载任意 HTTP 内容
- `NSAllowsLocalNetworking`: 允许本地网络连接（用于局域网 IPTV 服务器）

### 2. 更新 Entitlements

**重要：已禁用沙盒以支持网络流媒体**

在 `macos/Runner/Release.entitlements` 中：

```xml
<!-- Disable sandbox for IPTV streaming -->
<key>com.apple.security.app-sandbox</key>
<false/>
<key>com.apple.security.network.client</key>
<true/>
<key>com.apple.security.network.server</key>
<true/>
```

### 3. 重新构建和签名

```bash
# 清理并重新构建
flutter clean
flutter build macos --release

# 重新签名以确保 entitlements 生效
codesign --force --deep --sign - --entitlements macos/Runner/Release.entitlements "build/macos/Build/Products/Release/IPTV Player.app"
```

### 4. 运行测试脚本

```bash
# 运行网络配置测试
./test_macos_network.sh
```

## 技术说明

### App Transport Security (ATS)

ATS 是 Apple 在 iOS 9 和 macOS 10.11 中引入的安全特性，默认要求所有网络连接使用 HTTPS。对于 IPTV 应用，由于许多流媒体源使用 HTTP 协议，需要禁用 ATS 限制。

### 安全考虑

虽然允许 HTTP 连接会降低安全性，但对于 IPTV 播放器来说这是必需的：
- 许多 IPTV 提供商使用 HTTP 协议
- 本地网络的 IPTV 服务器通常不使用 HTTPS
- 用户输入的 URL 由应用验证

### 替代方案

如果只需要支持特定域名的 HTTP 连接，可以使用更精细的配置：

```xml
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSExceptionDomains</key>
    <dict>
        <key>example.com</key>
        <dict>
            <key>NSExceptionAllowsInsecureHTTPLoads</key>
            <true/>
        </dict>
    </dict>
</dict>
```

但由于 IPTV 源的多样性，使用 `NSAllowsArbitraryLoads` 更实用。

## 测试

构建后的应用现在应该能够：
1. 播放 HTTP 协议的 IPTV 流
2. 播放 HTTPS 协议的 IPTV 流
3. 连接本地网络的 IPTV 服务器
4. 正常使用 M3U 和 Xtream Codes API

## 相关文件

- `macos/Runner/Info.plist` - 主要配置文件
- `macos/Runner/Release.entitlements` - Release 构建权限
- `macos/Runner/DebugProfile.entitlements` - Debug/Profile 构建权限

## 参考资料

- [Apple ATS 文档](https://developer.apple.com/documentation/security/preventing_insecure_network_connections)
- [Flutter macOS 网络配置](https://docs.flutter.dev/platform-integration/macos/building)
