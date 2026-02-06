import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/models/configuration.dart';
import 'package:iptv_player/models/favorite.dart';
import 'package:iptv_player/models/browse_history.dart';
import 'package:iptv_player/repositories/configuration_repository.dart';
import 'package:iptv_player/repositories/favorite_repository.dart';
import 'package:iptv_player/repositories/history_repository.dart';
import 'package:iptv_player/viewmodels/configuration_viewmodel.dart';
import 'dart:math';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  group('Application Startup Property Tests', () {
    // Feature: iptv-player, Property 13: Application startup loads all saved data
    test('Property 13: Application startup loads all saved data', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        // Generate random number of configurations (1-5)
        final configCount = 1 + random.nextInt(5);
        final configurations = List.generate(
          configCount,
          (index) => _generateRandomConfiguration(random),
        );
        
        // Generate random number of favorites (0-10)
        final favoriteCount = random.nextInt(11);
        final favorites = List.generate(
          favoriteCount,
          (index) => Favorite(
            channelId: 'channel-${random.nextInt(1000000)}',
            addedAt: DateTime.now().subtract(Duration(hours: random.nextInt(100))),
          ),
        );
        
        // Generate random number of history entries (0-10)
        final historyCount = random.nextInt(11);
        final history = List.generate(
          historyCount,
          (index) => BrowseHistory(
            channelId: 'channel-${random.nextInt(1000000)}',
            watchedAt: DateTime.now().subtract(Duration(hours: random.nextInt(100))),
          ),
        );
        
        // Save all data to repositories
        final configRepo = ConfigurationRepository();
        final favoriteRepo = FavoriteRepository();
        final historyRepo = HistoryRepository();
        
        for (final config in configurations) {
          await configRepo.save(config);
        }
        
        for (final favorite in favorites) {
          await favoriteRepo.add(favorite.channelId);
        }
        
        for (final historyEntry in history) {
          await historyRepo.add(historyEntry.channelId);
        }
        
        // Simulate application startup by loading data
        final configViewModel = ConfigurationViewModel(
          configRepository: configRepo,
        );
        
        await configViewModel.loadConfigurations();
        
        // Verify all configurations were loaded
        expect(configViewModel.configurations.length, equals(configCount),
            reason: 'All saved configurations should be loaded on startup');
        
        for (final config in configurations) {
          final loaded = configViewModel.configurations.firstWhere(
            (c) => c.id == config.id,
            orElse: () => throw Exception('Configuration ${config.id} not found'),
          );
          expect(loaded, equals(config),
              reason: 'Loaded configuration should match saved configuration');
        }
        
        // Verify favorites were persisted
        final loadedFavorites = await favoriteRepo.getAll();
        expect(loadedFavorites.length, equals(favoriteCount),
            reason: 'All saved favorites should be loaded on startup');
        
        // Verify history was persisted
        final loadedHistory = await historyRepo.getAll();
        expect(loadedHistory.length, equals(historyCount),
            reason: 'All saved history entries should be loaded on startup');
      }
    });
  });
}

Configuration _generateRandomConfiguration(Random random) {
  final types = ConfigType.values;
  final type = types[random.nextInt(types.length)];
  
  Map<String, dynamic> credentials;
  switch (type) {
    case ConfigType.xtream:
      credentials = {
        'serverUrl': 'http://example${random.nextInt(1000)}.com',
        'username': _generateRandomString(random, 8),
        'password': _generateRandomString(random, 12),
      };
      break;
    case ConfigType.m3uLocal:
      credentials = {
        'filePath': '/path/to/file${random.nextInt(1000)}.m3u',
      };
      break;
    case ConfigType.m3uNetwork:
      credentials = {
        'url': 'http://example${random.nextInt(1000)}.com/playlist.m3u',
      };
      break;
  }
  
  final createdAt = DateTime.now().subtract(Duration(days: random.nextInt(365)));
  final lastRefreshed = random.nextBool() 
      ? createdAt.add(Duration(hours: random.nextInt(24)))
      : null;
  
  return Configuration(
    id: 'config-${random.nextInt(1000000)}',
    name: _generateRandomString(random, 10),
    type: type,
    credentials: credentials,
    createdAt: createdAt,
    lastRefreshed: lastRefreshed,
  );
}

String _generateRandomString(Random random, int length) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  return List.generate(length, (index) => chars[random.nextInt(chars.length)]).join();
}
