# Design Document

## Overview

本设计文档描述了如何将桌面播放引擎从 FFmpeg (JavaCV) 迁移到 libmpv。libmpv 是 MPV 媒体播放器的库形式,提供了简洁的 C API、优秀的性能和广泛的格式支持。

迁移策略:
1. 使用 JNA (Java Native Access) 创建 libmpv C API 的 Kotlin 绑定
2. 实现 LibmpvPlayerImplementation 类,遵循现有的 PlayerImplementation 接口
3. 集成 libmpv 的视频渲染到 Compose UI
4. 实现播放控制、状态管理和错误处理
5. 移除 FFmpeg 相关代码和依赖

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                    │
│  ┌────────────────────────────────────────────────────┐ │
│  │         VideoPlayer.desktop.kt                     │ │
│  │  (使用 PlayerImplementation 接口)                  │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│              Player Implementation Layer                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │      LibmpvPlayerImplementation.kt                 │ │
│  │  - 实现 PlayerImplementation 接口                  │ │
│  │  - 管理 libmpv 实例生命周期                        │ │
│  │  - 提供 PlayerControls                             │ │
│  │  - 更新 PlayerState                                │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  libmpv Engine Layer                     │
│  ┌────────────────────────────────────────────────────┐ │
│  │           LibmpvPlayerEngine.kt                    │ │
│  │  - 封装 libmpv 核心功能                            │ │
│  │  - 事件处理                                        │ │
│  │  - 属性管理                                        │ │
│  │  - 命令执行                                        │ │
│  └────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────┐ │
│  │           LibmpvFrameRenderer.kt                   │ │
│  │  - 获取视频帧                                      │ │
│  │  - 像素格式转换                                    │ │
│  │  - 渲染到 Compose                                  │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                   JNA Bindings Layer                     │
│  ┌────────────────────────────────────────────────────┐ │
│  │              LibmpvBindings.kt                     │ │
│  │  - JNA 接口定义                                    │ │
│  │  - C 函数映射                                      │ │
│  │  - 结构体定义                                      │ │
│  │  - 常量定义                                        │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│                  Native libmpv Library                   │
│                    (C Shared Library)                    │
└─────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

```
用户操作 → UI Component → PlayerControls → LibmpvPlayerEngine → libmpv C API
                                                    ↓
                                            事件回调 / 状态更新
                                                    ↓
                                            PlayerState 更新
                                                    ↓
                                            UI 重新渲染
```

## Components and Interfaces

### 1. LibmpvBindings (JNA Interface)

提供 libmpv C API 的 Kotlin/JNA 绑定。

```kotlin
interface LibmpvBindings : Library {
    // Core functions
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_destroy(ctx: Pointer)
    
    // Property functions
    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    fun mpv_set_property_double(ctx: Pointer, name: String, value: Double): Int
    fun mpv_get_property_double(ctx: Pointer, name: String, data: DoubleByReference): Int
    
    // Command functions
    fun mpv_command(ctx: Pointer, args: Array<String?>): Int
    fun mpv_command_string(ctx: Pointer, command: String): Int
    
    // Event functions
    fun mpv_wait_event(ctx: Pointer, timeout: Double): Pointer?
    
    // Render context functions
    fun mpv_render_context_create(
        ctx: PointerByReference,
        mpv: Pointer,
        params: Array<mpv_render_param>
    ): Int
    fun mpv_render_context_render(ctx: Pointer, params: Array<mpv_render_param>): Int
    fun mpv_render_context_free(ctx: Pointer)
    
    // Memory management
    fun mpv_free(data: Pointer)
}
```

### 2. LibmpvPlayerEngine

核心播放引擎,封装 libmpv 功能。

```kotlin
class LibmpvPlayerEngine {
    private var mpvHandle: Pointer? = null
    private var renderContext: Pointer? = null
    private val eventThread: Thread
    
    // Lifecycle
    fun initialize(): Boolean
    fun destroy()
    
    // Playback control
    fun loadFile(url: String)
    fun play()
    fun pause()
    fun stop()
    fun seek(position: Double)
    
    // Property management
    fun setVolume(volume: Int)
    fun getVolume(): Int
    fun getDuration(): Double
    fun getPosition(): Double
    fun isPaused(): Boolean
    
    // Configuration
    fun setOption(name: String, value: String)
    fun enableHardwareAcceleration()
    
    // Event handling
    fun setEventCallback(callback: (LibmpvEvent) -> Unit)
    
    // Frame rendering
    fun getNextFrame(): ByteArray?
    fun getVideoWidth(): Int
    fun getVideoHeight(): Int
}
```

### 3. LibmpvFrameRenderer

处理视频帧的获取和渲染。

```kotlin
class LibmpvFrameRenderer(
    private val engine: LibmpvPlayerEngine
) {
    private var frameBuffer: ByteArray? = null
    
    // Frame acquisition
    fun acquireFrame(): ImageBitmap?
    
    // Pixel format conversion
    private fun convertToRGBA(
        data: ByteArray,
        width: Int,
        height: Int,
        format: String
    ): ByteArray
    
    // Compose integration
    fun renderToCompose(
        drawScope: DrawScope,
        size: Size
    )
}
```

### 4. LibmpvPlayerImplementation

实现 PlayerImplementation 接口,提供与现有系统的兼容性。

```kotlin
class LibmpvPlayerImplementation : PlayerImplementation {
    override val type = PlayerImplementationType.LIBMPV
    override val name = "libmpv Player"
    override val description = "MPV-based player using libmpv library..."
    
    override fun isAvailable(): Boolean {
        // Check if libmpv is installed
    }
    
    override fun getUnavailableReason(): String? {
        // Return installation instructions if not available
    }
    
    @Composable
    override fun VideoPlayer(
        url: String,
        modifier: Modifier,
        playerState: MutableState<PlayerState>,
        onPlayerControls: (PlayerControls) -> Unit,
        onError: (String) -> Unit,
        onPlayerInitFailed: () -> Unit,
        isFullscreen: Boolean
    ) {
        // Implement video player composable
    }
}
```

### 5. LibmpvVideoPlayer (Composable)

Compose UI 组件,集成 libmpv 播放器。

```kotlin
@Composable
fun LibmpvVideoPlayer(
    url: String,
    modifier: Modifier,
    playerState: MutableState<PlayerState>,
    onPlayerControls: (PlayerControls) -> Unit,
    onError: (String) -> Unit,
    onPlayerInitFailed: () -> Unit,
    isFullscreen: Boolean
) {
    val engine = remember { LibmpvPlayerEngine() }
    val renderer = remember { LibmpvFrameRenderer(engine) }
    
    // Lifecycle management
    DisposableEffect(url) {
        // Initialize and load
        onDispose {
            // Cleanup
        }
    }
    
    // Render video frames
    Canvas(modifier) {
        renderer.renderToCompose(this, size)
    }
}
```

## Data Models

### LibmpvEvent

```kotlin
sealed class LibmpvEvent {
    object Idle : LibmpvEvent()
    object StartFile : LibmpvEvent()
    object EndFile : LibmpvEvent()
    object FileLoaded : LibmpvEvent()
    data class PropertyChange(val name: String, val value: Any?) : LibmpvEvent()
    data class LogMessage(val level: String, val message: String) : LibmpvEvent()
    data class Error(val code: Int, val message: String) : LibmpvEvent()
}
```

### LibmpvConfiguration

```kotlin
data class LibmpvConfiguration(
    val hardwareAcceleration: Boolean = true,
    val hwdecMethod: String = "auto",
    val videoOutput: String = "gpu",
    val audioOutput: String = "auto",
    val cacheSize: Int = 150000, // KB
    val cacheSecs: Int = 10,
    val demuxerReadahead: Int = 5,
    val networkTimeout: Int = 30,
    val userAgent: String = "IPTV-Player/1.0"
)
```

### PlayerImplementationType Extension

```kotlin
enum class PlayerImplementationType {
    VLC,      // 已移除
    FFMPEG,   // 将被替换
    LIBMPV    // 新增
}
```

## Data Flow

### 1. Initialization Flow

```
UI Component 创建
    ↓
LibmpvPlayerImplementation 实例化
    ↓
LibmpvPlayerEngine.initialize()
    ↓
mpv_create() → mpv_initialize()
    ↓
设置配置选项 (hwdec, vo, cache 等)
    ↓
启动事件处理线程
    ↓
返回 PlayerControls 给 UI
```

### 2. Playback Flow

```
用户点击播放
    ↓
PlayerControls.play()
    ↓
LibmpvPlayerEngine.loadFile(url)
    ↓
mpv_command(["loadfile", url])
    ↓
libmpv 开始加载和解码
    ↓
事件: START_FILE → FILE_LOADED
    ↓
更新 PlayerState.isPlaying = true
    ↓
开始帧渲染循环
```

### 3. Frame Rendering Flow

```
渲染循环 (LaunchedEffect)
    ↓
LibmpvFrameRenderer.acquireFrame()
    ↓
mpv_render_context_render()
    ↓
获取 RGBA 帧数据
    ↓
转换为 ImageBitmap
    ↓
Canvas 绘制
    ↓
等待下一帧
```

### 4. Event Handling Flow

```
事件线程循环
    ↓
mpv_wait_event(timeout)
    ↓
解析事件类型
    ↓
转换为 LibmpvEvent
    ↓
调用事件回调
    ↓
更新 PlayerState
    ↓
触发 UI 重组
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: libmpv initialization consistency

*For any* player initialization, if the initialization succeeds, then the player implementation type should be LIBMPV and libmpv functions should be callable
**Validates: Requirements 1.1**

### Property 2: Hardware acceleration configuration

*For any* configuration with hardware acceleration enabled, setting the configuration should result in the hwdec property being set to a non-"no" value in libmpv
**Validates: Requirements 1.3**

### Property 3: Play command state transition

*For any* valid player state, calling play() should transition the player to a playing state (isPlaying = true)
**Validates: Requirements 3.1**

### Property 4: Pause command state transition

*For any* playing player, calling pause() should transition the player to a paused state (isPaused = true)
**Validates: Requirements 3.2**

### Property 5: Stop command resource cleanup

*For any* player with loaded media, calling stop() should release resources and reset the player state
**Validates: Requirements 3.3**

### Property 6: Volume control consistency

*For any* valid volume value (0-100), calling setVolume(v) then getVolume() should return a value equal to v (within tolerance)
**Validates: Requirements 3.4**

### Property 7: Seek position consistency

*For any* valid seek position within the media duration, calling seek(pos) should result in getPosition() returning a value close to pos
**Validates: Requirements 3.5**

### Property 8: Position monotonicity during playback

*For any* playing media, the position reported by getPosition() should increase monotonically over time
**Validates: Requirements 3.6**

### Property 9: Error logging completeness

*For any* error condition, the system should log error information containing at least the error type and a descriptive message
**Validates: Requirements 4.5**

### Property 10: PlayerState update consistency

*For any* player state change event, the PlayerState should be updated to reflect the new state before UI callbacks are invoked
**Validates: Requirements 5.3**

### Property 11: Frame rendering availability

*For any* playing video with available frames, calling acquireFrame() should return a non-null ImageBitmap
**Validates: Requirements 6.1**

### Property 12: Pixel format conversion validity

*For any* video frame data, converting from libmpv format to RGBA should produce a byte array with length equal to width × height × 4
**Validates: Requirements 6.2**

### Property 13: Aspect ratio preservation

*For any* video with known dimensions, the rendered aspect ratio should match the source aspect ratio within a small tolerance
**Validates: Requirements 6.3**

### Property 14: Invalid configuration fallback

*For any* invalid configuration value, the system should use a safe default value and not crash
**Validates: Requirements 7.5**

## Error Handling

### Error Categories

1. **Initialization Errors**
   - libmpv library not found
   - mpv_create() failure
   - mpv_initialize() failure
   - Render context creation failure

2. **Playback Errors**
   - File not found
   - Network errors (timeout, connection refused)
   - Unsupported format/codec
   - Decoding errors

3. **Configuration Errors**
   - Invalid property values
   - Unsupported options
   - Permission errors

4. **Resource Errors**
   - Out of memory
   - GPU context errors
   - Audio device errors

### Error Handling Strategy

```kotlin
sealed class LibmpvError {
    data class InitializationError(val message: String) : LibmpvError()
    data class PlaybackError(val code: Int, val message: String) : LibmpvError()
    data class ConfigurationError(val option: String, val message: String) : LibmpvError()
    data class ResourceError(val message: String) : LibmpvError()
}

class LibmpvPlayerEngine {
    private var errorCallback: ((LibmpvError) -> Unit)? = null
    
    private fun handleError(error: LibmpvError) {
        // Log error
        logger.error("libmpv error: $error")
        
        // Notify callback
        errorCallback?.invoke(error)
        
        // Attempt recovery if possible
        when (error) {
            is LibmpvError.PlaybackError -> attemptReconnect()
            is LibmpvError.ConfigurationError -> useDefaultConfig()
            else -> {} // No recovery possible
        }
    }
}
```

### Error Recovery

- **Network errors**: Retry with exponential backoff (3 attempts)
- **Configuration errors**: Fall back to default values
- **Initialization errors**: Report to user, suggest installation
- **Resource errors**: Release resources and retry once

## Testing Strategy

### Unit Tests

Unit tests will verify specific functionality and edge cases:

1. **JNA Bindings Tests**
   - Test that all required libmpv functions are accessible
   - Test structure marshalling/unmarshalling
   - Test memory management (no leaks)

2. **Engine Lifecycle Tests**
   - Test initialization success/failure paths
   - Test proper cleanup on destroy
   - Test multiple init/destroy cycles

3. **Property Management Tests**
   - Test setting/getting string properties
   - Test setting/getting numeric properties
   - Test invalid property handling

4. **Command Execution Tests**
   - Test basic commands (play, pause, stop)
   - Test seek command
   - Test volume command
   - Test invalid command handling

5. **Event Handling Tests**
   - Test event parsing
   - Test event callback invocation
   - Test event thread lifecycle

6. **Frame Rendering Tests**
   - Test pixel format conversion
   - Test aspect ratio calculation
   - Test frame buffer management

### Property-Based Tests

Property-based tests will verify universal properties across many inputs using Kotest property testing:

1. **Property Test: Volume Control Consistency** (Property 6)
   - Generate random volume values (0-100)
   - Set volume and verify get returns same value
   - Validates: Requirements 3.4

2. **Property Test: Position Monotonicity** (Property 8)
   - Generate random playback scenarios
   - Verify position increases over time
   - Validates: Requirements 3.6

3. **Property Test: State Transition Consistency** (Property 3, 4, 5)
   - Generate random sequences of commands
   - Verify state transitions are correct
   - Validates: Requirements 3.1, 3.2, 3.3

4. **Property Test: Pixel Format Conversion** (Property 12)
   - Generate random frame dimensions
   - Verify output buffer size is correct
   - Validates: Requirements 6.2

5. **Property Test: Error Logging** (Property 9)
   - Generate random error conditions
   - Verify all errors are logged with required info
   - Validates: Requirements 4.5

6. **Property Test: Configuration Fallback** (Property 14)
   - Generate random invalid configurations
   - Verify system uses defaults and doesn't crash
   - Validates: Requirements 7.5

### Integration Tests

Integration tests will verify end-to-end functionality with real media:

1. **HLS Stream Test**
   - Test loading and playing HLS stream
   - Validates: Requirements 2.1

2. **HTTP Stream Test**
   - Test loading and playing HTTP stream
   - Validates: Requirements 2.2

3. **Local File Test**
   - Test loading and playing local file
   - Validates: Requirements 2.3

4. **RTSP Stream Test**
   - Test loading and playing RTSP stream
   - Validates: Requirements 2.4

5. **Error Handling Tests**
   - Test network error handling
   - Test file not found handling
   - Validates: Requirements 4.1, 4.3

### Testing Configuration

- **Property test iterations**: Minimum 100 iterations per property
- **Test framework**: Kotest for property-based testing
- **Mock framework**: MockK for unit tests
- **Integration test timeout**: 30 seconds per test
- **CI/CD integration**: All tests must pass before merge

## Implementation Notes

### Platform-Specific Considerations

#### macOS
- libmpv can be installed via Homebrew: `brew install mpv`
- Library location: `/opt/homebrew/lib/libmpv.dylib` (Apple Silicon) or `/usr/local/lib/libmpv.dylib` (Intel)
- Hardware acceleration: VideoToolbox (hwdec=videotoolbox)

#### Linux
- libmpv available in most package managers: `apt install libmpv-dev`
- Library location: `/usr/lib/x86_64-linux-gnu/libmpv.so`
- Hardware acceleration: VAAPI (hwdec=vaapi) or VDPAU (hwdec=vdpau)

#### Windows
- libmpv can be downloaded from MPV website
- Library location: User-specified or bundled with application
- Hardware acceleration: D3D11VA (hwdec=d3d11va)

### JNA Library Loading

```kotlin
object LibmpvLoader {
    fun load(): LibmpvBindings {
        val libraryName = when {
            System.getProperty("os.name").contains("Mac") -> "mpv"
            System.getProperty("os.name").contains("Windows") -> "mpv-2"
            else -> "mpv"
        }
        
        return Native.load(libraryName, LibmpvBindings::class.java)
    }
}
```

### Performance Considerations

1. **Frame Rendering**: Use double buffering to avoid tearing
2. **Event Thread**: Dedicated thread for event processing to avoid blocking
3. **Memory Management**: Properly free libmpv-allocated memory using mpv_free()
4. **Render Context**: Reuse render context instead of recreating
5. **Property Access**: Cache frequently accessed properties

### Migration Path

1. **Phase 1**: Implement libmpv bindings and engine (parallel to FFmpeg)
2. **Phase 2**: Implement LibmpvPlayerImplementation
3. **Phase 3**: Test thoroughly with all stream types
4. **Phase 4**: Switch default player to libmpv
5. **Phase 5**: Remove FFmpeg code and dependencies
6. **Phase 6**: Update documentation

### Dependencies

Add to `build.gradle.kts`:

```kotlin
dependencies {
    // JNA for native library access
    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")
    
    // Remove JavaCV dependencies (after migration)
    // implementation("org.bytedeco:javacv-platform:1.5.9")
}
```

### Configuration Defaults

```kotlin
val DEFAULT_LIBMPV_CONFIG = LibmpvConfiguration(
    hardwareAcceleration = true,
    hwdecMethod = "auto",  // Let libmpv choose best method
    videoOutput = "gpu",
    audioOutput = "auto",
    cacheSize = 150000,    // 150 MB
    cacheSecs = 10,
    demuxerReadahead = 5,
    networkTimeout = 30,
    userAgent = "IPTV-Player/1.0"
)
```

## Security Considerations

1. **URL Validation**: Validate URLs before passing to libmpv
2. **Resource Limits**: Set limits on cache size and memory usage
3. **Network Security**: Use HTTPS where possible
4. **Input Sanitization**: Sanitize user-provided configuration values
5. **Library Verification**: Verify libmpv library signature on load

## Future Enhancements

1. **Advanced Features**
   - Subtitle support
   - Audio track selection
   - Video track selection
   - Screenshot capability
   - Recording functionality

2. **Performance Optimizations**
   - Zero-copy rendering where possible
   - GPU-accelerated pixel format conversion
   - Adaptive buffering based on network conditions

3. **User Experience**
   - Playback statistics overlay
   - Network quality indicator
   - Codec information display
   - Performance metrics
