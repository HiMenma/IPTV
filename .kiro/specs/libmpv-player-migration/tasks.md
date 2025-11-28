# Implementation Plan

- [x] 1. Setup project dependencies and JNA bindings
  - Add JNA dependencies to build.gradle.kts
  - Create LibmpvBindings interface with core libmpv C API functions
  - Implement LibmpvLoader for platform-specific library loading
  - Create data structures for libmpv events and parameters
  - _Requirements: 1.4_

- [ ]* 1.1 Write unit tests for JNA bindings
  - Test library loading on different platforms
  - Test function accessibility
  - Test structure marshalling
  - _Requirements: 1.4, 9.1_

- [x] 2. Implement LibmpvPlayerEngine core functionality
  - Implement engine initialization (mpv_create, mpv_initialize)
  - Implement engine destruction and cleanup
  - Implement property management (get/set string, double properties)
  - Implement command execution (play, pause, stop, seek)
  - Implement event handling thread and callback mechanism
  - _Requirements: 1.1, 3.1, 3.2, 3.3, 3.5_

- [ ]* 2.1 Write property test for play command state transition
  - **Property 3: Play command state transition**
  - **Validates: Requirements 3.1**

- [ ]* 2.2 Write property test for pause command state transition
  - **Property 4: Pause command state transition**
  - **Validates: Requirements 3.2**

- [ ]* 2.3 Write property test for stop command resource cleanup
  - **Property 5: Stop command resource cleanup**
  - **Validates: Requirements 3.3**

- [ ]* 2.4 Write unit tests for engine lifecycle
  - Test initialization success/failure
  - Test proper cleanup
  - Test multiple init/destroy cycles
  - _Requirements: 1.1, 9.2_

- [x] 3. Implement playback control and property management
  - Implement volume control (setVolume, getVolume)
  - Implement position tracking (getPosition, getDuration)
  - Implement pause state query (isPaused)
  - Implement file loading (loadFile)
  - _Requirements: 3.4, 3.6, 3.7_

- [ ]* 3.1 Write property test for volume control consistency
  - **Property 6: Volume control consistency**
  - **Validates: Requirements 3.4**

- [ ]* 3.2 Write property test for seek position consistency
  - **Property 7: Seek position consistency**
  - **Validates: Requirements 3.5**

- [ ]* 3.3 Write property test for position monotonicity
  - **Property 8: Position monotonicity during playback**
  - **Validates: Requirements 3.6**

- [x] 4. Implement configuration and hardware acceleration
  - Create LibmpvConfiguration data class
  - Implement setOption method for libmpv options
  - Implement hardware acceleration configuration (hwdec)
  - Implement network buffering configuration (cache, demuxer)
  - Implement audio/video output configuration
  - _Requirements: 1.3, 7.1, 7.2, 7.3, 7.4_

- [ ]* 4.1 Write property test for hardware acceleration configuration
  - **Property 2: Hardware acceleration configuration**
  - **Validates: Requirements 1.3**

- [ ]* 4.2 Write property test for invalid configuration fallback
  - **Property 14: Invalid configuration fallback**
  - **Validates: Requirements 7.5**

- [x] 5. Implement LibmpvFrameRenderer for video rendering
  - Create render context (mpv_render_context_create)
  - Implement frame acquisition (mpv_render_context_render)
  - Implement pixel format conversion (libmpv format to RGBA)
  - Implement aspect ratio calculation
  - Implement Compose integration (renderToCompose)
  - _Requirements: 6.1, 6.2, 6.3_

- [ ]* 5.1 Write property test for frame rendering availability
  - **Property 11: Frame rendering availability**
  - **Validates: Requirements 6.1**

- [ ]* 5.2 Write property test for pixel format conversion validity
  - **Property 12: Pixel format conversion validity**
  - **Validates: Requirements 6.2**

- [ ]* 5.3 Write property test for aspect ratio preservation
  - **Property 13: Aspect ratio preservation**
  - **Validates: Requirements 6.3**

- [x] 6. Implement error handling and logging
  - Create LibmpvError sealed class hierarchy
  - Implement error callback mechanism
  - Implement error recovery strategies (retry, fallback)
  - Implement comprehensive error logging
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 6.1 Write property test for error logging completeness
  - **Property 9: Error logging completeness**
  - **Validates: Requirements 4.5**

- [ ]* 6.2 Write unit tests for error handling
  - Test network error handling
  - Test file not found handling
  - Test error callback invocation
  - _Requirements: 4.1, 4.3, 9.4_

- [x] 7. Implement LibmpvPlayerImplementation
  - Create LibmpvPlayerImplementation class implementing PlayerImplementation
  - Implement isAvailable() to check libmpv installation
  - Implement getUnavailableReason() with installation instructions
  - Add LIBMPV to PlayerImplementationType enum
  - _Requirements: 1.1, 1.2, 1.5, 5.1_

- [ ]* 7.1 Write property test for libmpv initialization consistency
  - **Property 1: libmpv initialization consistency**
  - **Validates: Requirements 1.1**

- [x] 8. Implement LibmpvVideoPlayer Composable
  - Create LibmpvVideoPlayer composable function
  - Implement lifecycle management (DisposableEffect)
  - Integrate LibmpvPlayerEngine and LibmpvFrameRenderer
  - Implement PlayerControls callback
  - Implement PlayerState updates
  - Implement error handling and callbacks
  - _Requirements: 5.2, 5.3, 5.4_

- [ ]* 8.1 Write property test for PlayerState update consistency
  - **Property 10: PlayerState update consistency**
  - **Validates: Requirements 5.3**

- [x] 9. Wire LibmpvPlayerImplementation into VideoPlayer composable
  - Update VideoPlayer.desktop.kt to use LibmpvPlayerImplementation
  - Ensure PlayerControls interface compatibility
  - Ensure PlayerState updates work correctly
  - Ensure error callbacks work correctly
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 10. Update dependency injection configuration
  - Update DesktopPlayerModule to provide LibmpvPlayerImplementation
  - Configure libmpv as default player
  - Remove FFmpeg player from DI (keep code for now)
  - _Requirements: 1.1_

- [ ] 11. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 12. Write integration tests for stream types
  - Write HLS stream integration test
  - Write HTTP stream integration test
  - Write local file integration test
  - Write RTSP stream integration test
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 9.3_

- [ ]* 13. Write codec support tests
  - Test H.264 codec support
  - Test H.265 codec support
  - Test VP9 codec support
  - Test AV1 codec support
  - _Requirements: 2.5_

- [x] 14. Remove FFmpeg player implementation
  - Remove FFmpegPlayerImplementation.kt
  - Remove FFmpegVideoPlayer.desktop.kt
  - Remove all files in player/ffmpeg/ directory
  - Remove FFmpeg-related test files
  - Update DesktopPlayerModule to remove FFmpeg references
  - _Requirements: 8.1, 8.3_

- [x] 15. Remove JavaCV dependencies
  - Remove JavaCV dependencies from build.gradle.kts
  - Remove JavaCV-related imports
  - Verify build succeeds without JavaCV
  - _Requirements: 8.2_

- [x] 16. Update documentation
  - Update README.md with libmpv installation instructions
  - Create LIBMPV_SETUP_GUIDE.md with platform-specific setup
  - Update BUILD_GUIDE.md to reflect new dependencies
  - Document libmpv configuration options
  - Add troubleshooting guide for common libmpv issues
  - _Requirements: 8.4_

- [x] 17. Test user preferences compatibility
  - Verify existing user preferences still work
  - Test player selection preferences
  - Test volume preferences
  - Ensure no data loss during migration
  - _Requirements: 8.5_

- [ ] 18. Final checkpoint - Comprehensive testing
  - Ensure all tests pass, ask the user if questions arise.
