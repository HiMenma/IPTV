# Security Architecture

## Overview

This document describes the security architecture of the macOS IPTV Player application, focusing on the three main security pillars: credential protection, network security, and input validation.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Interface                           │
│                      (SwiftUI Views)                             │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ User Input
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                        MainViewModel                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  1. Sanitize Input (InputValidator.sanitizeInput)        │  │
│  │  2. Validate Input (InputValidator.validate*)            │  │
│  │  3. Process Request                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────┬─────────────────────────┬──────────────────────┬────────┘
        │                         │                      │
        │ Save/Load               │ API Calls            │ Validate
        │ Credentials             │                      │
        ▼                         ▼                      ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ KeychainManager  │    │  XtreamClient    │    │ InputValidator   │
│                  │    │                  │    │                  │
│ • Save           │    │ • Validate HTTPS │    │ • URL Validation │
│ • Load           │    │ • Authenticate   │    │ • Creds Valid.   │
│ • Delete         │    │ • Get Streams    │    │ • Name Valid.    │
│ • List           │    │ • Get Categories │    │ • Sanitization   │
└────────┬─────────┘    └────────┬─────────┘    └──────────────────┘
         │                       │
         │ Encrypted             │ HTTPS Only
         ▼                       ▼
┌──────────────────┐    ┌──────────────────┐
│  macOS Keychain  │    │  Xtream Server   │
│  (System Level)  │    │  (HTTPS)         │
└──────────────────┘    └──────────────────┘
```

## Security Layers

### Layer 1: Input Validation (First Line of Defense)

**Component**: `InputValidator`

**Purpose**: Prevent malicious or malformed data from entering the system

**Protection Against**:
- XSS attacks (script injection)
- SQL injection
- Path traversal
- Buffer overflow (length limits)
- File system attacks (invalid characters)

**Implementation**:
```swift
// All user input flows through sanitization
let sanitized = inputValidator.sanitizeInput(userInput)

// Then validation
let validation = inputValidator.validateURL(sanitized, requireHTTPS: false)
guard validation.isValid else {
    // Reject invalid input
    return
}
```

**Validation Rules**:
- **URLs**: Format, scheme, host, suspicious patterns
- **Credentials**: Length, character set, HTTPS requirement
- **Names**: Length, file-safe characters

### Layer 2: Network Security (Transport Protection)

**Component**: `XtreamClient` with HTTPS enforcement

**Purpose**: Protect credentials and data in transit

**Protection Against**:
- Man-in-the-middle attacks
- Credential interception
- Data tampering
- Eavesdropping

**Implementation**:
```swift
// HTTPS validation before every API call
private func validateHTTPS(serverUrl: String) throws {
    guard enforceHTTPS else { return }
    
    guard let url = URL(string: serverUrl),
          let scheme = url.scheme?.lowercased(),
          scheme == "https" else {
        throw XtreamClientError.authenticationFailed(
            message: "Server URL must use HTTPS protocol for security"
        )
    }
}
```

**Enforcement Points**:
- Authentication
- Category fetching
- Stream fetching
- EPG data retrieval

### Layer 3: Credential Protection (Storage Security)

**Component**: `KeychainManager`

**Purpose**: Securely store sensitive credentials

**Protection Against**:
- Credential theft from file system
- Memory dumps
- Unauthorized access
- Data breaches

**Implementation**:
```swift
// Credentials stored with system-level encryption
let query: [String: Any] = [
    kSecClass as String: kSecClassGenericPassword,
    kSecAttrService as String: service,
    kSecAttrAccount as String: account.serverUrl,
    kSecValueData as String: data,
    kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlocked
]
```

**Security Features**:
- System-level encryption
- Access only when device unlocked
- Automatic cleanup on app deletion
- No plain-text storage

## Data Flow Security

### Adding Xtream Account (Secure Flow)

```
User Input
    │
    ├─► 1. Sanitize (InputValidator)
    │      Remove control chars, trim whitespace, limit length
    │
    ├─► 2. Validate (InputValidator)
    │      Check format, length, character set, HTTPS
    │
    ├─► 3. Create Account Object
    │      XtreamAccount(serverUrl, username, password)
    │
    ├─► 4. Authenticate (XtreamClient)
    │      ├─► Validate HTTPS
    │      ├─► Send request over HTTPS
    │      └─► Verify response
    │
    ├─► 5. Save to Keychain (KeychainManager)
    │      ├─► Encode to JSON
    │      ├─► Store with encryption
    │      └─► Set access policy
    │
    └─► 6. Load Channels
           Success!
```

### Loading Saved Account (Secure Flow)

```
App Startup
    │
    ├─► 1. List Accounts (KeychainManager)
    │      Query Keychain for all stored accounts
    │
    ├─► 2. Load Credentials (KeychainManager)
    │      ├─► Retrieve encrypted data
    │      ├─► Decode JSON
    │      └─► Return XtreamAccount
    │
    ├─► 3. Authenticate (XtreamClient)
    │      ├─► Validate HTTPS
    │      └─► Send request over HTTPS
    │
    └─► 4. Load Channels
           Success!
```

## Security Boundaries

### Trust Boundaries

```
┌─────────────────────────────────────────────────────────────┐
│                    Untrusted Zone                            │
│  • User Input                                                │
│  • Network Data                                              │
│  • External URLs                                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Input Validation
                         │ HTTPS Enforcement
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Trusted Zone                              │
│  • Validated Data                                            │
│  • Encrypted Credentials                                     │
│  • Secure Network Connections                                │
└─────────────────────────────────────────────────────────────┘
```

### Security Checkpoints

1. **Input Checkpoint**: All user input validated before processing
2. **Network Checkpoint**: All Xtream API calls use HTTPS
3. **Storage Checkpoint**: All credentials encrypted in Keychain

## Threat Model

### Threats Mitigated

| Threat | Mitigation | Component |
|--------|-----------|-----------|
| Credential Theft | Keychain encryption | KeychainManager |
| Man-in-the-Middle | HTTPS enforcement | XtreamClient |
| XSS Attacks | Input sanitization | InputValidator |
| SQL Injection | Input validation | InputValidator |
| Path Traversal | Character whitelisting | InputValidator |
| Buffer Overflow | Length limiting | InputValidator |
| Brute Force | Keychain rate limiting | macOS Keychain |
| Memory Dumps | No plain-text storage | KeychainManager |

### Residual Risks

| Risk | Severity | Mitigation Plan |
|------|----------|----------------|
| Compromised Keychain | Low | Requires device compromise |
| HTTPS Downgrade | Low | Certificate pinning (future) |
| Phishing | Medium | User education |
| Malicious M3U | Medium | M3U content validation (future) |

## Security Configuration

### Default Security Settings

```swift
// HTTPS Enforcement: ENABLED
let xtreamClient = XtreamClientImpl(enforceHTTPS: true)

// Keychain Access: When Unlocked
kSecAttrAccessible: kSecAttrAccessibleWhenUnlocked

// Input Validation: STRICT
// - Length limits enforced
// - Character whitelists enforced
// - Suspicious patterns detected
```

### Configurable Security Options

```swift
// For testing only - disable HTTPS enforcement
let testClient = XtreamClientImpl(enforceHTTPS: false)

// Note: This should NEVER be used in production
```

## Security Best Practices

### For Developers

1. **Always Sanitize First**
   ```swift
   let sanitized = inputValidator.sanitizeInput(userInput)
   ```

2. **Then Validate**
   ```swift
   let validation = inputValidator.validateURL(sanitized, requireHTTPS: true)
   guard validation.isValid else { return }
   ```

3. **Use Keychain for Credentials**
   ```swift
   try keychainManager.saveXtreamAccount(account)
   ```

4. **Enforce HTTPS**
   ```swift
   let client = XtreamClientImpl() // enforceHTTPS: true by default
   ```

5. **Handle Errors Securely**
   ```swift
   // Don't expose sensitive information in error messages
   catch {
       errorMessage = "Authentication failed" // Generic message
       logger.error("Auth failed: \(error)") // Detailed logging
   }
   ```

### For Users

1. **Use HTTPS URLs**: Always prefer HTTPS Xtream servers
2. **Strong Passwords**: Use unique, strong passwords for Xtream accounts
3. **Keep macOS Updated**: Security patches are important
4. **Lock Your Mac**: Keychain only accessible when unlocked

## Compliance

### Security Standards

- ✅ **OWASP Top 10**: Mitigates injection, broken authentication, sensitive data exposure
- ✅ **Apple Security Guidelines**: Uses Keychain, enforces HTTPS, validates input
- ✅ **CWE Top 25**: Addresses common weaknesses (injection, XSS, etc.)

### Privacy

- ✅ **No Telemetry**: No user data sent to third parties
- ✅ **Local Storage**: All data stored locally on device
- ✅ **Encrypted Credentials**: Credentials never stored in plain text

## Security Monitoring

### Logging

```swift
// Security events logged (without sensitive data)
logger.info("Keychain save successful")
logger.warning("Invalid URL rejected: \(sanitizedUrl)")
logger.error("HTTPS enforcement blocked HTTP request")
```

### Audit Trail

- Keychain operations logged
- Validation failures logged
- HTTPS enforcement logged
- No sensitive data in logs

## Future Enhancements

### Planned Security Features

1. **Certificate Pinning**: Pin Xtream server certificates
2. **Biometric Authentication**: Touch ID/Face ID for app access
3. **M3U Content Validation**: Validate M3U file contents
4. **Rate Limiting**: Limit authentication attempts
5. **Security Audit Logging**: Detailed security event logging
6. **Encrypted Database**: Encrypt local database
7. **Secure Memory**: Zero sensitive data from memory

### Security Roadmap

**Phase 1** (Completed):
- ✅ Keychain storage
- ✅ HTTPS enforcement
- ✅ Input validation

**Phase 2** (Future):
- [ ] Certificate pinning
- [ ] Biometric authentication
- [ ] M3U content validation

**Phase 3** (Future):
- [ ] Rate limiting
- [ ] Security audit logging
- [ ] Encrypted database

## Conclusion

The security architecture provides defense-in-depth protection through multiple layers:

1. **Input Validation**: First line of defense against malicious input
2. **Network Security**: HTTPS enforcement protects data in transit
3. **Credential Protection**: Keychain encryption protects data at rest

This multi-layered approach ensures that even if one layer is compromised, other layers provide continued protection.
