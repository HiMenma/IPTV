import 'package:flutter/foundation.dart';
import 'dart:async';
import 'package:chewie/chewie.dart';
import '../models/channel.dart';
import '../models/configuration.dart';
import '../services/player_service.dart';
import '../repositories/history_repository_sqlite.dart';
import '../repositories/configuration_repository.dart';
import '../utils/app_logger.dart';
import '../utils/log_redactor.dart';

class PlayerViewModel extends ChangeNotifier {
  final PlayerService _playerService;
  final HistoryRepositorySQLite _historyRepository;
  final ConfigurationRepository _configRepository;

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
    // 播放器异常文本可能内嵌含凭证的播放地址，入日志前统一脱敏
    final safeMessage = redactUrl(message);
    AppLogger.log('PlayerVM: Error received: $safeMessage. Current retry: $_retryCount');

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
      _error = safeMessage;
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
    // 取消待执行的自动重连，避免手动切换频道时与 3 秒重连定时器并发播放
    _reconnectTimer?.cancel();
    _reconnectTimer = null;

    try {
      _currentChannel = channel;
      _error = null;
      _retryCount = 0;
      notifyListeners();

      if (_playerService.currentState == PlayerState.idle) {
        await _playerService.initialize();
      }

      await _playerService.play(channel.streamUrl);
    } catch (e) {
      // 该文案会显示在错误遮罩上，脱敏避免向用户暴露自己的账号密码
      _error = 'Failed to play channel: ${redactUrl(e.toString())}';
      _state = PlayerState.error;
      notifyListeners();
      rethrow;
    }

    // 历史记录写入与播放状态解耦：DB 失败不应把正在播放的画面变成错误遮罩
    try {
      await _historyRepository.add(channel.id);
    } catch (e) {
      AppLogger.log('PlayerVM: history write failed: $e');
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
