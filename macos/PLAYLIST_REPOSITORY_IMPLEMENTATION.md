# PlaylistRepository Implementation

## Overview
This document describes the implementation of the PlaylistRepository for the macOS IPTV Player application.

## Implementation Date
November 28, 2025

## Files Created

### 1. PlaylistRepository.swift
**Location**: `macos/IPTVPlayer/Services/PlaylistRepository.swift`

**Purpose**: Provides data persistence layer for playlists using Core Data.

**Components**:

#### Protocol: PlaylistRepository
Defines the interface for playlist repository operations:
- `getAllPlaylists() async throws -> [Playlist]`
- `getPlaylist(id: String) async throws -> Playlist?`
- `savePlaylist(_ playlist: Playlist) async throws`
- `deletePlaylist(id: String) async throws`
- `updatePlaylist(_ playlist: Playlist) async throws`

#### Class: CoreDataPlaylistRepository
Concrete implementation using Core Data:

**Key Features**:
1. **Async/Await Support**: All operations use Swift's modern concurrency model
2. **Thread Safety**: Uses `context.perform` to ensure Core Data operations happen on the correct thread
3. **Cascade Deletion**: Deleting a playlist automatically deletes associated channels and Xtream accounts
4. **Bidirectional Conversion**: Converts between Swift models (Playlist, Channel, XtreamAccount) and Core Data entities

**Methods Implemented**:

1. **getAllPlaylists()**
   - Fetches all playlists from Core Data
   - Sorts by creation date (newest first)
   - Converts entities to Playlist models

2. **getPlaylist(id:)**
   - Fetches a specific playlist by ID
   - Returns nil if not found
   - Includes all related channels and Xtream account

3. **savePlaylist(_:)**
   - Checks if playlist exists
   - Updates existing or creates new playlist entity
   - Handles channels and Xtream account relationships
   - Saves to Core Data

4. **deletePlaylist(id:)**
   - Finds playlist by ID
   - Deletes playlist entity (cascade deletes channels)
   - Saves changes to Core Data

5. **updatePlaylist(_:)**
   - Finds existing playlist by ID
   - Throws error if not found
   - Updates all fields including relationships
   - Saves changes to Core Data

**Conversion Methods**:

1. **convertToPlaylist(_:)**: PlaylistEntity → Playlist
   - Converts Core Data entity to Swift model
   - Includes channels, categories, and Xtream account
   - Extracts categories from channel groups

2. **updateEntity(_:with:)**: Updates PlaylistEntity from Playlist
   - Updates all scalar fields
   - Replaces channels (deletes old, creates new)
   - Updates or creates Xtream account

3. **convertToChannel(_:)**: ChannelEntity → Channel
   - Converts channel entity to model

4. **updateChannelEntity(_:with:)**: Updates ChannelEntity from Channel
   - Updates all channel fields

5. **convertToXtreamAccount(_:)**: XtreamAccountEntity → XtreamAccount
   - Converts Xtream account entity to model

6. **updateXtreamAccountEntity(_:with:)**: Updates XtreamAccountEntity
   - Updates Xtream account fields

7. **extractCategories(from:)**: Extracts unique categories from channels
   - Creates Category models from unique channel groups

### 2. PlaylistRepositoryTests.swift
**Location**: `macos/IPTVPlayerTests/PlaylistRepositoryTests.swift`

**Purpose**: Unit tests for PlaylistRepository implementation.

**Test Cases**:

1. **testSaveAndLoadPlaylist**: Verifies save and retrieve operations
2. **testGetAllPlaylists**: Tests fetching multiple playlists
3. **testUpdatePlaylist**: Tests updating existing playlist
4. **testDeletePlaylist**: Tests deletion operation
5. **testDeletePlaylistCascadesChannels**: Verifies cascade deletion
6. **testSavePlaylistWithXtreamAccount**: Tests Xtream account persistence
7. **testUpdateNonExistentPlaylistThrowsError**: Tests error handling
8. **testGetNonExistentPlaylistReturnsNil**: Tests nil return for missing playlist

**Test Infrastructure**:
- Uses in-memory Core Data store for isolation
- Creates fresh context for each test
- Cleans up after each test

## Requirements Satisfied

This implementation satisfies the following requirements from the design document:

- **Requirement 8.1**: Playlist persistence - playlists are saved to Core Data
- **Requirement 8.2**: Data model support - supports all playlist fields including channels and Xtream accounts
- **Requirement 8.3**: Load playlists - getAllPlaylists and getPlaylist methods
- **Requirement 8.4**: Delete playlists - deletePlaylist with cascade deletion
- **Requirement 8.5**: Transaction consistency - uses Core Data's transaction support

## Design Patterns Used

1. **Repository Pattern**: Abstracts data access behind a clean interface
2. **Protocol-Oriented Programming**: Uses protocol for testability and flexibility
3. **Async/Await**: Modern Swift concurrency for clean asynchronous code
4. **Dependency Injection**: Context is injected, allowing for testing with in-memory stores

## Core Data Schema

The implementation works with the following Core Data entities:

- **PlaylistEntity**: Main playlist entity
  - Attributes: id, name, url, type, createdAt, updatedAt
  - Relationships: channels (one-to-many), xtreamAccount (one-to-one)

- **ChannelEntity**: Channel entity
  - Attributes: id, name, url, logoUrl, groupName, categoryId
  - Relationships: playlist (many-to-one)

- **XtreamAccountEntity**: Xtream account credentials
  - Attributes: id, serverUrl, username, password
  - Relationships: playlist (one-to-one)

## Error Handling

The implementation uses the existing AppError infrastructure:
- Throws `AppError.databaseError` when operations fail
- Provides descriptive error messages
- Maintains data consistency on errors

## Thread Safety

All Core Data operations are performed using `context.perform`:
- Ensures operations happen on the correct queue
- Prevents threading issues
- Supports async/await pattern

## Future Enhancements

Potential improvements for future iterations:

1. **Batch Operations**: Add methods for batch save/delete
2. **Query Optimization**: Add indexes for frequently queried fields
3. **Caching**: Implement in-memory cache for frequently accessed playlists
4. **Migration Support**: Add data migration logic for schema changes
5. **Conflict Resolution**: Handle concurrent modifications
6. **Pagination**: Support for large playlist collections

## Testing Status

- ✅ Unit tests created
- ⏳ Tests need to be run with Xcode (requires full Xcode installation)
- ✅ Code review completed
- ✅ Implementation matches design specification

## Integration

To use the PlaylistRepository in the application:

```swift
// Get the Core Data context
let context = PersistenceController.shared.container.viewContext

// Create repository instance
let repository = CoreDataPlaylistRepository(context: context)

// Use the repository
Task {
    // Save a playlist
    let playlist = Playlist(name: "My Playlist", type: .m3uUrl)
    try await repository.savePlaylist(playlist)
    
    // Load all playlists
    let playlists = try await repository.getAllPlaylists()
    
    // Get specific playlist
    if let playlist = try await repository.getPlaylist(id: "some-id") {
        print("Found: \(playlist.name)")
    }
    
    // Update playlist
    try await repository.updatePlaylist(updatedPlaylist)
    
    // Delete playlist
    try await repository.deletePlaylist(id: "some-id")
}
```

## Notes

- The implementation is complete and ready for integration
- All required methods from the task specification have been implemented
- Conversion methods handle bidirectional mapping between models and entities
- The code follows Swift best practices and conventions
- Error handling is consistent with the existing codebase
