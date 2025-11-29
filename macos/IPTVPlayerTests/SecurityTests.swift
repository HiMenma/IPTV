//
//  SecurityTests.swift
//  IPTVPlayerTests
//
//  Unit tests for security features (Keychain, input validation, HTTPS enforcement)
//

import XCTest
@testable import IPTVPlayer

class SecurityTests: XCTestCase {
    
    // MARK: - Keychain Manager Tests
    
    func testKeychainSaveAndLoad() throws {
        let keychainManager = KeychainManagerImpl()
        
        // Create test account
        let account = XtreamAccount(
            serverUrl: "https://test.example.com",
            username: "testuser",
            password: "testpass123"
        )
        
        // Save to Keychain
        try keychainManager.saveXtreamAccount(account)
        
        // Load from Keychain
        let loadedAccount = try keychainManager.loadXtreamAccount(serverUrl: account.serverUrl)
        
        // Verify
        XCTAssertNotNil(loadedAccount)
        XCTAssertEqual(loadedAccount?.serverUrl, account.serverUrl)
        XCTAssertEqual(loadedAccount?.username, account.username)
        XCTAssertEqual(loadedAccount?.password, account.password)
        
        // Cleanup
        try keychainManager.deleteXtreamAccount(serverUrl: account.serverUrl)
    }
    
    func testKeychainUpdate() throws {
        let keychainManager = KeychainManagerImpl()
        
        // Create test account
        let account1 = XtreamAccount(
            serverUrl: "https://test.example.com",
            username: "testuser",
            password: "testpass123"
        )
        
        // Save to Keychain
        try keychainManager.saveXtreamAccount(account1)
        
        // Update with new credentials
        let account2 = XtreamAccount(
            serverUrl: "https://test.example.com",
            username: "newuser",
            password: "newpass456"
        )
        
        try keychainManager.saveXtreamAccount(account2)
        
        // Load from Keychain
        let loadedAccount = try keychainManager.loadXtreamAccount(serverUrl: account2.serverUrl)
        
        // Verify updated values
        XCTAssertNotNil(loadedAccount)
        XCTAssertEqual(loadedAccount?.username, "newuser")
        XCTAssertEqual(loadedAccount?.password, "newpass456")
        
        // Cleanup
        try keychainManager.deleteXtreamAccount(serverUrl: account2.serverUrl)
    }
    
    func testKeychainDelete() throws {
        let keychainManager = KeychainManagerImpl()
        
        // Create test account
        let account = XtreamAccount(
            serverUrl: "https://test.example.com",
            username: "testuser",
            password: "testpass123"
        )
        
        // Save to Keychain
        try keychainManager.saveXtreamAccount(account)
        
        // Delete from Keychain
        try keychainManager.deleteXtreamAccount(serverUrl: account.serverUrl)
        
        // Try to load (should return nil)
        let loadedAccount = try keychainManager.loadXtreamAccount(serverUrl: account.serverUrl)
        XCTAssertNil(loadedAccount)
    }
    
    func testKeychainListAccounts() throws {
        let keychainManager = KeychainManagerImpl()
        
        // Create test accounts
        let account1 = XtreamAccount(
            serverUrl: "https://test1.example.com",
            username: "user1",
            password: "pass1"
        )
        
        let account2 = XtreamAccount(
            serverUrl: "https://test2.example.com",
            username: "user2",
            password: "pass2"
        )
        
        // Save to Keychain
        try keychainManager.saveXtreamAccount(account1)
        try keychainManager.saveXtreamAccount(account2)
        
        // List accounts
        let serverUrls = try keychainManager.listXtreamAccounts()
        
        // Verify
        XCTAssertTrue(serverUrls.contains(account1.serverUrl))
        XCTAssertTrue(serverUrls.contains(account2.serverUrl))
        
        // Cleanup
        try keychainManager.deleteXtreamAccount(serverUrl: account1.serverUrl)
        try keychainManager.deleteXtreamAccount(serverUrl: account2.serverUrl)
    }
    
    // MARK: - Input Validator Tests
    
    func testValidateURL_Valid() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateURL("https://example.com/playlist.m3u", requireHTTPS: false)
        XCTAssertTrue(result.isValid)
    }
    
    func testValidateURL_RequireHTTPS() {
        let validator = InputValidatorImpl()
        
        // HTTP should fail when HTTPS is required
        let result1 = validator.validateURL("http://example.com/playlist.m3u", requireHTTPS: true)
        XCTAssertFalse(result1.isValid)
        XCTAssertNotNil(result1.errorMessage)
        
        // HTTPS should pass
        let result2 = validator.validateURL("https://example.com/playlist.m3u", requireHTTPS: true)
        XCTAssertTrue(result2.isValid)
    }
    
    func testValidateURL_Empty() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateURL("", requireHTTPS: false)
        XCTAssertFalse(result.isValid)
        XCTAssertEqual(result.errorMessage, "URL cannot be empty")
    }
    
    func testValidateURL_InvalidFormat() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateURL("not a url", requireHTTPS: false)
        XCTAssertFalse(result.isValid)
    }
    
    func testValidateURL_NoScheme() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateURL("example.com/playlist.m3u", requireHTTPS: false)
        XCTAssertFalse(result.isValid)
    }
    
    func testValidateURL_SuspiciousContent() {
        let validator = InputValidatorImpl()
        
        // Test XSS patterns
        let result1 = validator.validateURL("http://example.com/<script>alert('xss')</script>", requireHTTPS: false)
        XCTAssertFalse(result1.isValid)
        
        let result2 = validator.validateURL("javascript:alert('xss')", requireHTTPS: false)
        XCTAssertFalse(result2.isValid)
    }
    
    func testValidateXtreamCredentials_Valid() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateXtreamCredentials(
            serverUrl: "https://xtream.example.com",
            username: "testuser",
            password: "testpass123"
        )
        XCTAssertTrue(result.isValid)
    }
    
    func testValidateXtreamCredentials_RequiresHTTPS() {
        let validator = InputValidatorImpl()
        
        // HTTP should fail (Xtream credentials require HTTPS)
        let result = validator.validateXtreamCredentials(
            serverUrl: "http://xtream.example.com",
            username: "testuser",
            password: "testpass123"
        )
        XCTAssertFalse(result.isValid)
    }
    
    func testValidateXtreamCredentials_EmptyUsername() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateXtreamCredentials(
            serverUrl: "https://xtream.example.com",
            username: "",
            password: "testpass123"
        )
        XCTAssertFalse(result.isValid)
        XCTAssertEqual(result.errorMessage, "Username cannot be empty")
    }
    
    func testValidateXtreamCredentials_EmptyPassword() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateXtreamCredentials(
            serverUrl: "https://xtream.example.com",
            username: "testuser",
            password: ""
        )
        XCTAssertFalse(result.isValid)
        XCTAssertEqual(result.errorMessage, "Password cannot be empty")
    }
    
    func testValidateXtreamCredentials_ShortUsername() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateXtreamCredentials(
            serverUrl: "https://xtream.example.com",
            username: "ab",
            password: "testpass123"
        )
        XCTAssertFalse(result.isValid)
    }
    
    func testValidateXtreamCredentials_InvalidCharacters() {
        let validator = InputValidatorImpl()
        
        let result = validator.validateXtreamCredentials(
            serverUrl: "https://xtream.example.com",
            username: "test<user>",
            password: "testpass123"
        )
        XCTAssertFalse(result.isValid)
    }
    
    func testValidatePlaylistName_Valid() {
        let validator = InputValidatorImpl()
        
        let result = validator.validatePlaylistName("My Playlist")
        XCTAssertTrue(result.isValid)
    }
    
    func testValidatePlaylistName_Empty() {
        let validator = InputValidatorImpl()
        
        let result = validator.validatePlaylistName("")
        XCTAssertFalse(result.isValid)
        XCTAssertEqual(result.errorMessage, "Playlist name cannot be empty")
    }
    
    func testValidatePlaylistName_TooLong() {
        let validator = InputValidatorImpl()
        
        let longName = String(repeating: "a", count: 101)
        let result = validator.validatePlaylistName(longName)
        XCTAssertFalse(result.isValid)
    }
    
    func testValidatePlaylistName_InvalidCharacters() {
        let validator = InputValidatorImpl()
        
        // Test various invalid characters
        let invalidNames = ["test/name", "test\\name", "test:name", "test*name", "test?name", "test\"name", "test<name", "test>name", "test|name"]
        
        for name in invalidNames {
            let result = validator.validatePlaylistName(name)
            XCTAssertFalse(result.isValid, "Should reject name with invalid character: \(name)")
        }
    }
    
    func testSanitizeInput() {
        let validator = InputValidatorImpl()
        
        // Test whitespace trimming
        XCTAssertEqual(validator.sanitizeInput("  test  "), "test")
        
        // Test control character removal
        let inputWithControl = "test\u{0001}string"
        let sanitized = validator.sanitizeInput(inputWithControl)
        XCTAssertFalse(sanitized.contains("\u{0001}"))
        
        // Test null byte removal
        let inputWithNull = "test\0string"
        let sanitizedNull = validator.sanitizeInput(inputWithNull)
        XCTAssertFalse(sanitizedNull.contains("\0"))
        
        // Test length limiting
        let longInput = String(repeating: "a", count: 20000)
        let sanitizedLong = validator.sanitizeInput(longInput)
        XCTAssertEqual(sanitizedLong.count, 10000)
    }
    
    // MARK: - HTTPS Enforcement Tests
    
    func testXtreamClientEnforcesHTTPS() async throws {
        let client = XtreamClientImpl(enforceHTTPS: true)
        
        // Create account with HTTP (should fail)
        let httpAccount = XtreamAccount(
            serverUrl: "http://xtream.example.com",
            username: "testuser",
            password: "testpass"
        )
        
        do {
            _ = try await client.authenticate(account: httpAccount)
            XCTFail("Should have thrown error for HTTP URL")
        } catch let error as XtreamClientError {
            // Verify it's an authentication failed error with HTTPS message
            if case .authenticationFailed(let message) = error {
                XCTAssertTrue(message.contains("HTTPS"))
            } else {
                XCTFail("Expected authenticationFailed error")
            }
        }
    }
    
    func testXtreamClientAllowsHTTPWhenNotEnforced() async throws {
        let client = XtreamClientImpl(enforceHTTPS: false)
        
        // Create account with HTTP (should not fail due to HTTPS check)
        let httpAccount = XtreamAccount(
            serverUrl: "http://xtream.example.com",
            username: "testuser",
            password: "testpass"
        )
        
        // This will fail due to network error (no actual server), but not due to HTTPS enforcement
        do {
            _ = try await client.authenticate(account: httpAccount)
        } catch let error as XtreamClientError {
            // Should be network error or invalid response, not authentication failed with HTTPS message
            if case .authenticationFailed(let message) = error {
                XCTAssertFalse(message.contains("HTTPS"), "Should not fail due to HTTPS when not enforced")
            }
        }
    }
}
