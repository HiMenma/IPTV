//
//  InputValidator.swift
//  IPTVPlayer
//
//  Input validation utilities for user-provided data
//

import Foundation

/// Protocol for input validation
protocol InputValidator {
    /// Validate a URL string
    /// - Parameters:
    ///   - urlString: The URL string to validate
    ///   - requireHTTPS: Whether to require HTTPS protocol
    /// - Returns: Validation result
    func validateURL(_ urlString: String, requireHTTPS: Bool) -> ValidationResult
    
    /// Validate Xtream Codes credentials
    /// - Parameters:
    ///   - serverUrl: The server URL
    ///   - username: The username
    ///   - password: The password
    /// - Returns: Validation result
    func validateXtreamCredentials(serverUrl: String, username: String, password: String) -> ValidationResult
    
    /// Validate playlist name
    /// - Parameter name: The playlist name
    /// - Returns: Validation result
    func validatePlaylistName(_ name: String) -> ValidationResult
    
    /// Sanitize user input to prevent injection attacks
    /// - Parameter input: The user input string
    /// - Returns: Sanitized string
    func sanitizeInput(_ input: String) -> String
}

/// Result of input validation
enum ValidationResult {
    case valid
    case invalid(reason: String)
    
    var isValid: Bool {
        if case .valid = self {
            return true
        }
        return false
    }
    
    var errorMessage: String? {
        if case .invalid(let reason) = self {
            return reason
        }
        return nil
    }
}

/// Implementation of input validator
class InputValidatorImpl: InputValidator {
    
    // MARK: - URL Validation
    
    func validateURL(_ urlString: String, requireHTTPS: Bool = false) -> ValidationResult {
        // Check for empty string
        let trimmed = urlString.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return .invalid(reason: "URL cannot be empty")
        }
        
        // Check for valid URL format
        guard let url = URL(string: trimmed) else {
            return .invalid(reason: "Invalid URL format")
        }
        
        // Check for scheme
        guard let scheme = url.scheme?.lowercased() else {
            return .invalid(reason: "URL must include a protocol (http:// or https://)")
        }
        
        // Validate scheme
        let validSchemes = requireHTTPS ? ["https"] : ["http", "https"]
        guard validSchemes.contains(scheme) else {
            if requireHTTPS {
                return .invalid(reason: "URL must use HTTPS protocol for security")
            } else {
                return .invalid(reason: "URL must use HTTP or HTTPS protocol")
            }
        }
        
        // Check for host
        guard url.host != nil else {
            return .invalid(reason: "URL must include a valid host")
        }
        
        // Check for suspicious patterns (basic XSS/injection prevention)
        let suspiciousPatterns = ["<script", "javascript:", "data:", "vbscript:"]
        let lowercasedUrl = trimmed.lowercased()
        for pattern in suspiciousPatterns {
            if lowercasedUrl.contains(pattern) {
                return .invalid(reason: "URL contains suspicious content")
            }
        }
        
        return .valid
    }
    
    // MARK: - Xtream Credentials Validation
    
    func validateXtreamCredentials(serverUrl: String, username: String, password: String) -> ValidationResult {
        // Validate server URL (require HTTPS for security)
        let urlValidation = validateURL(serverUrl, requireHTTPS: true)
        if !urlValidation.isValid {
            return urlValidation
        }
        
        // Validate username
        let trimmedUsername = username.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedUsername.isEmpty else {
            return .invalid(reason: "Username cannot be empty")
        }
        
        guard trimmedUsername.count >= 3 else {
            return .invalid(reason: "Username must be at least 3 characters")
        }
        
        guard trimmedUsername.count <= 100 else {
            return .invalid(reason: "Username is too long (max 100 characters)")
        }
        
        // Check for invalid characters in username
        let usernameCharacterSet = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "_-@."))
        guard trimmedUsername.unicodeScalars.allSatisfy({ usernameCharacterSet.contains($0) }) else {
            return .invalid(reason: "Username contains invalid characters")
        }
        
        // Validate password
        let trimmedPassword = password.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedPassword.isEmpty else {
            return .invalid(reason: "Password cannot be empty")
        }
        
        guard trimmedPassword.count >= 3 else {
            return .invalid(reason: "Password must be at least 3 characters")
        }
        
        guard trimmedPassword.count <= 100 else {
            return .invalid(reason: "Password is too long (max 100 characters)")
        }
        
        return .valid
    }
    
    // MARK: - Playlist Name Validation
    
    func validatePlaylistName(_ name: String) -> ValidationResult {
        // Check for empty string
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return .invalid(reason: "Playlist name cannot be empty")
        }
        
        // Check length
        guard trimmed.count >= 1 else {
            return .invalid(reason: "Playlist name must be at least 1 character")
        }
        
        guard trimmed.count <= 100 else {
            return .invalid(reason: "Playlist name is too long (max 100 characters)")
        }
        
        // Check for invalid characters (file system safety)
        let invalidCharacters = CharacterSet(charactersIn: "/\\:*?\"<>|")
        guard trimmed.unicodeScalars.allSatisfy({ !invalidCharacters.contains($0) }) else {
            return .invalid(reason: "Playlist name contains invalid characters (/ \\ : * ? \" < > |)")
        }
        
        return .valid
    }
    
    // MARK: - Input Sanitization
    
    func sanitizeInput(_ input: String) -> String {
        // Trim whitespace
        var sanitized = input.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // Remove control characters
        sanitized = sanitized.components(separatedBy: .controlCharacters).joined()
        
        // Remove null bytes
        sanitized = sanitized.replacingOccurrences(of: "\0", with: "")
        
        // Limit length to prevent DoS
        if sanitized.count > 10000 {
            sanitized = String(sanitized.prefix(10000))
        }
        
        return sanitized
    }
}
