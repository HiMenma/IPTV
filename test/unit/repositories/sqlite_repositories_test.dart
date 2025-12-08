import 'package:flutter_test/flutter_test.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:iptv_player/database/database_helper.dart';
import 'package:iptv_player/repositories/favorite_repository_sqlite.dart';
import 'package:iptv_player/repositories/history_repository_sqlite.dart';
import 'package:iptv_player/repositories/channel_cache_repository_sqlite.dart';
import 'package:iptv_player/models/channel.dart';

void main() {
  // Initialize FFI for testing
  setUpAll(() {
    sqfliteFfiInit();
    databaseFactory = databaseFactoryFfi;
  });

  setUp(() async {
    // Delete database before each test
    await DatabaseHelper.instance.deleteDatabase();
  });

  tearDown(() async {
    // Clean up after each test
    await DatabaseHelper.instance.close();
  });

  group('FavoriteRepositorySQLite Tests', () {
    test('add and getAll work correctly', () async {
      final repo = FavoriteRepositorySQLite();

      // Add favorites
      await repo.add('channel1');
      await repo.add('channel2');

      // Get all
      final favorites = await repo.getAll();

      expect(favorites.length, 2);
      expect(favorites[0].channelId, 'channel2'); // Most recent first
      expect(favorites[1].channelId, 'channel1');
    });

    test('isFavorite works correctly', () async {
      final repo = FavoriteRepositorySQLite();

      expect(await repo.isFavorite('channel1'), false);

      await repo.add('channel1');

      expect(await repo.isFavorite('channel1'), true);
    });

    test('remove works correctly', () async {
      final repo = FavoriteRepositorySQLite();

      await repo.add('channel1');
      expect(await repo.isFavorite('channel1'), true);

      await repo.remove('channel1');
      expect(await repo.isFavorite('channel1'), false);
    });

    test('clear works correctly', () async {
      final repo = FavoriteRepositorySQLite();

      await repo.add('channel1');
      await repo.add('channel2');

      await repo.clear();

      final favorites = await repo.getAll();
      expect(favorites.length, 0);
    });

    test('getCount works correctly', () async {
      final repo = FavoriteRepositorySQLite();

      expect(await repo.getCount(), 0);

      await repo.add('channel1');
      await repo.add('channel2');

      expect(await repo.getCount(), 2);
    });
  });

  group('HistoryRepositorySQLite Tests', () {
    test('add and getAll work correctly', () async {
      final repo = HistoryRepositorySQLite();

      // Add history
      await repo.add('channel1');
      await Future.delayed(const Duration(milliseconds: 10));
      await repo.add('channel2');

      // Get all
      final history = await repo.getAll();

      expect(history.length, 2);
      expect(history[0].channelId, 'channel2'); // Most recent first
      expect(history[1].channelId, 'channel1');
    });

    test('clear works correctly', () async {
      final repo = HistoryRepositorySQLite();

      await repo.add('channel1');
      await repo.add('channel2');

      await repo.clear();

      final history = await repo.getAll();
      expect(history.length, 0);
    });

    test('getRecent works correctly', () async {
      final repo = HistoryRepositorySQLite();

      await repo.add('channel1');
      await repo.add('channel2');
      await repo.add('channel3');

      final recent = await repo.getRecent(2);

      expect(recent.length, 2);
      expect(recent[0].channelId, 'channel3');
      expect(recent[1].channelId, 'channel2');
    });
  });

  group('ChannelCacheRepositorySQLite Tests', () {
    test('saveChannels and loadChannels work correctly', () async {
      final repo = ChannelCacheRepositorySQLite();

      final channels = [
        Channel(
          id: 'config1:channel1',
          name: 'Channel 1',
          streamUrl: 'http://example.com/stream1',
          configId: 'config1',
        ),
        Channel(
          id: 'config1:channel2',
          name: 'Channel 2',
          streamUrl: 'http://example.com/stream2',
          configId: 'config1',
        ),
      ];

      // Save channels
      await repo.saveChannels('config1', channels);

      // Load channels
      final loaded = await repo.loadChannels('config1');

      expect(loaded, isNotNull);
      expect(loaded!.length, 2);
      expect(loaded[0].id, 'config1:channel1');
      expect(loaded[1].id, 'config1:channel2');
    });

    test('hasCache works correctly', () async {
      final repo = ChannelCacheRepositorySQLite();

      expect(await repo.hasCache('config1'), false);

      final channels = [
        Channel(
          id: 'config1:channel1',
          name: 'Channel 1',
          streamUrl: 'http://example.com/stream1',
          configId: 'config1',
        ),
      ];

      await repo.saveChannels('config1', channels);

      expect(await repo.hasCache('config1'), true);
    });

    test('clearCache works correctly', () async {
      final repo = ChannelCacheRepositorySQLite();

      final channels = [
        Channel(
          id: 'config1:channel1',
          name: 'Channel 1',
          streamUrl: 'http://example.com/stream1',
          configId: 'config1',
        ),
      ];

      await repo.saveChannels('config1', channels);
      expect(await repo.hasCache('config1'), true);

      await repo.clearCache('config1');
      expect(await repo.hasCache('config1'), false);
    });
  });
}
