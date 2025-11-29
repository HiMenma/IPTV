//
//  FavoriteRepository.swift
//  IPTVPlayer
//
//  Repository for managing favorite channel persistence using Core Data
//

import Foundation
import CoreData

/// Protocol defining favorite repository operations
protocol FavoriteRepository {
    /// Adds a channel to favorites
    func addFavorite(channelId: String, playlistId: String) async throws
    
    /// Removes a channel from favorites
    func removeFavorite(channelId: String, playlistId: String) async throws
    
    /// Retrieves all favorites
    func getAllFavorites() async throws -> [Favorite]
    
    /// Checks if a channel is favorited
    func isFavorite(channelId: String, playlistId: String) async throws -> Bool
}

/// Core Data implementation of FavoriteRepository
class CoreDataFavoriteRepository: FavoriteRepository {
    private let context: NSManagedObjectContext
    
    init(context: NSManagedObjectContext) {
        self.context = context
    }
    
    /// Adds a channel to favorites
    func addFavorite(channelId: String, playlistId: String) async throws {
        try await context.perform {
            // Check if favorite already exists
            let fetchRequest: NSFetchRequest<FavoriteEntity> = FavoriteEntity.fetchRequest()
            fetchRequest.predicate = NSPredicate(
                format: "channelId == %@ AND playlistId == %@",
                channelId,
                playlistId
            )
            fetchRequest.fetchLimit = 1
            
            let existingFavorites = try self.context.fetch(fetchRequest)
            
            // Only add if it doesn't already exist
            if existingFavorites.isEmpty {
                let favorite = Favorite(
                    channelId: channelId,
                    playlistId: playlistId
                )
                
                let entity = FavoriteEntity(context: self.context)
                self.updateEntity(entity, with: favorite)
                
                try self.context.save()
            }
        }
    }
    
    /// Removes a channel from favorites
    func removeFavorite(channelId: String, playlistId: String) async throws {
        try await context.perform {
            let fetchRequest: NSFetchRequest<FavoriteEntity> = FavoriteEntity.fetchRequest()
            fetchRequest.predicate = NSPredicate(
                format: "channelId == %@ AND playlistId == %@",
                channelId,
                playlistId
            )
            
            let entities = try self.context.fetch(fetchRequest)
            
            for entity in entities {
                self.context.delete(entity)
            }
            
            try self.context.save()
        }
    }
    
    /// Retrieves all favorites
    func getAllFavorites() async throws -> [Favorite] {
        return try await context.perform {
            let fetchRequest: NSFetchRequest<FavoriteEntity> = FavoriteEntity.fetchRequest()
            fetchRequest.sortDescriptors = [NSSortDescriptor(key: "createdAt", ascending: false)]
            
            let entities = try self.context.fetch(fetchRequest)
            return entities.compactMap { self.convertToFavorite($0) }
        }
    }
    
    /// Checks if a channel is favorited
    func isFavorite(channelId: String, playlistId: String) async throws -> Bool {
        return try await context.perform {
            let fetchRequest: NSFetchRequest<FavoriteEntity> = FavoriteEntity.fetchRequest()
            fetchRequest.predicate = NSPredicate(
                format: "channelId == %@ AND playlistId == %@",
                channelId,
                playlistId
            )
            fetchRequest.fetchLimit = 1
            
            let count = try self.context.count(for: fetchRequest)
            return count > 0
        }
    }
    
    // MARK: - Conversion Methods
    
    /// Converts a FavoriteEntity to a Favorite model
    private func convertToFavorite(_ entity: FavoriteEntity) -> Favorite? {
        guard let id = entity.id,
              let channelId = entity.channelId,
              let playlistId = entity.playlistId,
              let createdAt = entity.createdAt else {
            return nil
        }
        
        return Favorite(
            id: id,
            channelId: channelId,
            playlistId: playlistId,
            createdAt: createdAt
        )
    }
    
    /// Updates a FavoriteEntity with data from a Favorite model
    private func updateEntity(_ entity: FavoriteEntity, with favorite: Favorite) {
        entity.id = favorite.id
        entity.channelId = favorite.channelId
        entity.playlistId = favorite.playlistId
        entity.createdAt = favorite.createdAt
    }
}
