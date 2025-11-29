//
//  KeychainManager.swift
//  IPTVPlayer
//
//  Secure storage manager for sensitive credentials using macOS Keychain
//

import Foundation
import Security

/// Protocol for secure credential storage
protocol KeychainManager {
    /// Save Xtream account credentials to Keychain
    /// - Parameter account: The Xtream account to save
    /// - Throws: KeychainError if save fails
    func saveXtreamAccount(_ account: XtreamAccount) throws
    
    /// Load Xtream account credentials from Keychain
    /// - Parameter serverUrl: The server URL to identify the account
    /// - Returns: The Xtream account if found
    /// - Throws: KeychainError if load fails
    func loadXtreamAccount(serverUrl: String) throws -> XtreamAccount?
    
    /// Delete Xtream account credentials from Keychain
    /// - Parameter serverUrl: The server URL to identify the account
    /// - Throws: KeychainError if delete fails
    func deleteXtreamAccount(serverUrl: String) throws
    
    /// List all stored Xtream account server URLs
    /// - Returns: Array of server URLs
    /// - Throws: KeychainError if query fails
    func listXtreamAccounts() throws -> [String]
}

/// Errors that can occur during Keychain operations
enum KeychainError: LocalizedError {
    case saveFailed(status: OSStatus)
    case loadFailed(status: OSStatus)
    case deleteFailed(status: OSStatus)
    case notFound
    case invalidData
    case duplicateItem
    
    var errorDescription: String? {
        switch self {
        case .saveFailed(let status):
            return "Failed to save to Keychain (status: \(status))"
        case .loadFailed(let status):
            return "Failed to load from Keychain (status: \(status))"
        case .deleteFailed(let status):
            return "Failed to delete from Keychain (status: \(status))"
        case .notFound:
            return "Item not found in Keychain"
        case .invalidData:
            return "Invalid data format in Keychain"
        case .duplicateItem:
            return "Item already exists in Keychain"
        }
    }
}

/// Implementation of Keychain manager for secure credential storage
class KeychainManagerImpl: KeychainManager {
    
    // Service identifier for Keychain items
    private let service = "com.menmapro.iptv.xtream"
    
    // MARK: - Public API
    
    func saveXtreamAccount(_ account: XtreamAccount) throws {
        // Encode account to JSON
        let encoder = JSONEncoder()
        guard let data = try? encoder.encode(account) else {
            throw KeychainError.invalidData
        }
        
        // Create query dictionary
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account.serverUrl,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlocked
        ]
        
        // Try to add the item
        var status = SecItemAdd(query as CFDictionary, nil)
        
        // If item already exists, update it
        if status == errSecDuplicateItem {
            let updateQuery: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: service,
                kSecAttrAccount as String: account.serverUrl
            ]
            
            let attributesToUpdate: [String: Any] = [
                kSecValueData as String: data
            ]
            
            status = SecItemUpdate(updateQuery as CFDictionary, attributesToUpdate as CFDictionary)
        }
        
        // Check for errors
        guard status == errSecSuccess else {
            throw KeychainError.saveFailed(status: status)
        }
    }
    
    func loadXtreamAccount(serverUrl: String) throws -> XtreamAccount? {
        // Create query dictionary
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: serverUrl,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        // Execute query
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        // Check for not found
        if status == errSecItemNotFound {
            return nil
        }
        
        // Check for other errors
        guard status == errSecSuccess else {
            throw KeychainError.loadFailed(status: status)
        }
        
        // Decode data
        guard let data = result as? Data else {
            throw KeychainError.invalidData
        }
        
        let decoder = JSONDecoder()
        guard let account = try? decoder.decode(XtreamAccount.self, from: data) else {
            throw KeychainError.invalidData
        }
        
        return account
    }
    
    func deleteXtreamAccount(serverUrl: String) throws {
        // Create query dictionary
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: serverUrl
        ]
        
        // Execute delete
        let status = SecItemDelete(query as CFDictionary)
        
        // Check for not found (not an error for delete)
        if status == errSecItemNotFound {
            return
        }
        
        // Check for other errors
        guard status == errSecSuccess else {
            throw KeychainError.deleteFailed(status: status)
        }
    }
    
    func listXtreamAccounts() throws -> [String] {
        // Create query dictionary to get all accounts
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecReturnAttributes as String: true,
            kSecMatchLimit as String: kSecMatchLimitAll
        ]
        
        // Execute query
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        // Check for not found (return empty array)
        if status == errSecItemNotFound {
            return []
        }
        
        // Check for other errors
        guard status == errSecSuccess else {
            throw KeychainError.loadFailed(status: status)
        }
        
        // Parse results
        guard let items = result as? [[String: Any]] else {
            return []
        }
        
        // Extract server URLs (account identifiers)
        let serverUrls = items.compactMap { item in
            item[kSecAttrAccount as String] as? String
        }
        
        return serverUrls
    }
}
