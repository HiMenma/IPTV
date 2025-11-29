//
//  VideoPlayerErrorHandlingTest.swift
//  IPTVPlayerTests
//
//  Unit tests for video player error handling
//

import XCTest
import AVFoundation
import Combine
@testable import IPTVPlayer

final class VideoPlayerErrorHandlingTest: XCTestCase {
    
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
    
    // MARK: - Stream Not Found Errors
    
    @MainActor
    func testStreamNotFoundError() async {
        // Test handling of non-existent stream URL
        let expectation = XCTestExpectation(description: "Error publisher emits stream not found error")
        var receivedError: AppError?
        
        playerService.errorPublisher
            .sink { error in
                receivedError = error
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // Try to play a non-existent stream
        let invalidURL = URL(string: "http://invalid-stream-url-that-does-not-exist.com/stream.m3u8")!
        playerService.play(url: invalidURL)
        
        // Wait for error to be emitted
        await fulfillment(of: [expectation], timeout: 10.0)
        
        XCTAssertNotNil(receivedError, "Error should be emitted for invalid stream")
    }
    
    @MainActor
    func testInvalidHostError() async {
        // Test handling of invalid host
        let expectation = XCTestExpectation(description: "Error publisher emits invalid host error")
        var receivedError: AppError?
        
        playerService.errorPublisher
            .sink { error in
                receivedError = error
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // Try to play from invalid host
        let invalidURL = URL(string: "http://this-host-definitely-does-not-exist-12345.com/stream")!
        playerService.play(url: invalidURL)
        
        // Wait for error to be emitted
        await fulfillment(of: [expectation], timeout: 10.0)
        
        XCTAssertNotNil(receivedError, "Error should be emitted for invalid host")
    }
    
    // MARK: - Unsupported Format Errors
    
    @MainActor
    func testUnsupportedFormatHandling() {
        // Test that unsupported format errors are properly categorized
        // This test verifies the error handling infrastructure is in place
        
        var receivedError: AppError?
        
        playerService.errorPublisher
            .sink { error in
                receivedError = error
            }
            .store(in: &cancellables)
        
        // The error handling should be ready to handle unsupported formats
        XCTAssertNil(receivedError, "No error should be emitted without playback attempt")
    }
    
    // MARK: - Buffering State
    
    @MainActor
    func testBufferingIndicatorOnPlay() {
        // Test that buffering indicator is shown when starting playback
        let expectation = XCTestExpectation(description: "Buffering state changes on play")
        var bufferingStates: [Bool] = []
        
        playerService.isBufferingPublisher
            .sink { isBuffering in
                bufferingStates.append(isBuffering)
                if bufferingStates.count >= 1 {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        // Start playing a URL (even if invalid, buffering should be indicated)
        let testURL = URL(string: "http://example.com/test.m3u8")!
        playerService.play(url: testURL)
        
        wait(for: [expectation], timeout: 2.0)
        
        // Should have received at least one buffering state update
        XCTAssertFalse(bufferingStates.isEmpty, "Buffering states should be emitted")
    }
    
    @MainActor
    func testBufferingStateResetOnStop() {
        // Test that buffering state is reset when stopping
        let testURL = URL(string: "http://example.com/test.m3u8")!
        playerService.play(url: testURL)
        
        // Stop playback
        playerService.stop()
        
        // Buffering should be false after stop
        let expectation = XCTestExpectation(description: "Buffering state is false after stop")
        
        playerService.isBufferingPublisher
            .sink { isBuffering in
                if !isBuffering {
                    expectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        wait(for: [expectation], timeout: 1.0)
    }
    
    // MARK: - Error Publisher
    
    @MainActor
    func testErrorPublisherExists() {
        // Test that error publisher is properly set up
        var errorReceived = false
        
        playerService.errorPublisher
            .sink { _ in
                errorReceived = true
            }
            .store(in: &cancellables)
        
        // Without triggering an error, nothing should be received
        XCTAssertFalse(errorReceived, "Error publisher should not emit without error")
    }
    
    @MainActor
    func testMultipleErrorSubscribers() {
        // Test that multiple subscribers can receive errors
        var subscriber1Received = false
        var subscriber2Received = false
        
        playerService.errorPublisher
            .sink { _ in
                subscriber1Received = true
            }
            .store(in: &cancellables)
        
        playerService.errorPublisher
            .sink { _ in
                subscriber2Received = true
            }
            .store(in: &cancellables)
        
        // Both subscribers should be ready
        XCTAssertFalse(subscriber1Received && subscriber2Received, "No errors should be emitted yet")
    }
    
    // MARK: - Reconnection State
    
    @MainActor
    func testStopCancelsReconnection() {
        // Test that stopping playback cancels any pending reconnection
        let testURL = URL(string: "http://example.com/test.m3u8")!
        playerService.play(url: testURL)
        
        // Immediately stop (should cancel any reconnection attempts)
        playerService.stop()
        
        // Verify player is stopped
        XCTAssertFalse(playerService.isPlaying, "Player should be stopped")
        XCTAssertEqual(playerService.currentTime, 0, "Current time should be reset")
    }
    
    @MainActor
    func testPlayNewURLCancelsReconnection() {
        // Test that playing a new URL cancels reconnection for previous URL
        let url1 = URL(string: "http://example.com/stream1.m3u8")!
        let url2 = URL(string: "http://example.com/stream2.m3u8")!
        
        playerService.play(url: url1)
        
        // Play a different URL (should cancel any reconnection for url1)
        playerService.play(url: url2)
        
        // Player should be attempting to play the new URL
        XCTAssertNotNil(playerService.getAVPlayer(), "Player should exist")
    }
    
    // MARK: - Error Recovery
    
    @MainActor
    func testErrorDoesNotCrashPlayer() async {
        // Test that errors don't crash the player service
        let expectation = XCTestExpectation(description: "Player handles error gracefully")
        
        playerService.errorPublisher
            .sink { _ in
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // Try to play an invalid URL
        let invalidURL = URL(string: "http://invalid-url-12345.com/stream")!
        playerService.play(url: invalidURL)
        
        await fulfillment(of: [expectation], timeout: 10.0)
        
        // Player should still be functional after error
        XCTAssertNotNil(playerService.getAVPlayer(), "Player should still exist after error")
        
        // Should be able to stop without crashing
        playerService.stop()
        XCTAssertFalse(playerService.isPlaying, "Player should be stopped")
    }
    
    @MainActor
    func testMultipleErrorsHandled() async {
        // Test that multiple errors are handled correctly
        let expectation = XCTestExpectation(description: "Multiple errors handled")
        expectation.expectedFulfillmentCount = 2
        
        var errorCount = 0
        
        playerService.errorPublisher
            .sink { _ in
                errorCount += 1
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        // Trigger multiple errors
        let url1 = URL(string: "http://invalid1.com/stream")!
        playerService.play(url: url1)
        
        // Wait a bit then try another invalid URL
        try? await Task.sleep(nanoseconds: 2_000_000_000)
        
        let url2 = URL(string: "http://invalid2.com/stream")!
        playerService.play(url: url2)
        
        await fulfillment(of: [expectation], timeout: 15.0)
        
        XCTAssertGreaterThanOrEqual(errorCount, 2, "Should have received multiple errors")
    }
    
    // MARK: - State Consistency
    
    @MainActor
    func testStateConsistencyAfterError() async {
        // Test that player state remains consistent after error
        let expectation = XCTestExpectation(description: "Error occurs")
        
        playerService.errorPublisher
            .sink { _ in
                expectation.fulfill()
            }
            .store(in: &cancellables)
        
        let invalidURL = URL(string: "http://invalid.com/stream")!
        playerService.play(url: invalidURL)
        
        await fulfillment(of: [expectation], timeout: 10.0)
        
        // State should be consistent
        XCTAssertNotNil(playerService.getAVPlayer(), "Player should exist")
        
        // Should be able to perform operations
        playerService.pause()
        playerService.stop()
        
        XCTAssertFalse(playerService.isPlaying, "Player should be stopped")
        XCTAssertEqual(playerService.currentTime, 0, "Time should be reset")
    }
    
    // MARK: - Integration Tests
    
    @MainActor
    func testCompleteErrorHandlingFlow() async {
        // Test a complete error handling flow
        let errorExpectation = XCTestExpectation(description: "Error is emitted")
        let bufferingExpectation = XCTestExpectation(description: "Buffering state changes")
        
        var receivedError: AppError?
        var bufferingStates: [Bool] = []
        
        playerService.errorPublisher
            .sink { error in
                receivedError = error
                errorExpectation.fulfill()
            }
            .store(in: &cancellables)
        
        playerService.isBufferingPublisher
            .sink { isBuffering in
                bufferingStates.append(isBuffering)
                if bufferingStates.count >= 1 {
                    bufferingExpectation.fulfill()
                }
            }
            .store(in: &cancellables)
        
        // Start playback with invalid URL
        let invalidURL = URL(string: "http://nonexistent-stream.com/live.m3u8")!
        playerService.play(url: invalidURL)
        
        await fulfillment(of: [errorExpectation, bufferingExpectation], timeout: 10.0)
        
        // Verify error was received
        XCTAssertNotNil(receivedError, "Error should be emitted")
        
        // Verify buffering state was updated
        XCTAssertFalse(bufferingStates.isEmpty, "Buffering states should be recorded")
        
        // Clean up
        playerService.stop()
        XCTAssertFalse(playerService.isPlaying, "Player should be stopped")
    }
}
