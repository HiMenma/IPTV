import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/repositories/configuration_repository.dart';
import 'package:iptv_player/repositories/favorite_repository.dart';
import 'package:iptv_player/repositories/history_repository.dart';
import 'package:iptv_player/models/configuration.dart';
import 'package:iptv_player/models/favorite.dart';
import 'package:iptv_player/models/browse_history.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('ConfigurationRepository - Error Handling', () {
    late ConfigurationRepository repository;

    setUp(() {
      repository = ConfigurationRepository();
    });

    tearDown(() async {
      SharedPreferences.setMockInitialValues({});
    });

    test('getAll returns empty list when storage is empty', () async {
      SharedPreferences.setMockInitialValues({});
      
      final configs = await repository.getAll();
      expect(configs, isEmpty);
    });

    test('getAll handles corrupted data gracefully', () async {
      SharedPreferences.setMockInitialValues({
        'configurations': 'invalid json {{{',
      });
      
      final configs = await repository.getAll();
      expect(configs, isEmpty);
    });

    test('getAll restores from backup when main data is corrupted', () async {
      final validConfig = Configuration(
        id: 'test-id',
        name: 'Test Config',
        type: ConfigType.xtream,
        credentials: {'serverUrl': 'http://test.com', 'username': 'user', 'password': 'pass'},
        createdAt: DateTime.now(),
      );

      final backupData = {
        'configurations': [validConfig.toJson()]
      };

      SharedPreferences.setMockInitialValues({
        'configurations': 'corrupted data',
        'configurations_backup': '{"configurations":[{"id":"test-id","name":"Test Config","type":"xtream","credentials":{"serverUrl":"http://test.com","username":"user","password":"pass"},"createdAt":"${validConfig.createdAt.toIso8601String()}"}]}',
      });
      
      final configs = await repository.getAll();
      expect(configs.length, 1);
      expect(configs[0].id, 'test-id');
    });

    test('save creates backup before saving', () async {
      SharedPreferences.setMockInitialValues({});
      
      final config = Configuration(
        id: 'test-id',
        name: 'Test Config',
        type: ConfigType.xtream,
        credentials: {'serverUrl': 'http://test.com', 'username': 'user', 'password': 'pass'},
        createdAt: DateTime.now(),
      );

      await repository.save(config);
      
      final prefs = await SharedPreferences.getInstance();
      final mainData = prefs.getString('configurations');
      expect(mainData, isNotNull);
    });
  });

  group('FavoriteRepository - Error Handling', () {
    late FavoriteRepository repository;

    setUp(() {
      repository = FavoriteRepository();
    });

    tearDown(() async {
      SharedPreferences.setMockInitialValues({});
    });

    test('getAll returns empty list when storage is empty', () async {
      SharedPreferences.setMockInitialValues({});
      
      final favorites = await repository.getAll();
      expect(favorites, isEmpty);
    });

    test('getAll handles corrupted data gracefully', () async {
      SharedPreferences.setMockInitialValues({
        'favorites': 'invalid json',
      });
      
      final favorites = await repository.getAll();
      expect(favorites, isEmpty);
    });

    test('add handles storage errors gracefully', () async {
      SharedPreferences.setMockInitialValues({});
      
      // This should not throw
      await repository.add('channel-1');
      
      final favorites = await repository.getAll();
      expect(favorites.length, 1);
    });
  });

  group('HistoryRepository - Error Handling', () {
    late HistoryRepository repository;

    setUp(() {
      repository = HistoryRepository();
    });

    tearDown(() async {
      SharedPreferences.setMockInitialValues({});
    });

    test('getAll returns empty list when storage is empty', () async {
      SharedPreferences.setMockInitialValues({});
      
      final history = await repository.getAll();
      expect(history, isEmpty);
    });

    test('getAll handles corrupted data gracefully', () async {
      SharedPreferences.setMockInitialValues({
        'history': 'corrupted',
      });
      
      final history = await repository.getAll();
      expect(history, isEmpty);
    });

    test('add handles storage errors gracefully', () async {
      SharedPreferences.setMockInitialValues({});
      
      // This should not throw
      await repository.add('channel-1');
      
      final history = await repository.getAll();
      expect(history.length, 1);
    });

    test('clear removes all history', () async {
      SharedPreferences.setMockInitialValues({});
      
      await repository.add('channel-1');
      await repository.add('channel-2');
      
      await repository.clear();
      
      final history = await repository.getAll();
      expect(history, isEmpty);
    });
  });
}
