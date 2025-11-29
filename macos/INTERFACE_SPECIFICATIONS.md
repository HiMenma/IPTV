# Interface Specifications for macOS Native Implementation

This document defines the interfaces that must be implemented for the macOS IPTV Player application. These specifications ensure consistency with the design document and provide clear contracts for all components.

## Core Protocols

### M3UParser

Parses M3U playlist format and extracts channel information.

```swift
/// Protocol for parsing M3U playlist files
protocol M3UParser {
    /// Parses M3U playlist content and returns a list of channels
    /// - Parameter content: The M3U playlist content as a string
    /// - Returns: Array of Channel objects
    /// - Throws: M3UParseError if parsing fails
    func parse(content: String) async throws -> [Channel]
}

/// Errors that can occur during M3U parsing
enum M3UParseError: Error, LocalizedError {
    case invalidFormat(message: String)
    case emptyContent
    
    var errorDescription: String? {
        switch self {
        case .invalidFormat(let message):
            return "Invalid M3U format: \(message)"
        case .emptyContent:
            return "M3U content is empty"
        }
    }
}
```

**Implementation Requirements**:
- Must extract channel name from EXTINF tag (after last comma)
- Must extract tvg-logo attribute if present
- Must extract group-title attribute if present
- Must extract tvg-id attribute if present
- Must handle malformed entries gracefully (skip and continue)
- Must use channel URL as fallback ID if tvg-id not present
- Must trim whitespace from all extracted values

**Test Coverage**:
- Valid M3U with all attributes
- M3U with missing attributes
- Malformed entries mixed with valid entries
- Empty content
- Content with only comments

### XtreamClient

Client for interacting with Xtream Codes API.

```swift
/// Protocol for Xtream Codes API client
protocol XtreamClient {
    /// Authenticates with Xtream Codes server
    /// - Parameter account: Xtream account credentials
    /// - Returns: true if authentication successful, false otherwise
    /// - Throws: XtreamError if request fails
    func authenticate(account: XtreamAccount) async throws -> Bool
    
    /// Fetches live categories from Xtream server
    /// - Parameter account: Xtream account credentials
    /// - Returns: Array of Category objects
    /// - Throws: XtreamError if request fails
    func getLiveCategories(account: XtreamAccount) async throws -> [Category]
    
    /// Fetches live streams from Xtream server
    /// - Parameter account: Xtream account credentials
    /// - Returns: Array of Channel objects
    /// - Throws: XtreamError if request fails
    func getLiveStreams(account: XtreamAccount) async throws -> [Channel]
    
    /// Fetches VOD categories from Xtream server
    /// - Parameter account: Xtream account credentials
    /// - Returns: Array of Category objects
    /// - Throws: XtreamError if request fails
    func getVODCategories(account: XtreamAccount) async throws -> [Category]
    
    /// Fetches VOD streams from Xtream server
    /// - Parameter account: Xtream account credentials
    /// - Returns: Array of Channel objects (VOD content)
    /// - Throws: XtreamError if request fails
    func getVODStreams(account: XtreamAccount) async throws -> [Channel]
}

/// Errors that can occur during Xtream API operations
enum XtreamError: Error, LocalizedError {
    case networkError(underlying: Error)
    case authenticationFailed
    case invalidResponse
    case serverError(statusCode: Int)
    case timeout
    
    var errorDescription: String? {
        switch self {
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .authenticationFailed:
            return "Authentication failed. Please check your credentials."
        case .invalidResponse:
            return "Invalid response from server"
        case .serverError(let code):
            return "Server error (status code: \(code))"
        case .timeout:
            return "Request timed out"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .networkError, .timeout:
            return "Please check your internet connection and try again."
        case .authenticationFailed:
            return "Please verify your username and password."
        case .invalidResponse:
            return "The server returned an unexpected response. Please try again later."
        case .serverError:
            return "The server encountered an error. Please try again later."
        }
    }
}
```

**Implementation Requirements**:
- Must use URLSession for HTTP requests
- Must implement exponential backoff retry (3 attempts, 1s initial delay)
- Must timeout after 30 seconds
- Must construct proper Xtream API URLs
- Must parse JSON responses using Codable
- Must handle network errors gracefully
- Must validate authentication response (auth == 1)

**API Endpoints**:
- Authentication: `{serverUrl}/player_api.php?username={user}&password={pass}`
- Live Categories: `{serverUrl}/player_api.php?username={user}&password={pass}&action=get_live_categories`
- Live Streams: `{serverUrl}/player_api.php?username={user}&password={pass}&action=get_live_streams`
- VOD Categories: `{serverUrl}/player_api.php?username={user}&password={pass}&action=get_vod_categories`
- VOD Streams: `{serverUrl}/player_api.php?username={user}&password={pass}&action=get_vod_streams`

**Stream URL Format**:
- Live: `{serverUrl}/live/{username}/{password}/{stream_id}.ts`
- VOD: `{serverUrl}/movie/{username}/{password}/{stream_id}.{extension}`

### PlaylistRepository

Repository for managing playlist persistence.

```swift
/// Protocol for playlist data persistence
protocol PlaylistRepository {
    /// Retrieves all playlists from storage
    /// - Returns: Array of all playlists
    /// - Throws: RepositoryError if operation fails
    func getAllPlaylists() async throws -> [Playlist]
    
    /// Retrieves a specific playlist by ID
    /// - Parameter id: The playlist ID
    /// - Returns: The playlist if found, nil otherwise
    /// - Throws: RepositoryError if operation fails
    func getPlaylist(id: String) async throws -> Playlist?
    
    /// Saves a new playlist or updates an existing one
    /// - Parameter playlist: The playlist to save
    /// - Throws: RepositoryError if operation fails
    func savePlaylist(_ playlist: Playlist) async throws
    
    /// Deletes a playlist and all associated data
    /// - Parameter id: The playlist ID to delete
    /// - Throws: RepositoryError if operation fails
    func deletePlaylist(id: String) async throws
    
    /// Updates an existing playlist
    /// - Parameter playlist: The playlist with updated data
    /// - Throws: RepositoryError if operation fails
    func updatePlaylist(_ playlist: Playlist) async throws
}

/// Errors that can occur during repository operations
enum RepositoryError: Error, LocalizedError {
    case notFound(id: String)
    case saveFailed(underlying: Error)
    case deleteFailed(underlying: Error)
    case databaseError(underlying: Error)
    
    var errorDescription: String? {
        switch self {
        case .notFound(let id):
            return "Playlist not found: \(id)"
        case .saveFailed(let error):
            return "Failed to save playlist: \(error.localizedDescription)"
        case .deleteFailed(let error):
            return "Failed to delete playlist: \(error.localizedDescription)"
        case .databaseError(let error):
            return "Database error: \(error.localizedDescription)"
        }
    }
}
```

**Implementation Requirements**:
- Must use Core Data for persistence
- Must implement cascading deletes (delete playlist → delete channels)
- Must maintain referential integrity
- Must handle concurrent access safely
- Must update timestamps (createdAt, updatedAt)
- Must rollback on transaction failure

### FavoriteRepository

Repository for managing favorite channels.

```swift
/// Protocol for favorite channel persistence
protocol FavoriteRepository {
    /// Adds a channel to favorites
    /// - Parameters:
    ///   - channelId: The channel ID
    ///   - playlistId: The playlist ID
    /// - Throws: RepositoryError if operation fails
    func addFavorite(channelId: String, playlistId: String) async throws
    
    /// Removes a channel from favorites
    /// - Parameters:
    ///   - channelId: The channel ID
    ///   - playlistId: The playlist ID
    /// - Throws: RepositoryError if operation fails
    func removeFavorite(channelId: String, playlistId: String) async throws
    
    /// Retrieves all favorite channels
    /// - Returns: Array of favorite records
    /// - Throws: RepositoryError if operation fails
    func getAllFavorites() async throws -> [Favorite]
    
    /// Checks if a channel is marked as favorite
    /// - Parameters:
    ///   - channelId: The channel ID
    ///   - playlistId: The playlist ID
    /// - Returns: true if channel is favorite, false otherwise
    /// - Throws: RepositoryError if operation fails
    func isFavorite(channelId: String, playlistId: String) async throws -> Bool
}
```

**Implementation Requirements**:
- Must use Core Data for persistence
- Must prevent duplicate favorites
- Must handle cascading deletes when playlist is deleted
- Must set createdAt timestamp

### VideoPlayerService

Service for video playback control.

```swift
/// Protocol for video player service
protocol VideoPlayerService {
    /// Plays a video from the given URL
    /// - Parameter url: The video stream URL
    func play(url: URL) async throws
    
    /// Pauses the current playback
    func pause()
    
    /// Resumes paused playback
    func resume()
    
    /// Stops playback and releases resources
    func stop()
    
    /// Seeks to a specific time position
    /// - Parameter time: The time position in seconds
    func seek(to time: TimeInterval)
    
    /// Sets the playback volume
    /// - Parameter volume: Volume level (0.0 to 1.0)
    func setVolume(_ volume: Double)
    
    /// Current playback time in seconds
    var currentTime: TimeInterval { get }
    
    /// Total duration in seconds
    var duration: TimeInterval { get }
    
    /// Current playback state
    var isPlaying: Bool { get }
    
    /// Publisher for playback state changes
    var statePublisher: AnyPublisher<PlayerState, Never> { get }
}

/// Video player states
enum PlayerState {
    case idle
    case loading
    case playing
    case paused
    case stopped
    case error(Error)
}

/// Errors that can occur during video playback
enum PlayerError: Error, LocalizedError {
    case streamNotFound
    case unsupportedFormat
    case decodingError
    case networkError(underlying: Error)
    
    var errorDescription: String? {
        switch self {
        case .streamNotFound:
            return "Video stream not found"
        case .unsupportedFormat:
            return "Unsupported video format"
        case .decodingError:
            return "Failed to decode video"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        }
    }
}
```

**Implementation Requirements**:
- Must use AVPlayer for video playback
- Must support HLS, RTSP, and HTTP streams
- Must enable hardware acceleration when available
- Must handle buffering states
- Must implement automatic reconnection on network interruption
- Must clean up resources on stop

## Data Models

### Channel

```swift
/// Represents a TV channel or video stream
struct Channel: Codable, Identifiable, Equatable, Hashable {
    /// Unique identifier for the channel
    let id: String
    
    /// Display name of the channel
    let name: String
    
    /// Stream URL
    let url: String
    
    /// Optional logo/thumbnail URL
    let logoUrl: String?
    
    /// Optional group/category name
    let group: String?
    
    /// Optional category ID (for Xtream)
    let categoryId: String?
    
    /// Optional HTTP headers for stream access
    let headers: [String: String]
    
    init(id: String, name: String, url: String, logoUrl: String? = nil,
         group: String? = nil, categoryId: String? = nil,
         headers: [String: String] = [:]) {
        self.id = id
        self.name = name
        self.url = url
        self.logoUrl = logoUrl
        self.group = group
        self.categoryId = categoryId
        self.headers = headers
    }
}
```

### Playlist

```swift
/// Represents a playlist containing channels
struct Playlist: Codable, Identifiable, Equatable {
    /// Unique identifier for the playlist
    let id: String
    
    /// Display name of the playlist
    var name: String
    
    /// Optional source URL (for M3U_URL type)
    let url: String?
    
    /// Type of playlist
    let type: PlaylistType
    
    /// Channels in this playlist
    var channels: [Channel]
    
    /// Categories in this playlist
    var categories: [Category]
    
    /// Xtream account credentials (for XTREAM type)
    let xtreamAccount: XtreamAccount?
    
    /// Creation timestamp
    let createdAt: Date
    
    /// Last update timestamp
    var updatedAt: Date
    
    init(id: String = UUID().uuidString, name: String, url: String? = nil,
         type: PlaylistType, channels: [Channel] = [], categories: [Category] = [],
         xtreamAccount: XtreamAccount? = nil, createdAt: Date = Date(),
         updatedAt: Date = Date()) {
        self.id = id
        self.name = name
        self.url = url
        self.type = type
        self.channels = channels
        self.categories = categories
        self.xtreamAccount = xtreamAccount
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

/// Types of playlists
enum PlaylistType: String, Codable {
    case m3uUrl = "M3U_URL"
    case m3uFile = "M3U_FILE"
    case xtream = "XTREAM"
}
```

### Category

```swift
/// Represents a channel category
struct Category: Codable, Identifiable, Equatable, Hashable {
    /// Unique identifier for the category
    let id: String
    
    /// Display name of the category
    let name: String
    
    /// Optional parent category ID (for hierarchical categories)
    let parentId: String?
    
    init(id: String, name: String, parentId: String? = nil) {
        self.id = id
        self.name = name
        self.parentId = parentId
    }
}
```

### XtreamAccount

```swift
/// Represents Xtream Codes account credentials
struct XtreamAccount: Codable, Equatable {
    /// Server URL (e.g., "http://example.com:8080")
    let serverUrl: String
    
    /// Username for authentication
    let username: String
    
    /// Password for authentication
    let password: String
    
    init(serverUrl: String, username: String, password: String) {
        self.serverUrl = serverUrl
        self.username = username
        self.password = password
    }
}
```

### Favorite

```swift
/// Represents a favorite channel
struct Favorite: Codable, Identifiable, Equatable {
    /// Unique identifier for the favorite record
    let id: String
    
    /// ID of the favorited channel
    let channelId: String
    
    /// ID of the playlist containing the channel
    let playlistId: String
    
    /// Creation timestamp
    let createdAt: Date
    
    init(id: String = UUID().uuidString, channelId: String,
         playlistId: String, createdAt: Date = Date()) {
        self.id = id
        self.channelId = channelId
        self.playlistId = playlistId
        self.createdAt = createdAt
    }
}
```

## Utility Types

### Result Extensions

```swift
extension Result {
    /// Convenience method to get value or throw error
    func get() throws -> Success {
        switch self {
        case .success(let value):
            return value
        case .failure(let error):
            throw error
        }
    }
}
```

### Retry Utility

```swift
/// Utility for retrying async operations with exponential backoff
func retryWithExponentialBackoff<T>(
    maxAttempts: Int = 3,
    initialDelay: TimeInterval = 1.0,
    operation: @escaping () async throws -> T
) async throws -> T {
    var attempt = 0
    var delay = initialDelay
    
    while attempt < maxAttempts {
        do {
            return try await operation()
        } catch {
            attempt += 1
            if attempt >= maxAttempts {
                throw error
            }
            try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
            delay *= 2
        }
    }
    
    fatalError("Should not reach here")
}
```

## Testing Requirements

### Unit Tests

Each protocol implementation must have unit tests covering:
- Happy path scenarios
- Error cases
- Edge cases (empty input, malformed data, etc.)
- Boundary conditions

### Property-Based Tests

The following properties must be tested:
1. M3U Parser: Field extraction correctness
2. M3U Parser: Error resilience
3. Playlist Repository: Round-trip persistence
4. Playlist Repository: Deletion completeness
5. Favorite Repository: Persistence correctness
6. Player Service: State consistency

### Integration Tests

- End-to-end playlist loading and playback
- Database operations with real Core Data stack
- Network operations with mock servers

## Implementation Order

1. Data Models (no dependencies)
2. M3U Parser (depends on Channel model)
3. Xtream Client (depends on models)
4. Repositories (depends on models)
5. Video Player Service (depends on models)
6. ViewModels (depends on all services)
7. Views (depends on ViewModels)

## Validation Checklist

Before marking task complete, verify:
- [ ] All protocols implemented
- [ ] All data models defined
- [ ] Unit tests written and passing
- [ ] Property tests written and passing
- [ ] Error handling implemented
- [ ] Documentation comments added
- [ ] Code follows Swift style guidelines
- [ ] No compiler warnings
- [ ] Integration tests passing

---

**Document Version**: 1.0  
**Last Updated**: 2024-11-28  
**Status**: Approved
