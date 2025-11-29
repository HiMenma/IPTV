//
//  XtreamClient.swift
//  IPTVPlayer
//
//  Xtream Codes API client implementation
//

import Foundation

/// Protocol for Xtream Codes API client
protocol XtreamClient {
    /// Authenticate with Xtream Codes server
    /// - Parameter account: The Xtream account credentials
    /// - Returns: True if authentication successful
    /// - Throws: XtreamClientError if authentication fails
    func authenticate(account: XtreamAccount) async throws -> Bool
    
    /// Get live stream categories
    /// - Parameter account: The Xtream account credentials
    /// - Returns: Array of categories
    /// - Throws: XtreamClientError if request fails
    func getLiveCategories(account: XtreamAccount) async throws -> [Category]
    
    /// Get live streams
    /// - Parameter account: The Xtream account credentials
    /// - Parameter categoryId: Optional category ID to filter streams
    /// - Returns: Array of channels
    /// - Throws: XtreamClientError if request fails
    func getLiveStreams(account: XtreamAccount, categoryId: String?) async throws -> [Channel]
    
    /// Get VOD categories
    /// - Parameter account: The Xtream account credentials
    /// - Returns: Array of categories
    /// - Throws: XtreamClientError if request fails
    func getVODCategories(account: XtreamAccount) async throws -> [Category]
    
    /// Get VOD streams
    /// - Parameter account: The Xtream account credentials
    /// - Parameter categoryId: Optional category ID to filter streams
    /// - Returns: Array of channels
    /// - Throws: XtreamClientError if request fails
    func getVODStreams(account: XtreamAccount, categoryId: String?) async throws -> [Channel]
    
    /// Get EPG data for a stream
    /// - Parameter account: The Xtream account credentials
    /// - Parameter streamId: The stream ID
    /// - Returns: EPG data as dictionary
    /// - Throws: XtreamClientError if request fails
    func getEPGData(account: XtreamAccount, streamId: String) async throws -> [String: Any]
}

/// Errors that can occur during Xtream API operations
enum XtreamClientError: LocalizedError {
    case invalidURL
    case authenticationFailed(message: String)
    case networkError(underlying: Error)
    case invalidResponse
    case decodingError(underlying: Error)
    case serverError(statusCode: Int)
    case noData
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid server URL"
        case .authenticationFailed(let message):
            return "Authentication failed: \(message)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .invalidResponse:
            return "Invalid response from server"
        case .decodingError(let error):
            return "Failed to decode response: \(error.localizedDescription)"
        case .serverError(let statusCode):
            return "Server error with status code: \(statusCode)"
        case .noData:
            return "No data received from server"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .invalidURL:
            return "Please check the server URL format"
        case .authenticationFailed:
            return "Please check your username and password"
        case .networkError:
            return "Please check your internet connection and try again"
        case .invalidResponse, .decodingError:
            return "The server response format is unexpected. Please contact support"
        case .serverError:
            return "The server encountered an error. Please try again later"
        case .noData:
            return "No data was returned. Please try again"
        }
    }
}

/// Implementation of Xtream Codes API client
class XtreamClientImpl: XtreamClient {
    private let session: URLSession
    private let maxRetries: Int
    private let initialRetryDelay: TimeInterval
    private let enforceHTTPS: Bool
    
    init(
        session: URLSession = .shared,
        maxRetries: Int = 3,
        initialRetryDelay: TimeInterval = 1.0,
        enforceHTTPS: Bool = true
    ) {
        self.session = session
        self.maxRetries = maxRetries
        self.initialRetryDelay = initialRetryDelay
        self.enforceHTTPS = enforceHTTPS
    }
    
    // MARK: - Public API
    
    func authenticate(account: XtreamAccount) async throws -> Bool {
        // Validate HTTPS if enforced
        try validateHTTPS(serverUrl: account.serverUrl)
        
        let urlString = "\(account.serverUrl)/player_api.php?username=\(account.username)&password=\(account.password)"
        
        guard let url = URL(string: urlString) else {
            throw XtreamClientError.invalidURL
        }
        
        let response: XtreamAuthResponse = try await retryWithExponentialBackoff {
            try await self.performRequest(url: url)
        }
        
        // Check authentication status
        if let auth = response.userInfo?.auth, auth == 1 {
            return true
        } else if let status = response.userInfo?.status, status == "Active" {
            return true
        } else if let message = response.userInfo?.message {
            throw XtreamClientError.authenticationFailed(message: message)
        } else {
            throw XtreamClientError.authenticationFailed(message: "Unknown authentication error")
        }
    }
    
    func getLiveCategories(account: XtreamAccount) async throws -> [Category] {
        // Validate HTTPS if enforced
        try validateHTTPS(serverUrl: account.serverUrl)
        
        let urlString = "\(account.serverUrl)/player_api.php?username=\(account.username)&password=\(account.password)&action=get_live_categories"
        
        guard let url = URL(string: urlString) else {
            throw XtreamClientError.invalidURL
        }
        
        let categories: [XtreamCategory] = try await retryWithExponentialBackoff {
            try await self.performRequest(url: url)
        }
        
        return categories.map { $0.toCategory() }
    }
    
    func getLiveStreams(account: XtreamAccount, categoryId: String? = nil) async throws -> [Channel] {
        // Validate HTTPS if enforced
        try validateHTTPS(serverUrl: account.serverUrl)
        
        var urlString = "\(account.serverUrl)/player_api.php?username=\(account.username)&password=\(account.password)&action=get_live_streams"
        
        if let categoryId = categoryId {
            urlString += "&category_id=\(categoryId)"
        }
        
        guard let url = URL(string: urlString) else {
            throw XtreamClientError.invalidURL
        }
        
        let streams: [XtreamStream] = try await retryWithExponentialBackoff {
            try await self.performRequest(url: url)
        }
        
        return streams.compactMap { $0.toChannel(account: account) }
    }
    
    func getVODCategories(account: XtreamAccount) async throws -> [Category] {
        // Validate HTTPS if enforced
        try validateHTTPS(serverUrl: account.serverUrl)
        
        let urlString = "\(account.serverUrl)/player_api.php?username=\(account.username)&password=\(account.password)&action=get_vod_categories"
        
        guard let url = URL(string: urlString) else {
            throw XtreamClientError.invalidURL
        }
        
        let categories: [XtreamCategory] = try await retryWithExponentialBackoff {
            try await self.performRequest(url: url)
        }
        
        return categories.map { $0.toCategory() }
    }
    
    func getVODStreams(account: XtreamAccount, categoryId: String? = nil) async throws -> [Channel] {
        // Validate HTTPS if enforced
        try validateHTTPS(serverUrl: account.serverUrl)
        
        var urlString = "\(account.serverUrl)/player_api.php?username=\(account.username)&password=\(account.password)&action=get_vod_streams"
        
        if let categoryId = categoryId {
            urlString += "&category_id=\(categoryId)"
        }
        
        guard let url = URL(string: urlString) else {
            throw XtreamClientError.invalidURL
        }
        
        let streams: [XtreamVODStream] = try await retryWithExponentialBackoff {
            try await self.performRequest(url: url)
        }
        
        return streams.compactMap { $0.toChannel(account: account) }
    }
    
    func getEPGData(account: XtreamAccount, streamId: String) async throws -> [String: Any] {
        // Validate HTTPS if enforced
        try validateHTTPS(serverUrl: account.serverUrl)
        
        let urlString = "\(account.serverUrl)/player_api.php?username=\(account.username)&password=\(account.password)&action=get_simple_data_table&stream_id=\(streamId)"
        
        guard let url = URL(string: urlString) else {
            throw XtreamClientError.invalidURL
        }
        
        return try await retryWithExponentialBackoff {
            try await self.performRequestRaw(url: url)
        }
    }
    
    // MARK: - Private Helpers
    
    /// Perform HTTP request and decode JSON response
    private func performRequest<T: Decodable>(url: URL) async throws -> T {
        let (data, response) = try await session.data(from: url)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw XtreamClientError.invalidResponse
        }
        
        guard (200...299).contains(httpResponse.statusCode) else {
            throw XtreamClientError.serverError(statusCode: httpResponse.statusCode)
        }
        
        guard !data.isEmpty else {
            throw XtreamClientError.noData
        }
        
        do {
            let decoder = JSONDecoder()
            return try decoder.decode(T.self, from: data)
        } catch {
            throw XtreamClientError.decodingError(underlying: error)
        }
    }
    
    /// Perform HTTP request and return raw JSON dictionary
    private func performRequestRaw(url: URL) async throws -> [String: Any] {
        let (data, response) = try await session.data(from: url)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw XtreamClientError.invalidResponse
        }
        
        guard (200...299).contains(httpResponse.statusCode) else {
            throw XtreamClientError.serverError(statusCode: httpResponse.statusCode)
        }
        
        guard !data.isEmpty else {
            throw XtreamClientError.noData
        }
        
        do {
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                throw XtreamClientError.invalidResponse
            }
            return json
        } catch {
            throw XtreamClientError.decodingError(underlying: error)
        }
    }
    
    /// Retry operation with exponential backoff
    private func retryWithExponentialBackoff<T>(
        operation: @escaping () async throws -> T
    ) async throws -> T {
        var attempt = 0
        var delay = initialRetryDelay
        var lastError: Error?
        
        while attempt < maxRetries {
            do {
                return try await operation()
            } catch let error as XtreamClientError {
                lastError = error
                
                // Don't retry on certain errors
                switch error {
                case .invalidURL, .authenticationFailed, .decodingError:
                    throw error
                default:
                    break
                }
                
                attempt += 1
                if attempt >= maxRetries {
                    throw error
                }
                
                // Wait before retrying
                try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                delay *= 2
            } catch {
                lastError = error
                attempt += 1
                
                if attempt >= maxRetries {
                    throw XtreamClientError.networkError(underlying: error)
                }
                
                // Wait before retrying
                try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                delay *= 2
            }
        }
        
        // This should never be reached, but just in case
        throw lastError ?? XtreamClientError.networkError(underlying: NSError(domain: "XtreamClient", code: -1))
    }
    
    /// Validate that the server URL uses HTTPS protocol
    private func validateHTTPS(serverUrl: String) throws {
        guard enforceHTTPS else { return }
        
        guard let url = URL(string: serverUrl),
              let scheme = url.scheme?.lowercased() else {
            throw XtreamClientError.invalidURL
        }
        
        guard scheme == "https" else {
            throw XtreamClientError.authenticationFailed(
                message: "Server URL must use HTTPS protocol for security. HTTP is not allowed."
            )
        }
    }
}
