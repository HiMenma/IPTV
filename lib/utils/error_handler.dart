import 'dart:async';
import 'package:dio/dio.dart';

/// Error handling utilities for network operations
/// Provides retry logic with exponential backoff and user-friendly error messages

class ErrorHandler {
  /// Maximum number of retry attempts
  static const int maxRetries = 3;

  /// Initial delay for exponential backoff (in milliseconds)
  static const int initialDelayMs = 1000;

  /// Maximum delay for exponential backoff (in milliseconds)
  static const int maxDelayMs = 10000;

  /// Execute a network operation with retry logic and exponential backoff
  /// Requirements: 2.3, 3.5, 5.3
  static Future<T> executeWithRetry<T>(
    Future<T> Function() operation, {
    int maxAttempts = maxRetries,
    bool Function(Exception)? shouldRetry,
  }) async {
    int attempt = 0;
    Exception? lastException;

    while (attempt < maxAttempts) {
      try {
        return await operation();
      } on DioException catch (e) {
        lastException = e;
        
        // Check if we should retry this error
        if (!_shouldRetryDioException(e)) {
          throw _convertDioException(e);
        }

        // Check custom retry condition
        if (shouldRetry != null && !shouldRetry(e)) {
          throw _convertDioException(e);
        }

        attempt++;
        
        // If this was the last attempt, throw the error
        if (attempt >= maxAttempts) {
          throw _convertDioException(e);
        }

        // Calculate exponential backoff delay
        final delay = _calculateBackoffDelay(attempt);
        await Future.delayed(Duration(milliseconds: delay));
      } on Exception catch (e) {
        lastException = e;
        
        // For non-Dio exceptions, check custom retry condition
        if (shouldRetry != null && shouldRetry(e)) {
          attempt++;
          
          if (attempt >= maxAttempts) {
            rethrow;
          }

          final delay = _calculateBackoffDelay(attempt);
          await Future.delayed(Duration(milliseconds: delay));
        } else {
          rethrow;
        }
      }
    }

    // This should never be reached, but just in case
    throw lastException ?? Exception('Operation failed after $maxAttempts attempts');
  }

  /// Calculate exponential backoff delay
  static int _calculateBackoffDelay(int attempt) {
    // Exponential backoff: initialDelay * 2^(attempt-1)
    final delay = initialDelayMs * (1 << (attempt - 1));
    return delay > maxDelayMs ? maxDelayMs : delay;
  }

  /// Determine if a DioException should be retried
  static bool _shouldRetryDioException(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return true;
      
      case DioExceptionType.connectionError:
        // Retry connection errors (network unavailable, etc.)
        return true;
      
      case DioExceptionType.badResponse:
        // Retry on server errors (5xx), but not client errors (4xx)
        final statusCode = e.response?.statusCode;
        return statusCode != null && statusCode >= 500;
      
      case DioExceptionType.cancel:
      case DioExceptionType.badCertificate:
      case DioExceptionType.unknown:
      default:
        return false;
    }
  }

  /// Convert DioException to user-friendly error message
  /// Requirements: 2.3, 3.5, 5.3
  static Exception _convertDioException(DioException e) {
    switch (e.type) {
      case DioExceptionType.connectionTimeout:
        return NetworkException(
          'Connection timeout. Please check your internet connection and try again.',
          type: NetworkErrorType.timeout,
          originalError: e,
        );
      
      case DioExceptionType.sendTimeout:
        return NetworkException(
          'Request timeout. The server is taking too long to respond.',
          type: NetworkErrorType.timeout,
          originalError: e,
        );
      
      case DioExceptionType.receiveTimeout:
        return NetworkException(
          'Response timeout. The server is taking too long to respond.',
          type: NetworkErrorType.timeout,
          originalError: e,
        );
      
      case DioExceptionType.connectionError:
        return NetworkException(
          'Connection error. Please check your internet connection.',
          type: NetworkErrorType.connectionError,
          originalError: e,
        );
      
      case DioExceptionType.badResponse:
        final statusCode = e.response?.statusCode;
        if (statusCode == 401 || statusCode == 403) {
          return NetworkException(
            'Authentication failed. Please check your credentials.',
            type: NetworkErrorType.authenticationError,
            originalError: e,
          );
        } else if (statusCode == 404) {
          return NetworkException(
            'Resource not found. The requested content is not available.',
            type: NetworkErrorType.notFound,
            originalError: e,
          );
        } else if (statusCode != null && statusCode >= 500) {
          return NetworkException(
            'Server error. Please try again later.',
            type: NetworkErrorType.serverError,
            originalError: e,
          );
        } else {
          return NetworkException(
            'Request failed with status code $statusCode.',
            type: NetworkErrorType.unknown,
            originalError: e,
          );
        }
      
      case DioExceptionType.cancel:
        return NetworkException(
          'Request was cancelled.',
          type: NetworkErrorType.cancelled,
          originalError: e,
        );
      
      case DioExceptionType.badCertificate:
        return NetworkException(
          'SSL certificate error. The connection is not secure.',
          type: NetworkErrorType.sslError,
          originalError: e,
        );
      
      case DioExceptionType.unknown:
      default:
        return NetworkException(
          'Network error: ${e.message ?? "Unknown error"}',
          type: NetworkErrorType.unknown,
          originalError: e,
        );
    }
  }

  /// Get user-friendly error message from any exception
  static String getUserFriendlyMessage(dynamic error) {
    if (error is NetworkException) {
      return error.message;
    } else if (error is DioException) {
      return _convertDioException(error).toString();
    } else if (error is Exception) {
      final message = error.toString();
      // Remove "Exception: " prefix if present
      if (message.startsWith('Exception: ')) {
        return message.substring(11);
      }
      return message;
    } else {
      return error.toString();
    }
  }
}

/// Custom exception for network errors with user-friendly messages
class NetworkException implements Exception {
  final String message;
  final NetworkErrorType type;
  final Exception? originalError;

  NetworkException(
    this.message, {
    required this.type,
    this.originalError,
  });

  @override
  String toString() => message;
}

/// Types of network errors
enum NetworkErrorType {
  timeout,
  connectionError,
  authenticationError,
  notFound,
  serverError,
  cancelled,
  sslError,
  unknown,
}
