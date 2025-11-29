//
//  AppError.swift
//  IPTVPlayer
//
//  Centralized error handling infrastructure
//

import Foundation

/// Main application error type that categorizes all possible errors
enum AppError: LocalizedError {
    case networkError(underlying: Error)
    case parsingError(message: String)
    case databaseError(underlying: Error)
    case playerError(message: String)
    
    var errorDescription: String? {
        switch self {
        case .networkError(let error):
            return "网络错误: \(error.localizedDescription)"
        case .parsingError(let message):
            return "解析错误: \(message)"
        case .databaseError(let error):
            return "数据库错误: \(error.localizedDescription)"
        case .playerError(let message):
            return "播放器错误: \(message)"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .networkError:
            return "请检查网络连接并重试"
        case .parsingError:
            return "请检查文件格式是否正确"
        case .databaseError:
            return "请尝试重启应用"
        case .playerError:
            return "请尝试播放其他频道"
        }
    }
    
    var category: ErrorCategory {
        switch self {
        case .networkError:
            return .network
        case .parsingError:
            return .parsing
        case .databaseError:
            return .database
        case .playerError:
            return .player
        }
    }
}

/// Error categories for classification and handling
enum ErrorCategory {
    case network
    case parsing
    case database
    case player
    case general
    
    var displayName: String {
        switch self {
        case .network:
            return "网络错误"
        case .parsing:
            return "解析错误"
        case .database:
            return "数据库错误"
        case .player:
            return "播放器错误"
        case .general:
            return "错误"
        }
    }
}

/// Network-specific errors
enum NetworkError: LocalizedError {
    case connectionTimeout
    case noInternetConnection
    case serverError(statusCode: Int)
    case clientError(statusCode: Int, message: String?)
    case invalidURL
    case requestFailed(underlying: Error)
    
    var errorDescription: String? {
        switch self {
        case .connectionTimeout:
            return "连接超时"
        case .noInternetConnection:
            return "无网络连接"
        case .serverError(let statusCode):
            return "服务器错误 (状态码: \(statusCode))"
        case .clientError(let statusCode, let message):
            if let message = message {
                return "请求错误 (状态码: \(statusCode)): \(message)"
            }
            return "请求错误 (状态码: \(statusCode))"
        case .invalidURL:
            return "无效的 URL"
        case .requestFailed(let error):
            return "请求失败: \(error.localizedDescription)"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .connectionTimeout:
            return "请检查网络连接并重试"
        case .noInternetConnection:
            return "请检查网络设置"
        case .serverError:
            return "服务器暂时不可用，请稍后重试"
        case .clientError:
            return "请检查请求参数是否正确"
        case .invalidURL:
            return "请检查 URL 格式是否正确"
        case .requestFailed:
            return "请检查网络连接并重试"
        }
    }
}

/// Parsing-specific errors
enum ParsingError: LocalizedError {
    case invalidM3UFormat(details: String?)
    case invalidJSONResponse(details: String?)
    case encodingIssue
    case emptyContent
    case noValidData
    
    var errorDescription: String? {
        switch self {
        case .invalidM3UFormat(let details):
            if let details = details {
                return "M3U 格式无效: \(details)"
            }
            return "M3U 格式无效"
        case .invalidJSONResponse(let details):
            if let details = details {
                return "JSON 响应格式无效: \(details)"
            }
            return "JSON 响应格式无效"
        case .encodingIssue:
            return "文件编码问题"
        case .emptyContent:
            return "内容为空"
        case .noValidData:
            return "未找到有效数据"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .invalidM3UFormat:
            return "请确保文件是有效的 M3U 格式"
        case .invalidJSONResponse:
            return "服务器响应格式异常，请联系技术支持"
        case .encodingIssue:
            return "请尝试使用不同的文件编码"
        case .emptyContent:
            return "请检查文件内容是否为空"
        case .noValidData:
            return "请检查数据源是否正确"
        }
    }
}

/// Database-specific errors
enum DatabaseError: LocalizedError {
    case connectionFailed
    case queryFailed(details: String?)
    case constraintViolation(details: String?)
    case diskFull
    case migrationFailed(details: String?)
    case dataCorruption
    
    var errorDescription: String? {
        switch self {
        case .connectionFailed:
            return "数据库连接失败"
        case .queryFailed(let details):
            if let details = details {
                return "数据库查询失败: \(details)"
            }
            return "数据库查询失败"
        case .constraintViolation(let details):
            if let details = details {
                return "数据完整性约束违反: \(details)"
            }
            return "数据完整性约束违反"
        case .diskFull:
            return "磁盘空间不足"
        case .migrationFailed(let details):
            if let details = details {
                return "数据库迁移失败: \(details)"
            }
            return "数据库迁移失败"
        case .dataCorruption:
            return "数据损坏"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .connectionFailed:
            return "请尝试重启应用"
        case .queryFailed:
            return "请尝试重新执行操作"
        case .constraintViolation:
            return "操作违反数据完整性约束，请检查输入"
        case .diskFull:
            return "请清理磁盘空间"
        case .migrationFailed:
            return "请尝试重新安装应用"
        case .dataCorruption:
            return "数据已损坏，可能需要重置应用数据"
        }
    }
}

/// Player-specific errors
enum PlayerError: LocalizedError {
    case streamNotFound
    case unsupportedFormat(format: String?)
    case decodingError(details: String?)
    case hardwareAccelerationFailed
    case bufferingTimeout
    case networkInterruption
    
    var errorDescription: String? {
        switch self {
        case .streamNotFound:
            return "流媒体不可用"
        case .unsupportedFormat(let format):
            if let format = format {
                return "不支持的视频格式: \(format)"
            }
            return "不支持的视频格式"
        case .decodingError(let details):
            if let details = details {
                return "视频解码错误: \(details)"
            }
            return "视频解码错误"
        case .hardwareAccelerationFailed:
            return "硬件加速失败，已切换到软件解码"
        case .bufferingTimeout:
            return "缓冲超时"
        case .networkInterruption:
            return "网络连接中断"
        }
    }
    
    var recoverySuggestion: String? {
        switch self {
        case .streamNotFound:
            return "频道流不可用，请尝试其他频道"
        case .unsupportedFormat:
            return "视频格式不支持，请尝试其他频道"
        case .decodingError:
            return "解码失败，请检查网络或尝试其他频道"
        case .hardwareAccelerationFailed:
            return "性能可能受影响，但播放将继续"
        case .bufferingTimeout:
            return "网络速度过慢，请检查网络连接"
        case .networkInterruption:
            return "网络连接已中断，正在尝试重新连接"
        }
    }
}
