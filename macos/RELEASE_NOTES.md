# IPTV Player for macOS - Release Notes

## Version 1.0.0 (Initial Release)

**Release Date**: November 29, 2025

### Overview

This is the initial release of IPTV Player for macOS, a native application built with Swift and SwiftUI. This release represents a complete migration from the Kotlin Multiplatform desktop implementation to a fully native macOS experience.

### What's New

#### Core Features
- **M3U Playlist Support**: Import and manage M3U playlists via URL or local file
  - Automatic parsing of EXTINF tags
  - Support for channel metadata (name, logo, group, tvg-id)
  - Graceful handling of malformed entries

- **Xtream Codes API Integration**: Full support for Xtream Codes protocol
  - Authentication with server credentials
  - Live TV categories and streams
  - VOD content browsing
  - EPG data integration
  - Automatic retry with exponential backoff

- **Video Playback**: High-performance native video player
  - HLS (HTTP Live Streaming) support
  - RTSP (Real-Time Streaming Protocol) support
  - HTTP direct streaming support
  - Hardware-accelerated decoding
  - Automatic reconnection on network interruption

- **Playlist Management**
  - Add playlists from M3U URL
  - Import playlists from local M3U files
  - Add Xtream Codes accounts
  - Rename and delete playlists
  - Automatic playlist synchronization

- **Channel Browsing**
  - Category-based organization
  - Search and filter functionality
  - Channel thumbnails and metadata
  - Favorite channels management

- **Favorites System**
  - Mark channels as favorites
  - Quick access to favorite channels
  - Persistent favorites across sessions

#### User Interface
- **Native macOS Design**: Built with SwiftUI following macOS Human Interface Guidelines
- **Sidebar Navigation**: Intuitive playlist and favorites sidebar
- **Channel List**: Efficient scrolling with lazy loading
- **Video Player**: Full-screen support with playback controls
- **Error Handling**: User-friendly error messages with recovery suggestions

#### Performance
- **Hardware Acceleration**: Leverages macOS native video decoding
- **Image Caching**: Efficient thumbnail caching system
- **Performance Monitoring**: Built-in performance tracking
- **Memory Optimization**: Efficient memory management for large playlists

#### Data Persistence
- **Core Data Integration**: Reliable local data storage
- **Automatic Backups**: Data preserved across app updates
- **Migration Support**: Seamless database schema updates

#### Security
- **Keychain Integration**: Secure storage for Xtream Codes credentials
- **HTTPS Enforcement**: Secure network communications
- **Input Validation**: Protection against malformed data
- **Sandboxing**: macOS App Sandbox compliance

### Technical Highlights

#### Architecture
- **MVVM Pattern**: Clean separation of concerns
- **Swift Async/Await**: Modern asynchronous programming
- **Combine Framework**: Reactive data flow
- **Protocol-Oriented Design**: Flexible and testable architecture

#### Testing
- **Comprehensive Test Suite**: 
  - Unit tests for all services
  - Property-based tests for parsers and repositories
  - Integration tests for data persistence
  - Error handling tests
- **Test Coverage**: 80%+ code coverage
- **CI/CD Integration**: Automated testing on every commit

#### Build & Distribution
- **Automated Builds**: GitHub Actions CI/CD pipeline
- **Code Signing**: Developer ID signed application
- **DMG Distribution**: Professional installer package
- **Notarization**: Apple notarized for Gatekeeper

### System Requirements

- **Operating System**: macOS 13.0 (Ventura) or later
- **Architecture**: Apple Silicon (M1/M2/M3) or Intel x86_64
- **Memory**: 4 GB RAM minimum, 8 GB recommended
- **Storage**: 100 MB available space
- **Network**: Internet connection required for streaming

### Installation

1. Download the DMG file from the release page
2. Open the DMG file
3. Drag "IPTV Player.app" to your Applications folder
4. Launch from Applications (first launch may require right-click → Open)
5. Grant necessary permissions when prompted

### Getting Started

#### Adding an M3U Playlist
1. Click the "+" button in the sidebar
2. Select "Add M3U URL" or "Add M3U File"
3. Enter the playlist URL or select a local file
4. Wait for the playlist to load
5. Browse channels by category

#### Adding a Xtream Codes Account
1. Click the "+" button in the sidebar
2. Select "Add Xtream Account"
3. Enter server URL, username, and password
4. Click "Add" to authenticate
5. Browse available channels and categories

#### Playing a Channel
1. Select a playlist from the sidebar
2. Browse or search for a channel
3. Click on a channel to start playback
4. Use playback controls for volume, seek, and fullscreen

#### Managing Favorites
1. Click the star icon next to any channel
2. Access favorites from the "Favorites" section in sidebar
3. Click the star again to remove from favorites

### Known Issues

- **First Launch Delay**: Initial launch may take a few seconds while setting up Core Data
- **Large Playlists**: Playlists with 1000+ channels may take time to load initially
- **Network Errors**: Some IPTV streams may have intermittent connectivity issues (provider-dependent)

### Troubleshooting

#### App Won't Open
- Right-click the app and select "Open" to bypass Gatekeeper
- Check System Settings → Privacy & Security for blocked apps

#### Video Won't Play
- Verify your internet connection
- Check if the stream URL is still valid
- Try a different channel to isolate the issue
- Check Console.app for detailed error messages

#### Playlist Won't Load
- Verify the M3U URL is accessible
- Check if the file format is valid M3U/M3U8
- Ensure Xtream Codes credentials are correct
- Check network connectivity

#### Performance Issues
- Close other resource-intensive applications
- Reduce the number of open playlists
- Clear the image cache (Settings → Advanced)
- Restart the application

### Feedback & Support

We welcome your feedback! Please report issues or suggest features:

- **GitHub Issues**: [Repository URL]
- **Email**: [Support Email]
- **Documentation**: See USER_GUIDE.md for detailed instructions

### Privacy

IPTV Player respects your privacy:
- No analytics or tracking
- No data collection
- Credentials stored securely in macOS Keychain
- All data stays on your device

### Credits

**Development Team**: MenmaPro
**Architecture**: Native Swift/SwiftUI implementation
**Testing**: Comprehensive test suite with property-based testing
**Design**: Following macOS Human Interface Guidelines

### License

Copyright © 2025 MenmaPro. All rights reserved.

### Acknowledgments

- Apple for the excellent SwiftUI and AVFoundation frameworks
- The open-source community for testing frameworks and tools
- IPTV service providers for protocol documentation

---

## Upgrade Notes

This is the initial release. Future versions will include:
- EPG (Electronic Program Guide) display
- Recording functionality
- Multi-window support
- Keyboard shortcuts customization
- Theme customization
- Advanced search filters
- Playlist import/export

---

## Version History

### 1.0.0 (November 29, 2025)
- Initial release
- Native macOS application
- M3U and Xtream Codes support
- Full video playback capabilities
- Favorites management
- Secure credential storage

---

**For detailed technical documentation, see:**
- [User Guide](USER_GUIDE.md)
- [Architecture Decision](ARCHITECTURE_DECISION.md)
- [API Documentation](API_DOCUMENTATION.md)
- [Security Architecture](SECURITY_ARCHITECTURE.md)
