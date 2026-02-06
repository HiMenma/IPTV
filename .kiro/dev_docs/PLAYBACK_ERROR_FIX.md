# 播放错误修复说明

## 问题描述
所有频道播放时都出现错误：
```
Playback Error
PlatformException(VideoError, Video player had error Z.n: Source error, null, null)
```

## 根本原因

之前的修复中，`player_screen.dart`的`dispose()`方法使用了不正确的方式来停止播放：

```dart
// 错误的方式
Future.microtask(() {
  final viewModel = context.read<PlayerViewModel>();
  viewModel.stop();
});
```

这导致：
1. dispose执行时机不对
2. 资源没有正确释放
3. 新的播放器初始化时旧的资源还在使用中

## 修复方案

### 1. 修复PlayerScreen的dispose方法

```dart
@override
void dispose() {
  // Stop playback when leaving the screen
  try {
    context.read<PlayerViewModel>().stop();
  } catch (e) {
    // Ignore errors during dispose
    debugPrint('Error stopping playback during dispose: $e');
  }
  super.dispose();
}
```

**改进点：**
- 在`super.dispose()`之前调用stop
- 添加try-catch避免dispose时的错误
- 使用debugPrint记录错误但不中断流程

### 2. 改进PlayerService的play方法

```dart
// 在创建新播放器前，确保旧资源完全释放
if (_chewieController != null) {
  try {
    await _chewieController!.pause();
    _chewieController!.dispose();
  } catch (e) {
    debugPrint('Error disposing chewie controller: $e');
  }
  _chewieController = null;
}

if (_videoController != null) {
  try {
    if (_videoController!.value.isPlaying) {
      await _videoController!.pause();
    }
    await _videoController!.dispose();
  } catch (e) {
    debugPrint('Error disposing video controller: $e');
  }
  _videoController = null;
}

// 添加小延迟确保资源完全释放
await Future.delayed(const Duration(milliseconds: 100));
```

**改进点：**
- 先暂停再释放视频控制器
- 添加try-catch处理释放时的错误
- 添加100ms延迟确保资源完全释放
- 在创建新播放器前验证URL

## 测试步骤

### 1. 清理并重新构建
```bash
flutter clean
flutter pub get
flutter build apk --release
```

### 2. 测试场景

#### 场景1：正常播放
1. 打开应用
2. 选择一个配置
3. 点击任意频道
4. 确认视频开始播放
5. 等待几秒确认播放稳定

#### 场景2：快速切换频道
1. 播放一个频道
2. 返回列表
3. 立即点击另一个频道
4. 确认新频道正常播放

#### 场景3：返回主界面
1. 播放一个频道
2. 点击返回按钮
3. 确认声音已停止
4. 再次进入播放
5. 确认可以正常播放

#### 场景4：错误处理
1. 尝试播放一个无效的频道
2. 确认显示友好的错误消息
3. 点击Retry重试
4. 或点击Back返回列表

## 如果问题仍然存在

### 检查网络连接
```bash
# 在设备上测试URL是否可访问
adb shell
curl -I http://your-stream-url
```

### 检查日志
```bash
# 查看详细日志
flutter run --release
# 或
adb logcat | grep -i "video\|player\|error"
```

### 常见原因

1. **网络问题**
   - 检查设备网络连接
   - 确认流媒体服务器可访问
   - 检查防火墙设置

2. **URL格式问题**
   - 确认URL以http://或https://开头
   - 检查URL是否包含特殊字符
   - 验证端口号是否正确

3. **视频格式问题**
   - Android支持的格式：HLS (.m3u8), MP4, RTSP
   - 某些编码格式可能不支持
   - 检查流媒体服务器的编码设置

4. **设备兼容性**
   - 某些旧设备可能不支持特定编码
   - 检查Android版本（最低支持API 21）
   - 尝试在不同设备上测试

## 调试模式

如果需要更详细的调试信息，可以临时修改`player_service.dart`：

```dart
// 在play方法开始处添加
debugPrint('=== Starting playback ===');
debugPrint('Stream URL: $streamUrl');
debugPrint('Current state: $_currentState');

// 在各个关键点添加日志
debugPrint('Video controller initialized');
debugPrint('Chewie controller created');
debugPrint('Playback started');
```

## 回滚方案

如果新版本仍有问题，可以回滚到之前的版本：

```bash
git log --oneline  # 查看提交历史
git checkout <commit-hash>  # 回滚到特定提交
flutter clean
flutter pub get
flutter build apk --release
```

## 联系支持

如果问题持续存在，请提供：
1. 设备型号和Android版本
2. 完整的错误日志
3. 测试的流媒体URL（如果可以分享）
4. 重现步骤
