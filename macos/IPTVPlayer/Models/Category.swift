//
//  Category.swift
//  IPTVPlayer
//
//  Data model for channel categories
//

import Foundation

/// Represents a channel category
struct Category: Identifiable, Codable, Equatable {
    let id: String
    let name: String
    let parentId: String?
    
    init(id: String, name: String, parentId: String? = nil) {
        self.id = id
        self.name = name
        self.parentId = parentId
    }
}

/// Response from Xtream live categories endpoint
struct XtreamCategory: Codable {
    let categoryId: String
    let categoryName: String
    let parentId: Int?
    
    enum CodingKeys: String, CodingKey {
        case categoryId = "category_id"
        case categoryName = "category_name"
        case parentId = "parent_id"
    }
    
    /// Convert to Category model
    func toCategory() -> Category {
        return Category(
            id: categoryId,
            name: categoryName,
            parentId: parentId.map { String($0) }
        )
    }
}

/// Response from Xtream live streams endpoint
struct XtreamStream: Codable {
    let num: Int?
    let name: String?
    let streamType: String?
    let streamId: Int?
    let streamIcon: String?
    let epgChannelId: String?
    let added: String?
    let categoryId: String?
    let customSid: String?
    let tvArchive: Int?
    let directSource: String?
    let tvArchiveDuration: Int?
    
    enum CodingKeys: String, CodingKey {
        case num
        case name
        case streamType = "stream_type"
        case streamId = "stream_id"
        case streamIcon = "stream_icon"
        case epgChannelId = "epg_channel_id"
        case added
        case categoryId = "category_id"
        case customSid = "custom_sid"
        case tvArchive = "tv_archive"
        case directSource = "direct_source"
        case tvArchiveDuration = "tv_archive_duration"
    }
    
    /// Convert to Channel model
    func toChannel(account: XtreamAccount) -> Channel? {
        guard let name = name,
              let streamId = streamId else {
            return nil
        }
        
        // Build stream URL
        let url = "\(account.serverUrl)/live/\(account.username)/\(account.password)/\(streamId).m3u8"
        
        return Channel(
            id: String(streamId),
            name: name,
            url: url,
            logoUrl: streamIcon,
            group: categoryId,
            tvgId: epgChannelId
        )
    }
}

/// Response from Xtream VOD streams endpoint
struct XtreamVODStream: Codable {
    let num: Int?
    let name: String?
    let streamType: String?
    let streamId: Int?
    let streamIcon: String?
    let rating: String?
    let rating5based: Double?
    let added: String?
    let categoryId: String?
    let containerExtension: String?
    let customSid: String?
    let directSource: String?
    
    enum CodingKeys: String, CodingKey {
        case num
        case name
        case streamType = "stream_type"
        case streamId = "stream_id"
        case streamIcon = "stream_icon"
        case rating
        case rating5based = "rating_5based"
        case added
        case categoryId = "category_id"
        case containerExtension = "container_extension"
        case customSid = "custom_sid"
        case directSource = "direct_source"
    }
    
    /// Convert to Channel model
    func toChannel(account: XtreamAccount) -> Channel? {
        guard let name = name,
              let streamId = streamId else {
            return nil
        }
        
        let ext = containerExtension ?? "mp4"
        let url = "\(account.serverUrl)/movie/\(account.username)/\(account.password)/\(streamId).\(ext)"
        
        return Channel(
            id: "vod_\(streamId)",
            name: name,
            url: url,
            logoUrl: streamIcon,
            group: categoryId
        )
    }
}
