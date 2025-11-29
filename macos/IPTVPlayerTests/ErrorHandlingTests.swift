//
//  ErrorHandlingTests.swift
//  IPTVPlayerTests
//
//  Tests for error handling infrastructure
//

import XCTest
@testable import IPTVPlayer

class ErrorHandlingTests: XCTestCase {
    
    // MARK: - AppError Tests
    
    func testAppErrorNetworkCategory() {
        let error = AppError.networkError(underlying: NSError(domain: "test", code: -1))
        XCTAssertEqual(error.category, .network)
        XCTAssertNotNil(error.errorDescription)
        XCTAssertNotNil(error.recoverySuggestion)
    }
    
    func testAppErrorParsingCategory() {
        let error = AppError.parsingError(message: "Test parsing error")
        XCTAssertEqual(error.category, .parsing)
        XCTAssertTrue(error.errorDescription?.contains("解析错误") == true)
    }
    
    func testAppErrorDatabaseCategory() {
        let error = AppError.databaseError(underlying: NSError(domain: "test", code: -1))
        XCTAssertEqual(error.category, .database)
        XCTAssertNotNil(error.recoverySuggestion)
    }
    
    func testAppErrorPlayerCategory() {
        let error = AppError.playerError(message: "Test player error")
        XCTAssertEqual(error.category, .player)
        XCTAssertTrue(error.errorDescription?.contains("播放器错误") == true)
    }
    
    // MARK: - NetworkError Tests
    
    func testNetworkErrorDescriptions() {
        let timeout = NetworkError.connectionTimeout
        XCTAssertNotNil(timeout.errorDescription)
        XCTAssertNotNil(timeout.recoverySuggestion)
        
        let noInternet = NetworkError.noInternetConnection
        XCTAssertNotNil(noInternet.errorDescription)
        
        let serverError = NetworkError.serverError(statusCode: 500)
        XCTAssertTrue(serverError.errorDescription?.contains("500") == true)
    }
    
    // MARK: - ParsingError Tests
    
    func testParsingErrorDescriptions() {
        let invalidM3U = ParsingError.invalidM3UFormat(details: "Missing header")
        XCTAssertTrue(invalidM3U.errorDescription?.contains("M3U") == true)
        XCTAssertNotNil(invalidM3U.recoverySuggestion)
        
        let emptyContent = ParsingError.emptyContent
        XCTAssertNotNil(emptyContent.errorDescription)
    }
    
    // MARK: - DatabaseError Tests
    
    func testDatabaseErrorDescriptions() {
        let connectionFailed = DatabaseError.connectionFailed
        XCTAssertNotNil(connectionFailed.errorDescription)
        XCTAssertNotNil(connectionFailed.recoverySuggestion)
        
        let diskFull = DatabaseError.diskFull
        XCTAssertTrue(diskFull.errorDescription?.contains("磁盘") == true)
    }
    
    // MARK: - PlayerError Tests
    
    func testPlayerErrorDescriptions() {
        let streamNotFound = PlayerError.streamNotFound
        XCTAssertNotNil(streamNotFound.errorDescription)
        XCTAssertNotNil(streamNotFound.recoverySuggestion)
        
        let unsupportedFormat = PlayerError.unsupportedFormat(format: "xyz")
        XCTAssertTrue(unsupportedFormat.errorDescription?.contains("xyz") == true)
    }
    
    // MARK: - Logger Tests
    
    func testLoggerLevels() {
        XCTAssertTrue(LogLevel.debug < LogLevel.info)
        XCTAssertTrue(LogLevel.info < LogLevel.warning)
        XCTAssertTrue(LogLevel.warning < LogLevel.error)
        XCTAssertTrue(LogLevel.error < LogLevel.critical)
    }
    
    func testLoggerCreation() {
        let logger = OSLogger(subsystem: "test", minimumLevel: .warning)
        XCTAssertNotNil(logger)
        
        // Test that logger doesn't crash when logging
        logger.debug("Debug message")
        logger.info("Info message")
        logger.warning("Warning message")
        logger.error("Error message", error: NSError(domain: "test", code: -1))
    }
    
    // MARK: - ErrorPresenter Tests
    
    func testErrorPresenterWithAppError() {
        let presenter = DefaultErrorPresenter()
        let error = AppError.networkError(underlying: NSError(domain: "test", code: -1))
        let presentation = presenter.present(error: error)
        
        XCTAssertNotNil(presentation.title)
        XCTAssertNotNil(presentation.message)
        XCTAssertEqual(presentation.category, .network)
    }
    
    func testErrorPresenterWithNetworkError() {
        let presenter = DefaultErrorPresenter()
        let error = NetworkError.connectionTimeout
        let presentation = presenter.present(error: error)
        
        XCTAssertEqual(presentation.title, "网络错误")
        XCTAssertNotNil(presentation.message)
        XCTAssertEqual(presentation.severity, .warning)
    }
    
    func testErrorPresenterWithParsingError() {
        let presenter = DefaultErrorPresenter()
        let error = ParsingError.invalidM3UFormat(details: "Test")
        let presentation = presenter.present(error: error)
        
        XCTAssertEqual(presentation.title, "解析错误")
        XCTAssertEqual(presentation.category, .parsing)
    }
    
    func testErrorPresenterWithDatabaseError() {
        let presenter = DefaultErrorPresenter()
        let error = DatabaseError.dataCorruption
        let presentation = presenter.present(error: error)
        
        XCTAssertEqual(presentation.title, "数据库错误")
        XCTAssertEqual(presentation.severity, .critical)
    }
    
    func testErrorPresenterWithPlayerError() {
        let presenter = DefaultErrorPresenter()
        let error = PlayerError.streamNotFound
        let presentation = presenter.present(error: error)
        
        XCTAssertEqual(presentation.title, "播放器错误")
        XCTAssertEqual(presentation.category, .player)
    }
    
    // MARK: - RetryConfiguration Tests
    
    func testRetryConfigurationDefault() {
        let config = RetryConfiguration.default
        XCTAssertEqual(config.maxAttempts, 3)
        XCTAssertEqual(config.initialDelay, 1.0)
        XCTAssertEqual(config.backoffMultiplier, 2.0)
    }
    
    func testRetryConfigurationNetwork() {
        let config = RetryConfiguration.network
        XCTAssertEqual(config.maxAttempts, 3)
        
        // Test shouldRetry logic
        let networkError = NetworkError.connectionTimeout
        XCTAssertTrue(config.shouldRetry(networkError))
        
        let invalidURL = NetworkError.invalidURL
        XCTAssertFalse(config.shouldRetry(invalidURL))
    }
    
    func testRetryConfigurationDatabase() {
        let config = RetryConfiguration.database
        XCTAssertEqual(config.maxAttempts, 2)
        
        // Test shouldRetry logic
        let connectionFailed = DatabaseError.connectionFailed
        XCTAssertTrue(config.shouldRetry(connectionFailed))
        
        let diskFull = DatabaseError.diskFull
        XCTAssertFalse(config.shouldRetry(diskFull))
    }
    
    // MARK: - RetryMechanism Tests
    
    func testRetryMechanismSuccess() async throws {
        var attemptCount = 0
        
        let result = try await RetryMechanism.execute(
            configuration: RetryConfiguration.default
        ) {
            attemptCount += 1
            return "Success"
        }
        
        XCTAssertEqual(result, "Success")
        XCTAssertEqual(attemptCount, 1)
    }
    
    func testRetryMechanismRetryAndSuccess() async throws {
        var attemptCount = 0
        
        let result = try await RetryMechanism.execute(
            configuration: RetryConfiguration.default
        ) {
            attemptCount += 1
            if attemptCount < 2 {
                throw NetworkError.connectionTimeout
            }
            return "Success"
        }
        
        XCTAssertEqual(result, "Success")
        XCTAssertEqual(attemptCount, 2)
    }
    
    func testRetryMechanismFailureAfterMaxAttempts() async {
        var attemptCount = 0
        
        do {
            _ = try await RetryMechanism.execute(
                configuration: RetryConfiguration(
                    maxAttempts: 2,
                    initialDelay: 0.1,
                    maxDelay: 1.0,
                    backoffMultiplier: 2.0,
                    shouldRetry: { _ in true }
                )
            ) {
                attemptCount += 1
                throw NetworkError.connectionTimeout
            }
            XCTFail("Should have thrown an error")
        } catch {
            XCTAssertEqual(attemptCount, 2)
            XCTAssertTrue(error is NetworkError)
        }
    }
    
    func testRetryMechanismNonRetryableError() async {
        var attemptCount = 0
        
        do {
            _ = try await RetryMechanism.execute(
                configuration: RetryConfiguration.default
            ) {
                attemptCount += 1
                throw NetworkError.invalidURL
            }
            XCTFail("Should have thrown an error")
        } catch {
            XCTAssertEqual(attemptCount, 1) // Should not retry
            XCTAssertTrue(error is NetworkError)
        }
    }
}
