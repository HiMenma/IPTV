//
//  XtreamAccount.swift
//  IPTVPlayer
//
//  Data model for Xtream Codes account credentials
//

import Foundation

/// Represents Xtream Codes API account credentials
struct XtreamAccount: Codable, Equatable {
    let serverUrl: String
    let username: String
    let password: String
    
    init(serverUrl: String, username: String, password: String) {
        self.serverUrl = serverUrl
        self.username = username
        self.password = password
    }
}

/// Response from Xtream authentication endpoint
struct XtreamAuthResponse: Codable {
    let userInfo: UserInfo?
    let serverInfo: ServerInfo?
    
    struct UserInfo: Codable {
        let username: String?
        let password: String?
        let message: String?
        let auth: Int?
        let status: String?
        let expDate: String?
        let isTrial: String?
        let activeCons: String?
        let createdAt: String?
        let maxConnections: String?
        let allowedOutputFormats: [String]?
        
        enum CodingKeys: String, CodingKey {
            case username
            case password
            case message
            case auth
            case status
            case expDate = "exp_date"
            case isTrial = "is_trial"
            case activeCons = "active_cons"
            case createdAt = "created_at"
            case maxConnections = "max_connections"
            case allowedOutputFormats = "allowed_output_formats"
        }
    }
    
    struct ServerInfo: Codable {
        let url: String?
        let port: String?
        let httpsPort: String?
        let serverProtocol: String?
        let rtmpPort: String?
        let timezone: String?
        let timestampNow: Int?
        let timeNow: String?
        
        enum CodingKeys: String, CodingKey {
            case url
            case port
            case httpsPort = "https_port"
            case serverProtocol = "server_protocol"
            case rtmpPort = "rtmp_port"
            case timezone
            case timestampNow = "timestamp_now"
            case timeNow = "time_now"
        }
    }
    
    enum CodingKeys: String, CodingKey {
        case userInfo = "user_info"
        case serverInfo = "server_info"
    }
}
