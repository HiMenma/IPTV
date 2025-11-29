//
//  XtreamClientTests.swift
//  IPTVPlayerTests
//
//  Unit tests for Xtream API client
//

import XCTest
@testable import IPTVPlayer

class XtreamClientTests: XCTestCase {
    var client: XtreamClientImpl!
    var mockSession: MockURLSession!
    
    override func setUp() {
        super.setUp()
        mockSession = MockURLSession()
        client = XtreamClientImpl(session: mockSession, maxRetries: 2, initialRetryDelay: 0.1)
    }
    
    override func tearDown() {
        client = nil
        mockSession = nil
        super.tearDown()
    }
    
    // MARK: - Authentication Tests
    
    func testAuthenticateWithValidCredentials() async throws {
        // Given
        let account = XtreamAccount(
            serverUrl: "http://example.com",
            username: "testuser",
            password: "testpass"
        )
        
        let authResponse = """
        {
            "user_info": {
                "username": "testuser",
                "password": "testpass",
                "auth": 1,
                "status": "Active",
                "exp_date": "1735689600",
                "is_trial": "0",
                "active_cons": "1",
                "created_at": "1704153600",
                "max_connections": "1"
            },
            "server_info": {
                "url": "example.com",
                "port": "8080",
                "https_port": "8443",
                "server_protocol": "http",
                "rtmp_port": "1935",
                "timezone": "UTC",
                "timestamp_now": 1704240000,
                "time_now": "2024-01-03 00:00:00"
            }
        }
        """
        
        mockSession.data = authResponse.data(using: .utf8)
        mockSession.response = HTTPURLResponse(
            url: URL(string: "http://example.com")!,
            statusCode: 200,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When
        let result = try await client.authenticate(account: account)
        
        // Then
        XCTAssertTrue(result)
    }
    
    func testAuthenticateWithInvalidCredentials() async throws {
        // Given
        let account = XtreamAccount(
            serverUrl: "http://example.com",
            username: "wronguser",
            password: "wrongpass"
        )
        
        let authResponse = """
        {
            "user_info": {
                "username": "",
                "password": "",
                "message": "Invalid credentials",
                "auth": 0,
                "status": "Disabled"
            }
        }
        """
        
        mockSession.data = authResponse.data(using: .utf8)
        mockSession.response = HTTPURLResponse(
            url: URL(string: "http://example.com")!,
            statusCode: 200,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When/Then
        do {
            _ = try await client.authenticate(account: account)
            XCTFail("Expected authentication to fail")
        } catch let error as XtreamClientError {
            if case .authenticationFailed(let message) = error {
                XCTAssertEqual(message, "Invalid credentials")
            } else {
                XCTFail("Expected authenticationFailed error, got \(error)")
            }
        }
    }
    
    // MARK: - Live Categories Tests
    
    func testGetLiveCategories() async throws {
        // Given
        let account = XtreamAccount(
            serverUrl: "http://example.com",
            username: "testuser",
            password: "testpass"
        )
        
        let categoriesResponse = """
        [
            {
                "category_id": "1",
                "category_name": "Sports",
                "parent_id": 0
            },
            {
                "category_id": "2",
                "category_name": "News",
                "parent_id": 0
            }
        ]
        """
        
        mockSession.data = categoriesResponse.data(using: .utf8)
        mockSession.response = HTTPURLResponse(
            url: URL(string: "http://example.com")!,
            statusCode: 200,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When
        let categories = try await client.getLiveCategories(account: account)
        
        // Then
        XCTAssertEqual(categories.count, 2)
        XCTAssertEqual(categories[0].id, "1")
        XCTAssertEqual(categories[0].name, "Sports")
        XCTAssertEqual(categories[1].id, "2")
        XCTAssertEqual(categories[1].name, "News")
    }
    
    // MARK: - Live Streams Tests
    
    func testGetLiveStreams() async throws {
        // Given
        let account = XtreamAccount(
            serverUrl: "http://example.com",
            username: "testuser",
            password: "testpass"
        )
        
        let streamsResponse = """
        [
            {
                "num": 1,
                "name": "ESPN",
                "stream_type": "live",
                "stream_id": 100,
                "stream_icon": "http://example.com/espn.png",
                "epg_channel_id": "espn",
                "category_id": "1"
            },
            {
                "num": 2,
                "name": "CNN",
                "stream_type": "live",
                "stream_id": 101,
                "stream_icon": "http://example.com/cnn.png",
                "epg_channel_id": "cnn",
                "category_id": "2"
            }
        ]
        """
        
        mockSession.data = streamsResponse.data(using: .utf8)
        mockSession.response = HTTPURLResponse(
            url: URL(string: "http://example.com")!,
            statusCode: 200,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When
        let channels = try await client.getLiveStreams(account: account)
        
        // Then
        XCTAssertEqual(channels.count, 2)
        XCTAssertEqual(channels[0].name, "ESPN")
        XCTAssertEqual(channels[0].url, "http://example.com/live/testuser/testpass/100.m3u8")
        XCTAssertEqual(channels[0].logoUrl, "http://example.com/espn.png")
        XCTAssertEqual(channels[1].name, "CNN")
    }
    
    // MARK: - Error Handling Tests
    
    func testNetworkErrorWithRetry() async throws {
        // Given
        let account = XtreamAccount(
            serverUrl: "http://example.com",
            username: "testuser",
            password: "testpass"
        )
        
        // Simulate network error
        mockSession.error = URLError(.notConnectedToInternet)
        
        // When/Then
        do {
            _ = try await client.getLiveCategories(account: account)
            XCTFail("Expected network error")
        } catch let error as XtreamClientError {
            if case .networkError = error {
                // Success - expected error
            } else {
                XCTFail("Expected networkError, got \(error)")
            }
        }
    }
    
    func testServerError() async throws {
        // Given
        let account = XtreamAccount(
            serverUrl: "http://example.com",
            username: "testuser",
            password: "testpass"
        )
        
        mockSession.data = Data()
        mockSession.response = HTTPURLResponse(
            url: URL(string: "http://example.com")!,
            statusCode: 500,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When/Then
        do {
            _ = try await client.getLiveCategories(account: account)
            XCTFail("Expected server error")
        } catch let error as XtreamClientError {
            if case .serverError(let statusCode) = error {
                XCTAssertEqual(statusCode, 500)
            } else {
                XCTFail("Expected serverError, got \(error)")
            }
        }
    }
    
    func testInvalidURL() async throws {
        // Given
        let account = XtreamAccount(
            serverUrl: "not a valid url",
            username: "testuser",
            password: "testpass"
        )
        
        // When/Then
        do {
            _ = try await client.authenticate(account: account)
            XCTFail("Expected invalid URL error")
        } catch let error as XtreamClientError {
            if case .invalidURL = error {
                // Success - expected error
            } else {
                XCTFail("Expected invalidURL, got \(error)")
            }
        }
    }
    
    func testRetryMechanism() async throws {
        // Given
        let account = XtreamAccount(
            serverUrl: "http://example.com",
            username: "testuser",
            password: "testpass"
        )
        
        let categoriesResponse = """
        [
            {
                "category_id": "1",
                "category_name": "Sports",
                "parent_id": 0
            }
        ]
        """
        
        // First attempt fails, second succeeds
        mockSession.shouldFailFirstAttempt = true
        mockSession.data = categoriesResponse.data(using: .utf8)
        mockSession.response = HTTPURLResponse(
            url: URL(string: "http://example.com")!,
            statusCode: 200,
            httpVersion: nil,
            headerFields: nil
        )
        
        // When
        let categories = try await client.getLiveCategories(account: account)
        
        // Then
        XCTAssertEqual(categories.count, 1)
        XCTAssertTrue(mockSession.attemptCount > 1, "Should have retried")
    }
}

// MARK: - Mock URLSession

class MockURLSession: URLSession {
    var data: Data?
    var response: URLResponse?
    var error: Error?
    var shouldFailFirstAttempt = false
    var attemptCount = 0
    
    override func data(from url: URL) async throws -> (Data, URLResponse) {
        attemptCount += 1
        
        if shouldFailFirstAttempt && attemptCount == 1 {
            throw URLError(.networkConnectionLost)
        }
        
        if let error = error {
            throw error
        }
        
        guard let data = data, let response = response else {
            throw URLError(.badServerResponse)
        }
        
        return (data, response)
    }
}
