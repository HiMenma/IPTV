# 播放问题快速修复参考

## 问题速查表

| 错误信息 | 原因 | 修复位置 | 状态 |
|---------|------|---------|------|
| `invalid frame rate: 0.0` | 采样率为 0 | AudioPlayer.kt | ✅ 已修复 |
| `Index 1 out of bounds for length 1` | 通道数不匹配 | AudioPlayer.kt | ✅ 已修复 |
| `DirectFloatBufferU cannot be cast to ShortBuffer` | 缓冲区类型不兼容 | AudioPlayer.kt | ✅ 已修复 |
| `avformat_open_input() error -1482175736` | 流打开失败 | StreamTypeDetector.kt | ✅ 已修复 |
| `Cannot invoke "java.awt.color.ColorSpace.getNumComponents()"` | ColorSpace 为 null | VideoRenderer.kt | ✅ 已修复 |

## 修复清单

### ✅ 音频修复
- [x] 采样率验证和默认值（48kHz）
- [x] 通道数验证和默认值（2 通道）
- [x] 多种缓冲区类型支持（Short/Byte/Float）
- [x] 直播流缓冲配置优化
- [x] FFmpeg 默认音频参数设置

### ✅ 视频修复
- [x] ColorSpace 验证和自动修复
- [x] 图像格式兼容性增强

## 快速测试命令

```bash
# 编译
./gradlew :composeApp:compileKotlinDesktop

# 运行
./gradlew :composeApp:run
```

## 验证要点

### 音频验证
```
✓ 日志显示：[AudioPlayer] Audio line initialized successfully
✓ 没有错误：invalid frame rate
✓ 没有错误：Index out of bounds
✓ 没有错误：cannot be cast to ShortBuffer
```

### 视频验证
```
✓ 视频正常显示
✓ 没有错误：ColorSpace.getNumComponents
✓ 没有黑屏或花屏
```

### 同步验证
```
✓ 音视频同步误差 < 500ms
✓ 没有频繁的跳帧警告
```

## 性能基准

| 指标 | 正常范围 | 异常阈值 |
|-----|---------|---------|
| CPU 使用率 | 10-30% | > 50% |
| 内存使用 | 200-500MB | > 1GB |
| 同步误差 | < 200ms | > 500ms |
| 丢帧率 | < 1% | > 5% |

## 故障排查流程

### 1. 音频无声
```
1. 检查日志：[AudioPlayer] Audio line initialized successfully
2. 检查采样率：Sample Rate: 48000.0 Hz
3. 检查通道数：Channels: 2
4. 检查音量：getVolume() 返回值
```

### 2. 视频黑屏
```
1. 检查日志：[VideoRenderer] Created back buffer
2. 检查 Canvas 尺寸：> 0
3. 检查帧转换：converter.convert() 不为 null
4. 检查 ColorSpace：ensureValidColorSpace() 被调用
```

### 3. 流无法打开
```
1. 检查网络连接
2. 检查 URL 有效性
3. 检查缓冲配置：probesize, analyzeduration
4. 查看 FFmpeg 详细日志
```

### 4. 音视频不同步
```
1. 检查同步误差日志
2. 检查缓冲区大小
3. 检查帧率是否稳定
4. 检查是否频繁跳帧
```

## 回滚方案

如果修复导致新问题，可以回滚特定部分：

### 回滚音频修复
```kotlin
// AudioPlayer.kt - 移除默认值设置
// 恢复原始的 sampleRate 和 channels 使用
```

### 回滚视频修复
```kotlin
// VideoRenderer.kt - 移除 ensureValidColorSpace 调用
val image = converter.convert(frame) ?: return
// 不调用 ensureValidColorSpace
```

### 回滚缓冲配置
```kotlin
// StreamTypeDetector.kt - 恢复原始配置
isLive -> BufferConfiguration(
    probeSize = 32 * 1024,
    maxAnalyzeDuration = 0L,
    bufferSize = 64 * 1024
)
```

## 联系支持

如果问题仍然存在，请提供：
1. 完整的错误日志
2. 测试的流 URL
3. 系统信息（macOS 版本）
4. 重现步骤

## 相关文档

- 📄 [完整修复总结](./PLAYBACK_FIXES_SUMMARY.md)
- 📄 [音频修复详情](./AUDIO_FIX_SUMMARY.md)
- 📄 [视频修复详情](./VIDEO_COLORSPACE_FIX.md)
- 📄 [测试指南](./AUDIO_TEST_GUIDE.md)
