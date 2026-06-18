# IPTV Player

A Flutter-based IPTV player focused on stability, UI customization, and reliable playback.

## Features

- Multi-source support: M3U network URLs, local M3U files, and Xtream Codes API
- Playback watchdog with automatic reconnection on stalls or token expiry
- Platform-optimized rendering for macOS and Android
- Glassmorphism UI with smooth animations
- SQLite schema auto-migration to prevent crashes after updates
- Manual Light/Dark/System theme toggle

## Technical Stack

- Framework: Flutter (Dart)
- Video: VideoPlayer + Chewie
- Storage: SQLite (sqflite)
- State: Provider (MVVM)

## Documentation

- [Architecture](./ARCHITECTURE.md)
- [Dev Notes](./DEV_NOTES.md)
- [Build Guide](./BUILD_GUIDE.md)

## Getting Started

1. Install Flutter SDK (>=3.4.0)
2. `flutter pub get`
3. `./build_all.sh --all`
