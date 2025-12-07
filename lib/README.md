# IPTV Player - Project Structure

This Flutter application follows the MVVM (Model-View-ViewModel) architecture pattern with a repository layer for data access.

## Directory Structure

```
lib/
├── main.dart                 # Application entry point
├── models/                   # Data models (Configuration, Channel, Favorite, BrowseHistory)
├── services/                 # Business logic services (XtreamService, M3UService, PlayerService)
├── repositories/             # Data access layer (ConfigurationRepository, FavoriteRepository, HistoryRepository)
├── viewmodels/              # State management (ConfigurationViewModel, ChannelViewModel, PlayerViewModel)
├── views/
│   ├── screens/             # Full-screen pages
│   └── widgets/             # Reusable UI components
└── utils/                   # Utility functions and constants
```

## Testing Structure

```
test/
├── unit/                    # Unit tests for individual components
│   ├── models/
│   ├── services/
│   ├── repositories/
│   └── viewmodels/
├── property/                # Property-based tests
└── integration/             # Integration tests
```

## Key Dependencies

- **provider**: State management
- **dio**: HTTP client for network requests
- **shared_preferences**: Local data persistence
- **file_picker**: File selection for M3U imports
- **path_provider**: File system path access
- **uuid**: Unique identifier generation
- **aliplayer_widget**: Aliyun video player integration

## Platform Support

This application supports:
- Android
- iOS
- Web
- Windows
- macOS
- Linux
