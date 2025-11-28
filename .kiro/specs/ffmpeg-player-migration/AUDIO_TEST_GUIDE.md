# 音频修复测试指南

## 修复内容

本次修复解决了以下音频播放问题：

1. ✅ 采样率为 0 导致的初始化失败
2. ✅ 通道数为 0 或不匹配的问题
3. ✅ 不同音频缓冲区类型（ShortBuffer/ByteBuffer/FloatBuffer）的兼容性
4. ✅ 直播流音频信息解析失败
5. ✅ 流打开失败（错误 -1482175736）

## 测试步骤

### 1. 编译项目

```bash
./gradlew :composeApp:compileKotlinDesktop
```

### 2. 运行应用

```bash
./gradlew :composeApp:run
```

### 3. 测试场景

#### 场景 1：音频元数据不完整的流

测试之前报错的流：
- URL: `http://xxip9.top:8080/live/QkJP84/106208/125947.ts`

**预期结果**：
- 不再出现 "invalid frame rate: 0.0" 错误
- 音频能够正常播放
- 日志显示使用默认值：
  ```
  [AudioPlayer] WARNING: Invalid sample rate (0.0 Hz), defaulting to 48000 Hz
  [AudioPlayer] WARNING: Invalid channel count (0), defaulting to stereo (2)
  ```

#### 场景 2：不同音频格式的流

测试不同编码格式：
- AAC 编码流
- MP3 编码流
- AC3 编码流

**预期结果**：
- 所有格式都能正常播放
- 不出现类型转换错误

#### 场景 3：不同通道配置

测试不同通道数：
- 单声道流
- 立体声流
- 多声道流

**预期结果**：
- 自动适配通道数
- 单声道自动扩展为立体声

#### 场景 4：流打开测试

测试之前无法打开的流：
- URL: `http://xxip9.top:8080/live/QkJP84/106208/255666.ts`

**预期结果**：
- 流能够成功打开
- 日志显示：
  ```
  Buffer Configuration:
    Probe Size: 131072 bytes (128KB)
    Analyze Duration: 1000000 μs (1秒)
    Buffer Size: 131072 bytes (128KB)
  ```

## 关键日志检查

### 正常启动日志

```
[FFmpegGrabberConfigurator] Basic configuration applied
[FFmpegGrabberConfigurator] Buffer configuration applied: probesize=131072, analyzeduration=1000000, buffer_size=131072
[FFmpegPlayerEngine] Media info extracted:
  Duration: 0ms
  Video: h264 1920x1080 @ 25.0fps
  Audio: aac 2ch @ 48000Hz
[AudioPlayer] Initializing audio line:
  Sample Rate: 48000.0 Hz
  Channels: 2
  Sample Size: 16 bits
[AudioPlayer] Audio line initialized successfully
```

### 容错处理日志

```
[AudioPlayer] WARNING: Frame reports 0 channels but has 2 sample buffers
[AudioPlayer] WARNING: Invalid sample rate (0.0 Hz), defaulting to 48000 Hz
[AudioPlayer] WARNING: Invalid channel count (0), defaulting to stereo (2)
```

### 不应出现的错误

❌ `invalid frame rate: 0.0`
❌ `Index 1 out of bounds for length 1`
❌ `DirectFloatBufferU cannot be cast to ShortBuffer`
❌ `avformat_open_input() error -1482175736`

## 性能检查

### 音频延迟

- 直播流延迟应在 1-3 秒范围内
- 不应出现频繁的同步警告

### CPU 使用率

- 音频处理不应占用过高 CPU
- 使用活动监视器检查 CPU 使用率

### 内存使用

- 长时间播放不应出现内存泄漏
- 切换频道后内存应正常释放

## 故障排查

### 问题：音频仍然无法播放

1. 检查日志中的采样率和通道数
2. 确认是否使用了默认值
3. 检查音频线是否成功初始化

### 问题：音频有杂音或断续

1. 检查缓冲区配置是否合适
2. 查看同步误差日志
3. 尝试调整缓冲区大小

### 问题：流无法打开

1. 检查网络连接
2. 验证 URL 是否有效
3. 查看 FFmpeg 详细错误信息

## 回归测试

确保修复没有影响其他功能：

- ✅ 视频播放正常
- ✅ 音视频同步正常
- ✅ 暂停/恢复功能正常
- ✅ 音量控制正常
- ✅ 频道切换正常
- ✅ 全屏功能正常

## 报告问题

如果发现问题，请提供：

1. 完整的错误日志
2. 测试的流 URL（如果可以分享）
3. 系统信息（macOS 版本等）
4. 重现步骤

## 下一步优化

如果测试通过，可以考虑：

1. 添加音频格式自动检测
2. 实现动态采样率转换
3. 优化音频缓冲策略
4. 添加音频质量监控
