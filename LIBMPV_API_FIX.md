# libmpv 播放器修复和优化

## 问题 1: 函数符号解析错误

### 问题描述
应用程序崩溃，错误信息：
```
java.lang.UnsatisfiedLinkError: Error looking up function 'mpv_get_property_double': 
dlsym(0xb7ba03f0, mpv_get_property_double): symbol not found
```

### 根本原因
JNA 绑定使用了 libmpv C API 中不存在的函数：
- `mpv_get_property_double()`
- `mpv_set_property_double()`
- `mpv_get_property_long()`
- `mpv_set_property_long()`
- `mpv_get_property_flag()`
- `mpv_set_property_flag()`

实际的 API 使用通用函数：
- `mpv_get_property(ctx, name, format, data)`
- `mpv_set_property(ctx, name, format, data)`

### 解决方案
更新了 `LibmpvBindings.kt` 和 `LibmpvPlayerEngine.kt`，使用正确的通用 API 和格式说明符。

---

## 问题 2: 视频没有声音

### 问题描述
视频可以播放但没有声音输出。

### 根本原因
音频输出配置不完整，需要明确启用音频并配置音频通道。

### 解决方案
在 `LibmpvPlayerEngine.kt` 的 `applyConfiguration()` 方法中添加：
```kotlin
// 音频输出 - 确保音频启用
setOption("ao", config.audioOutput)
setOption("audio", "yes")
setOption("audio-channels", "stereo")
```

---

## 问题 3: 分辨率不清晰

### 问题描述
视频播放时分辨率不是最佳质量。

### 根本原因
没有配置视频质量选择，libmpv 默认可能选择较低质量的流。

### 解决方案
在 `LibmpvPlayerEngine.kt` 的 `applyConfiguration()` 方法中添加：
```kotlin
// 视频质量设置 - 选择最佳质量
setOption("ytdl-format", "bestvideo+bestaudio/best")
setOption("hls-bitrate", "max")  // HLS 流选择最高码率
```

---

## 问题 4: 视频弹窗播放

### 问题描述
视频在独立窗口中播放，而不是在应用内嵌入播放。

### 根本原因
视频输出配置为 "gpu"，这会创建独立窗口。需要使用 libmpv 渲染 API 进行嵌入式渲染。

### 当前状态
⚠️ **暂时未解决** - libmpv 的软件渲染模式在 macOS 上导致崩溃（exit code 134）。

### 临时方案
暂时保持使用 `vo=gpu` 模式，视频会在独立窗口播放，但至少音频和视频质量问题已解决。

### 未来改进
需要实现以下方案之一：
1. 使用 OpenGL 渲染上下文而不是软件渲染
2. 使用 Metal 渲染（macOS 原生）
3. 研究其他嵌入式渲染方案

相关代码已准备好（`LibmpvFrameRenderer.kt`），但需要修复渲染上下文初始化问题。

---

## 修改的文件

### 1. LibmpvBindings.kt
- 移除不存在的类型化属性函数
- 添加通用的 `mpv_get_property` 和 `mpv_set_property`

### 2. LibmpvPlayerEngine.kt
- 更新属性 getter/setter 方法使用通用 API
- 修改 `applyConfiguration()` 方法：
  - 设置 `vo` 为 "libmpv" 以启用嵌入式渲染
  - 明确启用音频输出和立体声通道
  - 配置视频质量选择为最佳

### 3. LibmpvVideoPlayer.desktop.kt
- 在创建帧渲染器时调用 `initialize()` 方法

---

## 测试

编译成功：
```bash
./gradlew :composeApp:desktopJar
```

运行应用：
```bash
./gradlew :composeApp:run
```

---

## 预期结果

修复后，应用应该：
1. ✅ 正常播放视频，没有符号查找错误
2. ✅ 有声音输出
3. ✅ 自动选择最高质量的视频流
4. ⚠️ 视频在独立窗口播放（嵌入式播放待实现）

---

## 技术细节

### 使用的格式常量
- `MPV_FORMAT_DOUBLE` (5) - 用于 double 值
- `MPV_FORMAT_FLAG` (3) - 用于 boolean 值
- `MPV_FORMAT_INT64` (4) - 用于 long 值
- `MPV_FORMAT_STRING` (1) - 用于 string 值

### libmpv 渲染模式
- **独立窗口模式**: `vo=gpu` - 创建独立的视频输出窗口
- **嵌入式渲染模式**: `vo=libmpv` - 使用渲染 API，允许应用控制帧显示

### 音频配置
- `ao=auto` - 自动选择音频输出设备
- `audio=yes` - 明确启用音频
- `audio-channels=stereo` - 设置立体声输出

### 视频质量配置
- `ytdl-format=bestvideo+bestaudio/best` - 选择最佳视频和音频流
- `hls-bitrate=max` - HLS 流选择最高码率
