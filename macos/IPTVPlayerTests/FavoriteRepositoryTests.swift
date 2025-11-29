//
//  FavoriteRepositoryTests.swift
//  IPTVPlayerTests
//
//  Unit tests for FavoriteRepository
//

import XCTest
import CoreData
@testable import IPTVPlayer

class FavoriteRepositoryTests: XCTestCase {
    var repository: CoreDataFavoriteRepository!
    var context: NSManagedObjectContext!
    
    override func setUp() {
        super.setUp()
        // Create in-memory Core Data stack for testing
        context = createInMemoryContext()
        repository = CoreDataFavoriteRepository(context: context)
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
    
    func testAddFavorite() async throws {
        // Given
        let channelId = UUID().uuidString
        let playlistId = UUID().uuidString
        
        // When
        try await repository.addFavorite(channelId: channelId, playlistId: playlistId)
        
        // Then
        let isFavorite = try await repository.isFavorite(channelId: channelId, playlistId: playlistId)
        XCTAssertTrue(isFavorite)
    }
    
    func testAddDuplicateFavoriteDoesNotCreateDuplicate() async throws {
        // Given
        let channelId = UUID().uuidString
        let playlistId = UUID().uuidString
        
        // When - add the same favorite twice
        try await repository.addFavorite(channelId: channelId, playlistId: playlistId)
        try await repository.addFavorite(channelId: channelId, playlistId: playlistId)
        
        // Then - should only have one favorite
        let favorites = try await repository.getAllFavorites()
        let matchingFavorites = favorites.filter { $0.channelId == channelId && $0.playlistId == playlistId }
        XCTAssertEqual(matchingFavorites.count, 1, "Should not create duplicate favorites")
    }
    
    func testRemoveFavorite() async throws {
        // Given
        let channelId = UUID().uuidString
        let playlistId = UUID().uuidString
        try await repository.addFavorite(channelId: channelId, playlistId: playlistId)
        
        // Verify it exists
        var isFavorite = try await repository.isFavorite(channelId: channelId, playlistId: playlistId)
        XCTAssertTrue(isFavorite)
        
        // When
        try await repository.removeFavorite(channelId: channelId, playlistId: playlistId)
        
        // Then
        isFavorite = try await repository.isFavorite(channelId: channelId, playlistId: playlistId)
        XCTAssertFalse(isFavorite)
    }
    
    func testRemoveNonExistentFavoriteDoesNotThrow() async throws {
        // Given
        let channelId = UUID().uuidString
        let playlistId = UUID().uuidString
        
        // When/Then - should not throw error
        try await repository.removeFavorite(channelId: channelId, playlistId: playlistId)
        
        // Verify it's still not a favorite
        let isFavorite = try await repository.isFavorite(channelId: channelId, playlistId: playlistId)
        XCTAssertFalse(isFavorite)
    }
    
    func testGetAllFavorites() async throws {
        // Given
        let favorite1 = (channelId: UUID().uuidString, playlistId: UUID().uuidString)
        let favorite2 = (channelId: UUID().uuidString, playlistId: UUID().uuidString)
        let favorite3 = (channelId: UUID().uuidString, playlistId: UUID().uuidString)
        
        // When
        try await repository.addFavorite(channelId: favorite1.channelId, playlistId: favorite1.playlistId)
        try await repository.addFavorite(channelId: favorite2.channelId, playlistId: favorite2.playlistId)
        try await repository.addFavorite(channelId: favorite3.channelId, playlistId: favorite3.playlistId)
        
        let favorites = try await repository.getAllFavorites()
        
        // Then
        XCTAssertEqual(favorites.count, 3)
        XCTAssertTrue(favorites.contains { $0.channelId == favorite1.channelId && $0.playlistId == favorite1.playlistId })
        XCTAssertTrue(favorites.contains { $0.channelId == favorite2.channelId && $0.playlistId == favorite2.playlistId })
        XCTAssertTrue(favorites.contains { $0.channelId == favorite3.channelId && $0.playlistId == favorite3.playlistId })
    }
    
    func testGetAllFavoritesReturnsEmptyArrayWhenNoFavorites() async throws {
        // When
        let favorites = try await repository.getAllFavorites()
        
        // Then
        XCTAssertEqual(favorites.count, 0)
    }
    
    func testIsFavoriteReturnsFalseForNonExistentFavorite() async throws {
        // Given
        let channelId = UUID().uuidString
        let playlistId = UUID().uuidString
        
        // When
        let isFavorite = try await repository.isFavorite(channelId: channelId, playlistId: playlistId)
        
        // Then
        XCTAssertFalse(isFavorite)
    }
    
    func testFavoritesSortedByCreatedAtDescending() async throws {
        // Given
        let favorite1 = (channelId: UUID().uuidString, playlistId: UUID().uuidString)
        let favorite2 = (channelId: UUID().uuidString, playlistId: UUID().uuidString)
        
        // When - add favorites with a small delay to ensure different timestamps
        try await repository.addFavorite(channelId: favorite1.channelId, playlistId: favorite1.playlistId)
        try await Task.sleep(nanoseconds: 100_000_000) // 0.1 second
        try await repository.addFavorite(channelId: favorite2.channelId, playlistId: favorite2.playlistId)
        
        let favorites = try await repository.getAllFavorites()
        
        // Then - most recent should be first
        XCTAssertEqual(favorites.count, 2)
        XCTAssertEqual(favorites[0].channelId, favorite2.channelId)
        XCTAssertEqual(favorites[1].channelId, favorite1.channelId)
    }
    
    func testFavoriteHasCorrectMetadata() async throws {
        // Given
        let channelId = UUID().uuidString
        let playlistId = UUID().uuidString
        let beforeAdd = Date()
        
        // When
        try await repository.addFavorite(channelId: channelId, playlistId: playlistId)
        let favorites = try await repository.getAllFavorites()
        
        // Then
        XCTAssertEqual(favorites.count, 1)
        let favorite = favorites[0]
        XCTAssertFalse(favorite.id.isEmpty)
        XCTAssertEqual(favorite.channelId, channelId)
        XCTAssertEqual(favorite.playlistId, playlistId)
        XCTAssertGreaterThanOrEqual(favorite.createdAt, beforeAdd)
        XCTAssertLessThanOrEqual(favorite.createdAt, Date())
    }
    
    func testMultipleFavoritesForSameChannel() async throws {
        // Given - same channel in different playlists
        let channelId = UUID().uuidString
        let playlist1Id = UUID().uuidString
        let playlist2Id = UUID().uuidString
        
        // When
        try await repository.addFavorite(channelId: channelId, playlistId: playlist1Id)
        try await repository.addFavorite(channelId: channelId, playlistId: playlist2Id)
        
        // Then - both should exist
        let isFavoriteInPlaylist1 = try await repository.isFavorite(channelId: channelId, playlistId: playlist1Id)
        let isFavoriteInPlaylist2 = try await repository.isFavorite(channelId: channelId, playlistId: playlist2Id)
        XCTAssertTrue(isFavoriteInPlaylist1)
        XCTAssertTrue(isFavoriteInPlaylist2)
        
        let favorites = try await repository.getAllFavorites()
        XCTAssertEqual(favorites.count, 2)
    }
    
    func testRemoveFavoriteOnlyRemovesSpecificPlaylistChannel() async throws {
        // Given - same channel in different playlists
        let channelId = UUID().uuidString
        let playlist1Id = UUID().uuidString
        let playlist2Id = UUID().uuidString
        
        try await repository.addFavorite(channelId: channelId, playlistId: playlist1Id)
        try await repository.addFavorite(channelId: channelId, playlistId: playlist2Id)
        
        // When - remove from one playlist
        try await repository.removeFavorite(channelId: channelId, playlistId: playlist1Id)
        
        // Then - should still be favorite in other playlist
        let isFavoriteInPlaylist1 = try await repository.isFavorite(channelId: channelId, playlistId: playlist1Id)
        let isFavoriteInPlaylist2 = try await repository.isFavorite(channelId: channelId, playlistId: playlist2Id)
        XCTAssertFalse(isFavoriteInPlaylist1)
        XCTAssertTrue(isFavoriteInPlaylist2)
    }
}
