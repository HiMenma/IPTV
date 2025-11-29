//
//  PlayerViewModel.swift
//  IPTVPlayer
//
//  View model for managing video player state and controls
//

import Foundation
import SwiftUI
import Combine
import AVFoundation

/// View model that manages video player state and controls
@MainActor
class PlayerViewModel: ObservableObject {
    // MARK: - Published Properties
    
    /// Current channel being played
    @Published var currentChannel: Channel?
    
    /// Whether the player is currently playing
    @Published var isPlaying: Bool = false
    
    /// Current playback volume (0.0 to 1.0)
    @Published var volume: Double = 1.0
    
    /// Whether the player is in fullscreen mode
    @Published var isFullscreen: Bool = false
    
    /// Current playback time in seconds
    @Published var currentTime: TimeInterval = 0
    
    /// Total duration of the current media in seconds
    @Published var duration: TimeInterval = 0
    
    /// Whether the player is currently buffering
    @Published var isBuffering: Bool = false
    
    /// Error message to display
    @Published var errorMessage: String?
    
    /// Whether to show player controls
    @Published var showControls: Bool = true
    
    // MARK: - Dependencies
    
    private let playerService: VideoPlayerService
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Control Timeout
    
    private var controlsTimeoutTask: Task<Void, Never>?
    private let controlsTimeoutDuration: TimeInterval = 3.0
    
    // MARK: - Initialization
    
    init(playerService: VideoPlayerService, channel: Channel? = nil) {
        self.playerService = playerService
        setupSubscriptions()
        
        // Auto-play if channel is provided
        if let channel = channel {
            Task { @MainActor in
                self.play(channel: channel)
            }
        }
    }
    
    // MARK: - Setup
    
    private func setupSubscriptions() {
        // Subscribe to player state changes
        playerService.isPlayingPublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$isPlaying)
        
        playerService.currentTimePublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$currentTime)
        
        playerService.durationPublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$duration)
        
        playerService.isBufferingPublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$isBuffering)
        
        playerService.isFullscreenPublisher
            .receive(on: DispatchQueue.main)
            .assign(to: &$isFullscreen)
        
        // Subscribe to player errors
        playerService.errorPublisher
            .receive(on: DispatchQueue.main)
            .sink { [weak self] error in
                self?.handlePlayerError(error)
            }
            .store(in: &cancellables)
        
        // Sync volume changes to player service
        $volume
            .dropFirst() // Skip initial value
            .sink { [weak self] newVolume in
                self?.playerService.volume = Float(newVolume)
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Playback Control
    
    /// Play a channel
    /// - Parameter channel: The channel to play
    func play(channel: Channel) {
        guard let url = URL(string: channel.url) else {
            errorMessage = "Invalid channel URL: \(channel.url)"
            return
        }
        
        currentChannel = channel
        errorMessage = nil
        
        playerService.play(url: url)
        
        // Show controls briefly when starting playback
        showControlsBriefly()
    }
    
    /// Pause playback
    func pause() {
        playerService.pause()
        
        // Keep controls visible when paused
        cancelControlsTimeout()
        showControls = true
    }
    
    /// Resume playback
    func resume() {
        playerService.resume()
        
        // Hide controls after resuming
        showControlsBriefly()
    }
    
    /// Stop playback
    func stop() {
        playerService.stop()
        currentChannel = nil
        errorMessage = nil
        
        // Reset state
        showControls = true
        cancelControlsTimeout()
    }
    
    /// Toggle play/pause
    func togglePlayPause() {
        if isPlaying {
            pause()
        } else {
            resume()
        }
    }
    
    /// Seek to a specific time
    /// - Parameter time: Time in seconds to seek to
    func seek(to time: TimeInterval) {
        playerService.seek(to: time)
        
        // Show controls briefly when seeking
        showControlsBriefly()
    }
    
    /// Seek forward by a specific amount
    /// - Parameter seconds: Number of seconds to seek forward
    func seekForward(by seconds: TimeInterval = 10) {
        let newTime = min(currentTime + seconds, duration)
        seek(to: newTime)
    }
    
    /// Seek backward by a specific amount
    /// - Parameter seconds: Number of seconds to seek backward
    func seekBackward(by seconds: TimeInterval = 10) {
        let newTime = max(currentTime - seconds, 0)
        seek(to: newTime)
    }
    
    // MARK: - Volume Control
    
    /// Set volume
    /// - Parameter volume: Volume level (0.0 to 1.0)
    func setVolume(_ volume: Double) {
        self.volume = max(0.0, min(1.0, volume))
        
        // Show controls briefly when adjusting volume
        showControlsBriefly()
    }
    
    /// Increase volume
    /// - Parameter amount: Amount to increase (default 0.1)
    func increaseVolume(by amount: Double = 0.1) {
        setVolume(volume + amount)
    }
    
    /// Decrease volume
    /// - Parameter amount: Amount to decrease (default 0.1)
    func decreaseVolume(by amount: Double = 0.1) {
        setVolume(volume - amount)
    }
    
    /// Toggle mute
    func toggleMute() {
        if volume > 0 {
            setVolume(0)
        } else {
            setVolume(1.0)
        }
    }
    
    // MARK: - Fullscreen Control
    
    /// Toggle fullscreen mode
    func toggleFullscreen() {
        playerService.toggleFullscreen()
        
        // Show controls briefly when toggling fullscreen
        showControlsBriefly()
    }
    
    // MARK: - Controls Visibility
    
    /// Show controls briefly and then hide them
    func showControlsBriefly() {
        showControls = true
        
        // Cancel any existing timeout
        cancelControlsTimeout()
        
        // Only auto-hide if playing
        guard isPlaying else { return }
        
        // Schedule auto-hide
        controlsTimeoutTask = Task { @MainActor in
            do {
                try await Task.sleep(nanoseconds: UInt64(controlsTimeoutDuration * 1_000_000_000))
                
                guard !Task.isCancelled else { return }
                
                // Hide controls if still playing
                if isPlaying {
                    showControls = false
                }
            } catch {
                // Task was cancelled, ignore
            }
        }
    }
    
    /// Toggle controls visibility
    func toggleControls() {
        showControls.toggle()
        
        if showControls {
            showControlsBriefly()
        } else {
            cancelControlsTimeout()
        }
    }
    
    private func cancelControlsTimeout() {
        controlsTimeoutTask?.cancel()
        controlsTimeoutTask = nil
    }
    
    // MARK: - Error Handling
    
    private func handlePlayerError(_ error: AppError) {
        errorMessage = error.localizedDescription
        
        // Show controls when there's an error
        showControls = true
        cancelControlsTimeout()
    }
    
    /// Clear error message
    func clearError() {
        errorMessage = nil
    }
    
    // MARK: - Computed Properties
    
    /// Formatted current time string (MM:SS)
    var currentTimeFormatted: String {
        formatTime(currentTime)
    }
    
    /// Formatted duration string (MM:SS)
    var durationFormatted: String {
        formatTime(duration)
    }
    
    /// Playback progress (0.0 to 1.0)
    var progress: Double {
        guard duration > 0 else { return 0 }
        return currentTime / duration
    }
    
    /// Whether the player has content loaded
    var hasContent: Bool {
        currentChannel != nil && duration > 0
    }
    
    // MARK: - Helper Methods
    
    private func formatTime(_ time: TimeInterval) -> String {
        guard time.isFinite && time >= 0 else { return "00:00" }
        
        let totalSeconds = Int(time)
        let minutes = totalSeconds / 60
        let seconds = totalSeconds % 60
        
        return String(format: "%02d:%02d", minutes, seconds)
    }
    
    /// Get the underlying AVPlayer for UI integration
    func getAVPlayer() -> AVPlayer? {
        return playerService.getAVPlayer()
    }
    
    // MARK: - Cleanup
    
    deinit {
        // Task will be automatically cancelled when the object is deallocated
        // cancellables will be automatically cleaned up
    }
}
