import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/models/configuration.dart';
import 'package:iptv_player/repositories/configuration_repository.dart';
import 'dart:math';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  group('Configuration Repository Property Tests', () {
    // Feature: iptv-player, Property 9: Configuration creation persists data
    test('Property 9: Configuration creation persists data', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        final repository = ConfigurationRepository();
        final config = _generateRandomConfiguration(random);
        
        await repository.save(config);
        final retrieved = await repository.getById(config.id);
        
        expect(retrieved, isNotNull, reason: 'Configuration should be retrievable after save');
        expect(retrieved, equals(config), reason: 'Retrieved configuration should equal saved configuration');
      }
    });

    // Feature: iptv-player, Property 10: Configuration rename preserves all other fields
    test('Property 10: Configuration rename preserves all other fields', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        final repository = ConfigurationRepository();
        final originalConfig = _generateRandomConfiguration(random);
        await repository.save(originalConfig);
        
        final newName = _generateRandomString(random, 10);
        final renamedConfig = Configuration(
          id: originalConfig.id,
          name: newName,
          type: originalConfig.type,
          credentials: originalConfig.credentials,
          createdAt: originalConfig.createdAt,
          lastRefreshed: originalConfig.lastRefreshed,
        );
        
        await repository.update(renamedConfig);
        final retrieved = await repository.getById(originalConfig.id);
        
        expect(retrieved, isNotNull);
        expect(retrieved!.name, equals(newName), reason: 'Name should be updated');
        expect(retrieved.id, equals(originalConfig.id), reason: 'ID should remain unchanged');
        expect(retrieved.type, equals(originalConfig.type), reason: 'Type should remain unchanged');
        expect(retrieved.credentials, equals(originalConfig.credentials), reason: 'Credentials should remain unchanged');
        expect(retrieved.createdAt, equals(originalConfig.createdAt), reason: 'CreatedAt should remain unchanged');
        expect(retrieved.lastRefreshed, equals(originalConfig.lastRefreshed), reason: 'LastRefreshed should remain unchanged');
      }
    });

    // Feature: iptv-player, Property 11: Configuration deletion removes from storage
    test('Property 11: Configuration deletion removes from storage', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        final repository = ConfigurationRepository();
        final config = _generateRandomConfiguration(random);
        
        await repository.save(config);
        final beforeDelete = await repository.getById(config.id);
        expect(beforeDelete, isNotNull, reason: 'Configuration should exist before deletion');
        
        await repository.delete(config.id);
        final afterDelete = await repository.getById(config.id);
        
        expect(afterDelete, isNull, reason: 'Configuration should not be retrievable after deletion');
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
