# Implementation Plan

## Part 1: macOS Application Development

### Phase 1: macOS Project Setup and Infrastructure

- [ ] 1. Set up macOS project structure
  - Create Xcode project with MVVM architecture
  - Configure Swift Package Manager dependencies (Alamofire for networking, etc.)
  - Set up Core Data model with entities (Playlist, Channel, Favorite, XtreamAccount)
  - Configure project folders (App, Views, ViewModels, Services, Models)
  - Set up development and release build configurations
  - _Requirements: 9.1, 9.2, 9.3_

- [ ] 2. Configure macOS CI/CD pipeline
  - Create GitHub Actions workflow for macOS builds
  - Configure automated testing in CI pipeline
  - Set up release artifact generation (DMG)
  - _Requirements: 9.6_

- [ ] 3. Decide on code sharing strategy for macOS
  - Evaluate Kotlin/Native vs native Swift implementation
  - If using Kotlin/Native, configure build target for macOS
  - Define interface specifications
  - Document the chosen approach and rationale
  - _Requirements: 2.4_

### Phase 2: macOS Core Business Logic

- [ ] 4. Implement M3U parser (macOS)
  - Create M3UParser protocol and implementation
  - Implement parsing logic for EXTINF tags
  - Extract channel metadata (name, URL, logo, group, tvg-id)
  - Handle malformed entries gracefully
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 4.1 Write property test for M3U parser field extraction (macOS)
  - **Property 1: M3U Parser Field Extraction**
  - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**

- [ ] 4.2 Write property test for M3U parser error resilience (macOS)
  - **Property 2: M3U Parser Error Resilience**
  - **Validates: Requirements 5.5**

- [ ] 5. Implement Xtream API client (macOS)
  - Create XtreamClient protocol and implementation
  - Implement authentication endpoint
  - Implement get live categories endpoint
  - Implement get live streams endpoint
  - Implement get VOD content endpoint
  - Implement get EPG data endpoint
  - Add retry mechanism with exponential backoff
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [ ] 5.1 Write unit tests for Xtream API client (macOS)
  - Test authentication with valid credentials
  - Test authentication with invalid credentials
  - Test fetching live categories
  - Test fetching live streams
  - Test error handling and retry mechanism
  - _Requirements: 6.1, 6.2, 6.3, 6.6_

- [ ] 5.2 Write property test for Xtream API error handling (macOS)
  - **Property 9: Xtream API Error Handling**
  - **Validates: Requirements 6.6**

- [ ] 6. Implement error handling infrastructure (macOS)
  - Define error types and categories (network, parsing, database, player)
  - Implement error presentation logic
  - Add logging infrastructure
  - Implement retry mechanisms
  - _Requirements: All error handling requirements_

### Phase 3: macOS Data Persistence

- [ ] 7. Implement Core Data stack (macOS)
  - Define Core Data entities (PlaylistEntity, ChannelEntity, FavoriteEntity, XtreamAccountEntity)
  - Set up relationships between entities
  - Configure persistent store coordinator
  - Implement data migration logic
  - _Requirements: 3.8, 8.1, 8.2, 8.3, 8.4_

- [ ] 8. Implement PlaylistRepository (macOS)
  - Create PlaylistRepository protocol
  - Implement getAllPlaylists method
  - Implement getPlaylist method
  - Implement savePlaylist method
  - Implement deletePlaylist method
  - Implement updatePlaylist method
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 8.1 Write property test for playlist persistence round trip (macOS)
  - **Property 6: Playlist Persistence Round Trip**
  - **Validates: Requirements 8.1, 8.3**

- [ ] 8.2 Write property test for playlist deletion completeness (macOS)
  - **Property 7: Playlist Deletion Completeness**
  - **Validates: Requirements 8.4**

- [ ] 8.3 Write property test for database transaction consistency (macOS)
  - **Property 8: Database Transaction Consistency**
  - **Validates: Requirements 8.5**

- [ ] 8.4 Write integration tests for PlaylistRepository (macOS)
  - Test save and load playlist
  - Test delete playlist with cascading deletes
  - Test update playlist
  - Test transaction rollback on error
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 9. Implement FavoriteRepository (macOS)
  - Create FavoriteRepository protocol
  - Implement add favorite method
  - Implement remove favorite method
  - Implement get all favorites method
  - Implement check if favorite method
  - _Requirements: 3.7, 8.2_

- [ ] 9.1 Write property test for favorite persistence (macOS)
  - **Property 5: Favorite Persistence**
  - **Validates: Requirements 3.7, 8.2**

### Phase 4: macOS Video Player Integration

- [ ] 10. Integrate AVPlayer (macOS)
  - Create VideoPlayerService protocol
  - Implement AVPlayerService with AVPlayer
  - Configure player for HLS streams
  - Configure player for RTSP streams
  - Configure player for HTTP streams
  - Enable hardware acceleration
  - _Requirements: 3.5, 7.1, 7.2, 7.3, 7.4_

- [ ] 11. Implement player controls (macOS)
  - Implement play method
  - Implement pause method
  - Implement stop method
  - Implement seek method
  - Implement volume control
  - Implement fullscreen toggle
  - _Requirements: 3.6, 7.7_

- [ ] 11.1 Write property test for player control state consistency (macOS)
  - **Property 4: Player Control State Consistency**
  - **Validates: Requirements 3.6, 7.7**

- [ ] 11.2 Write unit tests for player controls (macOS)
  - Test play/pause/stop operations
  - Test seek functionality
  - Test volume control
  - Test state transitions
  - _Requirements: 3.6, 7.7_

- [ ] 12. Implement player error handling (macOS)
  - Handle stream not found errors
  - Handle unsupported format errors
  - Handle decoding errors
  - Implement automatic reconnection on network interruption
  - Display loading indicator during buffering
  - _Requirements: 7.5, 7.6_

### Phase 5: macOS UI Implementation

- [ ] 13. Implement MainViewModel (macOS)
  - Create MainViewModel class with ObservableObject
  - Implement playlist management methods
  - Implement channel loading logic
  - Implement error handling and loading states
  - Wire up dependencies (repositories, parsers, clients)
  - _Requirements: 3.2, 3.3, 3.4_

- [ ] 14. Implement MainView and navigation (macOS)
  - Create MainView with NavigationSplitView
  - Implement navigation structure
  - Set up view routing
  - _Requirements: 3.1_

- [ ] 15. Implement PlaylistSidebarView (macOS)
  - Display list of playlists
  - Implement add playlist button and dialog
  - Implement delete playlist action
  - Implement rename playlist action
  - Display favorites section
  - _Requirements: 3.2, 3.3, 3.4_

- [ ] 16. Implement ChannelListView (macOS)
  - Display channel list with LazyVStack for performance
  - Show channel name, category, and thumbnail
  - Implement search and filter functionality
  - Implement channel selection
  - Implement favorite toggle button
  - _Requirements: 3.4, 3.7_

- [ ] 16.1 Write property test for channel list display completeness (macOS)
  - **Property 3: Channel List Display Completeness**
  - **Validates: Requirements 3.4**

- [ ] 17. Implement PlayerViewModel (macOS)
  - Create PlayerViewModel class with ObservableObject
  - Implement player control methods
  - Implement state management (isPlaying, volume, currentTime, etc.)
  - Wire up VideoPlayerService
  - _Requirements: 3.5, 3.6_

- [ ] 18. Implement PlayerView (macOS)
  - Create video player view with AVPlayerLayer
  - Implement playback controls UI
  - Implement fullscreen mode
  - Display channel information
  - Show loading indicator
  - Show error messages
  - _Requirements: 3.5, 3.6_

- [ ] 18.1 Write UI tests for player view (macOS)
  - Test video playback initiation
  - Test playback controls
  - Test fullscreen toggle
  - Test error display
  - _Requirements: 3.5, 3.6_

- [ ] 19. Implement add playlist dialogs (macOS)
  - Create M3U URL input dialog
  - Create M3U file picker dialog
  - Create Xtream Codes credentials dialog
  - Implement form validation
  - _Requirements: 3.2, 3.3_

### Phase 6: macOS Polish and Testing

- [ ] 20. Checkpoint - Ensure all macOS tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 21. Performance optimization (macOS)
  - Profile app with Instruments
  - Optimize memory usage
  - Optimize UI rendering performance
  - Optimize database queries
  - Implement image caching
  - _Requirements: Performance considerations_

- [ ] 22. UI/UX improvements (macOS)
  - Refine UI according to macOS Human Interface Guidelines
  - Add animations and transitions
  - Improve error message presentation
  - Add keyboard shortcuts
  - Implement drag and drop for playlists
  - _Requirements: 3.1_

- [ ] 23. Security implementation (macOS)
  - Implement Keychain storage for Xtream credentials
  - Enforce HTTPS for network requests
  - Validate all user inputs
  - _Requirements: Security considerations_

- [ ] 24. End-to-end testing (macOS)
  - Test complete user flow: add M3U playlist → browse channels → play video
  - Test complete user flow: add Xtream account → browse channels → play video
  - Test favorite management flow
  - Test error scenarios
  - _Requirements: All functional requirements_

- [ ] 25. Documentation (macOS)
  - Write user guide for macOS app
  - Document API interfaces
  - Document build and deployment process
  - _Requirements: All requirements_

### Phase 7: macOS Deployment

- [ ] 26. Configure code signing (macOS)
  - Set up Apple Developer account
  - Create signing certificates
  - Configure provisioning profiles
  - Enable hardened runtime
  - Configure entitlements
  - _Requirements: Security considerations, 9.5_

- [ ] 27. Create macOS installer
  - Configure DMG creation script
  - Design DMG background and layout
  - Test installation process
  - _Requirements: 9.5_

- [ ] 28. Update CI/CD for macOS releases
  - Configure automated DMG creation in GitHub Actions
  - Set up automatic release creation on tag push
  - Test complete CI/CD pipeline
  - _Requirements: 9.6_

- [ ] 29. Final checkpoint - Ensure all macOS tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 30. macOS release preparation
  - Create release notes
  - Update version numbers
  - Tag release in git
  - Verify release artifacts
  - Publish to GitHub Releases
  - _Requirements: 9.5, 9.6_

---

## Part 2: Windows Application Development

### Phase 1: Windows Project Setup and Infrastructure

- [ ] 31. Set up Windows project structure
  - Create Visual Studio solution with WPF project
  - Configure NuGet dependencies (Newtonsoft.Json, System.Data.SQLite or EF Core)
  - Set up database context and models
  - Configure project folders (Views, ViewModels, Services, Models)
  - Set up development and release build configurations
  - _Requirements: 9.1, 9.2, 9.4_

- [ ] 32. Configure Windows CI/CD pipeline
  - Create GitHub Actions workflow for Windows builds
  - Configure automated testing in CI pipeline
  - Set up release artifact generation (MSI)
  - _Requirements: 9.6_

- [ ] 33. Decide on code sharing strategy for Windows
  - Evaluate Kotlin/Native vs native C# implementation
  - If using Kotlin/Native, configure build target for Windows
  - Define interface specifications
  - Document the chosen approach and rationale
  - _Requirements: 2.4_

### Phase 2: Windows Core Business Logic

- [ ] 34. Implement M3U parser (Windows)
  - Create IM3UParser interface and implementation
  - Implement parsing logic for EXTINF tags
  - Extract channel metadata (name, URL, logo, group, tvg-id)
  - Handle malformed entries gracefully
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 34.1 Write property test for M3U parser field extraction (Windows)
  - **Property 1: M3U Parser Field Extraction**
  - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**

- [ ] 34.2 Write property test for M3U parser error resilience (Windows)
  - **Property 2: M3U Parser Error Resilience**
  - **Validates: Requirements 5.5**

- [ ] 35. Implement Xtream API client (Windows)
  - Create IXtreamClient interface and implementation
  - Implement authentication endpoint
  - Implement get live categories endpoint
  - Implement get live streams endpoint
  - Implement get VOD content endpoint
  - Implement get EPG data endpoint
  - Add retry mechanism with exponential backoff
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

- [ ] 35.1 Write unit tests for Xtream API client (Windows)
  - Test authentication with valid credentials
  - Test authentication with invalid credentials
  - Test fetching live categories
  - Test fetching live streams
  - Test error handling and retry mechanism
  - _Requirements: 6.1, 6.2, 6.3, 6.6_

- [ ] 35.2 Write property test for Xtream API error handling (Windows)
  - **Property 9: Xtream API Error Handling**
  - **Validates: Requirements 6.6**

- [ ] 36. Implement error handling infrastructure (Windows)
  - Define error types and categories (network, parsing, database, player)
  - Implement error presentation logic
  - Add logging infrastructure
  - Implement retry mechanisms
  - _Requirements: All error handling requirements_

### Phase 3: Windows Data Persistence

- [ ] 37. Implement database layer (Windows)
  - Create database schema (Playlists, Channels, Favorites, XtreamAccounts tables)
  - Set up SQLite connection or Entity Framework Core context
  - Implement database migrations
  - _Requirements: 4.8, 8.1, 8.2, 8.3, 8.4_

- [ ] 38. Implement PlaylistRepository (Windows)
  - Create IPlaylistRepository interface
  - Implement GetAllPlaylists method
  - Implement GetPlaylist method
  - Implement SavePlaylist method
  - Implement DeletePlaylist method
  - Implement UpdatePlaylist method
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 38.1 Write property test for playlist persistence round trip (Windows)
  - **Property 6: Playlist Persistence Round Trip**
  - **Validates: Requirements 8.1, 8.3**

- [ ] 38.2 Write property test for playlist deletion completeness (Windows)
  - **Property 7: Playlist Deletion Completeness**
  - **Validates: Requirements 8.4**

- [ ] 38.3 Write property test for database transaction consistency (Windows)
  - **Property 8: Database Transaction Consistency**
  - **Validates: Requirements 8.5**

- [ ] 38.4 Write integration tests for PlaylistRepository (Windows)
  - Test save and load playlist
  - Test delete playlist with cascading deletes
  - Test update playlist
  - Test transaction rollback on error
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 39. Implement FavoriteRepository (Windows)
  - Create IFavoriteRepository interface
  - Implement AddFavorite method
  - Implement RemoveFavorite method
  - Implement GetAllFavorites method
  - Implement IsFavorite method
  - _Requirements: 4.7, 8.2_

- [ ] 39.1 Write property test for favorite persistence (Windows)
  - **Property 5: Favorite Persistence**
  - **Validates: Requirements 4.7, 8.2**

### Phase 4: Windows Video Player Integration

- [ ] 40. Integrate video player (Windows)
  - Choose player library (MediaElement, LibVLC, or FFmpeg)
  - Create IVideoPlayerService interface
  - Implement player service with chosen library
  - Configure player for HLS streams
  - Configure player for RTSP streams
  - Configure player for HTTP streams
  - Enable hardware acceleration
  - _Requirements: 4.5, 7.1, 7.2, 7.3, 7.4_

- [ ] 41. Implement player controls (Windows)
  - Implement Play method
  - Implement Pause method
  - Implement Stop method
  - Implement Seek method
  - Implement volume control
  - Implement fullscreen toggle
  - _Requirements: 4.6, 7.7_

- [ ] 41.1 Write property test for player control state consistency (Windows)
  - **Property 4: Player Control State Consistency**
  - **Validates: Requirements 4.6, 7.7**

- [ ] 41.2 Write unit tests for player controls (Windows)
  - Test Play/Pause/Stop operations
  - Test Seek functionality
  - Test volume control
  - Test state transitions
  - _Requirements: 4.6, 7.7_

- [ ] 42. Implement player error handling (Windows)
  - Handle stream not found errors
  - Handle unsupported format errors
  - Handle decoding errors
  - Implement automatic reconnection on network interruption
  - Display loading indicator during buffering
  - _Requirements: 7.5, 7.6_

### Phase 5: Windows UI Implementation

- [ ] 43. Implement MainViewModel (Windows)
  - Create MainViewModel class with INotifyPropertyChanged
  - Implement playlist management commands
  - Implement channel loading logic
  - Implement error handling and loading states
  - Wire up dependencies (repositories, parsers, clients)
  - _Requirements: 4.2, 4.3, 4.4_

- [ ] 44. Implement MainWindow (Windows)
  - Create MainWindow XAML with Grid layout
  - Set up two-column layout (sidebar and content)
  - Implement window chrome and styling
  - _Requirements: 4.1_

- [ ] 45. Implement PlaylistSidebar (Windows)
  - Display list of playlists in ListBox
  - Implement add playlist button and dialogs
  - Implement delete playlist context menu
  - Implement rename playlist context menu
  - Display favorites section
  - _Requirements: 4.2, 4.3, 4.4_

- [ ] 46. Implement ChannelList (Windows)
  - Display channel list with VirtualizingStackPanel for performance
  - Show channel name, category, and thumbnail
  - Implement search and filter functionality
  - Implement channel selection
  - Implement favorite toggle button
  - _Requirements: 4.4, 4.7_

- [ ] 46.1 Write property test for channel list display completeness (Windows)
  - **Property 3: Channel List Display Completeness**
  - **Validates: Requirements 4.4**

- [ ] 47. Implement PlayerViewModel (Windows)
  - Create PlayerViewModel class with INotifyPropertyChanged
  - Implement player control commands
  - Implement state management (IsPlaying, Volume, CurrentTime, etc.)
  - Wire up IVideoPlayerService
  - _Requirements: 4.5, 4.6_

- [ ] 48. Implement PlayerWindow (Windows)
  - Create PlayerWindow XAML
  - Embed video player control
  - Implement playback controls UI
  - Implement fullscreen mode
  - Display channel information
  - Show loading indicator
  - Show error messages
  - _Requirements: 4.5, 4.6_

- [ ] 48.1 Write UI tests for player window (Windows)
  - Test video playback initiation
  - Test playback controls
  - Test fullscreen toggle
  - Test error display
  - _Requirements: 4.5, 4.6_

- [ ] 49. Implement add playlist dialogs (Windows)
  - Create M3U URL input dialog
  - Create M3U file picker dialog (OpenFileDialog)
  - Create Xtream Codes credentials dialog
  - Implement form validation
  - _Requirements: 4.2, 4.3_

### Phase 6: Windows Polish and Testing

- [ ] 50. Checkpoint - Ensure all Windows tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 51. Performance optimization (Windows)
  - Profile app with Visual Studio Profiler
  - Optimize memory usage
  - Optimize UI rendering performance
  - Optimize database queries
  - Implement image caching
  - _Requirements: Performance considerations_

- [ ] 52. UI/UX improvements (Windows)
  - Refine UI according to Windows design guidelines
  - Add animations and transitions
  - Improve error message presentation
  - Add keyboard shortcuts
  - Implement drag and drop for playlists
  - _Requirements: 4.1_

- [ ] 53. Security implementation (Windows)
  - Implement Credential Manager storage for Xtream credentials
  - Enforce HTTPS for network requests
  - Validate all user inputs
  - _Requirements: Security considerations_

- [ ] 54. End-to-end testing (Windows)
  - Test complete user flow: add M3U playlist → browse channels → play video
  - Test complete user flow: add Xtream account → browse channels → play video
  - Test favorite management flow
  - Test error scenarios
  - _Requirements: All functional requirements_

- [ ] 55. Documentation (Windows)
  - Write user guide for Windows app
  - Document API interfaces
  - Document build and deployment process
  - _Requirements: All requirements_

### Phase 7: Windows Deployment

- [ ] 56. Configure code signing (Windows)
  - Obtain code signing certificate
  - Configure signing in build process
  - Test signed executable
  - _Requirements: Security considerations, 9.5_

- [ ] 57. Create Windows installer
  - Create WiX installer project
  - Configure MSI properties and features
  - Design installer UI
  - Test installation and uninstallation
  - _Requirements: 9.5_

- [ ] 58. Update CI/CD for Windows releases
  - Configure automated MSI creation in GitHub Actions
  - Set up automatic release creation on tag push
  - Test complete CI/CD pipeline
  - _Requirements: 9.6_

- [ ] 59. Final checkpoint - Ensure all Windows tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 60. Windows release preparation
  - Create release notes
  - Update version numbers
  - Tag release in git
  - Verify release artifacts
  - Publish to GitHub Releases
  - _Requirements: 9.5, 9.6_

---

## Summary

**Part 1 (macOS)**: Tasks 1-30 - Complete macOS application development
**Part 2 (Windows)**: Tasks 31-60 - Complete Windows application development

The plan is structured to complete the macOS application first (tasks 1-30), followed by the Windows application (tasks 31-60). This allows for:
- Focused development on one platform at a time
- Learning from macOS implementation when building Windows version
- Earlier delivery of macOS application
- Ability to reuse patterns and solutions across platforms
