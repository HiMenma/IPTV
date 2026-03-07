import 'package:flutter/foundation.dart';
import 'dart:async';
import 'package:chewie/chewie.dart';
import '../models/channel.dart';
import '../models/configuration.dart';
import '../services/player_service.dart';
import '../repositories/history_repository_sqlite.dart';
import '../repositories/configuration_repository.dart';
import '../services/xtream_service.dart';
import '../services/m3u_service.dart';
import '../utils/app_logger.dart';

class PlayerViewModel extends ChangeNotifier {
  final PlayerService _playerService;
  final HistoryRepositorySQLite _historyRepository;
  final ConfigurationRepository _configRepository;
  final XtreamService _xtreamService = XtreamService();
  final M3UService _m3uService = M3UService();

  PlayerState _state = PlayerState.idle;
  String? _error;
  Channel? _currentChannel;
  bool _isFullscreen = false;
  
  // Reconnect logic
  int _retryCount = 0;
  static const int _maxRetries = 3;
  Timer? _reconnectTimer;

  StreamSubscription<PlayerState>? _stateSubscription;
  StreamSubscription<String?>? _errorSubscription;

  PlayerViewModel({
    PlayerService? playerService,
    HistoryRepositorySQLite? historyRepository,
    ConfigurationRepository? configRepository,
  })  : _playerService = playerService ?? PlayerService(),
        _historyRepository = historyRepository ?? HistoryRepositorySQLite(),
        _configRepository = configRepository ?? ConfigurationRepository() {
    _setupListeners();
  }

  PlayerState get state => _state;
  String? get error => _error;
  Channel? get currentChannel => _currentChannel;
  bool get isFullscreen => _isFullscreen;
  bool get isRetrying => _state == PlayerState.retrying;
  
  ChewieController? get chewieController => _playerService.chewieController;

  void _setupListeners() {
    _stateSubscription = _playerService.stateStream.listen((newState) {
      _state = newState;
      if (newState == PlayerState.playing) {
        _retryCount = 0; // Reset count on success
        _error = null;   // Clear error message on success!
      }
      notifyListeners();
    });

    _errorSubscription = _playerService.errorStream.listen((errorMessage) {
      if (errorMessage != null) {
        _handlePlaybackError(errorMessage);
      }
    });
  }

  void _handlePlaybackError(String message) {
    AppLogger.log('PlayerVM: Error received: $message. Current retry: $_retryCount');
    
    if (_retryCount < _maxRetries && _currentChannel != null) {
      _retryCount++;
      _error = 'Connection error. Retrying ($_retryCount/$_maxRetries)...';
      notifyListeners();
      
      _reconnectTimer?.cancel();
      // Wait slightly longer (3s) to allow system to clear video buffers
      _reconnectTimer = Timer(const Duration(seconds: 3), () {
        _performReconnect();
      });
    } else {
      _error = message;
      _state = PlayerState.error;
      notifyListeners();
    }
  }

  Future<void> _performReconnect() async {
    if (_currentChannel == null) return;
    
    try {
      AppLogger.log('PlayerVM: Attempting silent reconnect for ${_currentChannel!.name}');
      
      // Optional: Refresh the channel URL from source if it's a dynamic source
      String urlToPlay = _currentChannel!.streamUrl;
      
      try {
        final config = await _configRepository.getById(_currentChannel!.configId);
        if (config != null) {
          if (config.type == ConfigType.xtream) {
            // Re-fetch Xtream URL to get fresh token if needed
            // This is a simplified version; real-world might need deeper API call
            AppLogger.log('PlayerVM: Refreshing Xtream config for fresh tokens');
          }
        }
      } catch (e) {
        AppLogger.log('PlayerVM: Background refresh failed, using old URL: $e');
      }

      await _playerService.play(urlToPlay, isRetry: true);
    } catch (e) {
      AppLogger.log('PlayerVM: Reconnect attempt failed: $e');
      // Error will be caught by listener and potentially trigger next retry
    }
  }

  Future<void> playChannel(Channel channel) async {
    try {
      _currentChannel = channel;
      _error = null;
      _retryCount = 0;
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

  Future<void> pause() async => await _playerService.pause();
  Future<void> resume() async => await _playerService.resume();

  Future<void> stop() async {
    _reconnectTimer?.cancel();
    _currentChannel = null;
    await _playerService.stop();
    notifyListeners();
  }

  Future<void> setVolume(double volume) async => await _playerService.setVolume(volume);

  void toggleFullscreen() {
    _isFullscreen = !_isFullscreen;
    notifyListeners();
  }

  @override
  void dispose() {
    _reconnectTimer?.cancel();
    _stateSubscription?.cancel();
    _errorSubscription?.cancel();
    _playerService.dispose();
    super.dispose();
  }
}
