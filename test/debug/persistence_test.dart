import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/repositories/favorite_repository.dart';
import 'package:iptv_player/repositories/history_repository.dart';

void main() {
  group('Data Persistence Tests', () {
    setUp(() async {
      // Clear all data before each test
      SharedPreferences.setMockInitialValues({});
    });

    test('Favorites persist across repository instances', () async {
      // Create first repository instance and add favorite
      final repo1 = FavoriteRepository();
      await repo1.add('test_channel_1');
      
      // Verify it was added
      final favorites1 = await repo1.getAll();
      expect(favorites1.length, 1);
      expect(favorites1[0].channelId, 'test_channel_1');
      
      // Create new repository instance (simulating app restart)
      final repo2 = FavoriteRepository();
      final favorites2 = await repo2.getAll();
      
      // Data should still be there
      expect(favorites2.length, 1);
      expect(favorites2[0].channelId, 'test_channel_1');
    });

    test('History persists across repository instances', () async {
      // Create first repository instance and add history
      final repo1 = HistoryRepository();
      await repo1.add('test_channel_1');
      
      // Verify it was added
      final history1 = await repo1.getAll();
      expect(history1.length, 1);
      expect(history1[0].channelId, 'test_channel_1');
      
      // Create new repository instance (simulating app restart)
      final repo2 = HistoryRepository();
      final history2 = await repo2.getAll();
      
      // Data should still be there
      expect(history2.length, 1);
      expect(history2[0].channelId, 'test_channel_1');
    });

    test('Multiple favorites persist', () async {
      final repo1 = FavoriteRepository();
      await repo1.add('channel_1');
      await repo1.add('channel_2');
      await repo1.add('channel_3');
      
      final repo2 = FavoriteRepository();
      final favorites = await repo2.getAll();
      
      expect(favorites.length, 3);
      expect(favorites.map((f) => f.channelId).toList(), 
             containsAll(['channel_1', 'channel_2', 'channel_3']));
    });

    test('Removing favorite persists', () async {
      final repo1 = FavoriteRepository();
      await repo1.add('channel_1');
      await repo1.add('channel_2');
      await repo1.remove('channel_1');
      
      final repo2 = FavoriteRepository();
      final favorites = await repo2.getAll();
      
      expect(favorites.length, 1);
      expect(favorites[0].channelId, 'channel_2');
    });

    test('Check actual SharedPreferences storage', () async {
      final repo = FavoriteRepository();
      await repo.add('test_channel');
      
      // Directly check SharedPreferences
      final prefs = await SharedPreferences.getInstance();
      final stored = prefs.getString('favorites');
      
      expect(stored, isNotNull);
      expect(stored, contains('test_channel'));
      print('Stored data: $stored');
    });
  });
}
