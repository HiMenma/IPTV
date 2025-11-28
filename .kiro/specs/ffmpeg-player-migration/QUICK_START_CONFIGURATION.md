# Quick Start: Player Configuration

## TL;DR

The application now supports both VLC and FFmpeg video players with automatic fallback.

## Default Behavior

```bash
./gradlew run
```
- Uses VLC if installed
- Automatically falls back to FFmpeg if VLC is not available
- No configuration needed

## Switch to FFmpeg

```bash
./gradlew run -Dplayer.implementation=FFMPEG
```
- Uses FFmpeg as primary
- Falls back to VLC if FFmpeg fails (unlikely)

## Force FFmpeg Only

```bash
./gradlew run -Dplayer.implementation=FFMPEG -Dplayer.auto.fallback=false
```
- Uses only FFmpeg
- No fallback to VLC

## Force VLC Only

```bash
./gradlew run -Dplayer.implementation=VLC -Dplayer.auto.fallback=false
```
- Uses only VLC
- No fallback to FFmpeg
- Shows error if VLC not installed

## Check What's Being Used

Look for this in the console output:

```
=== Player Implementation Selection ===
Configuration:
  Preferred: FFMPEG
  Auto-fallback: true
  Fallback: VLC

Checking preferred implementation: FFmpeg Player
✓ FFmpeg Player is available
  Selected: FFmpeg Player
=======================================
```

## Common Use Cases

### Testing FFmpeg
```bash
./gradlew run -Dplayer.implementation=FFMPEG
```

### Development (Safe Mode)
```bash
./gradlew run
# Uses VLC with FFmpeg fallback
```

### Production (FFmpeg)
```bash
./gradlew run -Dplayer.implementation=FFMPEG
# Uses FFmpeg with VLC fallback
```

## Environment Variables (Alternative)

```bash
# Linux/macOS
export PLAYER_IMPLEMENTATION=FFMPEG
export PLAYER_AUTO_FALLBACK=true
./gradlew run

# Windows
set PLAYER_IMPLEMENTATION=FFMPEG
set PLAYER_AUTO_FALLBACK=true
gradlew.bat run
```

## Code Usage

### Automatic (Recommended)

```kotlin
// Uses configured implementation automatically
ConfigurableVideoPlayer(
    url = streamUrl,
    modifier = Modifier.fillMaxSize(),
    playerState = playerState,
    onPlayerControls = { controls -> },
    onError = { error -> },
    onPlayerInitFailed = { },
    isFullscreen = false
)
```

### Manual Selection

```kotlin
// Force VLC
VlcPlayerImplementation().VideoPlayer(...)

// Force FFmpeg
FFmpegPlayerImplementation().VideoPlayer(...)
```

## Troubleshooting

### VLC Not Found
**Symptom**: "VLC Media Player not installed"
**Solution**: 
- Install VLC, or
- Use FFmpeg: `-Dplayer.implementation=FFMPEG`

### Want to See Diagnostic Info
```bash
./gradlew run
# Check console for detailed diagnostic report
```

## More Information

See `PLAYER_CONFIGURATION_GUIDE.md` for complete documentation.
