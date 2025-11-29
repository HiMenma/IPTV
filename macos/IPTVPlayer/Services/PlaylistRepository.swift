//
//  PlaylistRepository.swift
//  IPTVPlayer
//
//  Repository for managing playlist persistence using Core Data
//

import Foundation
import CoreData

/// Protocol defining playlist repository operations
protocol PlaylistRepository {
    /// Retrieves all playlists from the database
    func getAllPlaylists() async throws -> [Playlist]
    
    /// Retrieves a specific playlist by ID
    func getPlaylist(id: String) async throws -> Playlist?
    
    /// Saves a new playlist or updates an existing one
    func savePlaylist(_ playlist: Playlist) async throws
    
    /// Deletes a playlist by ID
    func deletePlaylist(id: String) async throws
    
    /// Updates an existing playlist
    func updatePlaylist(_ playlist: Playlist) async throws
}

/// Core Data implementation of PlaylistRepository
class CoreDataPlaylistRepository: PlaylistRepository {
    private let context: NSManagedObjectContext
    
    init(context: NSManagedObjectContext) {
        self.context = context
    }
    
    /// Retrieves all playlists from Core Data with optimized batch fetching
    func getAllPlaylists() async throws -> [Playlist] {
        return try await PerformanceMonitor.shared.profileDatabaseQuery("getAllPlaylists") {
            try await context.perform {
                let fetchRequest: NSFetchRequest<PlaylistEntity> = PlaylistEntity.fetchRequest()
                fetchRequest.sortDescriptors = [NSSortDescriptor(key: "createdAt", ascending: false)]
                
                // Optimize with batch fetching
                fetchRequest.relationshipKeyPathsForPrefetching = ["channels", "xtreamAccount"]
                fetchRequest.returnsObjectsAsFaults = false
                
                let entities = try self.context.fetch(fetchRequest)
                return entities.compactMap { self.convertToPlaylist($0) }
            }
        }
    }
    
    /// Retrieves a specific playlist by ID with optimized fetching
    func getPlaylist(id: String) async throws -> Playlist? {
        return try await PerformanceMonitor.shared.profileDatabaseQuery("getPlaylist") {
            try await context.perform {
                let fetchRequest: NSFetchRequest<PlaylistEntity> = PlaylistEntity.fetchRequest()
                fetchRequest.predicate = NSPredicate(format: "id == %@", id)
                fetchRequest.fetchLimit = 1
                
                // Optimize with batch fetching
                fetchRequest.relationshipKeyPathsForPrefetching = ["channels", "xtreamAccount"]
                fetchRequest.returnsObjectsAsFaults = false
                
                let entities = try self.context.fetch(fetchRequest)
                guard let entity = entities.first else { return nil }
                return self.convertToPlaylist(entity)
            }
        }
    }
    
    /// Saves a new playlist to Core Data
    func savePlaylist(_ playlist: Playlist) async throws {
        try await context.perform {
            // Check if playlist already exists
            let fetchRequest: NSFetchRequest<PlaylistEntity> = PlaylistEntity.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "id == %@", playlist.id)
            fetchRequest.fetchLimit = 1
            
            let existingEntities = try self.context.fetch(fetchRequest)
            
            if let existingEntity = existingEntities.first {
                // Update existing playlist
                self.updateEntity(existingEntity, with: playlist)
            } else {
                // Create new playlist
                let entity = PlaylistEntity(context: self.context)
                self.updateEntity(entity, with: playlist)
            }
            
            try self.context.save()
        }
    }
    
    /// Deletes a playlist by ID
    func deletePlaylist(id: String) async throws {
        try await context.perform {
            let fetchRequest: NSFetchRequest<PlaylistEntity> = PlaylistEntity.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "id == %@", id)
            
            let entities = try self.context.fetch(fetchRequest)
            
            for entity in entities {
                self.context.delete(entity)
            }
            
            try self.context.save()
        }
    }
    
    /// Updates an existing playlist
    func updatePlaylist(_ playlist: Playlist) async throws {
        try await context.perform {
            let fetchRequest: NSFetchRequest<PlaylistEntity> = PlaylistEntity.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "id == %@", playlist.id)
            fetchRequest.fetchLimit = 1
            
            let entities = try self.context.fetch(fetchRequest)
            
            guard let entity = entities.first else {
                throw AppError.databaseError(
                    message: "Playlist not found",
                    underlyingError: nil
                )
            }
            
            self.updateEntity(entity, with: playlist)
            try self.context.save()
        }
    }
    
    // MARK: - Conversion Methods
    
    /// Converts a PlaylistEntity to a Playlist model
    private func convertToPlaylist(_ entity: PlaylistEntity) -> Playlist? {
        guard let id = entity.id,
              let name = entity.name,
              let typeString = entity.type,
              let type = PlaylistType(rawValue: typeString),
              let createdAt = entity.createdAt,
              let updatedAt = entity.updatedAt else {
            return nil
        }
        
        // Convert channels
        let channels = (entity.channels?.allObjects as? [ChannelEntity])?.compactMap { channelEntity in
            convertToChannel(channelEntity)
        } ?? []
        
        // Convert categories (extract unique groups from channels)
        let categories = extractCategories(from: channels)
        
        // Convert xtream account if present
        let xtreamAccount = entity.xtreamAccount.flatMap { convertToXtreamAccount($0) }
        
        return Playlist(
            id: id,
            name: name,
            url: entity.url,
            type: type,
            channels: channels,
            categories: categories,
            xtreamAccount: xtreamAccount,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }
    
    /// Updates a PlaylistEntity with data from a Playlist model
    private func updateEntity(_ entity: PlaylistEntity, with playlist: Playlist) {
        entity.id = playlist.id
        entity.name = playlist.name
        entity.url = playlist.url
        entity.type = playlist.type.rawValue
        entity.createdAt = playlist.createdAt
        entity.updatedAt = playlist.updatedAt
        
        // Delete existing channels
        if let existingChannels = entity.channels?.allObjects as? [ChannelEntity] {
            for channel in existingChannels {
                context.delete(channel)
            }
        }
        
        // Add new channels
        for channel in playlist.channels {
            let channelEntity = ChannelEntity(context: context)
            updateChannelEntity(channelEntity, with: channel)
            entity.addToChannels(channelEntity)
        }
        
        // Handle xtream account
        if let xtreamAccount = playlist.xtreamAccount {
            if let existingAccount = entity.xtreamAccount {
                updateXtreamAccountEntity(existingAccount, with: xtreamAccount)
            } else {
                let accountEntity = XtreamAccountEntity(context: context)
                updateXtreamAccountEntity(accountEntity, with: xtreamAccount)
                entity.xtreamAccount = accountEntity
            }
        } else {
            // Delete existing xtream account if present
            if let existingAccount = entity.xtreamAccount {
                context.delete(existingAccount)
                entity.xtreamAccount = nil
            }
        }
    }
    
    /// Converts a ChannelEntity to a Channel model
    private func convertToChannel(_ entity: ChannelEntity) -> Channel? {
        guard let id = entity.id,
              let name = entity.name,
              let url = entity.url else {
            return nil
        }
        
        return Channel(
            id: id,
            name: name,
            url: url,
            logoUrl: entity.logoUrl,
            group: entity.groupName,
            tvgId: entity.categoryId,
            headers: [:]
        )
    }
    
    /// Updates a ChannelEntity with data from a Channel model
    private func updateChannelEntity(_ entity: ChannelEntity, with channel: Channel) {
        entity.id = channel.id
        entity.name = channel.name
        entity.url = channel.url
        entity.logoUrl = channel.logoUrl
        entity.groupName = channel.group
        entity.categoryId = channel.tvgId
    }
    
    /// Converts an XtreamAccountEntity to an XtreamAccount model
    private func convertToXtreamAccount(_ entity: XtreamAccountEntity) -> XtreamAccount? {
        guard let serverUrl = entity.serverUrl,
              let username = entity.username,
              let password = entity.password else {
            return nil
        }
        
        return XtreamAccount(
            serverUrl: serverUrl,
            username: username,
            password: password
        )
    }
    
    /// Updates an XtreamAccountEntity with data from an XtreamAccount model
    private func updateXtreamAccountEntity(_ entity: XtreamAccountEntity, with account: XtreamAccount) {
        entity.id = UUID().uuidString
        entity.serverUrl = account.serverUrl
        entity.username = account.username
        entity.password = account.password
    }
    
    /// Extracts unique categories from channels
    private func extractCategories(from channels: [Channel]) -> [Category] {
        let uniqueGroups = Set(channels.compactMap { $0.group })
        return uniqueGroups.map { groupName in
            Category(id: UUID().uuidString, name: groupName, parentId: nil)
        }
    }
}
