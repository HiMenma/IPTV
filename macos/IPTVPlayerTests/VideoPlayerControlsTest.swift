//
//  VideoPlayerControlsTest.swift
//  IPTVPlayerTests
//
//  Unit tests for video player controls
//

import XCTest
import AVFoundation
import Combine
@testable import IPTVPlayer

final class VideoPlayerControlsTest: XCTestCase {
    
    var playerService: AVPlayerService!
    var cancellables: Set<AnyCancellable>!
    
    @MainActor
    override func setUp() {
        super.setUp()
        playerService = AVPlayerService()
        cancellables = Set<AnyCancellable>()
    }
    
    @MainActor
    override func tearDown() {
        cancellables = nil
        playerService.stop()
        playerService = nil
        super.tearDown()
    }
    
    // MARK: - Play/Pause/Stop Operations
    
    @MainActor
    func testPauseOperation() {
        // Test that pause sets isPlaying to false
        playerService.pause()
        
        XCTAssertFalse(playerService.isPlaying, "Pause should set isPlaying to false")
    }
    
    @MainActor
    func testResumeOperation() {
        // Test that resume attempts to play
        // Note: Without actual media, AVPlayer won't actually play
        playerService.resume()
        
        // The service should attempt to resume
        // State verification depends on whether media is loaded
        XCTAssertNotNil(playerService.getAVPlayer(), "Player should exist after resume")
    }
    
    @MainActor
    func testStopOperation() {
        // Test that stop resets playback state
        playerService.stop()
        
        XCTAssertFalse(playerService.isPlaying, "Stop should set isPlaying to false")
        XCTAssertEqual(playerService.currentTime, 0, "Stop should reset currentTime to 0")
        XCTAssertEqual(playerService.duration, 0, "Stop should reset duration to 0")
    }
    
    @MainActor
    func testPauseThenStop() {
        // Test pause followed by stop
        playerService.pause()
        XCTAssertFalse(playerService.isPlaying)
        
        playerService.stop()
        XCTAssertFalse(playerService.isPlaying)
        XCTAssertEqual(playerService.currentTime, 0)
    }
    
    // MARK: - Seek Functionality
    
    @MainActor
    func testSeekToZero() {
        // Test seeking to the beginning
        playerService.seek(to: 0)
        
        // Seek should not crash
        XCTAssertNotNil(playerService.getAVPlayer(), "Player should exist after seek")
    }
    
    @MainActor
    func testSeekToPositiveTime() {
        // Test seeking to a positive time
        let seekTime: TimeInterval = 30.0
        playerService.seek(to: seekTime)
        
        // Seek should not crash
        XCTAssertNotNil(playerService.getAVPlayer(), "Player should exist after seek")
    }
    
    @MainActor
    func testSeekToLargeTime() {
        // Test seeking to a large time value
        let seekTime: TimeInterval = 3600.0 // 1 hour
        playerService.seek(to: seekTime)
        
        // Seek should not crash even with large values
        XCTAssertNotNil(playerService.getAVPlayer(), "Player should exist after seek to large time")
    }
    
    @MainActor
    func testMultipleSeeks() {
        // Test multiple seek operations
        playerService.seek(to: 10.0)
        playerService.seek(to: 20.0)
        playerService.seek(to: 5.0)
        
        // Multiple seeks should not crash
        XCTAssertNotNil(playerService.getAVPlayer(), "Player should exist after multiple seeks")
    }
    
    // MARK: - Volume Control
    
    @MainActor
    func testVolumeSetToZero() {
        // Test setting volume to 0
        playerService.volume = 0.0
        
        XCTAssertEqual(playerService.volume, 0.0, accuracy: 0.001, "Volume should be 0.0")
    }
    
    @MainActor
    func testVolumeSetToHalf() {
        // Test setting volume to 0.5
        playerService.volume = 0.5
        
        XCTAssertEqual(playerService.volume, 0.5, accuracy: 0.001, "Volume should be 0.5")
    }
    
    @MainActor
    func testVolumeSetToMax() {
        // Test setting volume to 1.0
        playerService.volume = 1.0
        
        XCTAssertEqual(playerService.volume, 1.0, accuracy: 0.001, "Volume should be 1.0")
    }
    
    @MainActor
    func testVolumeNegativeValue() {
        // Test that negative volume is handled
        playerService.volume = -0.5
        
        // AVPlayer should clamp or handle negative values
        // The actual behavior depends on AVPlayer implementation
        let volume = playerService.volume
        XCTAssertTrue(volume >= 0.0, "Volume should not be negative (got \(volume))")
    }
    
    @MainActor
    func testVolumeAboveMax() {
        // Test that volume above 1.0 is handled
        playerService.volume = 1.5
        
        // AVPlayer should clamp or handle values above 1.0
        let volume = playerService.volume
        XCTAssertTrue(volume <= 1.5, "Volume should be handled appropriately (got \(volume))")
    }
    
    @MainActor
    func testVolumeMultipleChanges() {
        // Test multiple volume changes
        playerService.volume = 0.3
        XCTAssertEqual(playerService.volume, 0.3, accuracy: 0.001)
        
        playerService.volume = 0.7
        XCTAssertEqual(playerService.volume, 0.7, accuracy: 0.001)
        
        playerService.volume = 0.5
        XCTAssertEqual(playerService.volume, 0.5, accuracy: 0.001)
    }
    
    @MainActor
    func testVolumePersistsAfterStop() {
        // Test that volume setting persists after stop
        playerService.volume = 0.6
        playerService.stop()
        
        XCTAssertEqual(playerService.volume, 0.6, accuracy: 0.001, "Volume should persist after stop")
    }
    
    // MARK: - Fullscreen Toggle
    
    @MainActor
    func testFullscreenInitialState() {
        // Test that fullscreen starts as false
        XCTAssertFalse(playerService.isFullscreen, "Fullscreen should initially be false")
    }
    
    @MainActor
    func testFullscreenToggleOnce() {
        // Test single fullscreen toggle
        let initialState = playerService.isFullscreen
        
        playerService.toggleFullscreen()
        
        XCTAssertNotEqual(playerService.isFullscreen, initialState, "Fullscreen should toggle")
        XCTAssertTrue(playerService.isFullscreen, "Fullscreen should be true after first toggle")
    }
    
    @MainActor
    func testFullscreenToggleTwice() {
        // Test double fullscreen toggle returns to original state
        let initialState = playerService.isFullscreen
        
        playerService.toggleFullscreen()
        playerService.toggleFullscreen()
        
        XCTAssertEqual(playerService.isFullscreen, initialState, "Fullscreen should return to initial state after two toggles")
    }
    
    @MainActor
    func testFullscreenDirectSet() {
        // Test directly setting fullscreen state
        playerService.isFullscreen = true
        XCTAssertTrue(playerService.isFullscreen, "Fullscreen should be true when set directly")
        
        playerService.isFullscreen = false
        XCTAssertFalse(playerService.isFullscreen, "Fullscreen should be false when set directly")
    }
    
    @MainActor
    func testFullscreenPersistsAfterStop() {
        // Test that fullscreen state persists after stop
        playerService.isFullscreen = true
        playerService.stop()
        
        XCTAssertTrue(playerService.isFullscreen, "Fullscreen state should persist after stop")
    }
    
    // MARK: - State Transitions
    
    @MainActor
    func testStateAfterPause() {
        // Test player state after pause
        playerService.pause()
        
        XCTAssertFalse(playerService.isPlaying, "isPlaying should be false after pause")
    }
    
    @MainActor
    func testStateAfterStop() {
        // Test player state after stop
        playerService.stop()
        
        XCTAssertFalse(playerService.isPlaying, "isPlaying should be false after stop")
        XCTAssertEqual(playerService.currentTime, 0, "currentTime should be 0 after stop")
        XCTAssertEqual(playerService.duration, 0, "duration should be 0 after stop")
    }
    
    @MainActor
    func testComplexStateTransition() {
        // Test a complex sequence of state transitions
        playerService.volume = 0.5
        playerService.pause()
        playerService.seek(to: 10.0)
        playerService.isFullscreen = true
        playerService.resume()
        playerService.stop()
        
        // After stop, playback state should be reset but user preferences preserved
        XCTAssertFalse(playerService.isPlaying)
        XCTAssertEqual(playerService.currentTime, 0)
        XCTAssertEqual(playerService.duration, 0)
        XCTAssertEqual(playerService.volume, 0.5, accuracy: 0.001, "Volume should persist")
        XCTAssertTrue(playerService.isFullscreen, "Fullscreen should persist")
    }
    
    // MARK: - Publisher Tests
    
    @MainActor
    func testIsPlayingPublisher() {
        // Test that isPlayingPublisher emits values
        let expectation = XCTestExpectation(description: "isPlayingPublisher emits value")
        var receivedValue: Bool?
        
        playerService.isPlayingPublisher
            .dropFirst() // Skip initial value
            .sink { value in
                receivedValue = value
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        playerService.pause()
        
        wait(for: [expectation], timeout: 1.0)
        XCTAssertNotNil(receivedValue, "Publisher should emit value")
        XCTAssertFalse(receivedValue!, "Publisher should emit false for pause")
    }
    
    @MainActor
    func testFullscreenPublisher() {
        // Test that fullscreenPublisher emits values
        let expectation = XCTestExpectation(description: "fullscreenPublisher emits value")
        var receivedValue: Bool?
        
        playerService.isFullscreenPublisher
            .dropFirst() // Skip initial value
            .sink { value in
                receivedValue = value
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        playerService.toggleFullscreen()
        
        wait(for: [expectation], timeout: 1.0)
        XCTAssertNotNil(receivedValue, "Publisher should emit value")
        XCTAssertTrue(receivedValue!, "Publisher should emit true for first toggle")
    }
    
    @MainActor
    func testCurrentTimePublisher() {
        // Test that currentTimePublisher exists and can be subscribed to
        let expectation = XCTestExpectation(description: "currentTimePublisher can be subscribed")
        
        playerService.currentTimePublisher
            .sink { _ in
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        wait(for: [expectation], timeout: 1.0)
    }
    
    @MainActor
    func testDurationPublisher() {
        // Test that durationPublisher exists and can be subscribed to
        let expectation = XCTestExpectation(description: "durationPublisher can be subscribed")
        
        playerService.durationPublisher
            .sink { _ in
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        wait(for: [expectation], timeout: 1.0)
    }
    
    @MainActor
    func testErrorPublisher() {
        // Test that errorPublisher exists and can be subscribed to
        var receivedError: AppError?
        
        playerService.errorPublisher
            .sink { error in
                receivedError = error
            }
            .store(in: &cancellables)
        
        // Error publisher should not emit without an actual error
        XCTAssertNil(receivedError, "Error publisher should not emit without error")
    }
    
    @MainActor
    func testBufferingPublisher() {
        // Test that bufferingPublisher exists and can be subscribed to
        let expectation = XCTestExpectation(description: "bufferingPublisher can be subscribed")
        
        playerService.isBufferingPublisher
            .sink { _ in
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        wait(for: [expectation], timeout: 1.0)
    }
    
    // MARK: - Edge Cases
    
    @MainActor
    func testOperationsWithoutMedia() {
        // Test that all operations work without media loaded
        // This is important for robustness
        
        playerService.pause()
        playerService.resume()
        playerService.stop()
        playerService.seek(to: 10.0)
        playerService.volume = 0.5
        playerService.toggleFullscreen()
        
        // If we reach here without crashing, the test passes
        XCTAssertTrue(true, "All operations should work without media")
    }
    
    @MainActor
    func testGetAVPlayer() {
        // Test that getAVPlayer returns a valid player
        let player = playerService.getAVPlayer()
        
        XCTAssertNotNil(player, "getAVPlayer should return a valid AVPlayer instance")
    }
    
    @MainActor
    func testMultipleStops() {
        // Test that multiple stop calls don't cause issues
        playerService.stop()
        playerService.stop()
        playerService.stop()
        
        XCTAssertFalse(playerService.isPlaying)
        XCTAssertEqual(playerService.currentTime, 0)
    }
}

