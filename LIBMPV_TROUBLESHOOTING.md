# libmpv 故障排除指南

## 常见问题和解决方案

### 1. 音频解码错误

#### 症状
```
libmpv [error] ffmpeg/audio: aac: channel element X.Y is not allocated
libmpv [error] ad: Error decoding audio.
libmpv [error] ffmpeg/audio: aac: Number of bands (X) exceeds limit (Y).
libmpv [error] ffmpeg/audio: aac: Reserved bit set.
libmpv [error] ffmpeg/audio: aac: Prediction is not allowed in AAC-LC.
```

#### 原因
- IPTV 流的音频编码不规范或损坏
- AAC 音频流包含非法数据
- 流提供商的编码器配置错误

#### 解决方案

##### 方案 1: 使用容错配置（已自动启用）

应用已自动配置以下容错选项：

```kotlin
// 音频解码容错
setOption("ad-lavc-downmix", "yes")
setOption("ad-lavc-threads", "auto")
setOption("audio-samplerate", "48000")
setOption("audio-fallback-to-null", "yes")
setOption("ad-lavc-o", "err_detect=ignore_err")

// FFmpeg 容错
setOption("demuxer-lavf-o", "fflags=+genpts+igndts+ignidx")
setOption("demuxer-lavf-o-append", "err_detect=ignore_err")
```

##### 方案 2: 禁用音频（仅播放视频）

如果音频严重损坏无法修复，可以禁用音频：

```kotlin
// 在初始化时使用 VIDEO_ONLY 配置
val engine = LibmpvPlayerEngine()
engine.initialize(LibmpvConfiguration.VIDEO_ONLY)
```

或者在运行时禁用：

```kotlin
engine.setPropertyString("aid", "no")  // 禁用音频轨道
```

##### 方案 3: 尝试不同的音频输出

```kotlin
// 使用空音频输出
engine.setPropertyString("ao", "null")

// 或者尝试其他音频输出
engine.setPropertyString("ao", "coreaudio")  // macOS
engine.setPropertyString("ao", "pulse")      // Linux
engine.setPropertyString("ao", "wasapi")     // Windows
```

#### 预期结果

- **最佳情况**: 音频错误被忽略，视频和音频都能播放
- **次佳情况**: 音频断断续续，但视频正常
- **最差情况**: 需要禁用音频，仅播放视频

---

### 2. 视频黑屏

#### 症状
- 音频正常但视频不显示
- 或者视频窗口是黑色的

#### 解决方案

```kotlin
// 尝试不同的视频输出
engine.setPropertyString("vo", "gpu")
engine.setPropertyString("vo", "x11")      // Linux
engine.setPropertyString("vo", "direct3d") // Windows

// 禁用硬件加速
engine.setPropertyString("hwdec", "no")
```

---

### 3. 播放卡顿

#### 症状
- 视频播放不流畅
- 音视频不同步

#### 解决方案

```kotlin
// 增加缓存
engine.setPropertyString("cache-secs", "20")
engine.setPropertyString("demuxer-readahead-secs", "10")

// 降低视频质量
engine.setPropertyString("vd-lavc-threads", "2")
engine.setPropertyString("scale", "bilinear")
```

---

### 4. 网络连接问题

#### 症状
```
libmpv [error] ffmpeg: http: HTTP error 404 Not Found
libmpv [error] ffmpeg: http: Connection timed out
```

#### 解决方案

```kotlin
// 增加超时时间
engine.setPropertyString("network-timeout", "60")

// 启用重连
engine.setPropertyString("stream-lavf-o", 
    "reconnect=1,reconnect_streamed=1,reconnect_delay_max=10")

// 设置 User-Agent
engine.setPropertyString("user-agent", "Mozilla/5.0")
```

---

### 5. 内存泄漏

#### 症状
- 应用运行时间长后内存占用持续增长

#### 解决方案

```kotlin
// 确保正确释放资源
override fun onDispose() {
    engine.stop()
    engine.destroy()
    renderer.release()
}

// 限制缓存大小
engine.setPropertyString("demuxer-max-bytes", "50M")
engine.setPropertyString("demuxer-max-back-bytes", "25M")
```

---

## 调试技巧

### 1. 启用详细日志

```kotlin
// 在初始化时设置日志级别
val config = LibmpvConfiguration.DEFAULT.copy(
    logLevel = "debug"  // 或 "trace" 获取更多信息
)
engine.initialize(config)
```

### 2. 查看流信息

```bash
# 使用 ffprobe 检查流
ffprobe -v error -show_format -show_streams "YOUR_STREAM_URL"

# 使用 mpv 命令行测试
mpv --log-file=mpv.log "YOUR_STREAM_URL"
```

### 3. 测试不同的流

```kotlin
// 测试已知良好的流
val testUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
engine.loadFile(testUrl)
```

---

## 特定流类型的配置

### HLS 流

```kotlin
val config = LibmpvConfiguration.DEFAULT.copy(
    cacheSize = 100000,
    cacheSecs = 10,
    demuxerReadahead = 5
)

// 选择最高质量
engine.setPropertyString("hls-bitrate", "max")
```

### RTSP 流

```kotlin
val config = LibmpvConfiguration.DEFAULT.copy(
    cacheSize = 50000,
    cacheSecs = 5,
    demuxerReadahead = 2,
    networkTimeout = 15
)

// RTSP 特定选项
engine.setPropertyString("rtsp-transport", "tcp")
```

### HTTP 流

```kotlin
val config = LibmpvConfiguration.DEFAULT.copy(
    cacheSize = 150000,
    cacheSecs = 15,
    networkTimeout = 30
)

// HTTP 特定选项
engine.setPropertyString("http-header-fields", 
    "Connection: keep-alive")
```

---

## 性能优化

### 低延迟配置

```kotlin
val config = LibmpvConfiguration.LOW_LATENCY.copy(
    cacheSize = 25000,
    cacheSecs = 2,
    demuxerReadahead = 1
)

engine.setPropertyString("video-sync", "audio")
engine.setPropertyString("interpolation", "no")
```

### 高质量配置

```kotlin
val config = LibmpvConfiguration.HIGH_QUALITY.copy(
    cacheSize = 300000,
    cacheSecs = 30,
    demuxerReadahead = 15
)

engine.setPropertyString("scale", "ewa_lanczossharp")
engine.setPropertyString("cscale", "ewa_lanczossharp")
```

---

## 平台特定问题

### macOS

#### 问题: 硬件加速不工作
```kotlin
// 使用 VideoToolbox
engine.setPropertyString("hwdec", "videotoolbox")
engine.setPropertyString("hwdec-codecs", "h264,hevc")
```

#### 问题: 音频输出问题
```kotlin
// 使用 CoreAudio
engine.setPropertyString("ao", "coreaudio")
engine.setPropertyString("audio-device", "auto")
```

### Linux

#### 问题: 视频撕裂
```kotlin
// 使用 VAAPI
engine.setPropertyString("hwdec", "vaapi")
engine.setPropertyString("vo", "gpu")
```

#### 问题: 音频延迟
```kotlin
// 使用 PulseAudio
engine.setPropertyString("ao", "pulse")
engine.setPropertyString("audio-buffer", "0.2")
```

### Windows

#### 问题: 性能问题
```kotlin
// 使用 D3D11VA
engine.setPropertyString("hwdec", "d3d11va")
engine.setPropertyString("vo", "gpu")
```

---

## 错误代码参考

| 错误代码 | 含义 | 解决方案 |
|---------|------|---------|
| -1 | 事件队列满 | 增加事件处理速度 |
| -2 | 内存不足 | 减少缓存大小 |
| -4 | 无效参数 | 检查配置选项 |
| -8 | 属性未找到 | 检查属性名称 |
| -12 | 命令错误 | 检查命令语法 |
| -13 | 加载失败 | 检查网络和 URL |
| -16 | 无内容播放 | 检查流是否有效 |

---

## 获取帮助

### 收集诊断信息

```kotlin
// 获取 libmpv 版本
val version = engine.getBindings()?.mpv_client_api_version()
println("libmpv API version: $version")

// 获取播放信息
val duration = engine.getDuration()
val position = engine.getPosition()
val videoWidth = engine.getVideoWidth()
val videoHeight = engine.getVideoHeight()

println("Duration: $duration")
println("Position: $position")
println("Resolution: ${videoWidth}x${videoHeight}")
```

### 提交问题时包含

1. 操作系统和版本
2. libmpv 版本
3. 完整的错误日志
4. 流 URL（如果可以公开）
5. 复现步骤

---

## 相关资源

- [libmpv 官方文档](https://mpv.io/manual/master/#libmpv)
- [FFmpeg 文档](https://ffmpeg.org/documentation.html)
- [IPTV 流测试工具](https://github.com/iptv-org/iptv)

---

**最后更新**: 2024-11-28
