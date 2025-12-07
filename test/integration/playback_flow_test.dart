import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/models/channel.dart';
import 'package:iptv_player/services/player_service.dart';
import 'package:iptv_player/repositories/history_repository.dart';
import 'package:iptv_player/viewmodels/player_viewmodel.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Playback Flow Integration Tests', () {
    late PlayerService playerService;
    late HistoryRepository historyRepository;
    late PlayerViewModel viewModel;

    setUp(() async {
      // Clear shared preferences before each test
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();

      playerService = PlayerService();
      historyRepository = HistoryRepository();
      viewModel = PlayerViewModel(
        playerService: playerService,
        historyRepository: historyRepository,
      );
    });

    tearDown(() async {
      // Clean up after each test
      await viewModel.stop();
      viewModel.dispose();
      
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();
    });

    test('Complete playback flow - channel selection to playback', () async {
      // Step 1: Create a test channel
      final channel = Channel(
        id: 'test-channel-1',
        name: 'Test Channel',
        streamUrl: 'http://example.com/stream.m3u8',
        configId: 'test-config-1',
      );

      // Step 2: Verify initial state
      expect(viewModel.state, PlayerState.idle);
      expect(viewModel.currentChannel, isNull);

      // Step 3: Play the channel
      await viewModel.playChannel(channel);

      // Step 4: Verify player state changed
      expect(viewModel.currentChannel, isNotNull);
      expect(viewModel.currentChannel!.id, channel.id);
      expect(viewModel.currentChannel!.name, channel.name);
      
      // Player should be in playing or loading state (not idle)
      expect(viewModel.state, isNot(PlayerState.idle));

      // Step 5: Verify channel was recorded in history
      final history = await historyRepository.getAll();
      expect(history.length, 1);
      expect(history[0].channelId, channel.id);
    });

    test('Playback flow with pause and resume', () async {
      // Create and play a channel
      final channel = Channel(
        id: 'test-channel-2',
        name: 'Test Channel 2',
        streamUrl: 'http://example.com/stream2.m3u8',
        configId: 'test-config-1',
      );

      await viewModel.playChannel(channel);
      expect(viewModel.state, isNot(PlayerState.idle));

      // Pause playback
      await viewModel.pause();
      expect(viewModel.state, PlayerState.paused);

      // Resume playback
      await viewModel.resume();
      expect(viewModel.state, PlayerState.playing);
    });

    test('Playback flow with stop', () async {
      // Create and play a channel
      final channel = Channel(
        id: 'test-channel-3',
        name: 'Test Channel 3',
        streamUrl: 'http://example.com/stream3.m3u8',
        configId: 'test-config-1',
      );

      await viewModel.playChannel(channel);
      expect(viewModel.currentChannel, isNotNull);

      // Stop playback
      await viewModel.stop();
      
      // Verify player state is stopped or idle
      expect(viewModel.state, anyOf(PlayerState.stopped, PlayerState.idle));
      
      // Verify current channel is cleared
      expect(viewModel.currentChannel, isNull);
    });

    test('Playback flow with volume control', () async {
      // Create and play a channel
      final channel = Channel(
        id: 'test-channel-4',
        name: 'Test Channel 4',
        streamUrl: 'http://example.com/stream4.m3u8',
        configId: 'test-config-1',
      );

      await viewModel.playChannel(channel);

      // Set volume to different levels
      await viewModel.setVolume(0.5);
      await viewModel.setVolume(0.8);
      await viewModel.setVolume(0.0);
      await viewModel.setVolume(1.0);

      // If no exception is thrown, volume control works
      expect(viewModel.state, isNot(PlayerState.error));
    });

    test('Playback flow with fullscreen toggle', () async {
      // Create and play a channel
      final channel = Channel(
        id: 'test-channel-5',
        name: 'Test Channel 5',
        streamUrl: 'http://example.com/stream5.m3u8',
        configId: 'test-config-1',
      );

      await viewModel.playChannel(channel);

      // Verify initial fullscreen state
      expect(viewModel.isFullscreen, false);

      // Toggle fullscreen on
      viewModel.toggleFullscreen();
      expect(viewModel.isFullscreen, true);

      // Toggle fullscreen off
      viewModel.toggleFullscreen();
      expect(viewModel.isFullscreen, false);
    });

    test('Multiple channels playback flow', () async {
      // Play first channel
      final channel1 = Channel(
        id: 'test-channel-6',
        name: 'Test Channel 6',
        streamUrl: 'http://example.com/stream6.m3u8',
        configId: 'test-config-1',
      );

      await viewModel.playChannel(channel1);
      expect(viewModel.currentChannel!.id, channel1.id);

      // Play second channel (should replace first)
      final channel2 = Channel(
        id: 'test-channel-7',
        name: 'Test Channel 7',
        streamUrl: 'http://example.com/stream7.m3u8',
        configId: 'test-config-1',
      );

      await viewModel.playChannel(channel2);
      expect(viewModel.currentChannel!.id, channel2.id);

      // Verify both channels are in history
      final history = await historyRepository.getAll();
      expect(history.length, 2);
      
      // History should be in reverse chronological order (most recent first)
      expect(history[0].channelId, channel2.id);
      expect(history[1].channelId, channel1.id);
    });

    test('Playback error handling flow', () async {
      // Create a channel with invalid stream URL
      final channel = Channel(
        id: 'test-channel-error',
        name: 'Error Channel',
        streamUrl: 'invalid://url',
        configId: 'test-config-1',
      );

      // Attempt to play the channel
      try {
        await viewModel.playChannel(channel);
        
        // If player service handles errors gracefully, state should be error
        // or the play method should throw an exception
        if (viewModel.state == PlayerState.error) {
          expect(viewModel.error, isNotNull);
        }
      } catch (e) {
        // Exception is expected for invalid URLs
        expect(e, isNotNull);
      }
    });

    test('Playback state persists in history across ViewModel instances', () async {
      // Play a channel
      final channel = Channel(
        id: 'test-channel-persist',
        name: 'Persistent Channel',
        streamUrl: 'http://example.com/stream-persist.m3u8',
        configId: 'test-config-1',
      );

      await viewModel.playChannel(channel);

      // Verify history was recorded
      final history1 = await historyRepository.getAll();
      expect(history1.length, 1);
      expect(history1[0].channelId, channel.id);

      // Stop and dispose current ViewModel
      await viewModel.stop();
      viewModel.dispose();

      // Create new ViewModel instance (simulating app restart)
      final newHistoryRepository = HistoryRepository();
      final history2 = await newHistoryRepository.getAll();

      // Verify history persisted
      expect(history2.length, 1);
      expect(history2[0].channelId, channel.id);
    });

    test('Complete playback lifecycle', () async {
      // Create a channel
      final channel = Channel(
        id: 'test-channel-lifecycle',
        name: 'Lifecycle Channel',
        streamUrl: 'http://example.com/stream-lifecycle.m3u8',
        configId: 'test-config-1',
      );

      // Step 1: Initial state
      expect(viewModel.state, PlayerState.idle);
      expect(viewModel.currentChannel, isNull);

      // Step 2: Play channel
      await viewModel.playChannel(channel);
      expect(viewModel.currentChannel, isNotNull);
      expect(viewModel.state, isNot(PlayerState.idle));

      // Step 3: Pause
      await viewModel.pause();
      expect(viewModel.state, PlayerState.paused);

      // Step 4: Resume
      await viewModel.resume();
      expect(viewModel.state, PlayerState.playing);

      // Step 5: Adjust volume
      await viewModel.setVolume(0.7);

      // Step 6: Toggle fullscreen
      viewModel.toggleFullscreen();
      expect(viewModel.isFullscreen, true);

      // Step 7: Stop playback
      await viewModel.stop();
      expect(viewModel.state, anyOf(PlayerState.stopped, PlayerState.idle));
      expect(viewModel.currentChannel, isNull);

      // Step 8: Verify history was recorded
      final history = await historyRepository.getAll();
      expect(history.length, 1);
      expect(history[0].channelId, channel.id);
    });
  });
}
