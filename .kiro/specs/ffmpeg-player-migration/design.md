# Design Document

## Overview

本设计文档描述了将桌面版 IPTV 播放器从 VLC Media Player 迁移到 FFmpeg/libav 的技术方案。迁移的主要目标是：

1. **更好的控制**: FFmpeg 提供了更细粒度的控制，可以精确管理解码、渲染和同步
2. **更广泛的格式支持**: FFmpeg 支持几乎所有主流音视频格式和协议
3. **更好的性能**: 可以针对特定场景优化解码和渲染流程
4. **更灵活的集成**: 通过 JavaCV 可以直接访问 FFmpeg 的底层 API
5. **硬件加速**: 支持多种硬件加速方案（VideoToolbox、VAAPI、DXVA2 等）

迁移将使用 JavaCV 库作为 FFmpeg 的 Java/Kotlin 绑定，保持与现有 API 的兼容性，确保平滑过渡。

## Architecture

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                    VideoPlayer Composable                    │
│                  (保持现有 API 不变)                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  FFmpegPlayerEngine                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Decoder      │  │ Renderer     │  │ AudioPlayer  │      │
│  │ Thread       │  │ Thread       │  │ Thread       │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    JavaCV / FFmpeg                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ FrameGrabber │  │ Frame        │  │ FrameRecorder│      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 播放流程

```
用户请求播放
    ↓
初始化 FFmpegFrameGrabber
    ├── 检测媒体格式
    ├── 配置硬件加速
    └── 打开媒体流
    ↓
启动三个线程
    ├── Decoder Thread: 解码音视频帧
    ├── Renderer Thread: 渲染视频帧到 Canvas
    └── AudioPlayer Thread: 播放音频帧
    ↓
音视频同步
    ├── 使用音频时钟作为主时钟
    ├── 视频帧根据时间戳调整渲染时机
    └── 动态调整以保持同步
    ↓
播放控制
    ├── 播放/暂停: 控制线程运行状态
    ├── 跳转: 调用 setTimestamp()
    └── 音量: 调整音频输出增益
```


## Components and Interfaces

### 1. FFmpegPlayerEngine

核心播放引擎，管理整个播放生命周期。

```kotlin
class FFmpegPlayerEngine(
    private val onStateChange: (PlayerState) -> Unit,
    private val onError: (String) -> Unit
) {
    private var grabber: FFmpegFrameGrabber? = null
    private var decoderThread: Thread? = null
    private var rendererThread: Thread? = null
    private var audioThread: Thread? = null
    
    private val videoFrameQueue = LinkedBlockingQueue<Frame>(30)
    private val audioFrameQueue = LinkedBlockingQueue<Frame>(100)
    
    private var isPlaying = AtomicBoolean(false)
    private var isPaused = AtomicBoolean(false)
    
    /**
     * 初始化并开始播放
     */
    fun play(url: String, canvas: Canvas)
    
    /**
     * 暂停播放
     */
    fun pause()
    
    /**
     * 恢复播放
     */
    fun resume()
    
    /**
     * 跳转到指定位置
     */
    fun seekTo(timestampMs: Long)
    
    /**
     * 设置音量
     */
    fun setVolume(volume: Float)
    
    /**
     * 停止播放并释放资源
     */
    fun stop()
    
    /**
     * 释放所有资源
     */
    fun release()
}
```

### 2. FFmpegDecoder

负责解码音视频帧。

```kotlin
class FFmpegDecoder(
    private val grabber: FFmpegFrameGrabber,
    private val videoQueue: BlockingQueue<Frame>,
    private val audioQueue: BlockingQueue<Frame>,
    private val isPlaying: AtomicBoolean,
    private val isPaused: AtomicBoolean
) : Runnable {
    
    override fun run() {
        while (isPlaying.get()) {
            if (isPaused.get()) {
                Thread.sleep(10)
                continue
            }
            
            val frame = grabber.grab()
            when {
                frame.image != null -> videoQueue.offer(frame, 100, TimeUnit.MILLISECONDS)
                frame.samples != null -> audioQueue.offer(frame, 100, TimeUnit.MILLISECONDS)
            }
        }
    }
}
```

### 3. VideoRenderer

负责渲染视频帧到 Canvas。

```kotlin
class VideoRenderer(
    private val canvas: Canvas,
    private val videoQueue: BlockingQueue<Frame>,
    private val isPlaying: AtomicBoolean,
    private val isPaused: AtomicBoolean,
    private val audioClock: AudioClock
) : Runnable {
    
    private val converter = Java2DFrameConverter()
    
    override fun run() {
        while (isPlaying.get()) {
            if (isPaused.get()) {
                Thread.sleep(10)
                continue
            }
            
            val frame = videoQueue.poll(100, TimeUnit.MILLISECONDS) ?: continue
            
            // 音视频同步
            val delay = calculateDelay(frame.timestamp, audioClock.getTime())
            if (delay > 0) {
                Thread.sleep(delay)
            }
            
            // 渲染到 Canvas
            val image = converter.convert(frame)
            renderToCanvas(canvas, image)
        }
    }
    
    private fun calculateDelay(videoTimestamp: Long, audioTimestamp: Long): Long
    private fun renderToCanvas(canvas: Canvas, image: BufferedImage)
}
```

### 4. AudioPlayer

负责播放音频帧。

```kotlin
class AudioPlayer(
    private val audioQueue: BlockingQueue<Frame>,
    private val isPlaying: AtomicBoolean,
    private val isPaused: AtomicBoolean,
    private val audioClock: AudioClock
) : Runnable {
    
    private var sourceDataLine: SourceDataLine? = null
    private var volume: Float = 1.0f
    
    override fun run() {
        initializeAudioLine()
        
        while (isPlaying.get()) {
            if (isPaused.get()) {
                Thread.sleep(10)
                continue
            }
            
            val frame = audioQueue.poll(100, TimeUnit.MILLISECONDS) ?: continue
            
            // 更新音频时钟
            audioClock.update(frame.timestamp)
            
            // 应用音量
            val samples = applySamples(frame.samples, volume)
            
            // 播放音频
            sourceDataLine?.write(samples, 0, samples.size)
        }
    }
    
    private fun initializeAudioLine()
    private fun applyVolume(samples: ByteArray, volume: Float): ByteArray
}
```

### 5. HardwareAccelerationManager

管理硬件加速配置。

```kotlin
object HardwareAccelerationManager {
    
    /**
     * 检测可用的硬件加速方案
     */
    fun detectHardwareAcceleration(): HardwareAcceleration
    
    /**
     * 配置 FFmpeg 使用硬件加速
     */
    fun configureHardwareAcceleration(grabber: FFmpegFrameGrabber, hwAccel: HardwareAcceleration)
    
    /**
     * 获取平台特定的硬件加速选项
     */
    fun getPlatformHardwareAcceleration(): HardwareAcceleration
}

data class HardwareAcceleration(
    val type: HardwareAccelerationType,
    val isAvailable: Boolean,
    val deviceName: String?
)

enum class HardwareAccelerationType {
    VIDEOTOOLBOX,  // macOS
    VAAPI,         // Linux
    VDPAU,         // Linux (NVIDIA)
    DXVA2,         // Windows
    D3D11VA,       // Windows
    NONE           // Software decoding
}
```

### 6. AudioVideoSynchronizer

管理音视频同步。

```kotlin
class AudioVideoSynchronizer(
    private val audioClock: AudioClock
) {
    private val syncThreshold = 40L // 40ms
    private val maxSyncDiff = 1000L // 1 second
    
    /**
     * 计算视频帧应该延迟的时间
     */
    fun calculateVideoDelay(videoTimestamp: Long): Long {
        val audioTime = audioClock.getTime()
        val diff = videoTimestamp - audioTime
        
        return when {
            diff > syncThreshold -> diff // 视频慢了，需要等待
            diff < -syncThreshold -> 0L  // 视频快了，立即显示
            else -> 0L // 在同步范围内
        }
    }
    
    /**
     * 判断是否需要丢帧
     */
    fun shouldDropFrame(videoTimestamp: Long): Boolean {
        val audioTime = audioClock.getTime()
        val diff = audioTime - videoTimestamp
        return diff > maxSyncDiff
    }
}
```

### 7. StreamTypeDetector

检测流类型和格式。

```kotlin
object StreamTypeDetector {
    
    /**
     * 检测是否为直播流
     */
    fun isLiveStream(url: String): Boolean {
        return url.contains("live") || 
               url.endsWith(".m3u8") ||
               url.startsWith("rtsp://")
    }
    
    /**
     * 检测流协议
     */
    fun detectProtocol(url: String): StreamProtocol
    
    /**
     * 获取推荐的缓冲配置
     */
    fun getBufferConfiguration(url: String): BufferConfiguration
}

enum class StreamProtocol {
    HTTP, HTTPS, RTSP, RTMP, FILE, HLS
}

data class BufferConfiguration(
    val probeSize: Int,
    val maxAnalyzeDuration: Long,
    val bufferSize: Int
)
```


## Data Models

### 1. PlayerState

播放器状态（保持与现有实现兼容）。

```kotlin
data class PlayerState(
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val position: Long = 0L,
    val duration: Long = 0L,
    val volume: Float = 1.0f,
    val errorMessage: String? = null
)

enum class PlaybackState {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR
}
```

### 2. MediaInfo

媒体信息。

```kotlin
data class MediaInfo(
    val duration: Long,
    val videoCodec: String,
    val audioCodec: String,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoFrameRate: Double,
    val audioBitrate: Int,
    val videoBitrate: Int,
    val audioChannels: Int,
    val audioSampleRate: Int
)
```

### 3. PlaybackStatistics

播放统计信息。

```kotlin
data class PlaybackStatistics(
    val framesDecoded: Long = 0,
    val framesRendered: Long = 0,
    val framesDropped: Long = 0,
    val currentFps: Double = 0.0,
    val bufferLevel: Int = 0,
    val syncDrift: Long = 0,
    val cpuUsage: Double = 0.0,
    val memoryUsage: Long = 0
)
```

### 4. AudioClock

音频时钟，用于音视频同步。

```kotlin
class AudioClock {
    private var timestamp: AtomicLong = AtomicLong(0)
    private var startTime: Long = 0
    
    fun start() {
        startTime = System.currentTimeMillis()
    }
    
    fun update(audioTimestamp: Long) {
        timestamp.set(audioTimestamp)
    }
    
    fun getTime(): Long {
        return timestamp.get()
    }
    
    fun reset() {
        timestamp.set(0)
        startTime = 0
    }
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Property 1: FFmpeg 解码初始化
*For any* 有效的媒体 URL，调用播放功能应该成功初始化 FFmpeg 解码器并开始解码帧
**Validates: Requirements 1.1**

Property 2: 视频帧渲染
*For any* 解码的视频帧，应该被渲染到 Canvas 组件上
**Validates: Requirements 1.2**

Property 3: 音频帧播放
*For any* 解码的音频帧，应该通过音频输出设备播放
**Validates: Requirements 1.3**

Property 4: 错误处理和通知
*For any* 播放过程中发生的错误，应该被记录并通知用户
**Validates: Requirements 1.4**

Property 5: 资源释放完整性
*For any* 停止播放操作，所有分配的 FFmpeg 资源应该被正确释放
**Validates: Requirements 1.5, 8.1**

Property 6: 播放控制响应
*For any* 播放控制命令（播放、暂停），播放器状态应该相应改变
**Validates: Requirements 2.1, 2.2**

Property 7: 跳转功能
*For any* 有效的时间位置，跳转操作应该改变当前播放位置
**Validates: Requirements 2.3**

Property 8: 音量控制
*For any* 音量值（0.0-1.0），音频输出音量应该相应改变
**Validates: Requirements 2.4**

Property 9: 控制命令错误处理
*For any* 失败的播放控制命令，错误应该被记录且播放器状态保持一致
**Validates: Requirements 2.5**

Property 10: 硬件加速检测和启用
*For any* 支持硬件加速的系统，硬件解码应该被自动检测并启用
**Validates: Requirements 4.1**

Property 11: 硬件加速回退
*For any* 硬件加速失败的情况，系统应该自动回退到软件解码
**Validates: Requirements 4.5**

Property 12: 直播流低延迟缓冲
*For any* 直播流 URL，系统应该使用较小的缓冲区配置
**Validates: Requirements 5.1**

Property 13: 网络抖动自适应
*For any* 检测到的网络抖动，缓冲区大小应该动态调整
**Validates: Requirements 5.2**

Property 14: 延迟累积跳帧
*For any* 直播流延迟累积超过阈值，系统应该丢弃视频帧以追赶进度
**Validates: Requirements 5.3**

Property 15: 直播流重连
*For any* 直播流连接中断，系统应该自动尝试重连
**Validates: Requirements 5.4**

Property 16: 直播流恢复
*For any* 直播流重连成功，播放应该从最新位置继续
**Validates: Requirements 5.5**

Property 17: 音视频同步
*For any* 播放中的音视频内容，音视频时间戳差异应该在可接受范围内（< 40ms）
**Validates: Requirements 6.1**

Property 18: 同步恢复机制
*For any* 检测到的音视频不同步，系统应该调整播放速度或丢帧以恢复同步
**Validates: Requirements 6.2, 6.3, 6.4**

Property 19: 同步误差监控
*For any* 超过阈值的同步误差，警告信息应该被记录
**Validates: Requirements 6.5**

Property 20: 播放信息日志
*For any* 开始播放的媒体，日志应该包含格式、编解码器和流信息
**Validates: Requirements 7.1**

Property 21: 播放统计更新
*For any* 正在播放的媒体，帧率、比特率和缓冲状态应该定期更新
**Validates: Requirements 7.2**

Property 22: 解码错误日志
*For any* 发生的解码错误，日志应该包含错误类型、时间戳和上下文信息
**Validates: Requirements 7.3**

Property 23: 性能监控
*For any* 检测到的性能问题，CPU 使用率和内存占用应该被记录
**Validates: Requirements 7.4**

Property 24: 资源切换管理
*For any* 视频切换操作，旧资源应该在新资源分配前被释放
**Validates: Requirements 8.2**

Property 25: 应用退出清理
*For any* 应用退出操作，所有播放器资源应该被释放
**Validates: Requirements 8.3**

Property 26: 空闲资源释放
*For any* 播放器空闲超过阈值，非必要资源应该被释放
**Validates: Requirements 8.4**

Property 27: 资源释放错误处理
*For any* 资源释放失败，错误应该被记录并尝试强制清理
**Validates: Requirements 8.5**

Property 28: 全屏模式切换
*For any* 全屏模式切换操作，视频应该渲染到正确的目标（全屏窗口或普通窗口）
**Validates: Requirements 10.1**

Property 29: 全屏宽高比保持
*For any* 全屏模式下的视频，宽高比应该被正确保持
**Validates: Requirements 10.2**

Property 30: 全屏退出恢复
*For any* 全屏退出操作，播放应该在窗口模式下继续
**Validates: Requirements 10.3**

Property 31: 全屏尺寸自适应
*For any* 全屏模式下的窗口大小变化，视频渲染尺寸应该相应调整
**Validates: Requirements 10.4**

Property 32: 全屏切换错误处理
*For any* 全屏切换失败，错误应该被记录且当前模式保持不变
**Validates: Requirements 10.5**


## Error Handling

### 1. 解码错误处理

**问题**: FFmpeg 解码可能因格式不支持、数据损坏等原因失败

**解决方案**:
```kotlin
fun handleDecodingError(exception: Exception, frame: Frame?) {
    val errorCategory = categorizeDecodingError(exception)
    
    when (errorCategory) {
        DecodingErrorType.UNSUPPORTED_FORMAT -> {
            onError("不支持的媒体格式: ${exception.message}")
            stop()
        }
        DecodingErrorType.CORRUPTED_DATA -> {
            // 跳过损坏的帧，继续播放
            println("跳过损坏的帧: ${frame?.timestamp}")
            statistics.framesDropped++
        }
        DecodingErrorType.NETWORK_ERROR -> {
            // 尝试重连
            attemptReconnection()
        }
        else -> {
            onError("解码错误: ${exception.message}")
        }
    }
}
```

### 2. 资源释放失败处理

**问题**: FFmpeg 资源释放可能因为线程状态、资源锁定等原因失败

**解决方案**:
```kotlin
fun safeRelease() {
    try {
        // 1. 停止所有线程
        isPlaying.set(false)
        decoderThread?.join(1000)
        rendererThread?.join(1000)
        audioThread?.join(1000)
        
        // 2. 清空队列
        videoFrameQueue.clear()
        audioFrameQueue.clear()
        
        // 3. 释放 FFmpeg 资源
        grabber?.stop()
        grabber?.release()
        
        // 4. 释放音频资源
        sourceDataLine?.stop()
        sourceDataLine?.close()
        
    } catch (e: Exception) {
        println("资源释放失败: ${e.message}")
        
        // 强制清理
        try {
            decoderThread?.interrupt()
            rendererThread?.interrupt()
            audioThread?.interrupt()
            
            // 强制释放
            grabber = null
            sourceDataLine = null
        } catch (e2: Exception) {
            println("强制清理失败: ${e2.message}")
        }
    }
}
```

### 3. 音视频不同步处理

**问题**: 音视频可能因为解码速度、网络延迟等原因不同步

**解决方案**:
```kotlin
fun handleSyncIssue(videoTimestamp: Long, audioTimestamp: Long) {
    val diff = videoTimestamp - audioTimestamp
    
    when {
        diff > 1000 -> {
            // 视频严重落后，丢帧追赶
            println("视频严重落后 ${diff}ms，开始丢帧")
            dropFramesUntilSync()
        }
        diff < -1000 -> {
            // 视频严重超前，暂停等待
            println("视频严重超前 ${-diff}ms，暂停等待")
            Thread.sleep((-diff).coerceAtMost(100))
        }
        diff > 40 -> {
            // 轻微不同步，调整延迟
            Thread.sleep(diff)
        }
    }
}
```

### 4. 网络中断处理

**问题**: 网络流可能因为连接中断、超时等原因失败

**解决方案**:
```kotlin
fun handleNetworkInterruption() {
    val maxRetries = 3
    var retryCount = 0
    
    while (retryCount < maxRetries && isPlaying.get()) {
        try {
            println("尝试重连 (${retryCount + 1}/$maxRetries)...")
            
            // 保存当前位置
            val currentPosition = grabber?.timestamp ?: 0
            
            // 重新初始化
            grabber?.stop()
            grabber?.release()
            grabber = FFmpegFrameGrabber(url)
            configureGrabber(grabber!!)
            grabber!!.start()
            
            // 跳转到之前的位置
            if (currentPosition > 0) {
                grabber!!.timestamp = currentPosition
            }
            
            println("重连成功")
            return
            
        } catch (e: Exception) {
            retryCount++
            println("重连失败: ${e.message}")
            Thread.sleep(1000 * retryCount) // 指数退避
        }
    }
    
    // 所有重试都失败
    onError("网络连接失败，无法恢复播放")
    stop()
}
```

## Testing Strategy

### 单元测试

**FFmpegPlayerEngine 测试**:
- 测试播放器初始化和资源分配
- 测试播放控制命令（播放、暂停、跳转、音量）
- 测试资源释放的完整性
- 测试错误处理逻辑

**HardwareAccelerationManager 测试**:
- 测试硬件加速检测逻辑
- 测试平台特定配置
- 测试回退机制

**AudioVideoSynchronizer 测试**:
- 测试同步延迟计算
- 测试丢帧判断逻辑
- 测试同步阈值

**StreamTypeDetector 测试**:
- 测试直播流检测
- 测试协议识别
- 测试缓冲配置生成

### 集成测试

**端到端播放测试**:
- 使用测试视频文件验证完整播放流程
- 测试不同格式和协议的支持
- 测试播放控制的响应
- 验证音视频同步

**硬件加速测试**:
- 在支持硬件加速的平台上测试
- 验证硬件解码的启用
- 测试回退到软件解码

**直播流测试**:
- 测试 HLS 流播放
- 测试 RTSP 流播放
- 验证低延迟配置
- 测试重连机制

### 属性测试

使用 Kotest property testing 框架：

**Property 1: FFmpeg 解码初始化**
```kotlin
"FFmpeg decoder should initialize for valid URLs" {
    checkAll(Arb.validMediaUrl()) { url ->
        val engine = FFmpegPlayerEngine(onStateChange = {}, onError = {})
        val canvas = Canvas()
        
        shouldNotThrowAny {
            engine.play(url, canvas)
        }
        
        // 验证解码器已初始化
        engine.isInitialized() shouldBe true
        
        engine.release()
    }
}
```

**Property 17: 音视频同步**
```kotlin
"audio and video should stay synchronized" {
    checkAll(Arb.mediaUrl(), Arb.duration(10000, 60000)) { url, duration ->
        val engine = FFmpegPlayerEngine(onStateChange = {}, onError = {})
        val canvas = Canvas()
        
        engine.play(url, canvas)
        delay(duration)
        
        val syncDrift = engine.getSyncDrift()
        syncDrift shouldBeLessThan 40L // 同步误差应小于 40ms
        
        engine.release()
    }
}
```

**Property 5: 资源释放完整性**
```kotlin
"all resources should be released after stop" {
    checkAll(Arb.validMediaUrl()) { url ->
        val engine = FFmpegPlayerEngine(onStateChange = {}, onError = {})
        val canvas = Canvas()
        
        engine.play(url, canvas)
        delay(1000)
        engine.stop()
        
        // 验证资源已释放
        engine.hasActiveThreads() shouldBe false
        engine.hasAllocatedResources() shouldBe false
    }
}
```

## Implementation Notes

### 1. JavaCV 依赖配置

在 `build.gradle.kts` 中添加 JavaCV 依赖：

```kotlin
dependencies {
    // JavaCV 核心库
    implementation("org.bytedeco:javacv-platform:1.5.9")
    
    // FFmpeg 平台特定库
    implementation("org.bytedeco:ffmpeg-platform:6.0-1.5.9")
    
    // 可选：OpenCV（如果需要图像处理）
    // implementation("org.bytedeco:opencv-platform:4.7.0-1.5.9")
}
```

### 2. FFmpegFrameGrabber 配置

关键配置选项：

```kotlin
fun configureGrabber(grabber: FFmpegFrameGrabber, isLiveStream: Boolean) {
    // 基础配置
    grabber.format = "auto" // 自动检测格式
    
    if (isLiveStream) {
        // 直播流优化
        grabber.option("fflags", "nobuffer")
        grabber.option("flags", "low_delay")
        grabber.option("probesize", "32")
        grabber.option("analyzeduration", "0")
        grabber.option("sync", "ext")
    } else {
        // VOD 优化
        grabber.option("probesize", "5000000")
        grabber.option("analyzeduration", "5000000")
    }
    
    // 硬件加速配置
    val hwAccel = HardwareAccelerationManager.detectHardwareAcceleration()
    if (hwAccel.isAvailable) {
        when (hwAccel.type) {
            HardwareAccelerationType.VIDEOTOOLBOX -> {
                grabber.videoCodec = avcodec.AV_CODEC_ID_H264
                grabber.option("hwaccel", "videotoolbox")
            }
            HardwareAccelerationType.VAAPI -> {
                grabber.option("hwaccel", "vaapi")
                grabber.option("hwaccel_device", "/dev/dri/renderD128")
            }
            HardwareAccelerationType.DXVA2 -> {
                grabber.option("hwaccel", "dxva2")
            }
            else -> {
                // 软件解码
            }
        }
    }
}
```

### 3. 视频帧渲染优化

使用双缓冲减少闪烁：

```kotlin
class VideoRenderer(private val canvas: Canvas) {
    private val converter = Java2DFrameConverter()
    private var backBuffer: BufferedImage? = null
    
    fun renderFrame(frame: Frame) {
        val image = converter.convert(frame)
        
        // 缩放到 Canvas 尺寸
        if (backBuffer == null || 
            backBuffer!!.width != canvas.width || 
            backBuffer!!.height != canvas.height) {
            backBuffer = BufferedImage(
                canvas.width, 
                canvas.height, 
                BufferedImage.TYPE_INT_RGB
            )
        }
        
        // 绘制到后缓冲
        val g = backBuffer!!.createGraphics()
        g.drawImage(image, 0, 0, canvas.width, canvas.height, null)
        g.dispose()
        
        // 交换缓冲
        val canvasGraphics = canvas.graphics
        canvasGraphics.drawImage(backBuffer, 0, 0, null)
        canvasGraphics.dispose()
    }
}
```

### 4. 音频播放配置

```kotlin
fun initializeAudioLine(audioFormat: AudioFormat): SourceDataLine {
    val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
    val line = AudioSystem.getLine(info) as SourceDataLine
    
    // 配置缓冲区大小
    val bufferSize = audioFormat.sampleRate.toInt() / 10 // 100ms 缓冲
    line.open(audioFormat, bufferSize)
    line.start()
    
    return line
}
```


## Performance Considerations

### 1. 线程管理

使用三个独立线程处理解码、渲染和音频播放：

- **Decoder Thread**: 高优先级，确保持续解码
- **Renderer Thread**: 正常优先级，根据音频时钟调整
- **Audio Thread**: 最高优先级，确保音频流畅

```kotlin
decoderThread = Thread(decoder, "FFmpeg-Decoder").apply {
    priority = Thread.NORM_PRIORITY + 1
    start()
}

rendererThread = Thread(renderer, "FFmpeg-Renderer").apply {
    priority = Thread.NORM_PRIORITY
    start()
}

audioThread = Thread(audioPlayer, "FFmpeg-Audio").apply {
    priority = Thread.MAX_PRIORITY
    start()
}
```

### 2. 队列大小优化

根据内容类型调整队列大小：

- **直播流**: 较小队列（视频 10 帧，音频 50 帧）以减少延迟
- **VOD**: 较大队列（视频 30 帧，音频 100 帧）以提高流畅度

### 3. 内存管理

及时释放帧对象：

```kotlin
fun processFrame(frame: Frame) {
    try {
        // 处理帧
        renderFrame(frame)
    } finally {
        // 释放帧资源
        frame.close()
    }
}
```

### 4. 硬件加速优化

优先使用硬件加速以降低 CPU 使用率：

- macOS: VideoToolbox（H.264/HEVC）
- Linux: VAAPI（Intel/AMD）、VDPAU（NVIDIA）
- Windows: DXVA2、D3D11VA

## Security Considerations

### 1. URL 验证

验证媒体 URL 的安全性：

```kotlin
fun validateUrl(url: String): Boolean {
    // 检查协议白名单
    val allowedProtocols = listOf("http", "https", "rtsp", "file")
    val protocol = url.substringBefore("://").lowercase()
    
    if (protocol !in allowedProtocols) {
        return false
    }
    
    // 防止路径遍历（file:// 协议）
    if (protocol == "file") {
        val path = url.substringAfter("file://")
        if (path.contains("..")) {
            return false
        }
    }
    
    return true
}
```

### 2. 资源限制

限制资源使用以防止 DoS：

```kotlin
// 限制队列大小
private val maxQueueSize = 100

// 限制解码线程数
private val maxDecoderThreads = 1

// 超时保护
private val connectionTimeout = 10000L // 10 seconds
private val readTimeout = 5000L // 5 seconds
```

### 3. 异常处理

捕获并处理所有 FFmpeg 异常，避免崩溃：

```kotlin
try {
    grabber.start()
} catch (e: Exception) {
    when (e) {
        is FFmpegFrameGrabber.Exception -> {
            // FFmpeg 特定错误
            handleFFmpegError(e)
        }
        is IOException -> {
            // 网络或文件错误
            handleIOError(e)
        }
        else -> {
            // 其他错误
            handleGenericError(e)
        }
    }
}
```

## Migration Strategy

### 阶段 1: 并行实现（1-2 周）

1. 创建新的 `FFmpegVideoPlayer.desktop.kt` 文件
2. 实现基础播放功能
3. 保持现有 VLC 实现不变
4. 通过配置开关选择使用哪个实现

```kotlin
// 在 Koin 配置中添加开关
single<VideoPlayerImplementation> {
    if (getProperty("use.ffmpeg.player", "false").toBoolean()) {
        FFmpegPlayerImplementation()
    } else {
        VlcPlayerImplementation()
    }
}
```

### 阶段 2: 功能对齐（1 周）

1. 实现所有播放控制功能
2. 实现硬件加速支持
3. 实现音视频同步
4. 添加完整的错误处理

### 阶段 3: 测试和优化（1-2 周）

1. 编写单元测试和集成测试
2. 进行性能测试和优化
3. 修复发现的 bug
4. 优化内存使用和 CPU 占用

### 阶段 4: 逐步迁移（1 周）

1. 默认启用 FFmpeg 实现
2. 保留 VLC 作为备选方案
3. 收集用户反馈
4. 根据反馈进行调整

### 阶段 5: 完全迁移（1 周）

1. 移除 VLC 依赖
2. 清理相关代码
3. 更新文档
4. 发布新版本

### 向后兼容

保持 API 完全兼容：

```kotlin
// 现有 API
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit,
    onError: (String) -> Unit,
    onPlayerInitFailed: () -> Unit,
    isFullscreen: Boolean
) {
    // 内部实现改为使用 FFmpeg
    FFmpegVideoPlayerImpl(
        url = url,
        modifier = modifier,
        playerState = playerState,
        onPlayerControls = onPlayerControls,
        onError = onError,
        onPlayerInitFailed = onPlayerInitFailed,
        isFullscreen = isFullscreen
    )
}
```

## Monitoring and Logging

### 日志级别

- **ERROR**: 播放失败、解码错误、资源释放失败
- **WARN**: 同步问题、性能警告、重连尝试
- **INFO**: 播放开始/停止、格式信息、硬件加速状态
- **DEBUG**: 帧统计、缓冲状态、详细时序信息

### 关键指标

- 播放初始化成功率
- 平均首帧时间
- 帧率和丢帧率
- 音视频同步误差
- CPU 和内存使用
- 硬件加速使用率
- 网络重连次数

### 诊断报告

生成详细的诊断报告：

```kotlin
fun generateDiagnosticReport(): String {
    return buildString {
        appendLine("=== FFmpeg Player Diagnostic Report ===")
        appendLine("Timestamp: ${System.currentTimeMillis()}")
        appendLine()
        
        appendLine("System Information:")
        appendLine("  OS: ${System.getProperty("os.name")}")
        appendLine("  Java Version: ${System.getProperty("java.version")}")
        appendLine("  JavaCV Version: 1.5.9")
        appendLine("  FFmpeg Version: 6.0")
        appendLine()
        
        appendLine("Hardware Acceleration:")
        val hwAccel = HardwareAccelerationManager.detectHardwareAcceleration()
        appendLine("  Type: ${hwAccel.type}")
        appendLine("  Available: ${hwAccel.isAvailable}")
        appendLine("  Device: ${hwAccel.deviceName ?: "N/A"}")
        appendLine()
        
        appendLine("Media Information:")
        appendLine("  URL: $url")
        appendLine("  Duration: ${mediaInfo?.duration ?: "Unknown"}")
        appendLine("  Video Codec: ${mediaInfo?.videoCodec ?: "Unknown"}")
        appendLine("  Audio Codec: ${mediaInfo?.audioCodec ?: "Unknown"}")
        appendLine("  Resolution: ${mediaInfo?.videoWidth}x${mediaInfo?.videoHeight}")
        appendLine("  Frame Rate: ${mediaInfo?.videoFrameRate}")
        appendLine()
        
        appendLine("Playback Statistics:")
        appendLine("  Frames Decoded: ${statistics.framesDecoded}")
        appendLine("  Frames Rendered: ${statistics.framesRendered}")
        appendLine("  Frames Dropped: ${statistics.framesDropped}")
        appendLine("  Current FPS: ${statistics.currentFps}")
        appendLine("  Buffer Level: ${statistics.bufferLevel}")
        appendLine("  Sync Drift: ${statistics.syncDrift}ms")
        appendLine("  CPU Usage: ${statistics.cpuUsage}%")
        appendLine("  Memory Usage: ${statistics.memoryUsage / 1024 / 1024}MB")
        appendLine()
        
        appendLine("Thread Status:")
        appendLine("  Decoder Thread: ${decoderThread?.state}")
        appendLine("  Renderer Thread: ${rendererThread?.state}")
        appendLine("  Audio Thread: ${audioThread?.state}")
        appendLine()
        
        appendLine("Queue Status:")
        appendLine("  Video Queue: ${videoFrameQueue.size}/${videoFrameQueue.remainingCapacity()}")
        appendLine("  Audio Queue: ${audioFrameQueue.size}/${audioFrameQueue.remainingCapacity()}")
        appendLine()
        
        appendLine("==========================================")
    }
}
```

## Comparison with VLC Implementation

### 优势

1. **更好的控制**: 直接访问解码和渲染流程
2. **更低的延迟**: 可以针对直播流优化缓冲策略
3. **更灵活的同步**: 自定义音视频同步算法
4. **更好的诊断**: 访问底层统计信息
5. **更小的依赖**: 不需要安装外部 VLC 应用

### 劣势

1. **实现复杂度**: 需要手动管理线程和同步
2. **初期稳定性**: 新实现可能存在未发现的 bug
3. **维护成本**: 需要维护更多代码

### 性能对比

预期性能指标：

| 指标 | VLC | FFmpeg | 改进 |
|------|-----|--------|------|
| 首帧时间 | 500-1000ms | 300-600ms | 40% |
| CPU 使用率 | 15-25% | 10-20% | 30% |
| 内存占用 | 150-200MB | 100-150MB | 30% |
| 直播延迟 | 2-3s | 0.5-1s | 60% |

## Risks and Mitigation

### 风险 1: 音视频不同步

**缓解措施**:
- 实现健壮的同步算法
- 添加详细的同步监控
- 提供手动同步调整选项

### 风险 2: 内存泄漏

**缓解措施**:
- 严格的资源管理
- 自动化测试检测泄漏
- 定期内存分析

### 风险 3: 平台兼容性问题

**缓解措施**:
- 在所有目标平台上测试
- 提供软件解码回退
- 保留 VLC 作为备选方案

### 风险 4: 性能问题

**缓解措施**:
- 性能基准测试
- 优化关键路径
- 使用硬件加速

