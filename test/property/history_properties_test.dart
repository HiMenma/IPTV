import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/repositories/history_repository.dart';
import 'dart:math';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  group('History Repository Property Tests', () {
    // Feature: iptv-player, Property 21: Playing channel records history with timestamp
    test('Property 21: Playing channel records history with timestamp', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        final repository = HistoryRepository();
        final channelId = 'channel-${random.nextInt(1000000)}';
        
        final beforeAdd = await repository.getAll();
        final beforeCount = beforeAdd.length;
        
        final beforeTimestamp = DateTime.now();
        await repository.add(channelId);
        final afterTimestamp = DateTime.now();
        
        final afterAdd = await repository.getAll();
        expect(afterAdd.length, equals(beforeCount + 1), reason: 'History should have one more entry');
        
        final addedEntry = afterAdd.firstWhere((h) => h.channelId == channelId);
        expect(addedEntry.channelId, equals(channelId), reason: 'Channel ID should match');
        expect(
          addedEntry.watchedAt.isAfter(beforeTimestamp.subtract(Duration(seconds: 1))) &&
          addedEntry.watchedAt.isBefore(afterTimestamp.add(Duration(seconds: 1))),
          isTrue,
          reason: 'Timestamp should be approximately now',
        );
      }
    });

    // Feature: iptv-player, Property 22: Browse history displays in reverse chronological order
    test('Property 22: Browse history displays in reverse chronological order', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        final repository = HistoryRepository();
        final numEntries = random.nextInt(10) + 2; // At least 2 entries
        
        // Add multiple entries with small delays
        for (int j = 0; j < numEntries; j++) {
          final channelId = 'channel-$j';
          await repository.add(channelId);
          // Small delay to ensure different timestamps
          await Future.delayed(Duration(milliseconds: 10));
        }
        
        final history = await repository.getAll();
        
        // Verify the list is in reverse chronological order (most recent first)
        for (int j = 0; j < history.length - 1; j++) {
          final current = history[j];
          final next = history[j + 1];
          
          expect(
            current.watchedAt.isAfter(next.watchedAt) || 
            current.watchedAt.isAtSameMomentAs(next.watchedAt),
            isTrue,
            reason: 'History should be in reverse chronological order (most recent first)',
          );
        }
      }
    });

    // Feature: iptv-player, Property 23: Clearing history removes all entries
    test('Property 23: Clearing history removes all entries', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        SharedPreferences.setMockInitialValues({});
        
        final repository = HistoryRepository();
        final numEntries = random.nextInt(10) + 1; // At least 1 entry
        
        // Add multiple entries
        for (int j = 0; j < numEntries; j++) {
          final channelId = 'channel-${random.nextInt(1000000)}';
          await repository.add(channelId);
        }
        
        final beforeClear = await repository.getAll();
        expect(beforeClear.length, greaterThan(0), reason: 'History should have entries before clear');
        
        await repository.clear();
        
        final afterClear = await repository.getAll();
        expect(afterClear.length, equals(0), reason: 'History should be empty after clear');
      }
    });
  });
}
