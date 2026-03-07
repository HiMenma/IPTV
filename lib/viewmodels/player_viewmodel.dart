import 'package:flutter/foundation.dart';
import 'dart:async';
import 'package:chewie/chewie.dart';
import '../models/channel.dart';
import '../services/player_service.dart';
import '../repositories/history_repository_sqlite.dart';

class PlayerViewModel extends ChangeNotifier {
  final PlayerService _playerService;
  final HistoryRepositorySQLite _historyRepository;

  PlayerState _state = PlayerState.idle;
  String? _error;
  Channel? _currentChannel;
  bool _isFullscreen = false;
  StreamSubscription<PlayerState>? _stateSubscription;
  StreamSubscription<String?>? _errorSubscription;

  PlayerViewModel({
    PlayerService? playerService,
    HistoryRepositorySQLite? historyRepository,
  })  : _playerService = playerService ?? PlayerService(),
        _historyRepository = historyRepository ?? HistoryRepositorySQLite() {
    _setupListeners();
  }

  PlayerState get state => _state;
  String? get error => _error;
  Channel? get currentChannel => _currentChannel;
  bool get isFullscreen => _isFullscreen;
  
  /// Get the Chewie controller for UI integration
  ChewieController? get chewieController => _playerService.chewieController;

  /// Set up listeners for player state and error streams
  void _setupListeners() {
    _stateSubscription = _playerService.stateStream.listen((newState) {
      _state = newState;
      notifyListeners();
    });

    _errorSubscription = _playerService.errorStream.listen((errorMessage) {
      _error = errorMessage;
      notifyListeners();
    });
  }

  /// Play a channel
  Future<void> playChannel(Channel channel) async {
    try {
      _currentChannel = channel;
      _error = null;
      notifyListeners();

      if (_playerService.currentState == PlayerState.idle) {
        await _playerService.initialize();
      }

      await _playerService.play(channel.streamUrl);
      await _historyRepository.add(channel.id);
    } catch (e) {
      _error = 'Failed to play channel: $e';
      _state = PlayerState.error;
      notifyListeners();
      rethrow;
    }
  }

  Future<void> pause() async {
    try {
      await _playerService.pause();
    } catch (e) {
      _error = 'Failed to pause playback: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> resume() async {
    try {
      await _playerService.resume();
    } catch (e) {
      _error = 'Failed to resume playback: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> stop() async {
    try {
      await _playerService.stop();
      _currentChannel = null;
      notifyListeners();
    } catch (e) {
      _error = 'Failed to stop playback: $e';
      notifyListeners();
      rethrow;
    }
  }

  Future<void> setVolume(double volume) async {
    try {
      await _playerService.setVolume(volume);
    } catch (e) {
      _error = 'Failed to set volume: $e';
      notifyListeners();
      rethrow;
    }
  }

  void toggleFullscreen() {
    _isFullscreen = !_isFullscreen;
    notifyListeners();
  }

  @override
  void dispose() {
    _stateSubscription?.cancel();
    _errorSubscription?.cancel();
    _playerService.dispose();
    super.dispose();
  }
}
