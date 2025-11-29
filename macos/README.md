# IPTV Player - macOS Native Application

This directory contains the native macOS implementation of the IPTV Player application, built with Swift and SwiftUI.

## Architecture Decision

After careful evaluation, we have decided to implement the macOS application **entirely in native Swift** without attempting to share Kotlin code via Kotlin/Native or other FFI mechanisms.

**Key Documents**:
- [CODE_SHARING_STRATEGY.md](./CODE_SHARING_STRATEGY.md) - Detailed analysis and rationale
- [ARCHITECTURE_DECISION.md](./ARCHITECTURE_DECISION.md) - Architecture Decision Record
- [INTERFACE_SPECIFICATIONS.md](./INTERFACE_SPECIFICATIONS.md) - Complete interface specifications

## Rationale Summary

**Why Native Swift?**
1. ✅ Minimal code duplication (~250 lines of simple logic)
2. ✅ Faster development than setting up Kotlin/Native
3. ✅ Better debugging and development experience
4. ✅ Perfect platform integration (async/await, Combine, SwiftUI)
5. ✅ Simpler maintenance and CI/CD
6. ✅ No FFI overhead or complexity

**What We're Not Sharing**:
- M3U Parser (~60 lines)
- Xtream API Client (~150 lines)
- Data Models (~40 lines)

This is straightforward code that's easy to implement natively and maintain.

## Project Structure

```
macos/
├── IPTVPlayer.xcodeproj/          # Xcode project
├── IPTVPlayer/                     # Main application
│   ├── App/                        # Application entry point
│   ├── Views/                      # SwiftUI views
│   ├── ViewModels/                 # MVVM view models
│   ├── Services/                   # Business logic services
│   │   ├── M3UParser.swift
│   │   ├── XtreamClient.swift
│   │   ├── PlaylistRepository.swift
│   │   ├── FavoriteRepository.swift
│   │   └── VideoPlayerService.swift
│   ├── Models/                     # Data models
│   │   ├── Channel.swift
│   │   ├── Playlist.swift
│   │   ├── Category.swift
│   │   └── XtreamAccount.swift
│   └── Resources/                  # Assets and resources
├── IPTVPlayerTests/                # Unit and integration tests
├── CODE_SHARING_STRATEGY.md        # Detailed strategy analysis
├── ARCHITECTURE_DECISION.md        # ADR for native implementation
├── INTERFACE_SPECIFICATIONS.md     # Complete interface specs
├── PROJECT_SETUP.md                # Project setup guide
└── README.md                       # This file
```

## Technology Stack

- **Language**: Swift 5.9+
- **UI Framework**: SwiftUI
- **Architecture**: MVVM (Model-View-ViewModel)
- **Persistence**: Core Data
- **Networking**: URLSession (native)
- **Video Player**: AVPlayer (native)
- **Testing**: XCTest + swift-check (property-based testing)
- **Dependency Management**: Swift Package Manager

## Key Features

- ✅ M3U playlist support (URL and file)
- ✅ Xtream Codes API support
- ✅ Live TV streaming (HLS, RTSP, HTTP)
- ✅ VOD content support
- ✅ Favorite channels
- ✅ Category browsing
- ✅ Hardware-accelerated video playback
- ✅ Native macOS UI/UX

## Development Status

### ✅ Version 1.0.0 - Released (November 29, 2025)

All core features have been implemented and tested:

- [x] Project structure setup
- [x] Core Data model definition
- [x] CI/CD pipeline configuration
- [x] Code sharing strategy decision
- [x] M3U parser implementation
- [x] Xtream API client implementation
- [x] Repository implementations (Playlist, Favorite)
- [x] Video player integration (AVPlayer)
- [x] UI implementation (all views and view models)
- [x] Error handling infrastructure
- [x] Performance optimization
- [x] Security implementation (Keychain, input validation)
- [x] Code signing and distribution setup
- [x] Comprehensive documentation
- [x] Test suite (unit, property-based, integration)
- [x] Manual testing and validation
- [x] DMG creation and distribution

### Planned for Future Releases
- [ ] EPG (Electronic Program Guide) display
- [ ] Recording functionality
- [ ] Multi-window support
- [ ] Keyboard shortcuts customization
- [ ] Theme customization
- [ ] Advanced search filters
- [ ] Playlist import/export

## Getting Started

### Prerequisites
- macOS 13.0 or later
- Xcode 15.0 or later
- Swift 5.9 or later

### Building

```bash
cd macos
xcodebuild -project IPTVPlayer.xcodeproj \
           -scheme IPTVPlayer \
           -configuration Debug \
           build
```

### Running Tests

```bash
xcodebuild -project IPTVPlayer.xcodeproj \
           -scheme IPTVPlayer \
           -configuration Debug \
           test
```

### Running the App

Open `IPTVPlayer.xcodeproj` in Xcode and press `Cmd+R` to build and run.

## Testing Strategy

### Unit Tests
- Test individual components in isolation
- Mock external dependencies
- Focus on business logic correctness

### Property-Based Tests
- Verify universal properties hold across all inputs
- Use swift-check for property testing
- Ensure behavioral equivalence with other platforms

### Integration Tests
- Test complete workflows
- Use real Core Data stack (in-memory)
- Test with real network requests (when appropriate)

### UI Tests
- Test user interactions
- Verify navigation flows
- Test error states

## Implementation Guidelines

### Code Style
- Follow Swift API Design Guidelines
- Use SwiftLint for consistency
- Document public APIs with DocC comments

### Error Handling
- Use Swift's typed error system
- Provide user-friendly error messages
- Include recovery suggestions

### Async/Await
- Use Swift's native async/await for asynchronous operations
- Avoid callback-based APIs
- Use actors for thread-safe state management

### SwiftUI Best Practices
- Keep views small and focused
- Use @StateObject for view model ownership
- Use @ObservedObject for passed-in view models
- Prefer composition over inheritance

## Contributing

When implementing features:

1. **Read the specifications**: Check [INTERFACE_SPECIFICATIONS.md](./INTERFACE_SPECIFICATIONS.md)
2. **Follow the design**: Reference the design document in `.kiro/specs/native-desktop-migration/`
3. **Write tests first**: Implement tests before or alongside implementation
4. **Document your code**: Add DocC comments for public APIs
5. **Run tests**: Ensure all tests pass before committing

## Related Documentation

- [Requirements Document](../.kiro/specs/native-desktop-migration/requirements.md)
- [Design Document](../.kiro/specs/native-desktop-migration/design.md)
- [Tasks Document](../.kiro/specs/native-desktop-migration/tasks.md)
- [CI/CD Guide](./CI_CD_GUIDE.md)
- [Project Setup](./PROJECT_SETUP.md)

## License

[Add license information]

## Contact

[Add contact information]

---

**Last Updated**: 2025-11-29  
**Version**: 1.0.0  
**Status**: Released  
**Release Notes**: See [RELEASE_NOTES.md](./RELEASE_NOTES.md)
