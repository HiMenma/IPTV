# HTTP播放问题修复

## 问题描述
HTTP协议的m3u8链接播放时，能听到声音但视频显示"Playback stopped"错误。

## 根本原因
1. **网络安全配置缺失**：AndroidManifest.xml引用了`network_security_config.xml`但文件不存在
2. **直播流状态判断错误**：播放器将直播流误判为已完成，因为：
   - 直播流的duration可能为0或非常大
   - 缓冲时的状态变化被误判为停止
   - position >= duration的判断逻辑不适用于直播流

## 修复内容

### 1. 创建网络安全配置文件
**文件**: `android/app/src/main/res/xml/network_security_config.xml`
- 允许HTTP明文传输
- 信任系统和用户添加的证书

### 2. 优化播放器状态判断逻辑
**文件**: `lib/services/player_service.dart`

#### 改进1: 直播流完成判断
```dart
// 之前：简单判断 position >= duration
// 现在：排除直播流（duration > 24小时的视频）
if (value.duration.inMilliseconds > 0 && 
    value.duration.inMilliseconds < 86400000 && // 小于24小时
    value.position >= value.duration) {
  _updateState(PlayerState.stopped);
}
```

#### 改进2: 缓冲状态处理
```dart
// 之前：播放中变为非播放就切换到paused
// 现在：检查是否在缓冲，避免误判
if (value.isPlaying) {
  if (_currentState != PlayerState.playing) {
    _updateState(PlayerState.playing);
  }
} else if (_currentState == PlayerState.playing && !value.isBuffering) {
  // 只在非缓冲状态下才切换到paused
  _updateState(PlayerState.paused);
}
```

#### 改进3: 增强错误显示
- 为Chewie控制器添加自定义errorBuilder
- 提供更友好的错误提示界面
- 改进placeholder显示加载指示器

## 测试建议
1. 测试HTTP协议的m3u8链接
2. 测试HTTPS协议的m3u8链接
3. 测试网络波动时的缓冲恢复
4. 测试长时间播放的稳定性

## 重新编译
```bash
flutter clean
flutter build apk
```
