# Changelog

All notable changes to the IPTV Player macOS application will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2025-11-29

### Added

#### Core Features
- M3U playlist parser with support for EXTINF tags and metadata
- Xtream Codes API client with full authentication and content retrieval
- Playlist repository with Core Data persistence
- Favorite repository for managing favorite channels
- Video player service using AVPlayer with hardware acceleration
- Support for HLS, RTSP, and HTTP streaming protocols

#### User Interface
- Main window with sidebar navigation (SwiftUI)
- Playlist sidebar view with add/delete/rename functionality
- Channel list view with search and filtering
- Video player view with playback controls
- Error view with user-friendly error messages
- Add playlist dialogs (M3U URL, M3U File, Xtream Codes)
- Cached async image loading for channel thumbnails

#### ViewModels
- MainViewModel for playlist and channel management
- PlayerViewModel for video playback control

#### Services
- AppError system with categorized error types
- Logger service for debugging and diagnostics
- RetryMechanism with exponential backoff
- ErrorPresenter for user-facing error messages
- ImageCache for efficient thumbnail caching
- PerformanceMonitor for tracking app performance
- KeychainManager for secure credential storage
- InputValidator for data validation

#### Data Models
- Channel model with metadata support
- Playlist model with type discrimination (M3U_URL, M3U_FILE, XTREAM)
- Category model for content organization
- XtreamAccount model for API credentials
- Favorite model for favorite channels
- Core Data entities with relationships

#### Testing
- Unit tests for all services
- Property-based tests for M3U parser
- Property-based tests for video player controls
- Integration tests for repositories
- Error handling tests
- Security tests for Keychain and input validation
- Test coverage: 80%+

#### Infrastructure
- Xcode project setup with MVVM architecture
- Core Data model with migration support
- GitHub Actions CI/CD pipeline
- Automated build and test workflow
- DMG creation and distribution
- Code signing configuration
- Notarization support

#### Documentation
- Architecture decision records
- Code sharing strategy analysis
- Interface specifications
- Project setup guide
- CI/CD guide
- User guide
- Security architecture documentation
- Performance optimization guide
- Manual testing guide
- API documentation

#### Security
- Keychain integration for credential storage
- HTTPS enforcement for network requests
- Input validation for all user inputs
- App Sandbox compliance
- Hardened runtime configuration

#### Performance
- Hardware-accelerated video decoding
- Lazy loading for channel lists
- Image caching system
- Memory optimization for large playlists
- Performance monitoring and logging

### Technical Details

#### Architecture
- MVVM (Model-View-ViewModel) pattern
- Protocol-oriented design
- Swift async/await for asynchronous operations
- Combine framework for reactive programming
- Core Data for persistence
- AVFoundation for video playback

#### Build Configuration
- Minimum deployment target: macOS 13.0
- Swift version: 5.0
- Xcode version: 15.0+
- Code signing: Developer ID Application
- Hardened runtime enabled
- App Sandbox enabled

#### Dependencies
- No external dependencies (all native frameworks)
- Swift Package Manager ready
- Modular architecture for future extensions

### Fixed
- N/A (initial release)

### Changed
- N/A (initial release)

### Deprecated
- N/A (initial release)

### Removed
- N/A (initial release)

### Security
- Implemented secure credential storage using macOS Keychain
- Added input validation to prevent injection attacks
- Enforced HTTPS for all network communications
- Enabled App Sandbox for system-level security

## [Unreleased]

### Planned Features
- EPG (Electronic Program Guide) display
- Recording functionality
- Multi-window support
- Keyboard shortcuts customization
- Theme customization (light/dark mode enhancements)
- Advanced search filters
- Playlist import/export
- Subtitle support
- Audio track selection
- Playback speed control
- Picture-in-Picture mode
- AirPlay support
- Chromecast support

### Planned Improvements
- Enhanced error recovery
- Improved playlist synchronization
- Better network error handling
- Optimized memory usage for very large playlists
- Enhanced video buffering strategies
- Improved UI animations and transitions

### Planned Fixes
- Address any issues reported by users
- Performance optimizations based on real-world usage
- UI/UX refinements based on user feedback

---

## Version Numbering

This project follows [Semantic Versioning](https://semver.org/):

- **MAJOR** version for incompatible API changes
- **MINOR** version for new functionality in a backwards compatible manner
- **PATCH** version for backwards compatible bug fixes

## Release Process

1. Update version number in Xcode project
2. Update CHANGELOG.md with release notes
3. Update RELEASE_NOTES.md with detailed information
4. Create git tag: `git tag -a v1.0.0 -m "Release version 1.0.0"`
5. Push tag: `git push origin v1.0.0`
6. GitHub Actions automatically builds and creates release
7. Verify DMG and checksums
8. Publish release notes

## Links

- [GitHub Repository](https://github.com/menmapro/iptv-player)
- [Issue Tracker](https://github.com/menmapro/iptv-player/issues)
- [Releases](https://github.com/menmapro/iptv-player/releases)
- [Documentation](./README.md)

---

**Note**: This changelog is maintained manually. For a complete list of commits, see the [GitHub commit history](https://github.com/menmapro/iptv-player/commits/main).
