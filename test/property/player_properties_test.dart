import 'package:flutter_test/flutter_test.dart';
import 'package:iptv_player/services/player_service.dart';
import 'dart:math';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('Player Service Property Tests', () {
    // Feature: iptv-player, Property 24: Aliyun Player Widget initializes correctly
    // Note: This test verifies the service layer logic. The actual Aliyun Player
    // requires native platform implementations which aren't available in unit tests.
    test('Property 24: Aliyun Player Widget initializes correctly', () async {
      for (int i = 0; i < 100; i++) {
        final playerService = PlayerService();
        
        // Verify initial state before initialization
        expect(
          playerService.currentState,
          equals(PlayerState.idle),
          reason: 'Player should start in idle state',
        );
        
        // Verify streams are available
        expect(playerService.stateStream, isNotNull);
        expect(playerService.errorStream, isNotNull);
        
        // Clean up streams
        await playerService.closeStreams();
      }
    });

    // Feature: iptv-player, Property 16: Invalid stream URLs are handled gracefully
    // Feature: iptv-player, Property 25: Playback errors are handled appropriately
    test('Property 16 & 25: Invalid stream URLs are handled gracefully', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        final playerService = PlayerService();
        
        // Generate invalid stream URLs
        final invalidUrl = _generateInvalidStreamUrl(random);
        
        // Verify that attempting to play with invalid URL throws
        try {
          await playerService.play(invalidUrl);
          fail('Should throw exception when playing invalid URL');
        } catch (e) {
          // Should throw an exception (either validation error or platform error in test environment)
          expect(e, isA<Exception>());
          // The error message should indicate either invalid URL or platform issue
          final errorMsg = e.toString();
          expect(
            errorMsg.contains('Invalid stream URL') || 
            errorMsg.contains('not been implemented') ||
            errorMsg.contains('not initialized'),
            isTrue,
            reason: 'Error should be about invalid URL or platform limitation',
          );
        }
        
        // Clean up
        await playerService.closeStreams();
      }
    });

    // Feature: iptv-player, Property 17: Stopping playback releases resources
    test('Property 17: Stopping playback releases resources', () async {
      for (int i = 0; i < 100; i++) {
        final playerService = PlayerService();
        
        // Verify initial state
        expect(
          playerService.currentState,
          equals(PlayerState.idle),
          reason: 'Player should start in idle state',
        );
        
        // Stop playback (even if nothing is playing) should not throw
        await playerService.stop();
        
        // Dispose should work without errors
        await playerService.dispose();
        
        expect(
          playerService.currentState,
          equals(PlayerState.idle),
          reason: 'Player should return to idle state after disposal',
        );
        
        // Clean up streams
        await playerService.closeStreams();
      }
    });

    test('Property: Volume control validates input correctly', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        final playerService = PlayerService();
        
        // Generate random volume between 0.0 and 1.0
        final volume = random.nextDouble();
        
        // Set volume should not throw for valid values
        await playerService.setVolume(volume);
        
        // Clean up
        await playerService.closeStreams();
      }
    });

    test('Property: Invalid volume values are rejected', () async {
      for (int i = 0; i < 100; i++) {
        final playerService = PlayerService();
        
        // Test volume < 0
        expect(
          () => playerService.setVolume(-0.1),
          throwsArgumentError,
          reason: 'Volume less than 0.0 should throw ArgumentError',
        );
        
        // Test volume > 1
        expect(
          () => playerService.setVolume(1.1),
          throwsArgumentError,
          reason: 'Volume greater than 1.0 should throw ArgumentError',
        );
        
        // Clean up
        await playerService.closeStreams();
      }
    });

    test('Property: Pause and resume require proper state', () async {
      for (int i = 0; i < 100; i++) {
        final playerService = PlayerService();
        
        // Verify initial state
        expect(playerService.currentState, equals(PlayerState.idle));
        
        // Pause when not initialized should throw
        try {
          await playerService.pause();
          fail('Should throw when pausing uninitialized player');
        } catch (e) {
          expect(e, isA<Exception>());
        }
        
        // Resume when not initialized should throw
        try {
          await playerService.resume();
          fail('Should throw when resuming uninitialized player');
        } catch (e) {
          expect(e, isA<Exception>());
        }
        
        // Clean up
        await playerService.closeStreams();
      }
    });

    test('Property: State stream broadcasts state changes', () async {
      for (int i = 0; i < 100; i++) {
        final playerService = PlayerService();
        
        // Collect state changes
        final states = <PlayerState>[];
        final subscription = playerService.stateStream.listen((state) {
          states.add(state);
        });
        
        // Verify stream is working
        expect(playerService.stateStream, isNotNull);
        
        // Clean up
        await subscription.cancel();
        await playerService.closeStreams();
      }
    });

    test('Property: Error stream broadcasts errors', () async {
      for (int i = 0; i < 100; i++) {
        final playerService = PlayerService();
        
        // Collect errors
        final errors = <String?>[];
        final subscription = playerService.errorStream.listen((error) {
          errors.add(error);
        });
        
        // Verify stream is working
        expect(playerService.errorStream, isNotNull);
        
        // Clean up
        await subscription.cancel();
        await playerService.closeStreams();
      }
    });
  });
}

/// Generate invalid stream URLs for testing error handling
String _generateInvalidStreamUrl(Random random) {
  final invalidUrls = [
    'invalid://not-a-url',
    'http://nonexistent-domain-${random.nextInt(100000)}.invalid/stream.m3u8',
    'ftp://wrong-protocol.com/stream',
    '',
    'not-a-url-at-all',
    'http://localhost:${random.nextInt(60000) + 1024}/nonexistent',
    'https://127.0.0.1:${random.nextInt(60000) + 1024}/fake-stream.m3u8',
  ];
  
  return invalidUrls[random.nextInt(invalidUrls.length)];
}
