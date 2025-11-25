# 最终测试指南 - macOS 视频播放修复

## 修复概述

本次修复解决了 macOS 桌面端视频播放的两个关键问题：

1. **Task 18**: 协程生命周期和视频表面初始化
2. **Task 19**: macOS 视频输出模块配置

## 快速测试步骤

### 1. 清理并构建

```bash
./gradlew clean
./gradlew :composeApp:build
```

### 2. 运行应用

```bash
./gradlew :composeApp:run
```

### 3. 检查初始化日志

应该看到以下成功消息：

```
=== VLC Media Player Initialization ===
Operating System: MACOS
OS Name: Mac OS X
OS Version: 26.0.1
OS Architecture: aarch64

=== Starting Video Rendering Recovery ===
Will try multiple configurations to initialize video player

[1] Attempting: Primary configuration with hardware acceleration
Options: --vout=caopengllayer, --no-video-title-show, --no-osd, --no-disable-screensaver, --avcodec-hw=videotoolbox
Result: ✓ Success

✓ Success! Using primary configuration with hardware acceleration
==========================================
配置尝试摘要:
总尝试次数: 1
成功次数: 1
失败次数: 0
详细记录:
✓ 尝试 1: 主要配置 + 硬件加速

✓ VLC player initialized successfully
Configuration used: PRIMARY_WITH_HW_ACCEL
Total attempts: 1
============================================
```

**关键点**:
- ✅ 使用 `--vout=caopengllayer` (不是 macosx)
- ✅ 初始化成功
- ✅ 硬件加速启用

### 4. 检查视频表面初始化

应该看到：

```
✓ Video surface initialized in factory
  Video surface parent: EmbeddedMediaPlayerComponent
  Video surface visible: true
  Video surface size: 800x600
```

**关键点**:
- ✅ 视频表面有父组件
- ✅ 视频表面可见
- ✅ 视频表面尺寸有效（不是 0x0）

### 5. 测试视频播放

1. 在应用中选择一个频道
2. 或者输入测试 URL: `https://123tv-mx1.flex-cdn.net/index.m3u8`
3. 点击播放

**预期日志**:
```
=== Video Playback Pre-Check ===
= 视频播放预检查报告 ===
整体状态: PASSED
可以继续播放: 是
✓ 所有检查通过，可以开始播放
============================================================

✓ Pre-check passed, proceeding with playback
Loading new URL: https://123tv-mx1.flex-cdn.net/index.m3u8
Current playback stopped

=== Building Media Options ===
URL type: Live Stream
Stream format: HLS (HTTP Live Streaming)
Video format: H.264/AVC
Format description: H.264/AVC - 广泛使用的视频编码标准，兼容性好
Applying live stream optimizations:
• Low-latency caching (300ms live, 1000ms network)
• Clock jitter disabled
• Clock synchronization disabled
• Audio time-stretch disabled
Hardware acceleration: Enabled (VIDEOTOOLBOX)
...
Media loaded successfully with options: https://...
```

**关键点**:
- ✅ 预检查通过
- ✅ 媒体加载成功
- ✅ 硬件加速启用

### 6. 验证视频和音频

**应该看到**:
- ✅ 视频画面正常显示
- ✅ 音频正常播放
- ✅ 音视频同步
- ✅ 画面流畅

**不应该看到的错误**:
- ❌ `No drawable-nsobject found`
- ❌ `No drawable-nsobject nor vout_window_t found`
- ❌ `The coroutine scope left the composition`
- ❌ `视频表面尺寸无效: 0x0`

### 7. 测试快速切换

这是关键测试，验证协程生命周期修复：

1. 播放第一个频道
2. 立即切换到第二个频道（不等待加载完成）
3. 再次快速切换到第三个频道

**预期结果**:
- ✅ 没有崩溃
- ✅ 没有协程取消错误
- ✅ 最后一个频道正常播放
- ✅ 可能看到 `Coroutine cancelled during media loading, ignoring error`（这是正常的）

### 8. 测试窗口调整

1. 调整窗口大小
2. 观察视频是否正确缩放

**预期日志**:
```
=== Video Surface Size Update ===
Component size changed:
  Previous: 800x600
  New: 1024x768
✓ Video surface updated to match component size
=================================
```

**预期结果**:
- ✅ 视频正确缩放
- ✅ 保持宽高比
- ✅ 没有黑边或拉伸

## 成功标准

所有以下条件都满足：

### 初始化
- [x] VLC 使用 caopengllayer 初始化
- [x] 硬件加速启用
- [x] 视频表面正确初始化（尺寸 > 0）

### 播放
- [ ] 视频画面正常显示
- [ ] 音频正常播放
- [ ] 音视频同步
- [ ] 画面流畅

### 稳定性
- [ ] 快速切换频道不崩溃
- [ ] 没有协程取消错误
- [ ] 窗口调整正常
- [ ] 长时间播放稳定

### 错误
- [ ] 没有 drawable-nsobject 错误
- [ ] 没有视频表面尺寸无效错误
- [ ] 没有协程生命周期错误

## 常见问题排查

### 问题 1: 仍然看到 "No drawable-nsobject found"

**检查**:
1. 确认日志中使用的是 `--vout=caopengllayer` 而不是 `--vout=macosx`
2. 如果仍然使用 macosx，清理并重新构建：
   ```bash
   ./gradlew clean
   ./gradlew :composeApp:build
   ```

### 问题 2: 视频表面尺寸仍然是 0x0

**检查**:
1. 确认看到 "Initial video surface size set to 800x600" 日志
2. 确认窗口尺寸足够大
3. 检查视频表面父组件是否正确

### 问题 3: 仍然看到协程取消错误

**检查**:
1. 确认 VideoPlayer.desktop.kt 包含 `if (!isActive)` 检查
2. 清理并重新构建
3. 查看完整的错误堆栈

### 问题 4: 黑屏但没有错误

**可能原因**:
- 硬件加速问题
- 视频格式不支持
- OpenGL 驱动问题

**解决方案**:
1. 查看完整的诊断报告
2. 尝试其他视频源
3. 检查系统 OpenGL 支持：
   ```bash
   system_profiler SPDisplaysDataType | grep OpenGL
   ```

## 性能测试

### 测试场景

1. **短视频播放** (< 5分钟)
   - 测试启动速度
   - 测试响应性

2. **长视频播放** (> 30分钟)
   - 测试稳定性
   - 测试内存使用

3. **直播流播放**
   - 测试延迟
   - 测试缓冲

4. **频繁切换**
   - 测试资源释放
   - 测试内存泄漏

### 性能指标

- **启动时间**: < 2秒
- **切换延迟**: < 1秒
- **内存使用**: 稳定，无泄漏
- **CPU 使用**: 合理（< 50%）
- **GPU 使用**: 硬件加速工作

## 自动化测试

运行所有相关测试：

```bash
# 视频输出配置测试
./gradlew :composeApp:desktopTest --tests "VideoOutputConfigurationTest"

# 生命周期测试
./gradlew :composeApp:desktopTest --tests "VideoPlayerLifecycleTest"

# 所有视频相关测试
./gradlew :composeApp:desktopTest --tests "*Video*"
```

## 相关文档

- [LIFECYCLE_FIX.md](./LIFECYCLE_FIX.md) - 协程生命周期修复
- [MACOS_VIDEO_OUTPUT_FIX.md](./MACOS_VIDEO_OUTPUT_FIX.md) - macOS 视频输出修复
- [QUICK_TEST_LIFECYCLE_FIX.md](./QUICK_TEST_LIFECYCLE_FIX.md) - 生命周期快速测试
- [VIDEO_TROUBLESHOOTING.md](./VIDEO_TROUBLESHOOTING.md) - 故障排除指南
- [task-18-summary.md](./task-18-summary.md) - Task 18 总结
- [task-19-summary.md](./task-19-summary.md) - Task 19 总结

## 测试报告模板

```
测试日期: _______________
测试人员: _______________
系统版本: macOS _______________
VLC 版本: _______________

初始化测试:
[ ] VLC 使用 caopengllayer
[ ] 硬件加速启用
[ ] 视频表面正确初始化

播放测试:
[ ] 视频画面显示
[ ] 音频播放
[ ] 音视频同步
[ ] 画面流畅

稳定性测试:
[ ] 快速切换正常
[ ] 窗口调整正常
[ ] 长时间播放稳定

错误检查:
[ ] 无 drawable-nsobject 错误
[ ] 无视频表面错误
[ ] 无协程错误

性能测试:
启动时间: _____ 秒
切换延迟: _____ 秒
内存使用: _____ MB
CPU 使用: _____ %

总体评价:
[ ] 通过
[ ] 失败

备注:
_________________________________
_________________________________
_________________________________
```

## 下一步

测试通过后：
1. ✅ 提交代码更改
2. ✅ 更新版本号
3. ✅ 准备发布说明
4. ✅ 通知用户更新

测试失败时：
1. 记录详细错误信息
2. 查看故障排除指南
3. 如需要，回滚更改
4. 重新分析问题
