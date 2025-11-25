# Walkthrough - IPTV Player

I have successfully implemented the cross-platform IPTV Player for Android and Desktop (macOS/Windows).

## Features Implemented
- **Playlist Management**: Support for adding M3U URLs and Xtream Codes accounts.
- **Channel Parsing**: Custom M3U parser and Xtream API client.
- **UI**: Material Design UI with `PlaylistScreen`, `ChannelListScreen`, and `PlayerScreen`.
- **Video Player**:
    - **Android**: Uses `ExoPlayer` (Media3).
    - **Desktop**: Uses `VLCJ` (requires VLC installation).

## Verification Steps

### 1. Build Verification
Run the following command to verify the project builds successfully:
```bash
./gradlew build
```

### 2. Android Verification
To run on an Android device/emulator:
```bash
./gradlew :composeApp:installDebug
```
- Launch the app.
- Click "+" FAB.
- Add a sample M3U URL (e.g., `https://iptv-org.github.io/iptv/index.m3u`).
- Verify channels load.
- Click a channel to play.

### 3. Desktop Verification
To run on macOS:
```bash
./gradlew :composeApp:run
```
> [!IMPORTANT]
> You must have VLC Media Player installed on your system for the desktop player to work, as it uses `vlcj` which wraps the native VLC library.

## Known Limitations
- **VLC Dependency**: The desktop version relies on a system installation of VLC.
- **Error Handling**: Basic error handling is implemented, but network errors could be more user-friendly.
