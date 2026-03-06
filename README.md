# IPTV Player

A professional, high-performance cross-platform IPTV player built with Flutter. Supporting Xtream Codes API, M3U/M3U8 playlists, and high-stability live streaming.

## 🚀 Features

-   **Multi-Source Support**: Seamlessly integrate Xtream Codes API, Remote M3U URLs, and local M3U files.
-   **Advanced Playback**:
    -   Stable live streaming with auto-reconnect and buffer optimization.
    -   Background audio playback and screen wake-lock support.
    -   Customizable playback speed and volume controls.
-   **Intelligent Caching**: SQLite-powered channel caching for instant loading of thousands of channels.
-   **User-Friendly UI**:
    -   Dark/Light mode support.
    -   Favorites management.
    -   Recently watched history.
-   **Cross-Platform**: Unified experience across Android, macOS, and Web.

## 🛠️ Performance Optimizations

-   **Background Parsing**: M3U parsing moved to Isolate to prevent UI jank.
-   **DB Indexing**: Optimized SQLite queries for sub-second retrieval of favorites/history.
-   **macOS Stability**: Integrated custom fixes for AVFoundation 1ms duration bugs and texture binding sync.

## 📦 Build Instructions

We provide unified build scripts for all platforms.

### macOS / Linux
```bash
# Build all platforms
./build_all.sh --all

# Build specific platform
./build_all.sh --apk
./build_all.sh --mac
./build_all.sh --web
```

### Windows
```cmd
# Run the batch script
build_all.bat --apk --win --web
```

Build outputs will be collected in the `dist/` directory.

## 🔍 Troubleshooting (macOS)

If you encounter video rendering issues (audio only) on macOS:
1.  **Xcode Frameworks**: Ensure your system content is up-to-date: `sudo xcodebuild -runFirstLaunch`.
2.  **Network Permissions**: The app is configured with `com.apple.security.app-sandbox` set to `false` to allow IPTV stream access.
3.  **HTTP Streams**: Cleartext traffic is enabled by default in `Info.plist`.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
