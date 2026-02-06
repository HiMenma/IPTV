import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:iptv_player/models/channel.dart';
import 'package:iptv_player/services/player_service.dart';
import 'package:iptv_player/viewmodels/player_viewmodel.dart';
import 'dart:math';
import 'dart:async';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
  });

  group('Playback State Management Property Tests', () {
    // Feature: iptv-player, Property 14: Channel selection initiates playback
    test('Property 14: Channel selection initiates playback', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        final channel = _generateRandomChannel(random);
        
        // Create mock player service
        final mockPlayerService = _MockPlayerService();
        final viewModel = PlayerViewModel(
          playerService: mockPlayerService,
        );
        
        // Play the channel
        await viewModel.playChannel(channel);
        
        // Verify playback was initiated
        expect(viewModel.currentChannel, equals(channel),
            reason: 'Current channel should be set after playChannel is called');
        expect(viewModel.state, isIn([PlayerState.preparing, PlayerState.playing, PlayerState.prepared]),
            reason: 'Player state should indicate playback is initiated');
        expect(mockPlayerService.playWasCalled, isTrue,
            reason: 'Player service play method should be called');
        
        // Clean up
        await viewModel.stop();
        viewModel.dispose();
      }
    });

    // Feature: iptv-player, Property 15: Playback displays controls
    test('Property 15: Playback displays controls', () async {
      final random = Random();
      
      for (int i = 0; i < 100; i++) {
        final channel = _generateRandomChannel(random);
        
        // Create mock player service
        final mockPlayerService = _MockPlayerService();
        final viewModel = PlayerViewModel(
          playerService: mockPlayerService,
        );
        
        // Play the channel
        await viewModel.playChannel(channel);
        
        // Verify control methods are available and functional
        // Test pause control
        await viewModel.pause();
        expect(mockPlayerService.pauseWasCalled, isTrue,
            reason: 'Pause control should be available during playback');
        
        // Test resume control
        await viewModel.resume();
        expect(mockPlayerService.resumeWasCalled, isTrue,
            reason: 'Resume control should be available during playback');
        
        // Test volume control
        final volume = random.nextDouble();
        await viewModel.setVolume(volume);
        expect(mockPlayerService.lastVolume, equals(volume),
            reason: 'Volume control should be available during playback');
        
        // Test fullscreen toggle
        final initialFullscreen = viewModel.isFullscreen;
        viewModel.toggleFullscreen();
        expect(viewModel.isFullscreen, equals(!initialFullscreen),
            reason: 'Fullscreen control should be available during playback');
        
        // Clean up
        await viewModel.stop();
        viewModel.dispose();
      }
    });
  });
}

// Mock PlayerService for testing
class _MockPlayerService extends PlayerService {
  bool playWasCalled = false;
  bool pauseWasCalled = false;
  bool resumeWasCalled = false;
  bool stopWasCalled = false;
  double? lastVolume;
  
  final StreamController<PlayerState> _stateController = StreamController<PlayerState>.broadcast();
  final StreamController<String?> _errorController = StreamController<String?>.broadcast();
  
  PlayerState _currentState = PlayerState.idle;
  
  @override
  Stream<PlayerState> get stateStream => _stateController.stream;
  
  @override
  Stream<String?> get errorStream => _errorController.stream;
  
  @override
  PlayerState get currentState => _currentState;
  
  @override
  Future<void> initialize() async {
    _currentState = PlayerState.idle;
    _stateController.add(_currentState);
  }
  
  @override
  Future<void> play(String streamUrl) async {
    playWasCalled = true;
    _currentState = PlayerState.playing;
    _stateController.add(_currentState);
  }
  
  @override
  Future<void> pause() async {
    pauseWasCalled = true;
    _currentState = PlayerState.paused;
    _stateController.add(_currentState);
  }
  
  @override
  Future<void> resume() async {
    resumeWasCalled = true;
    _currentState = PlayerState.playing;
    _stateController.add(_currentState);
  }
  
  @override
  Future<void> stop() async {
    stopWasCalled = true;
    _currentState = PlayerState.stopped;
    _stateController.add(_currentState);
  }
  
  @override
  Future<void> setVolume(double volume) async {
    lastVolume = volume;
  }
  
  @override
  Future<void> dispose() async {
    await _stateController.close();
    await _errorController.close();
  }
}

Channel _generateRandomChannel(Random random) {
  return Channel(
    id: 'channel-${random.nextInt(1000000)}',
    name: _generateRandomString(random, 15),
    streamUrl: 'http://stream${random.nextInt(1000)}.com/live.m3u8',
    logoUrl: random.nextBool() ? 'http://logo${random.nextInt(1000)}.com/logo.png' : null,
    category: random.nextBool() ? _generateRandomString(random, 8) : null,
    configId: 'config-${random.nextInt(1000)}',
  );
}

String _generateRandomString(Random random, int length) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  return List.generate(length, (index) => chars[random.nextInt(chars.length)]).join();
}
