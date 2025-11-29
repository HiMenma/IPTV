# PlayerViewModel Implementation

## Overview

This document describes the implementation of `PlayerViewModel` for the macOS IPTV Player application. The PlayerViewModel is responsible for managing video player state, handling user controls, and coordinating with the VideoPlayerService.

## Implementation Date

November 28, 2024

## Requirements Addressed

- **Requirement 3.5**: macOS app should support video playback with AVPlayer
- **Requirement 3.6**: macOS app should support playback controls (play, pause, volume, fullscreen)

## Architecture

### Class Structure

```swift
@MainActor
class PlayerViewModel: ObservableObject {
    // Published state properties
    @Published var currentChannel: Channel?
    @Published var isPlaying: Bool
    @Published var volume: Double
    @Published var isFullscreen: Bool
    @Published var currentTime: TimeInterval
    @Published var duration: TimeInterval
    @Published var isBuffering: Bool
    @Published var errorMessage: String?
    @Published var showControls: Bool
    
    // Dependencies
    private let playerService: VideoPlayerService
    private var cancellables: Set<AnyCancellable>
}
```

## Key Features

### 1. State Management

The PlayerViewModel maintains comprehensive playback state:

- **Current Channel**: Tracks which channel is currently playing
- **Playback State**: isPlaying, currentTime, duration
- **UI State**: volume, isFullscreen, showControls, isBuffering
- **Error State**: errorMessage for displaying player errors

### 2. Player Control Methods

#### Basic Playback Controls

```swift
func play(channel: Channel)      // Start playing a channel
func pause()                      // Pause playback
func resume()                     // Resume playback
func stop()                       // Stop and cleanup
func togglePlayPause()            // Toggle between play/pause
```

#### Seeking Controls

```swift
func seek(to time: TimeInterval)           // Seek to specific time
func seekForward(by seconds: TimeInterval) // Seek forward (default 10s)
func seekBackward(by seconds: TimeInterval) // Seek backward (default 10s)
```

#### Volume Controls

```swift
func setVolume(_ volume: Double)           // Set volume (0.0 to 1.0)
func increaseVolume(by amount: Double)     // Increase volume
func decreaseVolume(by amount: Double)     // Decrease volume
func toggleMute()                          // Toggle mute
```

#### Fullscreen Control

```swift
func toggleFullscreen()  // Toggle fullscreen mode
```

### 3. Reactive State Synchronization

The PlayerViewModel uses Combine to subscribe to VideoPlayerService publishers:

```swift
private func setupSubscriptions() {
    // Sync playing state
    playerService.isPlayingPublisher
        .receive(on: DispatchQueue.main)
        .assign(to: &$isPlaying)
    
    // Sync time updates
    playerService.currentTimePublisher
        .receive(on: DispatchQueue.main)
        .assign(to: &$currentTime)
    
    // Sync duration
    playerService.durationPublisher
        .receive(on: DispatchQueue.main)
        .assign(to: &$duration)
    
    // Sync buffering state
    playerService.isBufferingPublisher
        .receive(on: DispatchQueue.main)
        .assign(to: &$isBuffering)
    
    // Sync fullscreen state
    playerService.isFullscreenPublisher
        .receive(on: DispatchQueue.main)
        .assign(to: &$isFullscreen)
    
    // Handle errors
    playerService.errorPublisher
        .receive(on: DispatchQueue.main)
        .sink { [weak self] error in
            self?.handlePlayerError(error)
        }
        .store(in: &cancellables)
    
    // Sync volume changes to service
    $volume
        .dropFirst()
        .sink { [weak self] newVolume in
            self?.playerService.volume = Float(newVolume)
        }
        .store(in: &cancellables)
}
```

### 4. Controls Auto-Hide

The PlayerViewModel implements intelligent controls visibility:

- Controls show when user interacts with player
- Controls auto-hide after 3 seconds of inactivity (when playing)
- Controls remain visible when paused or when there's an error
- User can manually toggle controls visibility

```swift
func showControlsBriefly() {
    showControls = true
    cancelControlsTimeout()
    
    guard isPlaying else { return }
    
    controlsTimeoutTask = Task { @MainActor in
        try await Task.sleep(nanoseconds: 3_000_000_000)
        if isPlaying {
            showControls = false
        }
    }
}
```

### 5. Error Handling

The PlayerViewModel handles errors from the VideoPlayerService:

```swift
private func handlePlayerError(_ error: AppError) {
    errorMessage = error.localizedDescription
    showControls = true
    cancelControlsTimeout()
}

func clearError() {
    errorMessage = nil
}
```

### 6. Computed Properties

Convenient computed properties for UI binding:

```swift
var currentTimeFormatted: String  // "MM:SS" format
var durationFormatted: String     // "MM:SS" format
var progress: Double              // 0.0 to 1.0
var hasContent: Bool              // Whether content is loaded
```

### 7. AVPlayer Access

For UI integration (e.g., VideoPlayerView):

```swift
func getAVPlayer() -> AVPlayer? {
    return playerService.getAVPlayer()
}
```

## Usage Example

### In a SwiftUI View

```swift
struct PlayerView: View {
    @StateObject private var viewModel: PlayerViewModel
    
    var body: some View {
        VStack {
            // Video player view
            VideoPlayerView(player: viewModel.getAVPlayer())
            
            // Controls overlay
            if viewModel.showControls {
                PlayerControlsView(viewModel: viewModel)
            }
            
            // Buffering indicator
            if viewModel.isBuffering {
                ProgressView()
            }
            
            // Error message
            if let error = viewModel.errorMessage {
                Text(error)
                    .foregroundColor(.red)
            }
        }
        .onTapGesture {
            viewModel.toggleControls()
        }
    }
}
```

### Playing a Channel

```swift
// In MainViewModel or ContentView
func playChannel(_ channel: Channel) {
    playerViewModel.play(channel: channel)
}
```

## Integration with VideoPlayerService

The PlayerViewModel acts as a bridge between the UI and VideoPlayerService:

1. **UI → ViewModel → Service**: User interactions trigger ViewModel methods, which call VideoPlayerService
2. **Service → ViewModel → UI**: Service state changes are published, ViewModel subscribes and updates @Published properties, UI reacts

This separation ensures:
- Clean architecture with clear responsibilities
- Testable business logic
- Reactive UI updates
- Proper error handling and state management

## Thread Safety

All PlayerViewModel operations are marked with `@MainActor` to ensure:
- All UI updates happen on the main thread
- No race conditions with @Published properties
- Safe interaction with VideoPlayerService (also @MainActor)

## Memory Management

The PlayerViewModel properly manages resources:

```swift
deinit {
    cancelControlsTimeout()
    cancellables.removeAll()
}
```

- Cancels pending timeout tasks
- Removes all Combine subscriptions
- Prevents memory leaks

## Testing Considerations

The PlayerViewModel is designed to be testable:

1. **Dependency Injection**: VideoPlayerService is injected, allowing mock implementations
2. **Observable State**: All state is @Published, making it easy to verify in tests
3. **Synchronous Methods**: Most methods are synchronous (except play which is async in service)
4. **Clear Responsibilities**: Each method has a single, well-defined purpose

## Future Enhancements

Potential improvements for future iterations:

1. **Playback Speed Control**: Add methods for adjusting playback speed
2. **Subtitle Support**: Add subtitle track selection and display
3. **Audio Track Selection**: Support multiple audio tracks
4. **Picture-in-Picture**: Add PiP mode support
5. **Playback History**: Track recently played channels
6. **Keyboard Shortcuts**: Add keyboard shortcut handling
7. **Gesture Controls**: Add swipe gestures for volume/seeking

## Files Created

- `macos/IPTVPlayer/ViewModels/PlayerViewModel.swift` - Main implementation
- `macos/add_player_viewmodel.py` - Script to add file to Xcode project
- `macos/PLAYER_VIEWMODEL_IMPLEMENTATION.md` - This documentation

## Verification

The PlayerViewModel implementation has been verified to:

1. ✅ Compile without errors
2. ✅ Follow Swift and SwiftUI best practices
3. ✅ Use proper @MainActor annotations
4. ✅ Implement all required control methods
5. ✅ Handle errors appropriately
6. ✅ Manage memory correctly
7. ✅ Integrate with VideoPlayerService
8. ✅ Support reactive state updates

## Next Steps

To complete the player implementation:

1. **Task 18**: Implement PlayerView (UI component)
2. **Task 18.1**: Write UI tests for player view
3. Integration with MainViewModel for channel selection
4. End-to-end testing of playback flow

## References

- Design Document: `.kiro/specs/native-desktop-migration/design.md`
- Requirements: `.kiro/specs/native-desktop-migration/requirements.md`
- VideoPlayerService: `macos/IPTVPlayer/Services/VideoPlayerService.swift`
- MainViewModel: `macos/IPTVPlayer/ViewModels/MainViewModel.swift`
