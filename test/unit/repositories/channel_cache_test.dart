import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/repositories/channel_cache_repository.dart';
import 'package:iptv_player/models/channel.dart';

void main() {
  group('ChannelCacheRepository Tests', () {
    late ChannelCacheRepository repository;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      repository = ChannelCacheRepository();
    });

    test('saveChannels and loadChannels work correctly', () async {
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
      await repository.saveChannels('config1', channels);

      // Load channels
      final loaded = await repository.loadChannels('config1');

      expect(loaded, isNotNull);
      expect(loaded!.length, 2);
      expect(loaded[0].id, 'config1:channel1');
      expect(loaded[1].id, 'config1:channel2');
    });

    test('hasCache returns correct value', () async {
      expect(await repository.hasCache('config1'), false);

      final channels = [
        Channel(
          id: 'config1:channel1',
          name: 'Channel 1',
          streamUrl: 'http://example.com/stream1',
          configId: 'config1',
        ),
      ];

      await repository.saveChannels('config1', channels);

      expect(await repository.hasCache('config1'), true);
    });

    test('getCacheTimestamp returns correct timestamp', () async {
      final before = DateTime.now();

      final channels = [
        Channel(
          id: 'config1:channel1',
          name: 'Channel 1',
          streamUrl: 'http://example.com/stream1',
          configId: 'config1',
        ),
      ];

      await repository.saveChannels('config1', channels);

      final timestamp = await repository.getCacheTimestamp('config1');
      final after = DateTime.now();

      expect(timestamp, isNotNull);
      expect(timestamp!.isAfter(before.subtract(const Duration(seconds: 1))), true);
      expect(timestamp.isBefore(after.add(const Duration(seconds: 1))), true);
    });

    test('clearCache removes cache for specific config', () async {
      final channels = [
        Channel(
          id: 'config1:channel1',
          name: 'Channel 1',
          streamUrl: 'http://example.com/stream1',
          configId: 'config1',
        ),
      ];

      await repository.saveChannels('config1', channels);
      expect(await repository.hasCache('config1'), true);

      await repository.clearCache('config1');
      expect(await repository.hasCache('config1'), false);
    });

    test('clearAllCaches removes all caches', () async {
      final channels1 = [
        Channel(
          id: 'config1:channel1',
          name: 'Channel 1',
          streamUrl: 'http://example.com/stream1',
          configId: 'config1',
        ),
      ];

      final channels2 = [
        Channel(
          id: 'config2:channel1',
          name: 'Channel 1',
          streamUrl: 'http://example.com/stream1',
          configId: 'config2',
        ),
      ];

      await repository.saveChannels('config1', channels1);
      await repository.saveChannels('config2', channels2);

      expect(await repository.hasCache('config1'), true);
      expect(await repository.hasCache('config2'), true);

      await repository.clearAllCaches();

      expect(await repository.hasCache('config1'), false);
      expect(await repository.hasCache('config2'), false);
    });

    test('loadChannels returns null for non-existent cache', () async {
      final loaded = await repository.loadChannels('nonexistent');
      expect(loaded, isNull);
    });

    test('getCacheInfo returns correct information', () async {
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

      await repository.saveChannels('config1', channels);

      final info = await repository.getCacheInfo('config1');

      expect(info['hasCache'], true);
      expect(info['channelCount'], 2);
      expect(info['timestamp'], isNotNull);
    });
  });
}
