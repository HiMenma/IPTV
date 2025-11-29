//
//  Logger.swift
//  IPTVPlayer
//
//  Logging infrastructure for the application
//

import Foundation
import os.log

/// Log levels for categorizing log messages
enum LogLevel: Int, Comparable {
    case debug = 0
    case info = 1
    case warning = 2
    case error = 3
    case critical = 4
    
    var displayName: String {
        switch self {
        case .debug:
            return "DEBUG"
        case .info:
            return "INFO"
        case .warning:
            return "WARNING"
        case .error:
            return "ERROR"
        case .critical:
            return "CRITICAL"
        }
    }
    
    var osLogType: OSLogType {
        switch self {
        case .debug:
            return .debug
        case .info:
            return .info
        case .warning:
            return .default
        case .error:
            return .error
        case .critical:
            return .fault
        }
    }
    
    static func < (lhs: LogLevel, rhs: LogLevel) -> Bool {
        return lhs.rawValue < rhs.rawValue
    }
}

/// Logger protocol for dependency injection and testing
protocol Logger {
    func log(_ message: String, level: LogLevel, category: String?, error: Error?)
    func debug(_ message: String, category: String?)
    func info(_ message: String, category: String?)
    func warning(_ message: String, category: String?)
    func error(_ message: String, category: String?, error: Error?)
    func critical(_ message: String, category: String?, error: Error?)
}

/// Default implementation using os.log
class OSLogger: Logger {
    private let subsystem: String
    private let minimumLevel: LogLevel
    
    init(subsystem: String = "com.menmapro.iptv", minimumLevel: LogLevel = .debug) {
        self.subsystem = subsystem
        self.minimumLevel = minimumLevel
    }
    
    func log(_ message: String, level: LogLevel, category: String? = nil, error: Error? = nil) {
        // Skip if below minimum level
        guard level >= minimumLevel else { return }
        
        let categoryName = category ?? "General"
        let osLog = OSLog(subsystem: subsystem, category: categoryName)
        
        var logMessage = "[\(level.displayName)] \(message)"
        if let error = error {
            logMessage += " | Error: \(error.localizedDescription)"
        }
        
        os_log("%{public}@", log: osLog, type: level.osLogType, logMessage)
    }
    
    func debug(_ message: String, category: String? = nil) {
        log(message, level: .debug, category: category, error: nil)
    }
    
    func info(_ message: String, category: String? = nil) {
        log(message, level: .info, category: category, error: nil)
    }
    
    func warning(_ message: String, category: String? = nil) {
        log(message, level: .warning, category: category, error: nil)
    }
    
    func error(_ message: String, category: String? = nil, error: Error? = nil) {
        log(message, level: .error, category: category, error: error)
    }
    
    func critical(_ message: String, category: String? = nil, error: Error? = nil) {
        log(message, level: .critical, category: category, error: error)
    }
}

/// Shared logger instance
class AppLogger {
    static let shared: Logger = OSLogger()
    
    // Convenience methods for common categories
    static func network(_ message: String, level: LogLevel = .info, error: Error? = nil) {
        shared.log(message, level: level, category: "Network", error: error)
    }
    
    static func parsing(_ message: String, level: LogLevel = .info, error: Error? = nil) {
        shared.log(message, level: level, category: "Parsing", error: error)
    }
    
    static func database(_ message: String, level: LogLevel = .info, error: Error? = nil) {
        shared.log(message, level: level, category: "Database", error: error)
    }
    
    static func player(_ message: String, level: LogLevel = .info, error: Error? = nil) {
        shared.log(message, level: level, category: "Player", error: error)
    }
    
    static func ui(_ message: String, level: LogLevel = .info, error: Error? = nil) {
        shared.log(message, level: level, category: "UI", error: error)
    }
}
