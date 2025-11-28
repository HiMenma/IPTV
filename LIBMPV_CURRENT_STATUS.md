# libmpv 播放器当前状态

## ✅ 已修复的问题

### 1. API 符号解析错误
- **问题**: `mpv_get_property_double` 等函数不存在
- **状态**: ✅ 已修复
- **方案**: 使用通用的 `mpv_get_property` 和 `mpv_set_property` API

### 2. 音频输出
- **问题**: 视频播放没有声音
- **状态**: ✅ 已修复
- **方案**: 
  - 配置音频输出：`ao=auto`
  - 设置立体声通道：`audio-channels=stereo`
  - 设置音量：`volume=100`

### 3. 视频质量
- **问题**: 视频分辨率不清晰
- **状态**: ✅ 已修复
- **方案**:
  - 选择最佳质量：`ytdl-format=bestvideo+bestaudio/best`
  - HLS 最高码率：`hls-bitrate=max`

## ⚠️ 待解决的问题

### 4. 嵌入式播放
- **问题**: 视频在独立窗口播放，而不是应用内嵌入
- **状态**: ⚠️ 待实现
- **原因**: libmpv 软件渲染模式在 macOS 上崩溃
- **临时方案**: 使用 `vo=gpu` 模式（独立窗口）

## 当前配置

```kotlin
// LibmpvPlayerEngine.kt - applyConfiguration()

// 视频输出 - 独立窗口模式
setOption("vo", config.videoOutput)  // "gpu"

// 音频输出
setOption("ao", config.audioOutput)  // "auto"
setOption("audio-channels", "stereo")
setOption("volume", config.volume.toString())

// 硬件加速
setOption("hwdec", config.hwdecMethod)  // "auto"

// 视频质量
setOption("ytdl-format", "bestvideo+bestaudio/best")
setOption("hls-bitrate", "max")

// 缓存设置
setOption("cache", "yes")
setOption("demuxer-max-bytes", "${config.cacheSize * 1024}")
setOption("cache-secs", config.cacheSecs.toString())
setOption("demuxer-readahead-secs", config.demuxerReadahead.toString())

// 网络设置
setOption("network-timeout", config.networkTimeout.toString())
setOption("user-agent", config.userAgent)
```

## 测试方法

运行应用并播放视频：
```bash
./gradlew :composeApp:run
```

### 验证音频
- 播放视频时应该能听到声音
- 音量控制应该正常工作

### 验证视频质量
- 视频应该自动选择最高质量的流
- 对于 HLS 流，应该选择最高码率

### 验证播放稳定性
- 视频应该能正常播放，不崩溃
- 查看日志确认没有错误

## 日志输出示例

正常播放时的日志：
```
=== libmpv VideoPlayer Initialization ===
URL: http://example.com/stream.m3u8
Fullscreen: false
✓ Configuration applied successfully
  - Video output: gpu (separate window)
  - Audio output: auto
  - Hardware acceleration: auto
  - Video quality: best available
✓ Player controls provided
==========================================
Loading new URL: http://example.com/stream.m3u8
Media loaded successfully: http://example.com/stream.m3u8
Event: Start file
Event: File loaded
```

## 下一步计划

### 短期（保持当前功能）
1. ✅ 确保音频正常工作
2. ✅ 确保视频质量最佳
3. ✅ 添加详细的错误日志
4. ⏳ 测试不同类型的流（HLS, RTSP, HTTP）

### 中期（改进用户体验）
1. ⏳ 实现嵌入式播放（解决渲染上下文问题）
2. ⏳ 添加播放控制 UI（进度条、音量控制等）
3. ⏳ 优化缓冲策略
4. ⏳ 添加字幕支持

### 长期（高级功能）
1. ⏳ 多音轨切换
2. ⏳ 视频录制
3. ⏳ 截图功能
4. ⏳ 播放列表管理

## 已知限制

1. **独立窗口播放**: 视频在独立窗口显示，不在应用内嵌入
2. **macOS 特定**: 软件渲染模式在 macOS 上不稳定
3. **渲染 API**: 需要进一步研究 libmpv 渲染 API 的正确使用方法

## 技术债务

1. `LibmpvFrameRenderer.kt` - 已实现但未使用，需要修复渲染上下文初始化
2. 软件渲染参数创建 - 可能需要使用 OpenGL 或 Metal 渲染
3. 错误处理 - 需要更完善的错误恢复机制

## 参考资源

- [libmpv 文档](https://mpv.io/manual/master/#libmpv)
- [libmpv 客户端 API](https://github.com/mpv-player/mpv/blob/master/libmpv/client.h)
- [libmpv 渲染 API](https://github.com/mpv-player/mpv/blob/master/libmpv/render.h)
