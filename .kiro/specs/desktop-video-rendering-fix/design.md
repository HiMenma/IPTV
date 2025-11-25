# Design Document

## Overview

本设计文档描述了修复桌面版IPTV播放器视频渲染黑屏问题的技术方案。问题的核心在于VLC媒体播放器能够解码音频和视频流，但视频帧未能正确渲染到Swing组件的视频表面上。

主要解决方案包括：
1. 配置VLC的视频输出选项以确保与嵌入式播放器兼容
2. 添加媒体播放选项以优化直播流处理
3. 验证视频表面的正确初始化和配置
4. 实现平台特定的视频输出后端选择
5. 添加详细的诊断日志以快速识别问题

## Architecture

### 视频渲染流程

```
用户播放请求
    ↓
EmbeddedMediaPlayerComponent 初始化
    ├── 配置视频输出选项 (--vout, --avcodec-hw)
    ├── 创建视频表面 (Canvas/VideoSurface)
    └── 验证表面可见性和尺寸
    ↓
媒体加载
    ├── 应用媒体选项 (:network-caching, :live-caching)
    ├── 设置视频输出回调
    └── 开始解码
    ↓
视频渲染循环
    ├── VLC解码视频帧
    ├── 渲染到视频表面
    ├── 更新播放状态
    └── 监控渲染统计
```

### 平台特定视频输出

```
操作系统检测
    ├── macOS
    │   ├── 首选: vout=macosx
    │   └── 备用: vout=opengl
    ├── Linux
    │   ├── 首选: vout=xcb_x11
    │   └── 备用: vout=opengl
    └── Windows
        ├── 首选: vout=directdraw
        └── 备用: vout=opengl
```

## Components and Interfaces

### 1. VideoOutputConfiguration (新增)

负责配置VLC视频输出选项。

```kotlin
object VideoOutputConfiguration {
    /**
     * 获取当前平台的最佳视频输出选项
     */
    fun getPlatformVideoOptions(): Array<String>
    
    /**
     * 获取备用视频输出选项
     */
    fun getFallbackVideoOptions(): Array<String>
    
    /**
     * 检测当前操作系统
     */
    fun detectOperatingSystem(): OperatingSystem
}

enum class OperatingSystem {
    MACOS, LINUX, WINDOWS, UNKNOWN
}
```

### 2. MediaOptionsBuilder (新增)

构建媒体播放选项。

```kotlin
class MediaOptionsBuilder {
    private val options = mutableListOf<String>()
    
    fun withNetworkCaching(ms: Int): MediaOptionsBuilder
    fun withLiveCaching(ms: Int): MediaOptionsBuilder
    fun withHardwareAcceleration(enabled: Boolean): MediaOptionsBuilder
    fun withVideoOutput(vout: String): MediaOptionsBuilder
    fun build(): Array<String>
}
```

### 3. VideoSurfaceValidator (新增)

验证视频表面的正确性。

```kotlin
object VideoSurfaceValidator {
    /**
     * 验证视频表面是否正确初始化
     */
    fun validateVideoSurface(component: EmbeddedMediaPlayerComponent): ValidationResult
    
    /**
     * 检查视频表面的可见性
     */
    fun isVideoSurfaceVisible(component: EmbeddedMediaPlayerComponent): Boolean
    
    /**
     * 获取视频表面尺寸
     */
    fun getVideoSurfaceDimensions(component: EmbeddedMediaPlayerComponent): Dimension?
}

data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val suggestions: List<String>
)
```

### 4. VideoRenderingDiagnostics (新增)

诊断视频渲染问题。

```kotlin
object VideoRenderingDiagnostics {
    /**
     * 记录视频编解码器信息
     */
    fun logVideoCodecInfo(mediaPlayer: MediaPlayer)
    
    /**
     * 记录渲染统计信息
     */
    fun logRenderingStats(mediaPlayer: MediaPlayer)
    
    /**
     * 检测黑屏问题
     */
    fun detectBlackScreen(mediaPlayer: MediaPlayer): BlackScreenDiagnosis
    
    /**
     * 生成诊断报告
     */
    fun generateDiagnosticReport(mediaPlayer: MediaPlayer): String
}

data class BlackScreenDiagnosis(
    val isBlackScreen: Boolean,
    val possibleCauses: List<String>,
    val suggestedFixes: List<String>
)
```

## Data Models

### 1. VideoRenderingState (新增)

跟踪视频渲染状态。

```kotlin
data class VideoRenderingState(
    val isVideoSurfaceInitialized: Boolean = false,
    val videoOutputModule: String? = null,
    val videoCodec: String? = null,
    val videoResolution: Pair<Int, Int>? = null,
    val framesRendered: Long = 0,
    val lastFrameTime: Long = 0,
    val renderingIssues: List<String> = emptyList()
)
```

### 2. VlcConfiguration (新增)

VLC配置参数。

```kotlin
data class VlcConfiguration(
    val videoOutput: String,
    val hardwareAcceleration: Boolean,
    val networkCaching: Int,
    val liveCaching: Int,
    val additionalOptions: List<String> = emptyList()
) {
    fun toVlcArgs(): Array<String>
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Acceptance Criteria Testing Prework

1.1 WHEN 用户播放直播链接 THEN 系统 SHALL 正确显示视频画面和播放音频
Thoughts: 这是一个集成测试场景，涉及整个播放流程。我们可以通过检查视频表面是否接收到帧数据来验证。
Testable: yes - example

1.2 WHEN 视频开始播放 THEN 系统 SHALL 在视频表面上渲染视频帧
Thoughts: 这是关于所有视频播放的通用规则。我们可以监控帧渲染计数器，确保它在播放时递增。
Testable: yes - property

1.3 WHEN 视频格式改变 THEN 系统 SHALL 自动调整渲染参数以正确显示
Thoughts: 这是关于系统适应性的规则。我们可以测试不同格式的视频，确保都能正确渲染。
Testable: yes - property

1.4 IF 视频渲染失败 THEN 系统 SHALL 记录详细错误信息并尝试备用渲染方案
Thoughts: 这是错误处理逻辑。我们可以模拟渲染失败，验证错误日志和备用方案的触发。
Testable: yes - property

2.1 WHEN 初始化媒体播放器 THEN 系统 SHALL 配置适当的视频输出模块
Thoughts: 这是关于初始化过程的规则。我们可以验证初始化后的配置是否包含正确的视频输出选项。
Testable: yes - property

2.2 WHEN 在不同操作系统上运行 THEN 系统 SHALL 使用平台特定的最佳视频输出选项
Thoughts: 这是关于平台适配的规则。我们可以模拟不同的操作系统，验证选择的视频输出选项。
Testable: yes - property

2.3 WHEN 硬件加速可用 THEN 系统 SHALL 启用硬件加速以提高性能
Thoughts: 这是关于性能优化的规则。我们可以检查配置中是否包含硬件加速选项。
Testable: yes - property

2.4 IF 默认视频输出失败 THEN 系统 SHALL 尝试备用视频输出模块
Thoughts: 这是错误恢复逻辑。我们可以模拟主输出失败，验证备用输出的尝试。
Testable: yes - property

3.1 WHEN 创建EmbeddedMediaPlayerComponent THEN 系统 SHALL 确保视频表面已正确初始化
Thoughts: 这是关于组件创建的规则。我们可以在创建后验证视频表面的状态。
Testable: yes - property

3.2 WHEN 视频表面创建 THEN 系统 SHALL 验证其尺寸和可见性
Thoughts: 这是关于表面验证的规则。我们可以检查尺寸是否大于0且可见性为true。
Testable: yes - property

3.3 WHEN 组件布局改变 THEN 系统 SHALL 更新视频表面以匹配新尺寸
Thoughts: 这是关于动态调整的规则。我们可以改变布局，验证视频表面尺寸的更新。
Testable: yes - property

3.4 IF 视频表面初始化失败 THEN 系统 SHALL 提供清晰的错误消息
Thoughts: 这是错误处理。我们可以模拟初始化失败，验证错误消息的内容。
Testable: yes - example

4.1 WHEN 播放媒体 THEN 系统 SHALL 应用适当的网络缓存选项
Thoughts: 这是关于所有媒体播放的规则。我们可以验证媒体选项中包含缓存配置。
Testable: yes - property

4.2 WHEN 播放直播流 THEN 系统 SHALL 配置低延迟选项
Thoughts: 这是关于直播流的特定规则。我们可以检查直播URL的媒体选项。
Testable: yes - property

4.3 WHEN 播放不同格式 THEN 系统 SHALL 根据格式调整解码选项
Thoughts: 这是关于格式适配的规则。我们可以测试不同格式，验证解码选项的调整。
Testable: yes - property

4.4 IF 媒体选项配置失败 THEN 系统 SHALL 使用默认选项并记录警告
Thoughts: 这是错误处理。我们可以模拟配置失败，验证默认选项的使用和日志。
Testable: yes - example

5.1 WHEN 视频播放开始 THEN 系统 SHALL 记录视频编解码器和格式信息
Thoughts: 这是关于日志记录的规则。我们可以验证日志中包含编解码器信息。
Testable: yes - property

5.2 WHEN 视频帧渲染 THEN 系统 SHALL 记录渲染统计信息
Thoughts: 这是关于统计记录的规则。我们可以验证统计信息的更新。
Testable: yes - property

5.3 WHEN 检测到黑屏 THEN 系统 SHALL 记录可能的原因和建议的解决方案
Thoughts: 这是关于诊断的规则。我们可以模拟黑屏，验证诊断信息。
Testable: yes - example

5.4 WHEN 播放错误发生 THEN 系统 SHALL 记录VLC内部状态和错误代码
Thoughts: 这是关于错误日志的规则。我们可以触发错误，验证日志内容。
Testable: yes - property

6.1 WHEN 主要视频输出不可用 THEN 系统 SHALL 自动尝试备用输出方法
Thoughts: 这是关于容错的规则。我们可以模拟主输出失败，验证备用输出的尝试。
Testable: yes - property

6.2 WHEN 在macOS上运行 THEN 系统 SHALL 优先使用适合macOS的视频输出
Thoughts: 这是平台特定的规则。我们可以模拟macOS环境，验证输出选择。
Testable: yes - example

6.3 WHEN 在Linux上运行 THEN 系统 SHALL 优先使用适合Linux的视频输出
Thoughts: 这是平台特定的规则。我们可以模拟Linux环境，验证输出选择。
Testable: yes - example

6.4 WHEN 在Windows上运行 THEN 系统 SHALL 优先使用适合Windows的视频输出
Thoughts: 这是平台特定的规则。我们可以模拟Windows环境，验证输出选择。
testable: yes - example

### Property Reflection

审查所有属性以消除冗余：

- 属性 2.1 和 3.1 都涉及初始化验证，但2.1关注视频输出配置，3.1关注视频表面，它们是不同的方面，保留两者
- 属性 1.4 和 2.4 都涉及备用方案，但1.4是渲染失败，2.4是输出模块失败，它们是不同的失败场景，保留两者
- 属性 6.2、6.3、6.4 是平台特定的示例，可以合并为一个属性
- 属性 5.1 和 5.4 都涉及日志记录，但5.1是正常播放信息，5.4是错误信息，保留两者

合并后的属性：
- 将 6.2、6.3、6.4 合并为一个属性：平台特定视频输出选择

### Correctness Properties

Property 1: 视频帧渲染递增
*For any* 正在播放的视频，帧渲染计数器应该随时间递增
**Validates: Requirements 1.2**

Property 2: 格式适配性
*For any* 支持的视频格式，系统应该能够正确渲染而不出现黑屏
**Validates: Requirements 1.3**

Property 3: 渲染失败恢复
*For any* 渲染失败场景，系统应该记录错误并尝试备用方案
**Validates: Requirements 1.4**

Property 4: 视频输出配置完整性
*For any* 媒体播放器初始化，配置应该包含有效的视频输出模块选项
**Validates: Requirements 2.1**

Property 5: 平台特定输出选择
*For any* 操作系统平台，系统应该选择该平台的最佳视频输出选项
**Validates: Requirements 2.2, 6.2, 6.3, 6.4**

Property 6: 硬件加速启用
*For any* 支持硬件加速的系统，配置应该包含硬件加速选项
**Validates: Requirements 2.3**

Property 7: 输出模块备用机制
*For any* 视频输出模块失败，系统应该尝试备用输出模块
**Validates: Requirements 2.4, 6.1**

Property 8: 视频表面初始化验证
*For any* 创建的EmbeddedMediaPlayerComponent，视频表面应该被正确初始化
**Validates: Requirements 3.1**

Property 9: 视频表面尺寸和可见性
*For any* 初始化的视频表面，其尺寸应该大于0且可见性应该为true
**Validates: Requirements 3.2**

Property 10: 布局变化响应
*For any* 组件布局变化，视频表面尺寸应该更新以匹配新布局
**Validates: Requirements 3.3**

Property 11: 网络缓存配置
*For any* 媒体播放，媒体选项应该包含适当的网络缓存配置
**Validates: Requirements 4.1**

Property 12: 直播流低延迟配置
*For any* 直播流URL，媒体选项应该包含低延迟配置
**Validates: Requirements 4.2**

Property 13: 格式特定解码选项
*For any* 视频格式，系统应该应用该格式的适当解码选项
**Validates: Requirements 4.3**

Property 14: 播放信息日志记录
*For any* 开始播放的视频，日志应该包含编解码器和格式信息
**Validates: Requirements 5.1**

Property 15: 渲染统计更新
*For any* 正在渲染的视频，渲染统计信息应该定期更新
**Validates: Requirements 5.2**

Property 16: 错误状态日志记录
*For any* 播放错误，日志应该包含VLC内部状态和错误代码
**Validates: Requirements 5.4**

## Error Handling

### 1. 视频输出初始化失败

**问题**: VLC可能无法初始化请求的视频输出模块

**解决方案**:
```kotlin
fun initializeWithFallback(): EmbeddedMediaPlayerComponent {
    val primaryOptions = VideoOutputConfiguration.getPlatformVideoOptions()
    
    return try {
        EmbeddedMediaPlayerComponent(
            null, // 使用默认MediaPlayerFactory
            null, // 使用默认FullScreenStrategy
            null, // 使用默认InputEvents
            null, // 使用默认Overlay
            primaryOptions // 平台特定选项
        )
    } catch (e: Exception) {
        println("Primary video output failed: ${e.message}")
        println("Trying fallback options...")
        
        val fallbackOptions = VideoOutputConfiguration.getFallbackVideoOptions()
        EmbeddedMediaPlayerComponent(
            null, null, null, null, fallbackOptions
        )
    }
}
```

### 2. 视频表面未正确初始化

**问题**: 视频表面可能未正确创建或不可见

**解决方案**:
```kotlin
fun validateAndFixVideoSurface(component: EmbeddedMediaPlayerComponent) {
    val validation = VideoSurfaceValidator.validateVideoSurface(component)
    
    if (!validation.isValid) {
        println("Video surface validation failed:")
        validation.issues.forEach { println("  - $it") }
        
        println("Suggested fixes:")
        validation.suggestions.forEach { println("  - $it") }
        
        // 尝试修复
        if (!VideoSurfaceValidator.isVideoSurfaceVisible(component)) {
            component.videoSurfaceComponent().isVisible = true
        }
        
        val dimensions = VideoSurfaceValidator.getVideoSurfaceDimensions(component)
        if (dimensions == null || dimensions.width == 0 || dimensions.height == 0) {
            component.videoSurfaceComponent().setSize(800, 600)
        }
    }
}
```

### 3. 黑屏检测和诊断

**问题**: 视频播放但显示黑屏

**解决方案**:
```kotlin
fun monitorAndDiagnoseBlackScreen(mediaPlayer: MediaPlayer) {
    val diagnosis = VideoRenderingDiagnostics.detectBlackScreen(mediaPlayer)
    
    if (diagnosis.isBlackScreen) {
        println("⚠️ Black screen detected!")
        println("Possible causes:")
        diagnosis.possibleCauses.forEach { println("  - $it") }
        
        println("Suggested fixes:")
        diagnosis.suggestedFixes.forEach { println("  - $it") }
        
        // 生成完整诊断报告
        val report = VideoRenderingDiagnostics.generateDiagnosticReport(mediaPlayer)
        println(report)
    }
}
```

## Testing Strategy

### 单元测试

**VideoOutputConfiguration测试**:
- 测试不同操作系统的视频输出选项选择
- 测试备用选项的正确性
- 测试操作系统检测逻辑

**MediaOptionsBuilder测试**:
- 测试选项构建的正确性
- 测试不同选项组合
- 测试选项格式化

**VideoSurfaceValidator测试**:
- 测试视频表面验证逻辑
- 测试尺寸和可见性检查
- 测试验证结果的准确性

### 集成测试

**视频渲染测试**:
- 使用测试视频文件验证渲染
- 测试不同格式的视频
- 测试直播流URL
- 验证视频帧确实被渲染

**平台特定测试**:
- 在不同操作系统上测试视频输出
- 验证平台特定选项的效果
- 测试备用机制

### 属性测试

使用Kotest property testing框架：

**Property 1: 视频帧渲染递增**
```kotlin
"frames rendered should increase during playback" {
    checkAll(Arb.videoUrl()) { url ->
        val player = createTestPlayer()
        player.play(url)
        delay(1000)
        val frames1 = player.framesRendered()
        delay(1000)
        val frames2 = player.framesRendered()
        frames2 shouldBeGreaterThan frames1
    }
}
```

**Property 5: 平台特定输出选择**
```kotlin
"should select platform-specific video output" {
    checkAll(Arb.operatingSystem()) { os ->
        val options = VideoOutputConfiguration.getPlatformVideoOptions(os)
        options shouldContain expectedOutputFor(os)
    }
}
```

## Implementation Notes

### 1. VLC视频输出选项

关键的VLC选项：
- `--vout`: 指定视频输出模块
- `--avcodec-hw`: 启用硬件加速解码
- `--no-video-title-show`: 不显示视频标题
- `--no-osd`: 禁用屏幕显示

平台特定选项：
- **macOS**: `--vout=macosx` 或 `--vout=opengl`
- **Linux**: `--vout=xcb_x11` 或 `--vout=opengl`
- **Windows**: `--vout=directdraw` 或 `--vout=opengl`

### 2. 媒体选项

直播流优化：
```kotlin
val mediaOptions = arrayOf(
    ":network-caching=1000",  // 网络缓存1秒
    ":live-caching=300",      // 直播缓存300ms
    ":clock-jitter=0",        // 禁用时钟抖动
    ":clock-synchro=0"        // 禁用时钟同步
)
```

### 3. 视频表面验证

确保视频表面正确初始化：
```kotlin
val videoSurface = mediaPlayerComponent.videoSurfaceComponent()
videoSurface.isVisible = true
videoSurface.setSize(800, 600)
videoSurface.isOpaque = true
```

### 4. 诊断信息收集

收集关键诊断信息：
```kotlin
fun collectDiagnostics(mediaPlayer: MediaPlayer): Map<String, Any> {
    return mapOf(
        "video_codec" to mediaPlayer.video().codec(),
        "video_size" to "${mediaPlayer.video().videoDimension().width}x${mediaPlayer.video().videoDimension().height}",
        "video_fps" to mediaPlayer.video().fps(),
        "video_output" to mediaPlayer.video().videoOutput(),
        "frames_displayed" to mediaPlayer.video().frameCount(),
        "is_playing" to mediaPlayer.status().isPlaying,
        "has_video_output" to mediaPlayer.video().videoOutput().isNotEmpty()
    )
}
```

## Performance Considerations

### 1. 硬件加速

启用硬件加速可以显著提高性能：
```kotlin
val options = arrayOf(
    "--avcodec-hw=any",  // 使用任何可用的硬件加速
    "--ffmpeg-hw"        // 启用FFmpeg硬件加速
)
```

### 2. 缓存优化

根据网络条件调整缓存：
- 本地文件: 300ms
- 局域网: 1000ms
- 互联网: 3000ms
- 直播流: 300-1000ms

### 3. 内存管理

及时释放视频资源：
- 停止播放时释放解码器
- 切换视频时清理旧的视频表面
- 监控内存使用

## Security Considerations

### 1. URL验证

验证视频URL的安全性：
- 检查协议（http, https, rtsp等）
- 验证URL格式
- 防止路径遍历攻击

### 2. 资源限制

限制资源使用：
- 限制缓存大小
- 限制并发连接数
- 超时保护

## Migration Strategy

### 向后兼容

保持现有API不变，新功能作为可选配置：
```kotlin
@Composable
fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit,
    onError: (String) -> Unit,
    onPlayerInitFailed: () -> Unit,
    // 新增可选参数
    vlcConfiguration: VlcConfiguration? = null
)
```

### 渐进式改进

1. 第一阶段：添加视频输出配置
2. 第二阶段：添加诊断和日志
3. 第三阶段：优化性能和稳定性

## Monitoring and Logging

### 日志级别

- **ERROR**: 视频渲染失败、初始化失败
- **WARN**: 备用输出使用、性能问题
- **INFO**: 视频开始播放、编解码器信息
- **DEBUG**: 帧渲染统计、详细状态

### 关键指标

- 视频初始化成功率
- 黑屏发生频率
- 平均首帧时间
- 帧率和丢帧率
- 视频输出模块使用分布

### 诊断报告

生成详细的诊断报告：
```
=== Video Rendering Diagnostic Report ===
Timestamp: 2025-11-25 10:30:45
OS: macOS 14.0
VLC Version: 3.0.18

Video Output Configuration:
  - Primary: vout=macosx
  - Fallback: vout=opengl
  - Hardware Acceleration: enabled

Video Surface:
  - Initialized: true
  - Visible: true
  - Size: 1920x1080
  - Opaque: true

Media Information:
  - URL: http://example.com/stream.m3u8
  - Codec: H.264
  - Resolution: 1920x1080
  - FPS: 25
  - Bitrate: 5000 kbps

Rendering Statistics:
  - Frames Displayed: 1250
  - Frames Dropped: 5
  - Average Frame Time: 40ms
  - Last Frame: 2ms ago

Issues Detected:
  - None

Suggestions:
  - Video rendering is working correctly
==========================================
```
