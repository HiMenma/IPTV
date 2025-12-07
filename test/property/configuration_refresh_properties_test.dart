import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:dio/dio.dart';
import 'package:iptv_player/models/configuration.dart';
import 'package:iptv_player/models/channel.dart';
import 'package:iptv_player/viewmodels/configuration_viewmodel.dart';
import 'package:iptv_player/repositories/configuration_repository.dart';
import 'package:iptv_player/services/xtream_service.dart';
import 'package:iptv_player/services/m3u_service.dart';
import 'package:uuid/uuid.dart';
import 'dart:math';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  group('Configuration Refresh Property Tests', () {
    // Feature: iptv-player, Property 4: Xtream configuration refresh updates channel list
    test('Property 4: Xtream configuration refresh updates channel list', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        // Create a mock Xtream configuration
        final config = Configuration(
          id: 'config-${random.nextInt(1000000)}',
          name: _generateRandomString(random, 10),
          type: ConfigType.xtream,
          credentials: {
            'serverUrl': 'http://example${random.nextInt(1000)}.com',
            'username': _generateRandomString(random, 8),
            'password': _generateRandomString(random, 12),
          },
          createdAt: DateTime.now().subtract(Duration(days: random.nextInt(365))),
          lastRefreshed: null,
        );
        
        final repository = ConfigurationRepository();
        await repository.save(config);
        
        // Create mock services
        final mockXtreamService = _MockXtreamService(random);
        final viewModel = ConfigurationViewModel(
          configRepository: repository,
          xtreamService: mockXtreamService,
        );
        
        await viewModel.loadConfigurations();
        
        // Refresh the configuration
        final channels = await viewModel.refreshConfiguration(config.id);
        
        // Verify channels were returned
        expect(channels, isNotEmpty, reason: 'Refresh should return channels');
        
        // Verify lastRefreshed was updated
        final updatedConfig = await repository.getById(config.id);
        expect(updatedConfig, isNotNull);
        expect(updatedConfig!.lastRefreshed, isNotNull, 
            reason: 'lastRefreshed should be set after refresh');
        expect(updatedConfig.lastRefreshed!.isAfter(config.createdAt), isTrue,
            reason: 'lastRefreshed should be after createdAt');
      }
    });

    // Feature: iptv-player, Property 8: M3U network configuration refresh fetches latest content
    test('Property 8: M3U network configuration refresh fetches latest content', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        // Create a mock M3U network configuration
        final config = Configuration(
          id: 'config-${random.nextInt(1000000)}',
          name: _generateRandomString(random, 10),
          type: ConfigType.m3uNetwork,
          credentials: {
            'url': 'http://example${random.nextInt(1000)}.com/playlist.m3u',
          },
          createdAt: DateTime.now().subtract(Duration(days: random.nextInt(365))),
          lastRefreshed: null,
        );
        
        final repository = ConfigurationRepository();
        await repository.save(config);
        
        // Create mock services
        final mockM3UService = _MockM3UService(random);
        final viewModel = ConfigurationViewModel(
          configRepository: repository,
          m3uService: mockM3UService,
        );
        
        await viewModel.loadConfigurations();
        
        // Refresh the configuration
        final channels = await viewModel.refreshConfiguration(config.id);
        
        // Verify channels were returned
        expect(channels, isNotEmpty, reason: 'Refresh should return channels');
        
        // Verify lastRefreshed was updated
        final updatedConfig = await repository.getById(config.id);
        expect(updatedConfig, isNotNull);
        expect(updatedConfig!.lastRefreshed, isNotNull, 
            reason: 'lastRefreshed should be set after refresh');
        expect(updatedConfig.lastRefreshed!.isAfter(config.createdAt), isTrue,
            reason: 'lastRefreshed should be after createdAt');
      }
    });
  });
}

// Mock XtreamService that returns random channels
class _MockXtreamService extends XtreamService {
  final Random random;
  
  _MockXtreamService(this.random);
  
  @override
  Future<List<Channel>> getChannels(
    String serverUrl,
    String username,
    String password,
    String configId, {
    bool forceRefresh = false,
  }) async {
    // Generate random number of channels (1-10)
    final channelCount = 1 + random.nextInt(10);
    return List.generate(channelCount, (index) => _generateRandomChannel(random, configId));
  }
}

// Mock M3UService that returns random channels
class _MockM3UService extends M3UService {
  final Random random;
  
  _MockM3UService(this.random);
  
  @override
  Future<List<Channel>> parseNetworkFile(String url, String configId, {bool forceRefresh = false}) async {
    // Generate random number of channels (1-10)
    final channelCount = 1 + random.nextInt(10);
    return List.generate(channelCount, (index) => _generateRandomChannel(random, configId));
  }
}

Channel _generateRandomChannel(Random random, String configId) {
  return Channel(
    id: 'channel-${random.nextInt(1000000)}',
    name: _generateRandomString(random, 15),
    streamUrl: 'http://stream${random.nextInt(1000)}.com/live.m3u8',
    logoUrl: random.nextBool() ? 'http://logo${random.nextInt(1000)}.com/logo.png' : null,
    category: random.nextBool() ? _generateRandomString(random, 8) : null,
    configId: configId,
  );
}

String _generateRandomString(Random random, int length) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  return List.generate(length, (index) => chars[random.nextInt(chars.length)]).join();
}
