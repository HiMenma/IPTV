# Chewie Integration Testing Guide

## Overview
This document provides manual testing steps for the Chewie video player integration.

## Prerequisites
- Flutter development environment set up
- Physical device or emulator/simulator for testing
- Test IPTV stream URLs

## Test Stream URLs
You can use these public test streams:
- `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8` (HLS test stream)
- `http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4` (MP4 test stream)

## Testing Steps

### 1. Android Testing
```bash
flutter run -d android
```

**Test Cases:**
- [ ] App launches successfully
- [ ] Create a configuration (M3U network with test URL)
- [ ] Select a channel from the list
- [ ] Video player initializes and displays Chewie controls
- [ ] Play/pause controls work
- [ ] Volume control works
- [ ] Fullscreen mode works
- [ ] Back button returns to channel list
- [ ] Error handling works with invalid URL

### 2. iOS Testing
```bash
flutter run -d ios
```

**Test Cases:**
- [ ] App launches successfully
- [ ] Create a configuration (M3U network with test URL)
- [ ] Select a channel from the list
- [ ] Video player initializes and displays Chewie controls
- [ ] Play/pause controls work
- [ ] Volume control works
- [ ] Fullscreen mode works
- [ ] Back button returns to channel list
- [ ] Error handling works with invalid URL

### 3. Web Testing
```bash
flutter run -d chrome
```

**Test Cases:**
- [ ] App launches successfully in browser
- [ ] Create a configuration (M3U network with test URL)
- [ ] Select a channel from the list
- [ ] Video player initializes and displays Chewie controls
- [ ] Play/pause controls work
- [ ] Volume control works
- [ ] Fullscreen mode works
- [ ] Back button returns to channel list
- [ ] Error handling works with invalid URL

### 4. Windows Testing
```bash
flutter run -d windows
```

**Test Cases:**
- [ ] App launches successfully
- [ ] Create a configuration (M3U network with test URL)
- [ ] Select a channel from the list
- [ ] Video player initializes and displays Chewie controls
- [ ] Play/pause controls work
- [ ] Volume control works
- [ ] Fullscreen mode works
- [ ] Back button returns to channel list
- [ ] Error handling works with invalid URL

### 5. macOS Testing
```bash
flutter run -d macos
```

**Test Cases:**
- [ ] App launches successfully
- [ ] Create a configuration (M3U network with test URL)
- [ ] Select a channel from the list
- [ ] Video player initializes and displays Chewie controls
- [ ] Play/pause controls work
- [ ] Volume control works
- [ ] Fullscreen mode works
- [ ] Back button returns to channel list
- [ ] Error handling works with invalid URL

### 6. Linux Testing
```bash
flutter run -d linux
```

**Test Cases:**
- [ ] App launches successfully
- [ ] Create a configuration (M3U network with test URL)
- [ ] Select a channel from the list
- [ ] Video player initializes and displays Chewie controls
- [ ] Play/pause controls work
- [ ] Volume control works
- [ ] Fullscreen mode works
- [ ] Back button returns to channel list
- [ ] Error handling works with invalid URL

## Known Limitations

### Unit Test Environment
- Video player and wakelock plugins require platform implementations
- Unit tests will show platform errors - this is expected
- Integration tests should be run on actual devices/platforms

### Platform-Specific Notes
- **Web**: Some video formats may not be supported depending on browser
- **Desktop**: Fullscreen behavior may vary by platform
- **Mobile**: Screen wake lock should keep screen on during playback

## Verification Checklist

After testing on each platform, verify:
- [ ] Video playback works correctly
- [ ] Chewie controls are visible and functional
- [ ] No crashes or unexpected errors
- [ ] Error messages are user-friendly
- [ ] History is recorded when playing channels
- [ ] Resources are properly released when stopping playback

## Reporting Issues

If you encounter issues during testing:
1. Note the platform and Flutter version
2. Capture error messages or stack traces
3. Document steps to reproduce
4. Check if the issue is platform-specific or affects all platforms
