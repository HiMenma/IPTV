//
//  Favorite.swift
//  IPTVPlayer
//
//  Data model for favorite channels
//

import Foundation

/// Represents a favorite channel association
struct Favorite: Identifiable, Codable, Equatable {
    let id: String
    let channelId: String
    let playlistId: String
    let createdAt: Date
    
    init(
        id: String = UUID().uuidString,
        channelId: String,
        playlistId: String,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.channelId = channelId
        self.playlistId = playlistId
        self.createdAt = createdAt
    }
}
