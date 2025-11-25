# 视频播放器生命周期修复

## 问题描述

在 macOS 上运行桌面应用时，视频播放出现以下问题：

1. **协程取消错误**：`The coroutine scope left the composition`
2. **视频表面尺寸无效**：视频表面初始化时尺寸为 0x0
3. **视频表面未添加到显示层次结构**

## 根本原因

### 1. 协程生命周期问题

`LaunchedEffect(url)` 中包含异步操作（`delay(200)`），当 URL 改变或组件重组时：
- 之前的协程被取消
- VLC 播放器状态可能已部分改变
- 错误处理代码仍然执行，导致误报错误

### 2. 视频表面初始化时机问题

SwingPanel 的 `factory` lambda 只是返回组件，没有确保视频表面被正确初始化：
- 视频表面尺寸未设置
- 视频表面可见性未设置
- 导致 VLC 无法正确渲染视频

## 解决方案

### 1. 移除不必要的延迟

**修改前：**
```kotlin
// Step 2: Add delay to ensure resources are properly freed
delay(200)
```

**修改后：**
```kotlin
// 移除 delay，直接执行操作
// VLC 的 stop() 方法是同步的，不需要额外延迟
```

### 2. 添加协程取消检查

**修改前：**
```kotlin
} catch (e: Exception) {
    // 直接处理所有异常
    val errorCategory = categorizeMediaError(e)
    // ...
}
```

**修改后：**
```kotlin
} catch (e: Exception) {
    // 检查协程是否仍然活跃
    if (!isActive) {
        println("Coroutine cancelled during media loading, ignoring error")
        return@LaunchedEffect
    }
    
    // 只在协程仍活跃时处理错误
    val errorCategory = categorizeMediaError(e)
    // ...
}
```

### 3. 在 factory 中初始化视频表面

**修改前：**
```kotlin
factory = { mediaPlayerComponent }
```

**修改后：**
```kotlin
factory = { 
    mediaPlayerComponent.apply {
        try {
            // 确保视频表面从一开始就可见
            val videoSurface = videoSurfaceComponent()
            videoSurface.isVisible = true
            
            // 设置初始尺寸
            if (videoSurface.width <= 0 || videoSurface.height <= 0) {
                videoSurface.setSize(800, 600)
                println("✓ Initial video surface size set to 800x600")
            }
            
            println("✓ Video surface initialized in factory")
        } catch (e: Exception) {
            println("⚠️ Error initializing video surface in factory: ${e.message}")
        }
    }
}
```

### 4. 改进 update 回调的尺寸检查

**修改前：**
```kotlin
update = { component ->
    val currentSize = component.size
    val videoSurface = component.videoSurfaceComponent()
    // 直接使用 currentSize，可能为 0x0
}
```

**修改后：**
```kotlin
update = { component ->
    val currentSize = component.size
    
    // 只在组件有有效尺寸时更新
    if (currentSize.width > 0 && currentSize.height > 0) {
        val videoSurface = component.videoSurfaceComponent()
        // 更新视频表面尺寸
    }
}
```

## 验证步骤

1. **启动应用**
   ```bash
   ./gradlew :composeApp:run
   ```

2. **检查日志输出**
   - 应该看到：`✓ Initial video surface size set to 800x600`
   - 应该看到：`✓ Video surface initialized in factory`
   - 不应该看到：`The coroutine scope left the composition`

3. **测试播放**
   - 加载一个直播流 URL
   - 快速切换到另一个频道
   - 验证没有协程取消错误

4. **运行测试**
   ```bash
   ./gradlew :composeApp:desktopTest
   ```

## 预期结果

修复后，应该看到以下改进：

1. ✅ 视频表面正确初始化（尺寸 > 0）
2. ✅ 视频表面可见性正确设置
3. ✅ 协程取消不再导致错误报告
4. ✅ URL 快速切换不会崩溃
5. ✅ 视频能够正常播放

## 技术细节

### Compose 生命周期

- `LaunchedEffect` 的 key 改变时，之前的协程会被取消
- 取消是通过 `CancellationException` 实现的
- 需要使用 `isActive` 检查协程状态

### SwingPanel 生命周期

- `factory` 只在组件首次创建时调用
- `update` 在每次重组时调用
- 视频表面必须在 `factory` 中初始化

### VLC 视频表面要求

- 必须有非零尺寸
- 必须设置为可见
- 必须添加到显示层次结构（SwingPanel 自动处理）

## 相关文件

- `VideoPlayer.desktop.kt` - 主要修复
- `VideoPlayerLifecycleTest.kt` - 生命周期测试
- `VideoSurfaceValidator.kt` - 视频表面验证工具

## 参考资料

- [Compose Side Effects](https://developer.android.com/jetpack/compose/side-effects)
- [VLCJ Documentation](https://github.com/caprica/vlcj)
- [Kotlin Coroutines Cancellation](https://kotlinlang.org/docs/cancellation-and-timeouts.html)
