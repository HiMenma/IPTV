//
//  Channel.swift
//  IPTVPlayer
//
//  Data model for IPTV channels
//

import Foundation

/// Represents an IPTV channel with metadata
struct Channel: Identifiable, Codable, Equatable {
    let id: String
    let name: String
    let url: String
    let logoUrl: String?
    let group: String?
    let tvgId: String?
    let headers: [String: String]
    
    init(
        id: String = UUID().uuidString,
        name: String,
        url: String,
        logoUrl: String? = nil,
        group: String? = nil,
        tvgId: String? = nil,
        headers: [String: String] = [:]
    ) {
        self.id = id
        self.name = name
        self.url = url
        self.logoUrl = logoUrl
        self.group = group
        self.tvgId = tvgId
        self.headers = headers
    }
}
