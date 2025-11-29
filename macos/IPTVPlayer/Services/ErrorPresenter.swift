//
//  ErrorPresenter.swift
//  IPTVPlayer
//
//  Error presentation logic for displaying user-friendly error messages
//

import Foundation
import SwiftUI

// Note: This file depends on AppError.swift, Logger.swift, M3UParser.swift, and XtreamClient.swift
// Make sure those files are included in the build target

/// Protocol for presenting errors to the user
protocol ErrorPresenter {
    func present(error: Error) -> ErrorPresentation
}

/// Represents how an error should be presented to the user
struct ErrorPresentation {
    let title: String
    let message: String
    let recoverySuggestion: String?
    let severity: ErrorSeverity
    let category: ErrorCategory?
    
    init(
        title: String,
        message: String,
        recoverySuggestion: String? = nil,
        severity: ErrorSeverity = .error,
        category: ErrorCategory? = nil
    ) {
        self.title = title
        self.message = message
        self.recoverySuggestion = recoverySuggestion
        self.severity = severity
        self.category = category
    }
}

/// Error severity levels for UI presentation
enum ErrorSeverity {
    case info
    case warning
    case error
    case critical
}

/// Default implementation of error presenter
class DefaultErrorPresenter: ErrorPresenter {
    
    func present(error: Error) -> ErrorPresentation {
        // Handle AppError
        if let appError = error as? AppError {
            return presentAppError(appError)
        }
        
        // Handle NetworkError
        if let networkError = error as? NetworkError {
            return presentNetworkError(networkError)
        }
        
        // Handle ParsingError
        if let parsingError = error as? ParsingError {
            return presentParsingError(parsingError)
        }
        
        // Handle DatabaseError
        if let databaseError = error as? DatabaseError {
            return presentDatabaseError(databaseError)
        }
        
        // Handle PlayerError
        if let playerError = error as? PlayerError {
            return presentPlayerError(playerError)
        }
        
        // Handle M3UParserError
        if let m3uError = error as? M3UParserError {
            return presentM3UParserError(m3uError)
        }
        
        // Handle XtreamClientError
        if let xtreamError = error as? XtreamClientError {
            return presentXtreamClientError(xtreamError)
        }
        
        // Handle generic LocalizedError
        if let localizedError = error as? LocalizedError {
            return ErrorPresentation(
                title: "错误",
                message: localizedError.errorDescription ?? "发生未知错误",
                recoverySuggestion: localizedError.recoverySuggestion,
                severity: .error
            )
        }
        
        // Fallback for unknown errors
        return ErrorPresentation(
            title: "未知错误",
            message: error.localizedDescription,
            recoverySuggestion: "请尝试重新执行操作",
            severity: .error
        )
    }
    
    // MARK: - Private Presentation Methods
    
    private func presentAppError(_ error: AppError) -> ErrorPresentation {
        return ErrorPresentation(
            title: error.category.displayName,
            message: error.errorDescription ?? "发生错误",
            recoverySuggestion: error.recoverySuggestion,
            severity: .error,
            category: error.category
        )
    }
    
    private func presentNetworkError(_ error: NetworkError) -> ErrorPresentation {
        let severity: ErrorSeverity = {
            switch error {
            case .noInternetConnection:
                return .critical
            case .serverError:
                return .error
            case .connectionTimeout:
                return .warning
            default:
                return .error
            }
        }()
        
        return ErrorPresentation(
            title: "网络错误",
            message: error.errorDescription ?? "网络请求失败",
            recoverySuggestion: error.recoverySuggestion,
            severity: severity,
            category: .network
        )
    }
    
    private func presentParsingError(_ error: ParsingError) -> ErrorPresentation {
        return ErrorPresentation(
            title: "解析错误",
            message: error.errorDescription ?? "数据解析失败",
            recoverySuggestion: error.recoverySuggestion,
            severity: .error,
            category: .parsing
        )
    }
    
    private func presentDatabaseError(_ error: DatabaseError) -> ErrorPresentation {
        let severity: ErrorSeverity = {
            switch error {
            case .dataCorruption, .diskFull:
                return .critical
            case .migrationFailed:
                return .error
            default:
                return .warning
            }
        }()
        
        return ErrorPresentation(
            title: "数据库错误",
            message: error.errorDescription ?? "数据库操作失败",
            recoverySuggestion: error.recoverySuggestion,
            severity: severity,
            category: .database
        )
    }
    
    private func presentPlayerError(_ error: PlayerError) -> ErrorPresentation {
        let severity: ErrorSeverity = {
            switch error {
            case .hardwareAccelerationFailed:
                return .warning
            case .streamNotFound, .unsupportedFormat:
                return .error
            default:
                return .warning
            }
        }()
        
        return ErrorPresentation(
            title: "播放器错误",
            message: error.errorDescription ?? "视频播放失败",
            recoverySuggestion: error.recoverySuggestion,
            severity: severity,
            category: .player
        )
    }
    
    private func presentM3UParserError(_ error: M3UParserError) -> ErrorPresentation {
        return ErrorPresentation(
            title: "M3U 解析错误",
            message: error.errorDescription ?? "M3U 文件解析失败",
            recoverySuggestion: "请检查 M3U 文件格式是否正确",
            severity: .error,
            category: .parsing
        )
    }
    
    private func presentXtreamClientError(_ error: XtreamClientError) -> ErrorPresentation {
        let severity: ErrorSeverity = {
            switch error {
            case .authenticationFailed:
                return .error
            case .networkError:
                return .warning
            default:
                return .error
            }
        }()
        
        return ErrorPresentation(
            title: "Xtream API 错误",
            message: error.errorDescription ?? "Xtream API 请求失败",
            recoverySuggestion: error.recoverySuggestion,
            severity: severity,
            category: .network
        )
    }
}

/// Shared error presenter instance
class AppErrorPresenter {
    static let shared: ErrorPresenter = DefaultErrorPresenter()
}
