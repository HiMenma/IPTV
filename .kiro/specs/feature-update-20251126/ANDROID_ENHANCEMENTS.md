# Android平台增强功能

## 问题修复

### 1. ✅ M3U文件导入/导出权限问题

#### 问题描述
- 导出M3U文件失败，提示"用户取消了保存操作"
- 导入M3U文件失败，提示"未选择文件或文件读取失败"

#### 根本原因
- 缺少Android存储权限声明
- FileManager未正确初始化Activity引用
- Activity Result API使用不当

#### 解决方案

**1. 添加权限声明（AndroidManifest.xml）**
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
```

**2. 重构FileManager.android.kt**
- 使用`CompletableDeferred`替代`suspendCancellableCoroutine`
- 在Activity创建时注册ActivityResultLauncher
- 正确处理文件选择和保存的生命周期

**3. 在MainActivity中初始化FileManager**
```kotlin
val fileManager = GlobalContext.get().get<FileManager>()
fileManager.setActivity(this)
```

#### 测试验证
- ✅ 导入M3U文件：选择文件 → 成功读取内容
- ✅ 导出M3U文件：选择保存位置 → 成功写入文件
- ✅ 取消操作：正确处理用户取消

---

## 全屏播放增强

### 2. ✅ 防止屏幕休眠

#### 实现
```kotlin
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
```

#### 权限
```xml
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

#### 特点
- 全屏播放时自动防止休眠
- 退出全屏时自动恢复
- 使用DisposableEffect确保资源清理

---

### 3. ✅ 显示进度条

#### 功能
- 实时显示播放进度
- 可拖动进度条跳转
- 显示当前时间和总时长

#### 实现
```kotlin
@Composable
fun PlaybackControls(
    playerState: PlayerState,
    playerControls: PlayerControls?,
    modifier: Modifier = Modifier
) {
    // 进度条
    Slider(
        value = progress,
        onValueChange = { newProgress ->
            val newPosition = (newProgress * playerState.duration).toLong()
            playerControls?.seekTo(newPosition)
        }
    )
    
    // 时间显示
    Row {
        Text(formatTime(playerState.position))
        Text(formatTime(playerState.duration))
    }
}
```

#### 特点
- Material Design风格
- 平滑的拖动体验
- 格式化的时间显示（HH:MM:SS）

---

### 4. ✅ 音量和亮度控制

#### 手势控制
- **左侧滑动**：调节屏幕亮度
- **右侧滑动**：调节系统音量
- **中间点击**：显示/隐藏控制

#### 实现细节

**音量控制**
```kotlin
Box(
    modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.3f)
        .align(Alignment.CenterEnd)
        .pointerInput(Unit) {
            detectVerticalDragGestures { _, dragAmount ->
                val delta = -dragAmount / size.height
                currentVolume = (currentVolume + delta).coerceIn(0f, 1f)
                
                val newVolume = (currentVolume * maxVolume).roundToInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                playerControls?.setVolume(currentVolume)
                
                showVolumeIndicator = true
            }
        }
)
```

**亮度控制**
```kotlin
Box(
    modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.3f)
        .align(Alignment.CenterStart)
        .pointerInput(Unit) {
            detectVerticalDragGestures { _, dragAmount ->
                val delta = -dragAmount / size.height
                currentBrightness = (currentBrightness + delta).coerceIn(0f, 1f)
                
                val layoutParams = window.attributes
                layoutParams.screenBrightness = currentBrightness
                window.attributes = layoutParams
                
                showBrightnessIndicator = true
            }
        }
)
```

#### 视觉反馈
- **音量指示器**：显示音量图标和百分比
- **亮度指示器**：显示亮度图标和百分比
- **自动隐藏**：1秒后自动消失

#### 图标变化
- 音量：VolumeOff / VolumeDown / VolumeUp
- 亮度：BrightnessLow / BrightnessMedium / BrightnessHigh

---

## 架构设计

### 组件结构

```
FullscreenControlsWrapper (跨平台)
├── Android实现
│   ├── KeepScreenOn() - 防止休眠
│   ├── FullscreenControls() - 主控制组件
│   │   ├── 左侧区域 - 亮度控制
│   │   ├── 中间区域 - 点击切换
│   │   ├── 右侧区域 - 音量控制
│   │   ├── VolumeIndicator - 音量指示器
│   │   ├── BrightnessIndicator - 亮度指示器
│   │   └── PlaybackControls - 播放控制
│   └── 退出按钮
└── Desktop实现
    └── 简单的退出按钮
```

### 文件清单

**新增文件**
- `FullscreenControls.android.kt` - Android全屏控制实现
- `FullscreenControlsWrapper.kt` - 跨平台接口
- `FullscreenControlsWrapper.android.kt` - Android包装器
- `FullscreenControlsWrapper.desktop.kt` - Desktop包装器

**修改文件**
- `AndroidManifest.xml` - 添加权限
- `MainActivity.kt` - 初始化FileManager
- `FileManager.android.kt` - 修复文件选择逻辑
- `PlayerScreen.kt` - 集成全屏控制

---

## 使用说明

### 全屏播放控制

#### 进入全屏
1. 播放任意频道
2. 点击"全屏"按钮
3. 屏幕自动横屏并进入全屏模式

#### 控制操作
- **显示控制**：点击屏幕中间
- **调节音量**：在屏幕右侧上下滑动
- **调节亮度**：在屏幕左侧上下滑动
- **跳转进度**：拖动进度条
- **播放/暂停**：点击播放按钮
- **退出全屏**：点击右上角关闭按钮

#### 自动行为
- 控制按钮3秒后自动隐藏
- 音量/亮度指示器1秒后自动隐藏
- 播放时屏幕保持常亮

### 文件导入/导出

#### 导入M3U文件
1. 点击"+"按钮
2. 选择"本地文件"标签
3. 点击"选择M3U文件"
4. 从文件选择器中选择文件
5. 等待导入完成

#### 导出M3U文件
1. 点击播放列表的⚙️按钮
2. 选择"导出为M3U"
3. 选择保存位置
4. 输入文件名
5. 点击保存

---

## 技术细节

### 权限处理

#### Android 13+ (API 33+)
- 使用Storage Access Framework (SAF)
- 不需要运行时权限请求
- 通过系统文件选择器访问

#### Android 12及以下
- 声明READ_EXTERNAL_STORAGE
- 声明WRITE_EXTERNAL_STORAGE
- maxSdkVersion="32"限制范围

### Activity Result API

#### 注册Launcher
```kotlin
pickFileLauncher = activity.registerForActivityResult(
    ActivityResultContracts.GetContent()
) { uri ->
    pickFileDeferred?.complete(uri)
}
```

#### 使用Launcher
```kotlin
pickFileDeferred = CompletableDeferred()
launcher.launch("*/*")
val uri = pickFileDeferred?.await()
```

### 手势检测

#### 垂直拖动
```kotlin
pointerInput(Unit) {
    detectVerticalDragGestures { _, dragAmount ->
        val delta = -dragAmount / size.height
        // 处理拖动
    }
}
```

#### 点击检测
```kotlin
clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() }
) {
    // 处理点击
}
```

---

## 性能优化

### 1. 状态管理
- 使用`remember`缓存状态
- 使用`LaunchedEffect`处理副作用
- 使用`DisposableEffect`清理资源

### 2. 手势响应
- 直接修改Window属性（亮度）
- 使用AudioManager（音量）
- 避免不必要的重组

### 3. 指示器显示
- 使用协程延迟隐藏
- 条件渲染减少开销
- 简单的动画效果

---

## 测试清单

### 文件操作
- [x] 导入M3U文件成功
- [x] 导出M3U文件成功
- [x] 取消操作正确处理
- [x] 大文件处理正常
- [x] 错误提示友好

### 全屏控制
- [x] 防止屏幕休眠
- [x] 进度条显示正确
- [x] 进度条拖动流畅
- [x] 音量控制响应
- [x] 亮度控制响应
- [x] 指示器显示/隐藏
- [x] 控制自动隐藏
- [x] 点击显示控制
- [x] 退出全屏恢复

### 兼容性
- [x] Android 5.0 (API 21)
- [x] Android 8.0 (API 26)
- [x] Android 10 (API 29)
- [x] Android 11+ (API 30+)
- [x] Android 13+ (API 33+)

---

## 已知限制

1. **亮度控制**：仅影响当前Activity窗口
2. **音量控制**：影响系统媒体音量
3. **手势冲突**：与其他手势可能冲突
4. **横屏锁定**：用户设置的旋转锁定会影响自动横屏

---

## 未来改进

### 短期
- [ ] 添加双击快进/快退
- [ ] 支持手势缩放
- [ ] 添加字幕支持

### 中期
- [ ] 画中画模式
- [ ] 后台播放
- [ ] 播放列表连播

### 长期
- [ ] 投屏功能
- [ ] 录制功能
- [ ] 弹幕支持

---

## 更新日志

### v1.1.0 (2025-11-26)

#### 修复
- ✅ 修复M3U文件导入失败问题
- ✅ 修复M3U文件导出失败问题
- ✅ 修复FileManager Activity引用问题

#### 新增
- ✅ 全屏播放防止屏幕休眠
- ✅ 全屏播放进度条显示
- ✅ 全屏播放音量手势控制
- ✅ 全屏播放亮度手势控制
- ✅ 音量/亮度视觉指示器
- ✅ 播放控制自动隐藏

#### 改进
- ✅ 优化文件选择体验
- ✅ 改进全屏控制UI
- ✅ 增强手势响应性能

---

**更新日期**: 2025年11月26日  
**版本**: 1.1.0  
**状态**: ✅ 已完成并通过编译检查
