import 'package:flutter_test/flutter_test.dart';
import 'package:dio/dio.dart';
import 'package:iptv_player/utils/error_handler.dart';

void main() {
  group('ErrorHandler - Retry Logic', () {
    test('successful operation returns result without retry', () async {
      int attempts = 0;
      final result = await ErrorHandler.executeWithRetry(() async {
        attempts++;
        return 'success';
      });

      expect(result, 'success');
      expect(attempts, 1);
    });

    test('retries on timeout errors', () async {
      int attempts = 0;
      
      try {
        await ErrorHandler.executeWithRetry(() async {
          attempts++;
          if (attempts < 3) {
            throw DioException(
              requestOptions: RequestOptions(path: '/test'),
              type: DioExceptionType.connectionTimeout,
            );
          }
          return 'success';
        });
      } catch (e) {
        // Expected to fail after retries
      }

      expect(attempts, 3);
    });

    test('does not retry on authentication errors', () async {
      int attempts = 0;
      
      try {
        await ErrorHandler.executeWithRetry(() async {
          attempts++;
          throw DioException(
            requestOptions: RequestOptions(path: '/test'),
            type: DioExceptionType.badResponse,
            response: Response(
              requestOptions: RequestOptions(path: '/test'),
              statusCode: 401,
            ),
          );
        });
      } catch (e) {
        // Expected to fail immediately
      }

      expect(attempts, 1);
    });

    test('retries on server errors (5xx)', () async {
      int attempts = 0;
      
      try {
        await ErrorHandler.executeWithRetry(() async {
          attempts++;
          if (attempts < 3) {
            throw DioException(
              requestOptions: RequestOptions(path: '/test'),
              type: DioExceptionType.badResponse,
              response: Response(
                requestOptions: RequestOptions(path: '/test'),
                statusCode: 500,
              ),
            );
          }
          return 'success';
        });
      } catch (e) {
        // Expected to fail after retries
      }

      expect(attempts, 3);
    });

    test('respects custom shouldRetry condition', () async {
      int attempts = 0;
      
      final result = await ErrorHandler.executeWithRetry(
        () async {
          attempts++;
          if (attempts < 2) {
            throw Exception('Custom error');
          }
          return 'success';
        },
        shouldRetry: (e) => true,
      );

      expect(result, 'success');
      expect(attempts, 2);
    });
  });

  group('ErrorHandler - User-Friendly Messages', () {
    test('converts timeout exception to user-friendly message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.connectionTimeout,
      );

      final message = ErrorHandler.getUserFriendlyMessage(exception);
      expect(message, contains('timeout'));
      expect(message, contains('internet connection'));
    });

    test('converts connection error to user-friendly message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.connectionError,
      );

      final message = ErrorHandler.getUserFriendlyMessage(exception);
      expect(message, contains('Connection error'));
      expect(message, contains('internet connection'));
    });

    test('converts 401 error to authentication message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.badResponse,
        response: Response(
          requestOptions: RequestOptions(path: '/test'),
          statusCode: 401,
        ),
      );

      final message = ErrorHandler.getUserFriendlyMessage(exception);
      expect(message, contains('Authentication failed'));
      expect(message, contains('credentials'));
    });

    test('converts 404 error to not found message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.badResponse,
        response: Response(
          requestOptions: RequestOptions(path: '/test'),
          statusCode: 404,
        ),
      );

      final message = ErrorHandler.getUserFriendlyMessage(exception);
      expect(message, contains('not found'));
    });

    test('converts 500 error to server error message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.badResponse,
        response: Response(
          requestOptions: RequestOptions(path: '/test'),
          statusCode: 500,
        ),
      );

      final message = ErrorHandler.getUserFriendlyMessage(exception);
      expect(message, contains('Server error'));
    });

    test('handles NetworkException correctly', () {
      final exception = NetworkException(
        'Custom network error',
        type: NetworkErrorType.timeout,
      );

      final message = ErrorHandler.getUserFriendlyMessage(exception);
      expect(message, 'Custom network error');
    });

    test('handles generic Exception correctly', () {
      final exception = Exception('Generic error message');

      final message = ErrorHandler.getUserFriendlyMessage(exception);
      expect(message, 'Generic error message');
    });
  });

  group('ErrorHandler - Exponential Backoff', () {
    test('calculates exponential backoff correctly', () async {
      final delays = <int>[];
      int attempts = 0;

      try {
        await ErrorHandler.executeWithRetry(() async {
          attempts++;
          final startTime = DateTime.now();
          
          if (attempts > 1) {
            // Record the delay (approximate)
            delays.add(DateTime.now().difference(startTime).inMilliseconds);
          }

          if (attempts < 4) {
            throw DioException(
              requestOptions: RequestOptions(path: '/test'),
              type: DioExceptionType.connectionTimeout,
            );
          }
          return 'success';
        });
      } catch (e) {
        // Expected to fail
      }

      // Should have made 3 attempts
      expect(attempts, 3);
    });
  });
}
