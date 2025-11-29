//
//  RetryMechanism.swift
//  IPTVPlayer
//
//  Retry mechanism with exponential backoff for handling transient failures
//

import Foundation

/// Configuration for retry behavior
struct RetryConfiguration {
    let maxAttempts: Int
    let initialDelay: TimeInterval
    let maxDelay: TimeInterval
    let backoffMultiplier: Double
    let shouldRetry: (Error) -> Bool
    
    /// Default configuration with exponential backoff
    static let `default` = RetryConfiguration(
        maxAttempts: 3,
        initialDelay: 1.0,
        maxDelay: 30.0,
        backoffMultiplier: 2.0,
        shouldRetry: { error in
            // Retry on network errors, but not on authentication or validation errors
            if let networkError = error as? NetworkError {
                switch networkError {
                case .connectionTimeout, .noInternetConnection, .serverError, .requestFailed:
                    return true
                case .clientError, .invalidURL:
                    return false
                }
            }
            
            if let xtreamError = error as? XtreamClientError {
                switch xtreamError {
                case .networkError, .serverError:
                    return true
                case .invalidURL, .authenticationFailed, .decodingError, .invalidResponse, .noData:
                    return false
                }
            }
            
            // Default: retry on unknown errors
            return true
        }
    )
    
    /// Configuration for network requests
    static let network = RetryConfiguration(
        maxAttempts: 3,
        initialDelay: 1.0,
        maxDelay: 30.0,
        backoffMultiplier: 2.0,
        shouldRetry: { error in
            if let networkError = error as? NetworkError {
                switch networkError {
                case .connectionTimeout, .noInternetConnection, .serverError, .requestFailed:
                    return true
                case .clientError, .invalidURL:
                    return false
                }
            }
            return true
        }
    )
    
    /// Configuration for database operations
    static let database = RetryConfiguration(
        maxAttempts: 2,
        initialDelay: 0.5,
        maxDelay: 5.0,
        backoffMultiplier: 2.0,
        shouldRetry: { error in
            if let dbError = error as? DatabaseError {
                switch dbError {
                case .connectionFailed, .queryFailed:
                    return true
                case .constraintViolation, .diskFull, .migrationFailed, .dataCorruption:
                    return false
                }
            }
            return false
        }
    )
}

/// Retry mechanism with exponential backoff
class RetryMechanism {
    
    /// Execute an operation with retry logic
    /// - Parameters:
    ///   - configuration: Retry configuration
    ///   - operation: The async operation to retry
    /// - Returns: The result of the operation
    /// - Throws: The last error if all retries fail
    static func execute<T>(
        configuration: RetryConfiguration = .default,
        operation: @escaping () async throws -> T
    ) async throws -> T {
        var attempt = 0
        var delay = configuration.initialDelay
        var lastError: Error?
        
        while attempt < configuration.maxAttempts {
            do {
                AppLogger.shared.debug(
                    "Executing operation (attempt \(attempt + 1)/\(configuration.maxAttempts))",
                    category: "RetryMechanism"
                )
                
                let result = try await operation()
                
                if attempt > 0 {
                    AppLogger.shared.info(
                        "Operation succeeded after \(attempt + 1) attempts",
                        category: "RetryMechanism"
                    )
                }
                
                return result
            } catch {
                lastError = error
                attempt += 1
                
                // Check if we should retry this error
                guard configuration.shouldRetry(error) else {
                    AppLogger.shared.warning(
                        "Operation failed with non-retryable error",
                        category: "RetryMechanism"
                    )
                    throw error
                }
                
                // Check if we've exhausted retries
                if attempt >= configuration.maxAttempts {
                    AppLogger.shared.error(
                        "Operation failed after \(attempt) attempts",
                        category: "RetryMechanism",
                        error: error
                    )
                    throw error
                }
                
                // Calculate delay with exponential backoff
                let currentDelay = min(delay, configuration.maxDelay)
                
                AppLogger.shared.warning(
                    "Operation failed (attempt \(attempt)/\(configuration.maxAttempts)), retrying in \(currentDelay)s",
                    category: "RetryMechanism"
                )
                
                // Wait before retrying
                try await Task.sleep(nanoseconds: UInt64(currentDelay * 1_000_000_000))
                
                // Increase delay for next attempt
                delay *= configuration.backoffMultiplier
            }
        }
        
        // This should never be reached, but just in case
        throw lastError ?? NSError(
            domain: "RetryMechanism",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Unknown error in retry mechanism"]
        )
    }
    
    /// Execute an operation with default retry configuration
    /// - Parameter operation: The async operation to retry
    /// - Returns: The result of the operation
    /// - Throws: The last error if all retries fail
    static func withRetry<T>(
        _ operation: @escaping () async throws -> T
    ) async throws -> T {
        try await execute(configuration: .default, operation: operation)
    }
    
    /// Execute a network operation with network-specific retry configuration
    /// - Parameter operation: The async network operation to retry
    /// - Returns: The result of the operation
    /// - Throws: The last error if all retries fail
    static func withNetworkRetry<T>(
        _ operation: @escaping () async throws -> T
    ) async throws -> T {
        try await execute(configuration: .network, operation: operation)
    }
    
    /// Execute a database operation with database-specific retry configuration
    /// - Parameter operation: The async database operation to retry
    /// - Returns: The result of the operation
    /// - Throws: The last error if all retries fail
    static func withDatabaseRetry<T>(
        _ operation: @escaping () async throws -> T
    ) async throws -> T {
        try await execute(configuration: .database, operation: operation)
    }
}
