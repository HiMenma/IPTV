import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/repositories/favorite_repository.dart';
import 'dart:math';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  group('Favorite Repository Property Tests', () {
    // Feature: iptv-player, Property 18: Marking channel as favorite adds to favorites list
    test('Property 18: Marking channel as favorite adds to favorites list', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        final repository = FavoriteRepository();
        final channelId = 'channel-${random.nextInt(1000000)}';
        
        final beforeAdd = await repository.isFavorite(channelId);
        expect(beforeAdd, isFalse, reason: 'Channel should not be favorite before adding');
        
        await repository.add(channelId);
        
        final afterAdd = await repository.isFavorite(channelId);
        expect(afterAdd, isTrue, reason: 'Channel should be favorite after adding');
        
        final allFavorites = await repository.getAll();
        expect(
          allFavorites.any((f) => f.channelId == channelId),
          isTrue,
          reason: 'Channel should appear in favorites list',
        );
      }
    });

    // Feature: iptv-player, Property 19: Unmarking favorite removes from favorites list
    test('Property 19: Unmarking favorite removes from favorites list', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        final repository = FavoriteRepository();
        final channelId = 'channel-${random.nextInt(1000000)}';
        
        // First add the favorite
        await repository.add(channelId);
        final beforeRemove = await repository.isFavorite(channelId);
        expect(beforeRemove, isTrue, reason: 'Channel should be favorite before removing');
        
        // Then remove it
        await repository.remove(channelId);
        
        final afterRemove = await repository.isFavorite(channelId);
        expect(afterRemove, isFalse, reason: 'Channel should not be favorite after removing');
        
        final allFavorites = await repository.getAll();
        expect(
          allFavorites.any((f) => f.channelId == channelId),
          isFalse,
          reason: 'Channel should not appear in favorites list after removal',
        );
      }
    });
  });
}
