import 'dart:async';
import 'dart:io';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

enum NetworkErrorType {
  connection,
  timeout,
  serverError,
  unauthorized,
  forbidden,
  notFound,
  unknown,
}

class NetworkException implements Exception {
  final String message;
  final NetworkErrorType type;
  final int? statusCode;

  NetworkException(this.message, {required this.type, this.statusCode});

  @override
  String toString() => message;
}

class ErrorHandler {
  /// Execute an async network call with retry logic and error mapping
  static Future<T> executeWithRetry<T>(
    Future<T> Function() action, {
    int maxRetries = 3,
    Duration retryDelay = const Duration(seconds: 2),
  }) async {
    int retryCount = 0;
    while (true) {
      try {
        return await action();
      } on DioException catch (e) {
        if (retryCount >= maxRetries || !_isRetryable(e)) {
          throw _mapDioError(e);
        }
        retryCount++;
        debugPrint('Network error (Attempt $retryCount/$maxRetries): ${e.message}. Retrying in ${retryDelay.inSeconds}s...');
        await Future.delayed(retryDelay);
      } catch (e) {
        if (e is NetworkException) rethrow;
        throw NetworkException(e.toString(), type: NetworkErrorType.unknown);
      }
    }
  }

  /// Get a user-friendly error message from any exception
  static String getUserFriendlyMessage(dynamic error) {
    if (error is NetworkException) {
      switch (error.type) {
        case NetworkErrorType.connection:
          return 'No internet connection. Please check your network and try again.';
        case NetworkErrorType.timeout:
          return 'The connection timed out. The server might be slow or unreachable.';
        case NetworkErrorType.unauthorized:
          return 'Access denied. Please check your credentials or subscription.';
        case NetworkErrorType.forbidden:
          return 'Forbidden. You do not have permission to access this resource.';
        case NetworkErrorType.notFound:
          return 'Resource not found. The channel list might have moved.';
        case NetworkErrorType.serverError:
          return 'Server error (HTTP ${error.statusCode}). Please try again later.';
        case NetworkErrorType.unknown:
          return 'An unexpected network error occurred: ${error.message}';
      }
    } else if (error is SocketException) {
      return 'Network error: Failed to connect to the server.';
    } else if (error is TimeoutException) {
      return 'Operation timed out. Please try again.';
    }
    
    final msg = error.toString();
    if (msg.contains('not correctly configured') || msg.contains('12939')) {
      return 'Server error (12939): Possible byte range or content length issue.';
    }
    
    return 'An error occurred: $error';
  }

  static NetworkException _mapDioError(DioException e) {
    String message = e.message ?? 'Unknown network error';
    
    if (e.type == DioExceptionType.connectionTimeout || 
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout) {
      return NetworkException('Connection timeout', type: NetworkErrorType.timeout);
    }
    
    if (e.type == DioExceptionType.connectionError) {
      return NetworkException('Failed to connect to server', type: NetworkErrorType.connection);
    }

    if (e.response != null) {
      int status = e.response!.statusCode ?? 0;
      if (status == 401) return NetworkException('Unauthorized', type: NetworkErrorType.unauthorized, statusCode: status);
      if (status == 403) return NetworkException('Forbidden', type: NetworkErrorType.forbidden, statusCode: status);
      if (status == 404) return NetworkException('Not Found', type: NetworkErrorType.notFound, statusCode: status);
      if (status >= 500) return NetworkException('Server Error', type: NetworkErrorType.serverError, statusCode: status);
    }

    return NetworkException(message, type: NetworkErrorType.unknown);
  }

  static bool _isRetryable(DioException e) {
    return e.type != DioExceptionType.cancel &&
           e.response?.statusCode != 401 &&
           e.response?.statusCode != 403 &&
           e.response?.statusCode != 404;
  }
}
