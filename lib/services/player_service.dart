import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:chewie/chewie.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../utils/error_handler.dart';
import '../utils/app_logger.dart';

/// Player state enumeration
enum PlayerState {
  idle,
  preparing,
  prepared,
  playing,
  paused,
  stopped,
  error,
  retrying,
}

class PlayerService {
  VideoPlayerController? _videoController;
  ChewieController? _chewieController;
  PlayerState _currentState = PlayerState.idle;
  double _currentVolume = 1.0;
  
  // Stalls and Monitoring
  Timer? _stallTimer;
  static const Duration _stallThreshold = Duration(seconds: 8);

  final _stateController = StreamController<PlayerState>.broadcast();
  final _errorController = StreamController<String?>.broadcast();

  Stream<PlayerState> get stateStream => _stateController.stream;
  Stream<String?> get errorStream => _errorController.stream;
  PlayerState get currentState => _currentState;
  ChewieController? get chewieController => _chewieController;

  /// Initialize the player service
  Future<void> initialize() async {
    _updateState(PlayerState.idle);
  }

  /// Play a stream from the given URL
  Future<void> play(String streamUrl, {bool isRetry = false}) async {
    try {
      AppLogger.log('Player: Attempting to play URL: $streamUrl (isRetry: $isRetry)');
      if (streamUrl.isEmpty) {
        throw Exception('Invalid stream URL: URL cannot be empty');
      }

      _updateState(isRetry ? PlayerState.retrying : PlayerState.preparing);

      // Dispose existing controllers
      await _disposeControllers();
      _stopStallTimer();

      // Small delay to ensure resources are released
      await Future.delayed(const Duration(milliseconds: 100));

      _videoController = VideoPlayerController.networkUrl(
        Uri.parse(streamUrl),
        httpHeaders: {
          'User-Agent': 'VLC/3.0.12 LibVLC/3.0.12',
          'Accept': '*/*',
        },
      );
      
      _setupVideoPlayerListeners();
      
      await _videoController!.initialize().timeout(
        const Duration(seconds: 25),
        onTimeout: () {
          AppLogger.log('Player Error: Initialization Timeout');
          throw Exception('Connection timeout');
        },
      );
      
      _chewieController = ChewieController(
        videoPlayerController: _videoController!,
        autoPlay: true,
        looping: false,
        isLive: true, 
        allowFullScreen: true,
        allowMuting: true,
        showControls: true,
        aspectRatio: (_videoController!.value.aspectRatio > 0) 
            ? _videoController!.value.aspectRatio 
            : 16 / 9,
        materialProgressColors: ChewieProgressColors(
          playedColor: const Color(0xFF2196F3),
          handleColor: const Color(0xFF2196F3),
          backgroundColor: const Color(0x33FFFFFF),
          bufferedColor: const Color(0x66FFFFFF),
        ),
        autoInitialize: true,
      );
      
      await _videoController!.setVolume(_currentVolume);
      
      if (Platform.isMacOS) {
        await Future.delayed(const Duration(milliseconds: 600));
        if (_videoController != null && !_videoController!.value.isPlaying) {
          await _videoController!.play();
        }
      }

      // Force enable global wakelock as a fallback
      try {
        await WakelockPlus.enable();
        AppLogger.log('Player: Wakelock enabled');
      } catch (e) {
        AppLogger.log('Player: Failed to enable Wakelock: $e');
      }
      
      _updateState(PlayerState.playing);
    } catch (e) {
      _updateState(PlayerState.error);
      _errorController.add(e.toString());
      rethrow;
    }
  }

  void _setupVideoPlayerListeners() {
    if (_videoController == null) return;

    _videoController!.addListener(() {
      if (_videoController == null) return;
      final value = _videoController!.value;
      
      if (value.hasError) {
        _updateState(PlayerState.error);
        _errorController.add(value.errorDescription);
        return;
      }
      
      // Monitor Stalls
      if (value.isBuffering && _currentState == PlayerState.playing) {
        _startStallTimer();
      } else if (!value.isBuffering) {
        _stopStallTimer();
      }

      if (value.isInitialized) {
        if (value.isPlaying && _currentState != PlayerState.playing && _currentState != PlayerState.retrying) {
          _updateState(PlayerState.playing);
        }
      }
      
      bool isSuspiciouslyShort = value.duration.inSeconds < 10;
      if (!value.isBuffering && 
          value.duration.inMilliseconds > 0 && 
          !isSuspiciouslyShort && 
          value.position >= value.duration) {
        _updateState(PlayerState.stopped);
        _disableWakelock();
      }
    });
  }

  void _startStallTimer() {
    if (_stallTimer != null) return;
    _stallTimer = Timer(_stallThreshold, () {
      AppLogger.log('Player: Stall detected. Triggering retry.');
      _updateState(PlayerState.error);
      _errorController.add('Playback stalled');
    });
  }

  void _stopStallTimer() {
    _stallTimer?.cancel();
    _stallTimer = null;
  }

  Future<void> _disableWakelock() async {
    try {
      if (await WakelockPlus.enabled) {
        await WakelockPlus.disable();
        AppLogger.log('Player: Wakelock disabled');
      }
    } catch (e) {}
  }

  Future<void> _disposeControllers() async {
    if (_chewieController != null) {
      _chewieController!.dispose();
      _chewieController = null;
    }
    if (_videoController != null) {
      await _videoController!.dispose();
      _videoController = null;
    }
  }

  Future<void> pause() async {
    if (_videoController?.value.isInitialized ?? false) {
      await _videoController!.pause();
      _updateState(PlayerState.paused);
      // Optional: keep wakelock on even when paused, or disable it
      // For IPTV, usually keeping it on is better if user is just buffering
    }
  }

  Future<void> resume() async {
    if (_videoController?.value.isInitialized ?? false) {
      await _videoController!.play();
      _updateState(PlayerState.playing);
      await WakelockPlus.enable();
    }
  }

  Future<void> stop() async {
    _stopStallTimer();
    await _disposeControllers();
    _updateState(PlayerState.stopped);
    await _disableWakelock();
  }

  Future<void> setVolume(double volume) async {
    _currentVolume = volume;
    if (_videoController?.value.isInitialized ?? false) {
      await _videoController!.setVolume(volume);
    }
  }

  void dispose() {
    _stopStallTimer();
    _disposeControllers();
    _disableWakelock();
    _stateController.close();
    _errorController.close();
  }

  void _updateState(PlayerState newState) {
    _currentState = newState;
    _stateController.add(newState);
  }
}
