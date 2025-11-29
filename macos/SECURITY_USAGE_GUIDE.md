# Security Features Usage Guide

## Overview

This guide explains how to use the security features implemented in the macOS IPTV Player application.

## Keychain Manager

### Basic Usage

```swift
let keychainManager = KeychainManagerImpl()

// Save credentials
let account = XtreamAccount(
    serverUrl: "https://xtream.example.com",
    username: "myuser",
    password: "mypassword"
)

do {
    try keychainManager.saveXtreamAccount(account)
    print("Credentials saved securely")
} catch {
    print("Failed to save: \(error.localizedDescription)")
}

// Load credentials
do {
    if let loadedAccount = try keychainManager.loadXtreamAccount(serverUrl: "https://xtream.example.com") {
        print("Username: \(loadedAccount.username)")
        // Use the credentials
    } else {
        print("No credentials found")
    }
} catch {
    print("Failed to load: \(error.localizedDescription)")
}

// Delete credentials
do {
    try keychainManager.deleteXtreamAccount(serverUrl: "https://xtream.example.com")
    print("Credentials deleted")
} catch {
    print("Failed to delete: \(error.localizedDescription)")
}

// List all stored accounts
do {
    let serverUrls = try keychainManager.listXtreamAccounts()
    print("Stored accounts: \(serverUrls)")
} catch {
    print("Failed to list: \(error.localizedDescription)")
}
```

### Integration with ViewModels

```swift
class MyViewModel: ObservableObject {
    private let keychainManager: KeychainManager
    
    init(keychainManager: KeychainManager) {
        self.keychainManager = keychainManager
    }
    
    func saveCredentials(account: XtreamAccount) async {
        do {
            try keychainManager.saveXtreamAccount(account)
            // Continue with authentication
        } catch {
            errorMessage = "Failed to save credentials: \(error.localizedDescription)"
        }
    }
}
```

## Input Validator

### URL Validation

```swift
let validator = InputValidatorImpl()

// Validate M3U URL (HTTP allowed)
let m3uResult = validator.validateURL("http://example.com/playlist.m3u", requireHTTPS: false)
if m3uResult.isValid {
    // URL is valid, proceed
} else {
    print("Invalid URL: \(m3uResult.errorMessage ?? "Unknown error")")
}

// Validate Xtream URL (HTTPS required)
let xtreamResult = validator.validateURL("https://xtream.example.com", requireHTTPS: true)
if xtreamResult.isValid {
    // URL is valid and uses HTTPS
} else {
    print("Invalid URL: \(xtreamResult.errorMessage ?? "Unknown error")")
}
```

### Xtream Credentials Validation

```swift
let validator = InputValidatorImpl()

let result = validator.validateXtreamCredentials(
    serverUrl: "https://xtream.example.com",
    username: "myuser",
    password: "mypassword"
)

if result.isValid {
    // Credentials are valid, proceed with authentication
} else {
    print("Invalid credentials: \(result.errorMessage ?? "Unknown error")")
}
```

### Playlist Name Validation

```swift
let validator = InputValidatorImpl()

let result = validator.validatePlaylistName("My Playlist")
if result.isValid {
    // Name is valid, proceed with save
} else {
    print("Invalid name: \(result.errorMessage ?? "Unknown error")")
}
```

### Input Sanitization

```swift
let validator = InputValidatorImpl()

// Sanitize user input before processing
let userInput = "  My Playlist\n\0  "
let sanitized = validator.sanitizeInput(userInput)
// Result: "My Playlist" (trimmed, no control characters)

// Always sanitize before validation
let sanitizedUrl = validator.sanitizeInput(userUrlInput)
let validation = validator.validateURL(sanitizedUrl, requireHTTPS: false)
```

### Integration with ViewModels

```swift
class MyViewModel: ObservableObject {
    private let inputValidator: InputValidator
    
    init(inputValidator: InputValidator) {
        self.inputValidator = inputValidator
    }
    
    func addPlaylist(url: String) async {
        // Sanitize input
        let sanitizedUrl = inputValidator.sanitizeInput(url)
        
        // Validate URL
        let validation = inputValidator.validateURL(sanitizedUrl, requireHTTPS: false)
        guard validation.isValid else {
            errorMessage = validation.errorMessage ?? "Invalid URL"
            return
        }
        
        // Proceed with adding playlist
        // ...
    }
}
```

## HTTPS Enforcement

### XtreamClient Configuration

```swift
// Default: HTTPS enforced
let client = XtreamClientImpl()

// Explicitly enable HTTPS enforcement (recommended)
let secureClient = XtreamClientImpl(enforceHTTPS: true)

// Disable HTTPS enforcement (not recommended, for testing only)
let insecureClient = XtreamClientImpl(enforceHTTPS: false)
```

### Error Handling

```swift
let client = XtreamClientImpl(enforceHTTPS: true)

let account = XtreamAccount(
    serverUrl: "http://xtream.example.com", // HTTP, not HTTPS
    username: "user",
    password: "pass"
)

do {
    _ = try await client.authenticate(account: account)
} catch let error as XtreamClientError {
    if case .authenticationFailed(let message) = error {
        // Will contain: "Server URL must use HTTPS protocol for security. HTTP is not allowed."
        print(message)
    }
}
```

## Complete Example: Adding Xtream Account

```swift
class MainViewModel: ObservableObject {
    private let keychainManager: KeychainManager
    private let inputValidator: InputValidator
    private let xtreamClient: XtreamClient
    
    @Published var errorMessage: String?
    
    init(
        keychainManager: KeychainManager,
        inputValidator: InputValidator,
        xtreamClient: XtreamClient
    ) {
        self.keychainManager = keychainManager
        self.inputValidator = inputValidator
        self.xtreamClient = xtreamClient
    }
    
    func addXtreamAccount(serverUrl: String, username: String, password: String) async {
        // Step 1: Sanitize inputs
        let sanitizedServerUrl = inputValidator.sanitizeInput(serverUrl)
        let sanitizedUsername = inputValidator.sanitizeInput(username)
        let sanitizedPassword = inputValidator.sanitizeInput(password)
        
        // Step 2: Validate credentials (requires HTTPS)
        let validation = inputValidator.validateXtreamCredentials(
            serverUrl: sanitizedServerUrl,
            username: sanitizedUsername,
            password: sanitizedPassword
        )
        
        guard validation.isValid else {
            errorMessage = validation.errorMessage ?? "Invalid credentials"
            return
        }
        
        // Step 3: Create account
        let account = XtreamAccount(
            serverUrl: sanitizedServerUrl,
            username: sanitizedUsername,
            password: sanitizedPassword
        )
        
        // Step 4: Authenticate (HTTPS enforced)
        do {
            let authenticated = try await xtreamClient.authenticate(account: account)
            guard authenticated else {
                errorMessage = "Authentication failed"
                return
            }
        } catch {
            errorMessage = "Authentication error: \(error.localizedDescription)"
            return
        }
        
        // Step 5: Save credentials to Keychain
        do {
            try keychainManager.saveXtreamAccount(account)
        } catch {
            errorMessage = "Failed to save credentials: \(error.localizedDescription)"
            return
        }
        
        // Step 6: Proceed with loading channels, etc.
        // ...
    }
}
```

## Security Best Practices

### 1. Always Sanitize User Input

```swift
// ❌ Bad: Using raw user input
let url = userInput

// ✅ Good: Sanitize first
let url = inputValidator.sanitizeInput(userInput)
```

### 2. Validate Before Processing

```swift
// ❌ Bad: No validation
let account = XtreamAccount(serverUrl: url, username: user, password: pass)

// ✅ Good: Validate first
let validation = inputValidator.validateXtreamCredentials(
    serverUrl: url,
    username: user,
    password: pass
)
guard validation.isValid else {
    // Handle error
    return
}
let account = XtreamAccount(serverUrl: url, username: user, password: pass)
```

### 3. Use Keychain for Sensitive Data

```swift
// ❌ Bad: Storing credentials in UserDefaults or database
UserDefaults.standard.set(password, forKey: "password")

// ✅ Good: Use Keychain
try keychainManager.saveXtreamAccount(account)
```

### 4. Enforce HTTPS for Sensitive Operations

```swift
// ❌ Bad: Allowing HTTP for credentials
let client = XtreamClientImpl(enforceHTTPS: false)

// ✅ Good: Enforce HTTPS (default)
let client = XtreamClientImpl() // enforceHTTPS: true by default
```

### 5. Handle Errors Gracefully

```swift
// ❌ Bad: Ignoring errors
try? keychainManager.saveXtreamAccount(account)

// ✅ Good: Handle errors
do {
    try keychainManager.saveXtreamAccount(account)
} catch {
    errorMessage = "Failed to save credentials: \(error.localizedDescription)"
    // Log error, show user-friendly message
}
```

## Testing

### Unit Testing with Security Features

```swift
import XCTest
@testable import IPTVPlayer

class MyViewModelTests: XCTestCase {
    var viewModel: MyViewModel!
    var keychainManager: KeychainManager!
    var inputValidator: InputValidator!
    
    override func setUp() {
        super.setUp()
        keychainManager = KeychainManagerImpl()
        inputValidator = InputValidatorImpl()
        viewModel = MyViewModel(
            keychainManager: keychainManager,
            inputValidator: inputValidator
        )
    }
    
    override func tearDown() {
        // Clean up Keychain
        try? keychainManager.deleteXtreamAccount(serverUrl: "https://test.example.com")
        super.tearDown()
    }
    
    func testAddXtreamAccount() async {
        await viewModel.addXtreamAccount(
            serverUrl: "https://test.example.com",
            username: "testuser",
            password: "testpass"
        )
        
        // Verify credentials were saved
        let account = try? keychainManager.loadXtreamAccount(serverUrl: "https://test.example.com")
        XCTAssertNotNil(account)
    }
}
```

## Troubleshooting

### Keychain Access Denied

**Problem**: `KeychainError.saveFailed(status: -25291)`

**Solution**: Ensure your app has the correct entitlements. Add to `IPTVPlayer.entitlements`:
```xml
<key>keychain-access-groups</key>
<array>
    <string>$(AppIdentifierPrefix)com.menmapro.iptv</string>
</array>
```

### HTTPS Enforcement Blocking Valid Servers

**Problem**: Some Xtream servers only support HTTP

**Solution**: This is a security risk. Options:
1. Contact the server provider to enable HTTPS (recommended)
2. Temporarily disable enforcement for testing: `XtreamClientImpl(enforceHTTPS: false)`
3. Add a user preference to allow HTTP (with clear security warning)

### Input Validation Too Strict

**Problem**: Valid inputs being rejected

**Solution**: Review validation rules in `InputValidator.swift` and adjust as needed. Consider:
- Increasing character limits
- Expanding allowed character sets
- Adding special case handling

## Additional Resources

- [Apple Keychain Services Documentation](https://developer.apple.com/documentation/security/keychain_services)
- [OWASP Input Validation Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
- [Apple Security Best Practices](https://developer.apple.com/documentation/security)
