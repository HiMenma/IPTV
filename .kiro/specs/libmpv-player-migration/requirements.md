# Requirements Document

## Introduction

本文档定义了将桌面播放引擎从 FFmpeg (JavaCV) 迁移到 libmpv 的需求。libmpv 是一个强大、高效且广泛使用的媒体播放库,提供了更好的性能、更简单的 API 以及更可靠的流媒体支持。此迁移将提升 IPTV 应用的播放质量和用户体验。

## Glossary

- **libmpv**: MPV 媒体播放器的库形式,提供 C API 用于嵌入到应用程序中
- **MPV**: 一个免费、开源、跨平台的媒体播放器
- **JavaCV**: Java 接口到 OpenCV、FFmpeg 等计算机视觉库
- **FFmpeg**: 用于处理多媒体内容的开源项目
- **PlayerImplementation**: 播放器实现的抽象接口
- **JNA (Java Native Access)**: Java 框架,用于调用本地库而无需编写 JNI 代码
- **IPTV**: 互联网协议电视
- **HLS**: HTTP Live Streaming,一种流媒体协议
- **Hardware Acceleration**: 硬件加速,使用 GPU 进行视频解码

## Requirements

### Requirement 1

**User Story:** 作为开发者,我希望使用 libmpv 作为桌面播放引擎,以便获得更好的性能和更简单的集成。

#### Acceptance Criteria

1. WHEN the system initializes the desktop player THEN the system SHALL use libmpv library for video playback
2. WHEN libmpv is not available on the system THEN the system SHALL provide clear error messages indicating installation requirements
3. WHEN the player is configured THEN the system SHALL support hardware acceleration through libmpv
4. THE system SHALL provide JNA bindings to libmpv C API
5. THE system SHALL maintain the same PlayerImplementation interface for compatibility

### Requirement 2

**User Story:** 作为用户,我希望播放器能够流畅播放各种视频格式和流媒体协议,以便观看不同来源的 IPTV 内容。

#### Acceptance Criteria

1. WHEN a user plays an HLS stream THEN the system SHALL decode and render the stream using libmpv
2. WHEN a user plays an HTTP stream THEN the system SHALL decode and render the stream using libmpv
3. WHEN a user plays a local video file THEN the system SHALL decode and render the file using libmpv
4. WHEN a user plays RTSP streams THEN the system SHALL decode and render the stream using libmpv
5. THE system SHALL support common video codecs including H.264, H.265, VP9, and AV1

### Requirement 3

**User Story:** 作为用户,我希望播放器提供基本的播放控制功能,以便控制视频播放。

#### Acceptance Criteria

1. WHEN a user clicks play THEN the system SHALL start video playback
2. WHEN a user clicks pause THEN the system SHALL pause video playback
3. WHEN a user clicks stop THEN the system SHALL stop video playback and release resources
4. WHEN a user adjusts volume THEN the system SHALL change the audio volume level
5. WHEN a user seeks to a position THEN the system SHALL jump to the specified time position
6. THE system SHALL report current playback position in real-time
7. THE system SHALL report total duration for seekable content

### Requirement 4

**User Story:** 作为用户,我希望播放器能够正确处理错误情况,以便了解播放失败的原因。

#### Acceptance Criteria

1. WHEN a network error occurs THEN the system SHALL report the error to the user interface
2. WHEN a codec is not supported THEN the system SHALL report the error to the user interface
3. WHEN a file cannot be opened THEN the system SHALL report the error to the user interface
4. WHEN playback ends normally THEN the system SHALL update player state to indicate completion
5. THE system SHALL log detailed error information for debugging purposes

### Requirement 5

**User Story:** 作为开发者,我希望播放器实现能够与现有的 UI 组件无缝集成,以便最小化代码变更。

#### Acceptance Criteria

1. THE system SHALL implement the PlayerImplementation interface
2. THE system SHALL provide the same PlayerControls callback interface
3. THE system SHALL update PlayerState in the same manner as existing implementations
4. THE system SHALL use the same error callback mechanism
5. WHEN switching from FFmpeg to libmpv THEN the system SHALL require no changes to UI components

### Requirement 6

**User Story:** 作为用户,我希望播放器能够高效渲染视频帧,以便获得流畅的观看体验。

#### Acceptance Criteria

1. WHEN video frames are available THEN the system SHALL render them to Compose UI
2. WHEN rendering frames THEN the system SHALL convert libmpv pixel format to Compose-compatible format
3. WHEN rendering frames THEN the system SHALL maintain proper aspect ratio
4. THE system SHALL render frames at the video's native frame rate
5. THE system SHALL synchronize audio and video playback

### Requirement 7

**User Story:** 作为开发者,我希望能够配置 libmpv 的各种选项,以便优化不同场景下的播放性能。

#### Acceptance Criteria

1. THE system SHALL support configuring hardware acceleration options
2. THE system SHALL support configuring network buffering parameters
3. THE system SHALL support configuring audio output options
4. THE system SHALL support configuring video output options
5. WHEN configuration is invalid THEN the system SHALL use safe default values

### Requirement 8

**User Story:** 作为开发者,我希望移除 FFmpeg (JavaCV) 相关的代码和依赖,以便简化项目结构和减少包大小。

#### Acceptance Criteria

1. WHEN migration is complete THEN the system SHALL remove all FFmpeg player implementation code
2. WHEN migration is complete THEN the system SHALL remove JavaCV dependencies from build configuration
3. WHEN migration is complete THEN the system SHALL remove FFmpeg-related test files
4. WHEN migration is complete THEN the system SHALL update documentation to reflect libmpv usage
5. THE system SHALL maintain backward compatibility for saved user preferences

### Requirement 9

**User Story:** 作为开发者,我希望有完整的测试覆盖,以便确保 libmpv 播放器的正确性和稳定性。

#### Acceptance Criteria

1. THE system SHALL provide unit tests for libmpv JNA bindings
2. THE system SHALL provide unit tests for player lifecycle management
3. THE system SHALL provide integration tests for different stream types
4. THE system SHALL provide tests for error handling scenarios
5. WHEN all tests pass THEN the system SHALL be ready for production use
