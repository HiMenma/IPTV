# 📺 IPTV Player

A professional, high-performance IPTV player built with Flutter, focusing on stability, deep UI customization, and self-healing playback.

## 🚀 Key Features

- **Multi-Source Support**: M3U Network URLs, Local M3U Files, and Xtream Codes API.
- **Self-Healing Engine**: Integrated playback watchdog that automatically reconnects on stalls or token expiry.
- **Native Stability**: Fine-tuned for macOS and Android with dedicated rendering optimizations.
- **Glassmorphism UI**: Immersive, clutter-free player interface with smooth animations.
- **Database Self-Healing**: Automated SQLite schema maintenance to prevent crashes after updates.
- **Theme Management**: Manual toggle for Light/Dark/System modes.

## 🏗️ Technical Stack

- **Framework**: Flutter (Dart)
- **Engine**: VideoPlayer + Chewie (Highly customized)
- **Persistence**: SQLite (sqflite)
- **State Management**: Provider (MVVM)

## 📑 Documentation

- [Project Architecture & Key Files](./ARCHITECTURE.md) - Detailed overview of the technical stack and file roles.
- [Development Notes & Bug Fixes](./DEV_NOTES.md) - History of optimizations and platform-specific fixes.
- [Build Guide](./BUILD_GUIDE.md) - Instructions for compiling Android and macOS apps.

## 🛠️ Getting Started

1.  **Prerequisites**: Install Flutter SDK (>=3.4.0).
2.  **Install Dependencies**: `flutter pub get`
3.  **Run Build**: `./build_all.sh --all`

---
Developed with ❤️ by Gemini OmG Team
