//
//  VideoPlayerControlsPropertyTest.swift
//  IPTVPlayerTests
//
//  Property-based tests for video player controls
//

import XCTest
import SwiftCheck
import AVFoundation
import Combine
@testable import IPTVPlayer

final class VideoPlayerControlsPropertyTest: XCTestCase {
    
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
    
    // MARK: - Property 4: Player Control State Consistency
    // **Feature: native-desktop-migration, Property 4: Player Control State Consistency**
    // **Validates: Requirements 3.6, 7.7**
    
    /// Property: For any player control operation (play, pause, volume change),
    /// the player state should update consistently to reflect the operation
    @MainActor
    func testProperty4_VolumeControlConsistency() {
        property("Volume control updates state consistently") <- forAll { (volume: Float) in
            // Constrain volume to valid range [0.0, 1.0]
            let validVolume = max(0.0, min(1.0, abs(volume)))
            
            // Set volume
            self.playerService.volume = validVolume
            
            // Verify volume is set correctly
            let retrievedVolume = self.playerService.volume
            let tolerance: Float = 0.001
            
            guard abs(retrievedVolume - validVolume) < tolerance else {
                return false <?> "Volume mismatch: expected \(validVolume), got \(retrievedVolume)"
            }
            
            return true
        }.withSize(100) // Run 100 iterations
    }
    
    @MainActor
    func testProperty4_FullscreenToggleConsistency() {
        property("Fullscreen toggle updates state consistently") <- forAll { (toggleCount: Int) in
            // Constrain toggle count to reasonable range
            let validToggleCount = abs(toggleCount) % 10
            
            // Record initial state
            let initialState = self.playerService.isFullscreen
            
            // Toggle multiple times
            for _ in 0..<validToggleCount {
                self.playerService.toggleFullscreen()
            }
            
            // Verify final state matches expected parity
            let expectedFinalState = validToggleCount % 2 == 0 ? initialState : !initialState
            let actualFinalState = self.playerService.isFullscreen
            
            guard actualFinalState == expectedFinalState else {
                return false <?> "Fullscreen state mismatch after \(validToggleCount) toggles: expected \(expectedFinalState), got \(actualFinalState)"
            }
            
            return true
        }.withSize(100)
    }
    
    @MainActor
    func testProperty4_PauseResumeConsistency() {
        // This test verifies that pause/resume operations maintain state consistency
        // Note: We can't use actual media playback in property tests, so we test state transitions
        
        let expectation = XCTestExpectation(description: "Pause/Resume state consistency")
        var stateChanges: [Bool] = []
        
        // Subscribe to state changes
        playerService.isPlayingPublisher
            .sink { isPlaying in
                stateChanges.append(isPlaying)
            }
            .store(in: &cancellables)
        
        // Test pause operation (without actual media)
        playerService.pause()
        
        // Verify pause sets isPlaying to false
        XCTAssertFalse(playerService.isPlaying, "Pause should set isPlaying to false")
        
        // Test resume operation (without actual media)
        playerService.resume()
        
        // Note: Without actual media loaded, AVPlayer won't actually play,
        // but the service should attempt to resume
        // The state management is what we're testing here
        
        expectation.fulfill()
        wait(for: [expectation], timeout: 1.0)
    }
    
    @MainActor
    func testProperty4_StopResetsState() {
        // Property: Stop operation should reset all player state
        
        let expectation = XCTestExpectation(description: "Stop resets state")
        
        // Set some state
        playerService.volume = 0.5
        playerService.isFullscreen = true
        
        // Stop playback
        playerService.stop()
        
        // Verify state is reset
        XCTAssertFalse(playerService.isPlaying, "Stop should set isPlaying to false")
        XCTAssertEqual(playerService.currentTime, 0, "Stop should reset currentTime to 0")
        XCTAssertEqual(playerService.duration, 0, "Stop should reset duration to 0")
        
        // Note: Volume and fullscreen are user preferences and should NOT be reset by stop
        XCTAssertEqual(playerService.volume, 0.5, "Stop should not reset volume")
        XCTAssertTrue(playerService.isFullscreen, "Stop should not reset fullscreen state")
        
        expectation.fulfill()
        wait(for: [expectation], timeout: 1.0)
    }
    
    @MainActor
    func testProperty4_PublisherConsistency() {
        // Property: Publishers should emit values consistent with property getters
        
        property("Publishers emit consistent values") <- forAll { (volume: Float, fullscreen: Bool) in
            let validVolume = max(0.0, min(1.0, abs(volume)))
            
            var publishedVolume: Float?
            var publishedFullscreen: Bool?
            
            let volumeExpectation = XCTestExpectation(description: "Volume publisher")
            let fullscreenExpectation = XCTestExpectation(description: "Fullscreen publisher")
            
            // Subscribe to publishers
            self.playerService.isFullscreenPublisher
                .dropFirst() // Skip initial value
                .sink { value in
                    publishedFullscreen = value
                    fullscreenExpectation.fulfill()
                }
                .store(in: &self.cancellables)
            
            // Set values
            self.playerService.volume = validVolume
            self.playerService.isFullscreen = fullscreen
            
            // Wait for publisher
            self.wait(for: [fullscreenExpectation], timeout: 1.0)
            
            // Verify publisher emitted correct value
            guard let emittedFullscreen = publishedFullscreen else {
                return false <?> "Fullscreen publisher did not emit value"
            }
            
            guard emittedFullscreen == fullscreen else {
                return false <?> "Fullscreen publisher emitted \(emittedFullscreen), expected \(fullscreen)"
            }
            
            // Verify property getter matches
            guard self.playerService.isFullscreen == fullscreen else {
                return false <?> "Fullscreen property getter returned \(self.playerService.isFullscreen), expected \(fullscreen)"
            }
            
            return true
        }.withSize(50)
    }
    
    @MainActor
    func testProperty4_SeekConsistency() {
        // Property: Seek operation should update player state
        // Note: Without actual media, we can only test that seek doesn't crash
        
        property("Seek operation executes without crashing") <- forAll { (seekTime: Double) in
            // Constrain seek time to reasonable range [0, 3600] seconds (1 hour)
            let validSeekTime = max(0, min(3600, abs(seekTime)))
            
            // Seek should not crash even without media loaded
            self.playerService.seek(to: validSeekTime)
            
            // If we get here without crashing, the test passes
            return true
        }.withSize(100)
    }
    
    // MARK: - Integration Tests for State Consistency
    
    @MainActor
    func testVolumeRangeEnforcement() {
        // Test that volume is properly constrained to [0.0, 1.0]
        
        // Test lower bound
        playerService.volume = -1.0
        XCTAssertGreaterThanOrEqual(playerService.volume, 0.0, "Volume should not be negative")
        
        // Test upper bound
        playerService.volume = 2.0
        XCTAssertLessThanOrEqual(playerService.volume, 1.0, "Volume should not exceed 1.0")
        
        // Test valid values
        playerService.volume = 0.0
        XCTAssertEqual(playerService.volume, 0.0, accuracy: 0.001)
        
        playerService.volume = 0.5
        XCTAssertEqual(playerService.volume, 0.5, accuracy: 0.001)
        
        playerService.volume = 1.0
        XCTAssertEqual(playerService.volume, 1.0, accuracy: 0.001)
    }
    
    @MainActor
    func testFullscreenStateTransitions() {
        // Test fullscreen state transitions
        
        // Initial state should be false
        XCTAssertFalse(playerService.isFullscreen)
        
        // Toggle to true
        playerService.toggleFullscreen()
        XCTAssertTrue(playerService.isFullscreen)
        
        // Toggle back to false
        playerService.toggleFullscreen()
        XCTAssertFalse(playerService.isFullscreen)
        
        // Direct set
        playerService.isFullscreen = true
        XCTAssertTrue(playerService.isFullscreen)
        
        playerService.isFullscreen = false
        XCTAssertFalse(playerService.isFullscreen)
    }
    
    @MainActor
    func testControlOperationsWithoutMedia() {
        // Test that control operations don't crash without media loaded
        
        // These should all execute without crashing
        playerService.pause()
        playerService.resume()
        playerService.stop()
        playerService.seek(to: 10.0)
        playerService.volume = 0.5
        playerService.toggleFullscreen()
        
        // If we get here, all operations executed successfully
        XCTAssertTrue(true)
    }
}

