//
//  Playlist.swift
//  IPTVPlayer
//
//  Data model for IPTV playlists
//

import Foundation

/// Playlist type enumeration
enum PlaylistType: String, Codable, Equatable {
    case m3uUrl = "M3U_URL"
    case m3uFile = "M3U_FILE"
    case xtream = "XTREAM"
}

/// Represents an IPTV playlist with channels and metadata
struct Playlist: Identifiable, Codable, Equatable {
    let id: String
    let name: String
    let url: String?
    let type: PlaylistType
    let channels: [Channel]
    let categories: [Category]
    let xtreamAccount: XtreamAccount?
    let createdAt: Date
    let updatedAt: Date
    
    init(
        id: String = UUID().uuidString,
        name: String,
        url: String? = nil,
        type: PlaylistType,
        channels: [Channel] = [],
        categories: [Category] = [],
        xtreamAccount: XtreamAccount? = nil,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
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
