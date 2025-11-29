//
//  MainViewModel.swift
//  IPTVPlayer
//
//  Main view model for managing playlists and channels
//

import Foundation
import SwiftUI

/// Main view model that manages playlists, channels, and user interactions
@MainActor
class MainViewModel: ObservableObject {
    // MARK: - Published Properties
    
    /// List of all playlists
    @Published var playlists: [Playlist] = []
    
    /// Currently selected playlist
    @Published var selectedPlaylist: Playlist?
    
    /// Channels from the selected playlist
    @Published var channels: [Channel] = []
    
    /// Filtered channels based on search query
    @Published var filteredChannels: [Channel] = []
    
    /// Currently selected channel
    @Published var selectedChannel: Channel?
    
    /// Loading state indicator
    @Published var isLoading: Bool = false
    
    /// Error message to display
    @Published var errorMessage: String?
    
    /// Search query for filtering channels (with debouncing)
    @Published var searchQuery: String = "" {
        didSet {
            // Cancel previous debounce task
            searchDebounceTask?.cancel()
            
            // Create new debounce task
            searchDebounceTask = Task { @MainActor in
                try? await Task.sleep(nanoseconds: 300_000_000) // 300ms debounce
                guard !Task.isCancelled else { return }
                filterChannels()
            }
        }
    }
    
    private var searchDebounceTask: Task<Void, Never>?
    
    /// Selected category filter
    @Published var selectedCategory: String? {
        didSet {
            filterChannels()
        }
    }
    
    // MARK: - Dependencies
    
    private let playlistRepository: PlaylistRepository
    private let favoriteRepository: FavoriteRepository
    private let m3uParser: M3UParser
    private let xtreamClient: XtreamClient
    private let keychainManager: KeychainManager
    private let inputValidator: InputValidator
    
    // MARK: - Initialization
    
    init(
        playlistRepository: PlaylistRepository,
        favoriteRepository: FavoriteRepository,
        m3uParser: M3UParser,
        xtreamClient: XtreamClient,
        keychainManager: KeychainManager,
        inputValidator: InputValidator
    ) {
        self.playlistRepository = playlistRepository
        self.favoriteRepository = favoriteRepository
        self.m3uParser = m3uParser
        self.xtreamClient = xtreamClient
        self.keychainManager = keychainManager
        self.inputValidator = inputValidator
    }
    
    // MARK: - Playlist Management
    
    /// Load all playlists from the repository
    func loadPlaylists() async {
        isLoading = true
        errorMessage = nil
        
        do {
            playlists = try await playlistRepository.getAllPlaylists()
        } catch {
            errorMessage = "Failed to load playlists: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    /// Add a new M3U playlist from URL
    /// - Parameter url: The URL of the M3U playlist
    func addM3UPlaylist(url: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Sanitize input
            let sanitizedUrl = inputValidator.sanitizeInput(url)
            
            // Validate URL (don't require HTTPS for M3U as many servers use HTTP)
            let validation = inputValidator.validateURL(sanitizedUrl, requireHTTPS: false)
            guard validation.isValid else {
                throw AppError.parsingError(message: validation.errorMessage ?? "Invalid URL")
            }
            
            // Validate URL
            guard let playlistURL = URL(string: sanitizedUrl) else {
                throw AppError.parsingError(message: "Invalid URL format")
            }
            
            // Download M3U content
            let (data, _) = try await URLSession.shared.data(from: playlistURL)
            guard let content = String(data: data, encoding: .utf8) else {
                throw AppError.parsingError(message: "Failed to decode M3U content")
            }
            
            // Parse channels
            let parsedChannels = try await m3uParser.parse(content: content)
            
            // Create playlist
            let playlist = Playlist(
                name: extractPlaylistName(from: url),
                url: url,
                type: .m3uUrl,
                channels: parsedChannels
            )
            
            // Save to repository
            try await playlistRepository.savePlaylist(playlist)
            
            // Reload playlists
            await loadPlaylists()
            
        } catch let error as AppError {
            errorMessage = error.localizedDescription
        } catch {
            errorMessage = "Failed to add M3U playlist: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    /// Add a new M3U playlist from file
    /// - Parameter fileURL: The file URL of the M3U playlist
    func addM3UPlaylistFromFile(fileURL: URL) async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Read file content
            let content = try String(contentsOf: fileURL, encoding: .utf8)
            
            // Parse channels
            let parsedChannels = try await m3uParser.parse(content: content)
            
            // Create playlist
            let playlist = Playlist(
                name: fileURL.deletingPathExtension().lastPathComponent,
                url: fileURL.path,
                type: .m3uFile,
                channels: parsedChannels
            )
            
            // Save to repository
            try await playlistRepository.savePlaylist(playlist)
            
            // Reload playlists
            await loadPlaylists()
            
        } catch let error as AppError {
            errorMessage = error.localizedDescription
        } catch {
            errorMessage = "Failed to add M3U playlist from file: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    /// Add a new Xtream Codes account
    /// - Parameters:
    ///   - serverUrl: The Xtream server URL
    ///   - username: The username
    ///   - password: The password
    func addXtreamAccount(serverUrl: String, username: String, password: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Sanitize inputs
            let sanitizedServerUrl = inputValidator.sanitizeInput(serverUrl)
            let sanitizedUsername = inputValidator.sanitizeInput(username)
            let sanitizedPassword = inputValidator.sanitizeInput(password)
            
            // Validate credentials (requires HTTPS for security)
            let validation = inputValidator.validateXtreamCredentials(
                serverUrl: sanitizedServerUrl,
                username: sanitizedUsername,
                password: sanitizedPassword
            )
            guard validation.isValid else {
                throw AppError.parsingError(message: validation.errorMessage ?? "Invalid credentials")
            }
            
            // Create account
            let account = XtreamAccount(
                serverUrl: sanitizedServerUrl,
                username: sanitizedUsername,
                password: sanitizedPassword
            )
            
            // Save credentials to Keychain
            try keychainManager.saveXtreamAccount(account)
            
            // Authenticate
            let authenticated = try await xtreamClient.authenticate(account: account)
            guard authenticated else {
                throw AppError.networkError(underlying: NSError(
                    domain: "XtreamClient",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Authentication failed"]
                ))
            }
            
            // Fetch live streams (all categories)
            let liveChannels = try await xtreamClient.getLiveStreams(account: account, categoryId: nil)
            
            // Fetch categories
            let categories = try await xtreamClient.getLiveCategories(account: account)
            
            // Create playlist
            let playlist = Playlist(
                name: "Xtream - \(username)",
                type: .xtream,
                channels: liveChannels,
                categories: categories,
                xtreamAccount: account
            )
            
            // Save to repository
            try await playlistRepository.savePlaylist(playlist)
            
            // Reload playlists
            await loadPlaylists()
            
        } catch let error as AppError {
            errorMessage = error.localizedDescription
        } catch {
            errorMessage = "Failed to add Xtream account: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    /// Delete a playlist
    /// - Parameter playlist: The playlist to delete
    func deletePlaylist(_ playlist: Playlist) async {
        isLoading = true
        errorMessage = nil
        
        do {
            // If this is an Xtream playlist, delete credentials from Keychain
            if let xtreamAccount = playlist.xtreamAccount {
                try? keychainManager.deleteXtreamAccount(serverUrl: xtreamAccount.serverUrl)
            }
            
            try await playlistRepository.deletePlaylist(id: playlist.id)
            
            // If the deleted playlist was selected, clear selection
            if selectedPlaylist?.id == playlist.id {
                selectedPlaylist = nil
                channels = []
                filteredChannels = []
            }
            
            // Reload playlists
            await loadPlaylists()
            
        } catch {
            errorMessage = "Failed to delete playlist: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    /// Rename a playlist
    /// - Parameters:
    ///   - playlist: The playlist to rename
    ///   - newName: The new name
    func renamePlaylist(_ playlist: Playlist, newName: String) async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Sanitize and validate new name
            let sanitizedName = inputValidator.sanitizeInput(newName)
            let validation = inputValidator.validatePlaylistName(sanitizedName)
            guard validation.isValid else {
                throw AppError.parsingError(message: validation.errorMessage ?? "Invalid playlist name")
            }
            
            // Create updated playlist
            let updatedPlaylist = Playlist(
                id: playlist.id,
                name: sanitizedName,
                url: playlist.url,
                type: playlist.type,
                channels: playlist.channels,
                categories: playlist.categories,
                xtreamAccount: playlist.xtreamAccount,
                createdAt: playlist.createdAt,
                updatedAt: Date()
            )
            
            // Update in repository
            try await playlistRepository.updatePlaylist(updatedPlaylist)
            
            // Update selected playlist if it's the one being renamed
            if selectedPlaylist?.id == playlist.id {
                selectedPlaylist = updatedPlaylist
            }
            
            // Reload playlists
            await loadPlaylists()
            
        } catch {
            errorMessage = "Failed to rename playlist: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    /// Duplicate a playlist
    /// - Parameter playlist: The playlist to duplicate
    func duplicatePlaylist(_ playlist: Playlist) async {
        isLoading = true
        errorMessage = nil
        
        do {
            // Create duplicated playlist with new ID and name
            let duplicatedPlaylist = Playlist(
                name: "\(playlist.name) (Copy)",
                url: playlist.url,
                type: playlist.type,
                channels: playlist.channels,
                categories: playlist.categories,
                xtreamAccount: playlist.xtreamAccount
            )
            
            // Save to repository
            try await playlistRepository.savePlaylist(duplicatedPlaylist)
            
            // Reload playlists
            await loadPlaylists()
            
        } catch {
            errorMessage = "Failed to duplicate playlist: \(error.localizedDescription)"
        }
        
        isLoading = false
    }
    
    /// Select a playlist and load its channels
    /// - Parameter playlist: The playlist to select
    func selectPlaylist(_ playlist: Playlist) async {
        selectedPlaylist = playlist
        channels = playlist.channels
        
        // Load favorites cache for this playlist
        await loadFavoritesCache()
        
        filterChannels()
    }
    
    // MARK: - Channel Management
    
    /// Filter channels based on search query and category (optimized)
    private func filterChannels() {
        // Use lazy evaluation for better performance with large channel lists
        var filtered = channels[...]
        
        // Filter by search query
        if !searchQuery.isEmpty {
            let lowercasedQuery = searchQuery.lowercased()
            filtered = filtered.filter { channel in
                channel.name.lowercased().contains(lowercasedQuery)
            }
        }
        
        // Filter by category
        if let category = selectedCategory {
            filtered = filtered.filter { channel in
                channel.group == category
            }
        }
        
        // Only update if the result is different to avoid unnecessary UI updates
        let newFiltered = Array(filtered)
        if newFiltered != filteredChannels {
            filteredChannels = newFiltered
        }
    }
    
    /// Select a channel for playback
    /// - Parameter channel: The channel to select
    func selectChannel(_ channel: Channel) {
        selectedChannel = channel
    }
    
    // MARK: - Favorite Management
    
    /// Cache of favorite channel IDs for the current playlist
    private var favoritesCache: Set<String> = []
    
    /// Load favorites cache for the current playlist
    private func loadFavoritesCache() async {
        guard let playlistId = selectedPlaylist?.id else {
            favoritesCache = []
            return
        }
        
        do {
            let favorites = try await favoriteRepository.getAllFavorites()
            favoritesCache = Set(favorites
                .filter { $0.playlistId == playlistId }
                .map { $0.channelId })
        } catch {
            favoritesCache = []
        }
    }
    
    /// Toggle favorite status for a channel
    /// - Parameter channel: The channel to toggle
    func toggleFavorite(_ channel: Channel) async {
        guard let playlistId = selectedPlaylist?.id else { return }
        
        do {
            let isFavorite = favoritesCache.contains(channel.id)
            
            if isFavorite {
                try await favoriteRepository.removeFavorite(
                    channelId: channel.id,
                    playlistId: playlistId
                )
                favoritesCache.remove(channel.id)
            } else {
                try await favoriteRepository.addFavorite(
                    channelId: channel.id,
                    playlistId: playlistId
                )
                favoritesCache.insert(channel.id)
            }
        } catch {
            errorMessage = "Failed to update favorite: \(error.localizedDescription)"
        }
    }
    
    /// Check if a channel is favorited (using cache)
    /// - Parameter channel: The channel to check
    /// - Returns: True if the channel is favorited
    func isFavorite(_ channel: Channel) async -> Bool {
        return favoritesCache.contains(channel.id)
    }
    
    /// Load all favorite channels
    func loadFavorites() async {
        do {
            let favorites = try await favoriteRepository.getAllFavorites()
            
            // Get unique playlist IDs from favorites
            let playlistIds = Set(favorites.map { $0.playlistId })
            
            // Load channels from playlists
            var favoriteChannels: [Channel] = []
            for playlistId in playlistIds {
                if let playlist = try await playlistRepository.getPlaylist(id: playlistId) {
                    let channelIds = Set(favorites.filter { $0.playlistId == playlistId }.map { $0.channelId })
                    let channels = playlist.channels.filter { channelIds.contains($0.id) }
                    favoriteChannels.append(contentsOf: channels)
                }
            }
            
            // Update channels with favorites
            channels = favoriteChannels
            filterChannels()
            
        } catch {
            errorMessage = "Failed to load favorites: \(error.localizedDescription)"
        }
    }
    
    // MARK: - Helper Methods
    
    /// Extract playlist name from URL
    /// - Parameter url: The URL string
    /// - Returns: A friendly playlist name
    private func extractPlaylistName(from url: String) -> String {
        guard let urlComponents = URLComponents(string: url),
              let host = urlComponents.host else {
            return "New Playlist"
        }
        
        return "Playlist - \(host)"
    }
    
    /// Clear error message
    func clearError() {
        errorMessage = nil
    }
}
