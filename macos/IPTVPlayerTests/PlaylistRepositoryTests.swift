//
//  PlaylistRepositoryTests.swift
//  IPTVPlayerTests
//
//  Unit tests for PlaylistRepository
//

import XCTest
import CoreData
@testable import IPTVPlayer

class PlaylistRepositoryTests: XCTestCase {
    var repository: CoreDataPlaylistRepository!
    var context: NSManagedObjectContext!
    
    override func setUp() {
        super.setUp()
        // Create in-memory Core Data stack for testing
        context = createInMemoryContext()
        repository = CoreDataPlaylistRepository(context: context)
    }
    
    override func tearDown() {
        repository = nil
        context = nil
        super.tearDown()
    }
    
    // MARK: - Helper Methods
    
    private func createInMemoryContext() -> NSManagedObjectContext {
        let container = NSPersistentContainer(name: "IPTVPlayer")
        let description = NSPersistentStoreDescription()
        description.type = NSInMemoryStoreType
        container.persistentStoreDescriptions = [description]
        
        let expectation = self.expectation(description: "Store loaded")
        container.loadPersistentStores { _, error in
            XCTAssertNil(error)
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 2.0)
        
        return container.viewContext
    }
    
    // MARK: - Tests
    
    func testSaveAndLoadPlaylist() async throws {
        // Given
        let playlist = Playlist(
            id: UUID().uuidString,
            name: "Test Playlist",
            url: "http://example.com/playlist.m3u",
            type: .m3uUrl,
            channels: [
                Channel(
                    id: UUID().uuidString,
                    name: "Test Channel",
                    url: "http://example.com/stream",
                    logoUrl: "http://example.com/logo.png",
                    group: "News",
                    tvgId: "test-channel"
                )
            ],
            categories: [],
            xtreamAccount: nil
        )
        
        // When
        try await repository.savePlaylist(playlist)
        let loaded = try await repository.getPlaylist(id: playlist.id)
        
        // Then
        XCTAssertNotNil(loaded)
        XCTAssertEqual(loaded?.id, playlist.id)
        XCTAssertEqual(loaded?.name, playlist.name)
        XCTAssertEqual(loaded?.url, playlist.url)
        XCTAssertEqual(loaded?.type, playlist.type)
        XCTAssertEqual(loaded?.channels.count, 1)
        XCTAssertEqual(loaded?.channels.first?.name, "Test Channel")
    }
    
    func testGetAllPlaylists() async throws {
        // Given
        let playlist1 = Playlist(
            id: UUID().uuidString,
            name: "Playlist 1",
            type: .m3uUrl
        )
        let playlist2 = Playlist(
            id: UUID().uuidString,
            name: "Playlist 2",
            type: .m3uFile
        )
        
        // When
        try await repository.savePlaylist(playlist1)
        try await repository.savePlaylist(playlist2)
        let playlists = try await repository.getAllPlaylists()
        
        // Then
        XCTAssertEqual(playlists.count, 2)
        XCTAssertTrue(playlists.contains { $0.id == playlist1.id })
        XCTAssertTrue(playlists.contains { $0.id == playlist2.id })
    }
    
    func testUpdatePlaylist() async throws {
        // Given
        let originalPlaylist = Playlist(
            id: UUID().uuidString,
            name: "Original Name",
            type: .m3uUrl
        )
        try await repository.savePlaylist(originalPlaylist)
        
        // When
        let updatedPlaylist = Playlist(
            id: originalPlaylist.id,
            name: "Updated Name",
            type: .m3uUrl,
            createdAt: originalPlaylist.createdAt,
            updatedAt: Date()
        )
        try await repository.updatePlaylist(updatedPlaylist)
        let loaded = try await repository.getPlaylist(id: originalPlaylist.id)
        
        // Then
        XCTAssertEqual(loaded?.name, "Updated Name")
    }
    
    func testDeletePlaylist() async throws {
        // Given
        let playlist = Playlist(
            id: UUID().uuidString,
            name: "To Delete",
            type: .m3uUrl
        )
        try await repository.savePlaylist(playlist)
        
        // Verify it exists
        var loaded = try await repository.getPlaylist(id: playlist.id)
        XCTAssertNotNil(loaded)
        
        // When
        try await repository.deletePlaylist(id: playlist.id)
        loaded = try await repository.getPlaylist(id: playlist.id)
        
        // Then
        XCTAssertNil(loaded)
    }
    
    func testDeletePlaylistCascadesChannels() async throws {
        // Given
        let playlist = Playlist(
            id: UUID().uuidString,
            name: "Test Playlist",
            type: .m3uUrl,
            channels: [
                Channel(
                    id: UUID().uuidString,
                    name: "Channel 1",
                    url: "http://example.com/stream1"
                ),
                Channel(
                    id: UUID().uuidString,
                    name: "Channel 2",
                    url: "http://example.com/stream2"
                )
            ]
        )
        try await repository.savePlaylist(playlist)
        
        // When
        try await repository.deletePlaylist(id: playlist.id)
        
        // Then - verify channels are also deleted
        let fetchRequest: NSFetchRequest<ChannelEntity> = ChannelEntity.fetchRequest()
        let channels = try context.fetch(fetchRequest)
        XCTAssertEqual(channels.count, 0, "Channels should be deleted when playlist is deleted")
    }
    
    func testSavePlaylistWithXtreamAccount() async throws {
        // Given
        let xtreamAccount = XtreamAccount(
            serverUrl: "http://xtream.example.com",
            username: "testuser",
            password: "testpass"
        )
        let playlist = Playlist(
            id: UUID().uuidString,
            name: "Xtream Playlist",
            type: .xtream,
            xtreamAccount: xtreamAccount
        )
        
        // When
        try await repository.savePlaylist(playlist)
        let loaded = try await repository.getPlaylist(id: playlist.id)
        
        // Then
        XCTAssertNotNil(loaded?.xtreamAccount)
        XCTAssertEqual(loaded?.xtreamAccount?.serverUrl, xtreamAccount.serverUrl)
        XCTAssertEqual(loaded?.xtreamAccount?.username, xtreamAccount.username)
        XCTAssertEqual(loaded?.xtreamAccount?.password, xtreamAccount.password)
    }
    
    func testUpdateNonExistentPlaylistThrowsError() async throws {
        // Given
        let playlist = Playlist(
            id: "non-existent-id",
            name: "Non Existent",
            type: .m3uUrl
        )
        
        // When/Then
        do {
            try await repository.updatePlaylist(playlist)
            XCTFail("Should throw error for non-existent playlist")
        } catch {
            // Expected error
            XCTAssertTrue(error is AppError)
        }
    }
    
    func testGetNonExistentPlaylistReturnsNil() async throws {
        // When
        let loaded = try await repository.getPlaylist(id: "non-existent-id")
        
        // Then
        XCTAssertNil(loaded)
    }
}
