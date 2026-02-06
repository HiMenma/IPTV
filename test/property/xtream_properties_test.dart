import 'package:flutter_test/flutter_test.dart';
import 'package:dio/dio.dart';
import 'package:iptv_player/services/xtream_service.dart';
import 'package:iptv_player/models/configuration.dart';
import 'package:iptv_player/repositories/configuration_repository.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:math';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  group('Xtream Service Property Tests', () {
    // Feature: iptv-player, Property 1: Xtream authentication with valid credentials succeeds
    test('Property 1: Xtream authentication with valid credentials succeeds', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        // Generate random valid credentials
        final serverUrl = 'http://example${random.nextInt(1000)}.com';
        final username = _generateRandomString(random, 8);
        final password = _generateRandomString(random, 12);
        final configId = 'config-${random.nextInt(1000000)}';
        
        // Generate random channel data
        final channelCount = random.nextInt(50) + 1; // At least 1 channel
        final mockChannels = List.generate(channelCount, (index) => {
          'stream_id': '${random.nextInt(10000)}',
          'name': 'Channel ${_generateRandomString(random, 5)}',
          'stream_icon': 'http://logo${random.nextInt(100)}.com/icon.png',
          'category_name': 'Category ${random.nextInt(10)}',
        });
        
        // Create mock Dio that returns successful response
        final mockDio = _MockDio(
          statusCode: 200,
          responseData: mockChannels,
        );
        
        final service = XtreamService(dio: mockDio);
        
        // Authenticate should succeed and return channels
        final channels = await service.authenticate(serverUrl, username, password, configId);
        
        expect(channels, isNotEmpty, reason: 'Valid credentials should return non-empty channel list');
        expect(channels.length, equals(channelCount), reason: 'Should parse all channels from response');
        
        // Verify all channels have required fields
        for (final channel in channels) {
          expect(channel.name, isNotEmpty, reason: 'Channel should have a name');
          expect(channel.streamUrl, isNotEmpty, reason: 'Channel should have a stream URL');
          expect(channel.configId, equals(configId), reason: 'Channel should be associated with config');
        }
      }
    });

    // Feature: iptv-player, Property 3: Invalid Xtream credentials are rejected
    test('Property 3: Invalid Xtream credentials are rejected', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        // Generate random invalid credentials
        final serverUrl = 'http://example${random.nextInt(1000)}.com';
        final username = _generateRandomString(random, 8);
        final password = _generateRandomString(random, 12);
        final configId = 'config-${random.nextInt(1000000)}';
        
        // Create mock Dio that returns authentication failure
        final statusCode = random.nextBool() ? 401 : 403;
        final mockDio = _MockDio(
          statusCode: statusCode,
          responseData: {'error': 'Invalid credentials'},
        );
        
        final service = XtreamService(dio: mockDio);
        
        // Authentication should fail
        expect(
          () => service.authenticate(serverUrl, username, password, configId),
          throwsA(isA<Exception>().having(
            (e) => e.toString(),
            'message',
            contains('Invalid Xtream credentials'),
          )),
          reason: 'Invalid credentials should throw exception',
        );
      }
    });

    // Feature: iptv-player, Property 2: Successful Xtream authentication persists configuration
    test('Property 2: Successful Xtream authentication persists configuration', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        // Generate random valid credentials
        final serverUrl = 'http://example${random.nextInt(1000)}.com';
        final username = _generateRandomString(random, 8);
        final password = _generateRandomString(random, 12);
        final configId = 'config-${random.nextInt(1000000)}';
        final configName = _generateRandomString(random, 10);
        
        // Generate random channel data
        final channelCount = random.nextInt(50) + 1;
        final mockChannels = List.generate(channelCount, (index) => {
          'stream_id': '${random.nextInt(10000)}',
          'name': 'Channel ${_generateRandomString(random, 5)}',
        });
        
        // Create mock Dio that returns successful response
        final mockDio = _MockDio(
          statusCode: 200,
          responseData: mockChannels,
        );
        
        final service = XtreamService(dio: mockDio);
        final repository = ConfigurationRepository();
        
        // Authenticate successfully
        final channels = await service.authenticate(serverUrl, username, password, configId);
        expect(channels, isNotEmpty, reason: 'Authentication should return channels');
        
        // Create and save configuration
        final config = Configuration(
          id: configId,
          name: configName,
          type: ConfigType.xtream,
          credentials: {
            'serverUrl': serverUrl,
            'username': username,
            'password': password,
          },
          createdAt: DateTime.now(),
          lastRefreshed: DateTime.now(),
        );
        
        await repository.save(config);
        
        // Verify configuration is persisted and retrievable
        final retrieved = await repository.getById(configId);
        
        expect(retrieved, isNotNull, reason: 'Configuration should be retrievable after save');
        expect(retrieved!.id, equals(configId), reason: 'Configuration ID should match');
        expect(retrieved.type, equals(ConfigType.xtream), reason: 'Configuration type should be xtream');
        expect(retrieved.credentials['serverUrl'], equals(serverUrl), reason: 'Server URL should be preserved');
        expect(retrieved.credentials['username'], equals(username), reason: 'Username should be preserved');
        expect(retrieved.credentials['password'], equals(password), reason: 'Password should be preserved');
      }
    });

    // Feature: iptv-player, Property 4: Xtream configuration refresh updates channel list
    test('Property 4: Xtream configuration refresh updates channel list', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        // Generate random credentials
        final serverUrl = 'http://example${random.nextInt(1000)}.com';
        final username = _generateRandomString(random, 8);
        final password = _generateRandomString(random, 12);
        final configId = 'config-${random.nextInt(1000000)}';
        
        // Initial channel list
        final initialChannelCount = random.nextInt(30) + 10;
        final initialChannels = List.generate(initialChannelCount, (index) => {
          'stream_id': '${random.nextInt(10000)}',
          'name': 'Initial Channel ${_generateRandomString(random, 5)}',
        });
        
        // Create initial mock Dio
        final initialMockDio = _MockDio(
          statusCode: 200,
          responseData: initialChannels,
        );
        
        final initialService = XtreamService(dio: initialMockDio);
        final repository = ConfigurationRepository();
        
        // Initial authentication
        final initialChannelList = await initialService.authenticate(serverUrl, username, password, configId);
        
        // Save configuration with initial timestamp
        final initialTimestamp = DateTime.now();
        final config = Configuration(
          id: configId,
          name: 'Test Config',
          type: ConfigType.xtream,
          credentials: {
            'serverUrl': serverUrl,
            'username': username,
            'password': password,
          },
          createdAt: initialTimestamp,
          lastRefreshed: initialTimestamp,
        );
        
        await repository.save(config);
        
        // Wait a moment to ensure timestamp difference
        await Future.delayed(const Duration(milliseconds: 10));
        
        // Updated channel list (different from initial)
        final updatedChannelCount = random.nextInt(30) + 10;
        final updatedChannels = List.generate(updatedChannelCount, (index) => {
          'stream_id': '${random.nextInt(10000)}',
          'name': 'Updated Channel ${_generateRandomString(random, 5)}',
        });
        
        // Create refresh mock Dio
        final refreshMockDio = _MockDio(
          statusCode: 200,
          responseData: updatedChannels,
        );
        
        final refreshService = XtreamService(dio: refreshMockDio);
        
        // Refresh channels
        final refreshedChannelList = await refreshService.getChannels(serverUrl, username, password, configId);
        
        // Update configuration with new timestamp
        final refreshTimestamp = DateTime.now();
        final updatedConfig = Configuration(
          id: config.id,
          name: config.name,
          type: config.type,
          credentials: config.credentials,
          createdAt: config.createdAt,
          lastRefreshed: refreshTimestamp,
        );
        
        await repository.update(updatedConfig);
        
        // Verify refresh updated the channel list
        expect(refreshedChannelList.length, equals(updatedChannelCount), 
          reason: 'Refresh should return updated channel count');
        
        // Verify lastRefreshed timestamp was updated
        final retrieved = await repository.getById(configId);
        expect(retrieved, isNotNull);
        expect(retrieved!.lastRefreshed, isNotNull, reason: 'lastRefreshed should be set');
        expect(retrieved.lastRefreshed!.isAfter(initialTimestamp), isTrue, 
          reason: 'lastRefreshed should be updated to a later time');
      }
    });
  });
}

String _generateRandomString(Random random, int length) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  return List.generate(length, (index) => chars[random.nextInt(chars.length)]).join();
}

/// Mock Dio client for testing
class _MockDio implements Dio {
  final int statusCode;
  final dynamic responseData;

  _MockDio({
    required this.statusCode,
    required this.responseData,
  });

  @override
  Future<Response<T>> get<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    Options? options,
    CancelToken? cancelToken,
    ProgressCallback? onReceiveProgress,
  }) async {
    return Response<T>(
      data: responseData as T,
      statusCode: statusCode,
      requestOptions: RequestOptions(path: path),
    );
  }

  @override
  BaseOptions get options => BaseOptions();

  @override
  set options(BaseOptions _options) {}

  @override
  Interceptors get interceptors => Interceptors();

  @override
  void close({bool force = false}) {}

  @override
  dynamic noSuchMethod(Invocation invocation) => throw UnimplementedError();
}
