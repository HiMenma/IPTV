# macOS 视频输出修复

## 问题描述

在 macOS 上运行桌面应用时，视频只有声音没有画面，日志显示：

```
[000000030db18ac0] caopengllayer vout display error: No drawable-nsobject found!
[000000030db18ac0] macosx vout display error: No drawable-nsobject nor vout_window_t found, passing over.
```

## 根本原因

### VLC 视频输出模块问题

在 macOS 上，VLC 有多个视频输出模块：

1. **macosx** - 原生 macOS 视频输出
   - 适用于独立窗口
   - 在嵌入式场景中需要 NSObject 引用
   - 与 Swing/AWT 集成困难

2. **caopengllayer** - Core Animation OpenGL Layer
   - 专为嵌入式场景设计
   - 更好的 Swing/AWT 兼容性
   - 使用 OpenGL 渲染

3. **opengl** - 通用 OpenGL 输出
   - 跨平台备用方案
   - 兼容性最好但性能可能较低

### 问题分析

之前的配置使用 `--vout=macosx`，这个模块期望：
- 一个 NSObject 作为绘制目标
- 直接的 macOS 窗口系统访问
- 原生的 Cocoa 集成

但在 Compose Desktop + Swing 的环境中：
- SwingPanel 提供的是 AWT/Swing 组件
- 没有直接的 NSObject 引用
- VLC 无法找到合适的绘制目标

## 解决方案

### 1. 更改主要视频输出模块

**修改前：**
```kotlin
OperatingSystem.MACOS -> arrayOf(
    "--vout=macosx",
    "--no-video-title-show",
    "--no-osd"
)
```

**修改后：**
```kotlin
OperatingSystem.MACOS -> arrayOf(
    // Use caopengllayer for embedded playback on macOS
    // This is more reliable than macosx vout for embedded scenarios
    "--vout=caopengllayer",
    "--no-video-title-show",
    "--no-osd",
    // Disable screen saver
    "--no-disable-screensaver"
)
```

**理由：**
- `caopengllayer` 专为嵌入式场景设计
- 更好地与 AWT/Swing 集成
- 不需要直接的 NSObject 引用
- 使用 Core Animation 和 OpenGL，性能良好

### 2. 改进备用配置

**修改前：**
```kotlin
fun getFallbackVideoOptions(): Array<String> {
    return arrayOf(
        "--vout=opengl",
        "--no-video-title-show",
        "--no-osd"
    )
}
```

**修改后：**
```kotlin
fun getFallbackVideoOptions(): Array<String> {
    val os = detectOperatingSystem()
    
    return when (os) {
        OperatingSystem.MACOS -> arrayOf(
            // Fallback to OpenGL for macOS
            "--vout=opengl",
            "--no-video-title-show",
            "--no-osd"
        )
        else -> arrayOf(
            "--vout=opengl",
            "--no-video-title-show",
            "--no-osd"
        )
    }
}
```

**理由：**
- 为不同平台提供特定的备用方案
- macOS 使用 OpenGL 作为最后的备用
- 保持代码的可扩展性

### 3. 增强日志输出

在 `VideoPlayer.desktop.kt` 的 factory 中添加更详细的日志：

```kotlin
println("✓ Video surface initialized in factory")
println("  Video surface parent: ${videoSurface.parent?.javaClass?.simpleName ?: "null"}")
println("  Video surface visible: ${videoSurface.isVisible}")
println("  Video surface size: ${videoSurface.width}x${videoSurface.height}")
```

这有助于诊断视频表面的状态。

## 技术细节

### caopengllayer 的优势

1. **嵌入式友好**
   - 设计用于嵌入到其他应用中
   - 不需要独立窗口
   - 与 AWT/Swing 兼容性好

2. **性能**
   - 使用 Core Animation
   - 硬件加速的 OpenGL 渲染
   - 低延迟

3. **兼容性**
   - 支持所有 macOS 版本（10.7+）
   - 与 VLCJ 良好集成
   - 稳定可靠

### VLC 视频输出模块选择策略

```
macOS 嵌入式播放:
1. caopengllayer (主要) - 最佳嵌入式性能
2. opengl (备用) - 通用跨平台方案
3. macosx (不推荐) - 仅用于独立窗口
```

## 验证步骤

### 1. 清理并重新构建

```bash
./gradlew clean
./gradlew :composeApp:build
```

### 2. 运行应用

```bash
./gradlew :composeApp:run
```

### 3. 检查初始化日志

应该看到：

```
=== VLC Media Player Initialization ===
Operating System: MACOS
...
[1] Attempting: Primary configuration with hardware acceleration
Options: --vout=caopengllayer, --no-video-title-show, --no-osd, --no-disable-screensaver, --avcodec-hw=videotoolbox
Result: ✓ Success
```

### 4. 检查视频表面日志

应该看到：

```
✓ Video surface initialized in factory
  Video surface parent: EmbeddedMediaPlayerComponent
  Video surface visible: true
  Video surface size: 800x600
```

### 5. 测试播放

1. 加载测试 URL
2. 点击播放
3. 应该能看到视频画面

**不应该看到的错误：**
- ❌ `No drawable-nsobject found`
- ❌ `No drawable-nsobject nor vout_window_t found`

## 预期结果

修复后：

1. ✅ 视频和音频都能正常播放
2. ✅ 没有 drawable-nsobject 错误
3. ✅ 视频渲染流畅
4. ✅ 硬件加速正常工作

## 如果仍然有问题

### 问题 1: 仍然看到 caopengllayer 错误

**可能原因：**
- VLC 版本太旧
- OpenGL 驱动问题

**解决方案：**
1. 更新 VLC 到最新版本（3.0.18+）
2. 检查系统 OpenGL 支持：
   ```bash
   system_profiler SPDisplaysDataType | grep OpenGL
   ```
3. 应用会自动尝试 OpenGL 备用方案

### 问题 2: 视频表面仍然是 0x0

**可能原因：**
- SwingPanel 布局问题
- 窗口尺寸太小

**解决方案：**
1. 确保窗口足够大（至少 800x600）
2. 检查日志中的视频表面信息
3. 尝试调整窗口大小

### 问题 3: 黑屏但没有错误

**可能原因：**
- 硬件加速问题
- 视频格式不支持

**解决方案：**
1. 查看完整的诊断报告
2. 尝试其他视频源
3. 检查 VLC 日志中的解码信息

## 相关资源

### VLC 文档
- [VLC Video Output Modules](https://wiki.videolan.org/Documentation:Modules/vout/)
- [VLC macOS Integration](https://wiki.videolan.org/Mac_OS_X/)

### VLCJ 文档
- [VLCJ GitHub](https://github.com/caprica/vlcj)
- [VLCJ Examples](https://github.com/caprica/vlcj-examples)

### macOS 开发
- [Core Animation Programming Guide](https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/CoreAnimation_guide/)
- [OpenGL on macOS](https://developer.apple.com/opengl/)

## 相关文件

### 修改的文件
- `VideoOutputConfiguration.kt` - 更改 macOS 视频输出模块
- `VideoPlayer.desktop.kt` - 增强日志输出

### 相关文件
- `VideoRenderingRecovery.kt` - 自动尝试备用配置
- `HardwareAccelerationDetector.kt` - 检测硬件加速支持

## 总结

通过将 macOS 的视频输出模块从 `macosx` 更改为 `caopengllayer`，我们解决了嵌入式播放场景中的视频渲染问题。`caopengllayer` 专为这种场景设计，提供了更好的兼容性和性能。

这个修复确保了：
- 视频能够在 Compose Desktop + Swing 环境中正确渲染
- 不需要复杂的 NSObject 桥接
- 保持了硬件加速的性能优势
- 提供了可靠的备用方案
