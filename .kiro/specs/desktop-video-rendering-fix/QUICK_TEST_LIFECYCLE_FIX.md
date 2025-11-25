# 生命周期修复快速测试指南

## 修复内容

本次修复解决了以下问题：

1. ✅ **协程取消错误**：`The coroutine scope left the composition`
2. ✅ **视频表面尺寸无效**：初始化时尺寸为 0x0
3. ✅ **视频表面未添加到显示层次结构**

## 快速测试步骤

### 1. 运行应用

```bash
./gradlew :composeApp:run
```

### 2. 检查启动日志

应该看到以下成功消息：

```
=== VLC Media Player Initialization ===
Operating System: MACOS
...
✓ Success! Using primary configuration with hardware acceleration
✓ VLC player initialized successfully
```

### 3. 检查视频表面初始化

当视频播放器组件加载时，应该看到：

```
✓ Initial video surface size set to 800x600
✓ Video surface initialized in factory
```

### 4. 测试播放

1. **加载直播流**
   - 输入 URL：`https://123tv-mx1.flex-cdn.net/index.m3u8`
   - 点击播放

2. **检查播放前检查日志**
   ```
   === Video Playback Pre-Check ===
   = 视频播放预检查报告 ===
   整体状态: PASSED
   可以继续播放: 是
   ✓ 所有检查通过，可以开始播放
   ```

3. **检查媒体加载日志**
   ```
   Loading new URL: https://...
   Current playback stopped
   Media options configured: ...
   Media loaded successfully with options: ...
   ```

### 5. 测试快速切换频道

这是关键测试，用于验证协程取消处理：

1. 加载第一个频道
2. 立即切换到第二个频道（不等待第一个加载完成）
3. 再次快速切换到第三个频道

**预期结果：**
- ✅ 不应该看到 `The coroutine scope left the composition` 错误
- ✅ 如果看到 `Coroutine cancelled during media loading, ignoring error`，这是正常的
- ✅ 应用不应该崩溃
- ✅ 最后一个频道应该正常播放

### 6. 检查视频表面尺寸更新

当调整窗口大小时，应该看到：

```
=== Video Surface Size Update ===
Component size changed:
  Previous: 800x600
  New: 1024x768
✓ Video surface updated to match component size
```

## 不应该看到的错误

以下错误已被修复，不应该再出现：

❌ `Error loading media: 无法加载媒体: The coroutine scope left the composition`
❌ `视频表面尺寸无效: 0x0`
❌ `视频表面不可显示 (未添加到显示层次结构)`

## 如果仍然有问题

### 问题 1：视频表面仍然是 0x0

**可能原因：**
- SwingPanel 还没有完成布局
- 窗口尺寸太小

**解决方案：**
1. 确保窗口有足够的尺寸（至少 800x600）
2. 检查 `factory` lambda 是否正确执行
3. 查看日志中的 `✓ Initial video surface size set to 800x600`

### 问题 2：仍然看到协程取消错误

**可能原因：**
- 修复没有正确应用
- 其他地方也有类似问题

**解决方案：**
1. 确认 `VideoPlayer.desktop.kt` 已更新
2. 清理并重新构建：
   ```bash
   ./gradlew clean
   ./gradlew :composeApp:run
   ```

### 问题 3：视频仍然无法播放

**可能原因：**
- VLC 配置问题
- 网络连接问题
- 媒体源问题

**解决方案：**
1. 检查 VLC 是否正确安装
2. 测试其他已知可用的媒体源
3. 查看完整的诊断报告

## 运行自动化测试

```bash
# 运行生命周期测试
./gradlew :composeApp:desktopTest --tests "VideoPlayerLifecycleTest"

# 运行所有视频相关测试
./gradlew :composeApp:desktopTest --tests "*Video*"
```

## 成功标准

修复成功的标志：

1. ✅ 应用启动时视频表面正确初始化
2. ✅ 快速切换频道不会导致错误
3. ✅ 视频能够正常播放
4. ✅ 窗口调整大小时视频表面正确更新
5. ✅ 没有协程取消相关的错误消息

## 相关文档

- [LIFECYCLE_FIX.md](./LIFECYCLE_FIX.md) - 详细的技术说明
- [VIDEO_TROUBLESHOOTING.md](./VIDEO_TROUBLESHOOTING.md) - 故障排除指南
- [TECHNICAL_DOCUMENTATION.md](./TECHNICAL_DOCUMENTATION.md) - 技术文档

## 反馈

如果测试过程中遇到任何问题，请记录：
1. 完整的错误日志
2. 重现步骤
3. 系统信息（操作系统、VLC 版本等）
