//
//  VideoPlayerService.swift
//  IPTVPlayer
//
//  Video player service protocol and AVPlayer implementation
//

import Foundation
import AVFoundation
import Combine

/// Protocol defining video player service interface
@MainActor
protocol VideoPlayerService: AnyObject {
    /// Current playback state
    var isPlaying: Bool { get }
    
    /// Current playback time in seconds
    var currentTime: TimeInterval { get }
    
    /// Total duration of the current media in seconds
    var duration: TimeInterval { get }
    
    /// Current volume (0.0 to 1.0)
    var volume: Float { get set }
    
    /// Current fullscreen state
    var isFullscreen: Bool { get set }
    
    /// Publisher for playback state changes
    var isPlayingPublisher: AnyPublisher<Bool, Never> { get }
    
    /// Publisher for current time updates
    var currentTimePublisher: AnyPublisher<TimeInterval, Never> { get }
    
    /// Publisher for duration updates
    var durationPublisher: AnyPublisher<TimeInterval, Never> { get }
    
    /// Publisher for player errors
    var errorPublisher: AnyPublisher<AppError, Never> { get }
    
    /// Publisher for buffering state
    var isBufferingPublisher: AnyPublisher<Bool, Never> { get }
    
    /// Publisher for fullscreen state changes
    var isFullscreenPublisher: AnyPublisher<Bool, Never> { get }
    
    /// Play media from URL
    /// - Parameter url: URL of the media to play
    func play(url: URL)
    
    /// Pause playback
    func pause()
    
    /// Resume playback
    func resume()
    
    /// Stop playback and release resources
    func stop()
    
    /// Seek to specific time
    /// - Parameter time: Time in seconds to seek to
    func seek(to time: TimeInterval)
    
    /// Toggle fullscreen mode
    func toggleFullscreen()
    
    /// Get the underlying AVPlayer instance for UI integration
    func getAVPlayer() -> AVPlayer?
}

/// AVPlayer-based implementation of VideoPlayerService
@MainActor
class AVPlayerService: VideoPlayerService {
    // MARK: - Properties
    
    private let player: AVPlayer
    private var timeObserver: Any?
    private var statusObserver: NSKeyValueObservation?
    private var rateObserver: NSKeyValueObservation?
    private var durationObserver: NSKeyValueObservation?
    private var bufferEmptyObserver: NSKeyValueObservation?
    private var bufferKeepUpObserver: NSKeyValueObservation?
    
    // Publishers
    private let isPlayingSubject = CurrentValueSubject<Bool, Never>(false)
    private let currentTimeSubject = CurrentValueSubject<TimeInterval, Never>(0)
    private let durationSubject = CurrentValueSubject<TimeInterval, Never>(0)
    private let errorSubject = PassthroughSubject<AppError, Never>()
    private let isBufferingSubject = CurrentValueSubject<Bool, Never>(false)
    private let isFullscreenSubject = CurrentValueSubject<Bool, Never>(false)
    
    // Reconnection state
    private var currentURL: URL?
    private var reconnectionTask: Task<Void, Never>?
    private var reconnectionAttempts = 0
    private let maxReconnectionAttempts = 5
    private var isReconnecting = false
    
    // Buffering timeout
    private var bufferingTimeoutTask: Task<Void, Never>?
    private let bufferingTimeoutDuration: TimeInterval = 30.0
    
    // MARK: - VideoPlayerService Protocol
    
    var isPlaying: Bool {
        player.rate > 0 && player.error == nil
    }
    
    var currentTime: TimeInterval {
        player.currentTime().seconds
    }
    
    var duration: TimeInterval {
        player.currentItem?.duration.seconds ?? 0
    }
    
    var volume: Float {
        get { player.volume }
        set { 
            player.volume = newValue
            AppLogger.player("Volume set to: \(newValue)", level: .debug)
        }
    }
    
    var isFullscreen: Bool {
        get { isFullscreenSubject.value }
        set { 
            isFullscreenSubject.send(newValue)
            AppLogger.player("Fullscreen state changed to: \(newValue)", level: .debug)
        }
    }
    
    var isPlayingPublisher: AnyPublisher<Bool, Never> {
        isPlayingSubject.eraseToAnyPublisher()
    }
    
    var currentTimePublisher: AnyPublisher<TimeInterval, Never> {
        currentTimeSubject.eraseToAnyPublisher()
    }
    
    var durationPublisher: AnyPublisher<TimeInterval, Never> {
        durationSubject.eraseToAnyPublisher()
    }
    
    var errorPublisher: AnyPublisher<AppError, Never> {
        errorSubject.eraseToAnyPublisher()
    }
    
    var isBufferingPublisher: AnyPublisher<Bool, Never> {
        isBufferingSubject.eraseToAnyPublisher()
    }
    
    var isFullscreenPublisher: AnyPublisher<Bool, Never> {
        isFullscreenSubject.eraseToAnyPublisher()
    }
    
    // MARK: - Initialization
    
    init() {
        // Create AVPlayer with optimized settings
        self.player = AVPlayer()
        
        // Enable hardware acceleration
        player.allowsExternalPlayback = true
        player.appliesMediaSelectionCriteriaAutomatically = true
        
        // Configure audio session for optimal playback
        configureAudioSession()
        
        // Set up observers
        setupObservers()
        
        AppLogger.player("AVPlayerService initialized", level: .info)
    }
    
    deinit {
        // Cancel any pending reconnection
        reconnectionTask?.cancel()
        bufferingTimeoutTask?.cancel()
        
        // Cleanup must be synchronous in deinit
        // Remove time observer
        if let timeObserver = timeObserver {
            player.removeTimeObserver(timeObserver)
        }
        
        // Remove KVO observers
        statusObserver?.invalidate()
        rateObserver?.invalidate()
        durationObserver?.invalidate()
        bufferEmptyObserver?.invalidate()
        bufferKeepUpObserver?.invalidate()
        
        // Remove notification observers
        NotificationCenter.default.removeObserver(self)
        
        // Stop playback
        player.pause()
        player.replaceCurrentItem(with: nil)
    }
    
    // MARK: - Public Methods
    
    func play(url: URL) {
        AppLogger.player("Playing URL: \(url.absoluteString)", level: .info)
        
        // Store URL for potential reconnection
        currentURL = url
        reconnectionAttempts = 0
        isReconnecting = false
        
        // Cancel any pending reconnection
        reconnectionTask?.cancel()
        reconnectionTask = nil
        
        // Stop current playback
        stop()
        
        // Show buffering indicator
        isBufferingSubject.send(true)
        
        // Create player item with optimized settings
        let asset = AVURLAsset(url: url, options: createAssetOptions(for: url))
        let playerItem = AVPlayerItem(asset: asset)
        
        // Configure player item for optimal streaming
        configurePlayerItem(playerItem)
        
        // Set up item observers
        setupPlayerItemObservers(playerItem)
        
        // Replace current item and play
        player.replaceCurrentItem(with: playerItem)
        player.play()
        
        isPlayingSubject.send(true)
        
        // Start buffering timeout
        startBufferingTimeout()
    }
    
    func pause() {
        AppLogger.player("Pausing playback", level: .debug)
        player.pause()
        isPlayingSubject.send(false)
    }
    
    func resume() {
        AppLogger.player("Resuming playback", level: .debug)
        player.play()
        isPlayingSubject.send(true)
    }
    
    func stop() {
        AppLogger.player("Stopping playback", level: .debug)
        
        // Cancel any pending reconnection or buffering timeout
        reconnectionTask?.cancel()
        reconnectionTask = nil
        bufferingTimeoutTask?.cancel()
        bufferingTimeoutTask = nil
        
        // Clear reconnection state
        currentURL = nil
        reconnectionAttempts = 0
        isReconnecting = false
        
        player.pause()
        player.replaceCurrentItem(with: nil)
        isPlayingSubject.send(false)
        currentTimeSubject.send(0)
        durationSubject.send(0)
        isBufferingSubject.send(false)
    }
    
    func seek(to time: TimeInterval) {
        AppLogger.player("Seeking to time: \(time)", level: .debug)
        let cmTime = CMTime(seconds: time, preferredTimescale: 600)
        player.seek(to: cmTime, toleranceBefore: .zero, toleranceAfter: .zero)
    }
    
    func toggleFullscreen() {
        isFullscreen = !isFullscreen
        AppLogger.player("Toggled fullscreen to: \(isFullscreen)", level: .info)
    }
    
    func getAVPlayer() -> AVPlayer? {
        return player
    }
    
    // MARK: - Private Methods
    
    private func configureAudioSession() {
        // Audio session configuration is handled by AppKit on macOS
        // No explicit configuration needed like on iOS
    }
    
    private func createAssetOptions(for url: URL) -> [String: Any] {
        var options: [String: Any] = [:]
        
        // Detect stream type and configure accordingly
        let urlString = url.absoluteString.lowercased()
        
        if urlString.contains(".m3u8") {
            // HLS stream configuration
            options[AVURLAssetPreferPreciseDurationAndTimingKey] = false
            AppLogger.player("Configured for HLS stream", level: .debug)
        } else if urlString.hasPrefix("rtsp://") {
            // RTSP stream configuration
            options[AVURLAssetPreferPreciseDurationAndTimingKey] = false
            AppLogger.player("Configured for RTSP stream", level: .debug)
        } else if urlString.hasPrefix("http://") || urlString.hasPrefix("https://") {
            // HTTP stream configuration
            options[AVURLAssetPreferPreciseDurationAndTimingKey] = true
            AppLogger.player("Configured for HTTP stream", level: .debug)
        }
        
        // Enable hardware acceleration
        options["AVURLAssetHTTPHeaderFieldsKey"] = [
            "User-Agent": "IPTVPlayer/1.0"
        ]
        
        return options
    }
    
    private func configurePlayerItem(_ item: AVPlayerItem) {
        // Configure buffering
        item.preferredForwardBufferDuration = 5.0
        
        // Enable automatic quality selection for HLS
        item.automaticallyPreservesTimeOffsetFromLive = true
        
        // Configure for low latency when possible
        if #available(macOS 12.0, *) {
            item.preferredMaximumResolution = CGSize(width: 1920, height: 1080)
        }
    }
    
    private func setupObservers() {
        // Observe playback rate changes
        rateObserver = player.observe(\.rate, options: [.new]) { [weak self] player, _ in
            Task { @MainActor in
                self?.isPlayingSubject.send(player.rate > 0)
            }
        }
        
        // Observe current time periodically
        let interval = CMTime(seconds: 0.5, preferredTimescale: 600)
        timeObserver = player.addPeriodicTimeObserver(forInterval: interval, queue: .main) { [weak self] time in
            self?.currentTimeSubject.send(time.seconds)
        }
    }
    
    private func setupPlayerItemObservers(_ item: AVPlayerItem) {
        // Observe status changes
        statusObserver = item.observe(\.status, options: [.new]) { [weak self] item, _ in
            Task { @MainActor in
                self?.handleStatusChange(item.status, item: item)
            }
        }
        
        // Observe duration changes
        durationObserver = item.observe(\.duration, options: [.new]) { [weak self] item, _ in
            Task { @MainActor in
                let duration = item.duration.seconds
                if duration.isFinite && duration > 0 {
                    self?.durationSubject.send(duration)
                }
            }
        }
        
        // Observe buffering state
        bufferEmptyObserver = item.observe(\.isPlaybackBufferEmpty, options: [.new]) { [weak self] item, _ in
            Task { @MainActor in
                if item.isPlaybackBufferEmpty {
                    self?.isBufferingSubject.send(true)
                    AppLogger.player("Buffer empty - buffering", level: .debug)
                    self?.startBufferingTimeout()
                }
            }
        }
        
        bufferKeepUpObserver = item.observe(\.isPlaybackLikelyToKeepUp, options: [.new]) { [weak self] item, _ in
            Task { @MainActor in
                if item.isPlaybackLikelyToKeepUp {
                    self?.isBufferingSubject.send(false)
                    self?.cancelBufferingTimeout()
                    AppLogger.player("Buffer ready - resuming", level: .debug)
                    
                    // Reset reconnection attempts on successful buffer
                    if self?.isReconnecting == true {
                        AppLogger.player("Reconnection successful", level: .info)
                        self?.reconnectionAttempts = 0
                        self?.isReconnecting = false
                    }
                }
            }
        }
        
        // Observe playback errors
        NotificationCenter.default.addObserver(
            forName: .AVPlayerItemFailedToPlayToEndTime,
            object: item,
            queue: .main
        ) { [weak self] notification in
            if let error = notification.userInfo?[AVPlayerItemFailedToPlayToEndTimeErrorKey] as? Error {
                self?.handlePlayerError(error)
            }
        }
        
        // Observe playback stalls
        NotificationCenter.default.addObserver(
            forName: .AVPlayerItemPlaybackStalled,
            object: item,
            queue: .main
        ) { [weak self] _ in
            AppLogger.player("Playback stalled", level: .warning)
            self?.isBufferingSubject.send(true)
        }
    }
    
    private func handleStatusChange(_ status: AVPlayerItem.Status, item: AVPlayerItem) {
        switch status {
        case .readyToPlay:
            AppLogger.player("Player ready to play", level: .info)
            let duration = item.duration.seconds
            if duration.isFinite && duration > 0 {
                durationSubject.send(duration)
            }
            
            // Cancel buffering timeout when ready
            cancelBufferingTimeout()
            
            // If this was a reconnection, mark it as successful
            if isReconnecting {
                AppLogger.player("Reconnection successful - player ready", level: .info)
                reconnectionAttempts = 0
                isReconnecting = false
            }
            
        case .failed:
            AppLogger.player("Player failed with error: \(item.error?.localizedDescription ?? "unknown")", level: .error)
            
            // Cancel buffering timeout on failure
            cancelBufferingTimeout()
            
            if let error = item.error {
                handlePlayerError(error)
            } else {
                errorSubject.send(.playerError(message: "未知播放错误"))
            }
            
        case .unknown:
            AppLogger.player("Player status unknown", level: .debug)
            
        @unknown default:
            AppLogger.player("Player status: unknown default case", level: .warning)
        }
    }
    
    private func handlePlayerError(_ error: Error) {
        let nsError = error as NSError
        
        AppLogger.player("Handling player error: \(error.localizedDescription) (domain: \(nsError.domain), code: \(nsError.code))", level: .error)
        
        let appError: AppError
        var shouldAttemptReconnection = false
        
        switch nsError.code {
        case NSURLErrorNotConnectedToInternet, NSURLErrorNetworkConnectionLost:
            appError = .networkError(underlying: error)
            shouldAttemptReconnection = true
            
        case NSURLErrorTimedOut:
            appError = .networkError(underlying: error)
            shouldAttemptReconnection = true
            
        case NSURLErrorCannotFindHost, NSURLErrorCannotConnectToHost:
            appError = .playerError(message: "无法连接到流媒体服务器")
            shouldAttemptReconnection = true
            
        default:
            if nsError.domain == AVFoundationErrorDomain {
                switch nsError.code {
                case AVError.Code.contentIsUnavailable.rawValue:
                    appError = .playerError(message: "流媒体内容不可用")
                    shouldAttemptReconnection = false
                    
                case AVError.Code.noDataCaptured.rawValue:
                    appError = .playerError(message: "未从流媒体接收到数据")
                    shouldAttemptReconnection = true
                    
                case AVError.Code.decodeFailed.rawValue:
                    appError = .playerError(message: "视频解码失败")
                    shouldAttemptReconnection = false
                    
                case AVError.Code.unsupportedOutputSettings.rawValue:
                    appError = .playerError(message: "不支持的视频格式")
                    shouldAttemptReconnection = false
                    
                case AVError.Code.mediaServicesWereReset.rawValue:
                    appError = .playerError(message: "媒体服务已重置")
                    shouldAttemptReconnection = true
                    
                case AVError.Code.mediaDiscontinuity.rawValue:
                    appError = .playerError(message: "媒体流中断")
                    shouldAttemptReconnection = true
                    
                default:
                    appError = .playerError(message: error.localizedDescription)
                    shouldAttemptReconnection = false
                }
            } else {
                appError = .playerError(message: error.localizedDescription)
                shouldAttemptReconnection = false
            }
        }
        
        // Send error to subscribers
        errorSubject.send(appError)
        
        // Attempt automatic reconnection for network-related errors
        if shouldAttemptReconnection && !isReconnecting {
            attemptReconnection()
        }
    }
    
    private func attemptReconnection() {
        guard let url = currentURL else {
            AppLogger.player("Cannot reconnect: no URL stored", level: .warning)
            return
        }
        
        guard reconnectionAttempts < maxReconnectionAttempts else {
            AppLogger.player("Max reconnection attempts reached (\(maxReconnectionAttempts))", level: .error)
            errorSubject.send(.playerError(message: "无法重新连接到流媒体，已达到最大重试次数"))
            return
        }
        
        isReconnecting = true
        reconnectionAttempts += 1
        
        let delay = calculateReconnectionDelay(attempt: reconnectionAttempts)
        
        AppLogger.player("Attempting reconnection \(reconnectionAttempts)/\(maxReconnectionAttempts) in \(delay)s", level: .info)
        
        // Cancel any existing reconnection task
        reconnectionTask?.cancel()
        
        // Schedule reconnection
        reconnectionTask = Task { @MainActor in
            do {
                try await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                
                guard !Task.isCancelled else {
                    AppLogger.player("Reconnection cancelled", level: .debug)
                    return
                }
                
                AppLogger.player("Executing reconnection attempt \(reconnectionAttempts)", level: .info)
                
                // Stop current playback
                player.pause()
                player.replaceCurrentItem(with: nil)
                
                // Show buffering indicator
                isBufferingSubject.send(true)
                
                // Create new player item
                let asset = AVURLAsset(url: url, options: createAssetOptions(for: url))
                let playerItem = AVPlayerItem(asset: asset)
                
                configurePlayerItem(playerItem)
                setupPlayerItemObservers(playerItem)
                
                // Replace item and play
                player.replaceCurrentItem(with: playerItem)
                player.play()
                
                isPlayingSubject.send(true)
                isReconnecting = false
                
                // Start buffering timeout
                startBufferingTimeout()
                
                AppLogger.player("Reconnection attempt initiated", level: .info)
                
            } catch {
                AppLogger.player("Reconnection sleep interrupted: \(error.localizedDescription)", level: .warning)
                isReconnecting = false
            }
        }
    }
    
    private func calculateReconnectionDelay(attempt: Int) -> TimeInterval {
        // Exponential backoff: 2^(attempt-1) seconds, capped at 30 seconds
        let delay = pow(2.0, Double(attempt - 1))
        return min(delay, 30.0)
    }
    
    private func startBufferingTimeout() {
        // Cancel any existing timeout
        bufferingTimeoutTask?.cancel()
        
        bufferingTimeoutTask = Task { @MainActor in
            do {
                try await Task.sleep(nanoseconds: UInt64(bufferingTimeoutDuration * 1_000_000_000))
                
                guard !Task.isCancelled else { return }
                
                // If still buffering after timeout, report error
                if isBufferingSubject.value {
                    AppLogger.player("Buffering timeout after \(bufferingTimeoutDuration)s", level: .error)
                    errorSubject.send(.playerError(message: "缓冲超时，网络速度可能过慢"))
                    
                    // Attempt reconnection
                    if !isReconnecting {
                        attemptReconnection()
                    }
                }
            } catch {
                // Task was cancelled, ignore
            }
        }
    }
    
    private func cancelBufferingTimeout() {
        bufferingTimeoutTask?.cancel()
        bufferingTimeoutTask = nil
    }
    
}
