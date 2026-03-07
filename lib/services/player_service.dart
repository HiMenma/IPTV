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
}

class PlayerService {
  VideoPlayerController? _videoController;
  ChewieController? _chewieController;
  PlayerState _currentState = PlayerState.idle;
  double _currentVolume = 1.0;

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
  Future<void> play(String streamUrl) async {
    try {
      AppLogger.log('Player: Attempting to play URL: $streamUrl');
      if (streamUrl.isEmpty) {
        throw Exception('Invalid stream URL: URL cannot be empty');
      }

      _updateState(PlayerState.preparing);

      // Dispose existing controllers
      await _disposeControllers();

      // Small delay to ensure resources are released
      await Future.delayed(const Duration(milliseconds: 100));

      // Create video player controller with VLC User-Agent for compatibility
      _videoController = VideoPlayerController.networkUrl(
        Uri.parse(streamUrl),
        httpHeaders: {
          'User-Agent': 'VLC/3.0.12 LibVLC/3.0.12',
          'Accept': '*/*',
        },
      );
      
      _setupVideoPlayerListeners();
      
      // Initialize with 20s timeout
      await _videoController!.initialize().timeout(
        const Duration(seconds: 20),
        onTimeout: () {
          AppLogger.log('Player Error: Initialization Timeout');
          throw Exception('Connection timeout during initialization');
        },
      );
      
      AppLogger.log('Player: Initialization complete. Size: ${_videoController!.value.size}');
      
      // Create Chewie controller
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
      
      // macOS/Web specific fix: delay to ensure texture binding
      if (Platform.isMacOS) {
        await Future.delayed(const Duration(milliseconds: 500));
        if (_videoController != null && !_videoController!.value.isPlaying) {
          await _videoController!.play();
        }
      }

      try {
        await WakelockPlus.enable();
      } catch (e) {
        debugPrint('Wakelock error: $e');
      }
      
      _updateState(PlayerState.playing);
    } catch (e) {
      AppLogger.log('Player Error in play(): $e');
      _updateState(PlayerState.error);
      final errorMessage = ErrorHandler.getUserFriendlyMessage(e);
      _errorController.add(errorMessage);
      rethrow;
    }
  }

  void _setupVideoPlayerListeners() {
    if (_videoController == null) return;

    _videoController!.addListener(() {
      if (_videoController == null) return;
      final value = _videoController!.value;
      
      if (value.hasError) {
        AppLogger.log('Player Error Callback: ${value.errorDescription}');
        _updateState(PlayerState.error);
        _errorController.add(value.errorDescription);
        return;
      }
      
      // Handle state transitions
      if (value.isInitialized) {
        if (value.isPlaying && _currentState != PlayerState.playing) {
          _updateState(PlayerState.playing);
        }
      }
      
      // Improved completion check for IPTV: 1ms Bug Fix
      // Ignore completion for suspiciously short durations (< 10s)
      bool isSuspiciouslyShort = value.duration.inSeconds < 10;
      
      if (!value.isBuffering && 
          value.duration.inMilliseconds > 0 && 
          !isSuspiciouslyShort && 
          value.duration.inMilliseconds < 14400000 && 
          value.position >= value.duration) {
        AppLogger.log('Player: VOD reached end.');
        _updateState(PlayerState.stopped);
        WakelockPlus.disable().catchError((_) => null);
      }
    });
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
    try {
      if (_videoController != null && _videoController!.value.isInitialized) {
        await _videoController!.pause();
        _updateState(PlayerState.paused);
      }
    } catch (e) {}
  }

  Future<void> resume() async {
    try {
      if (_videoController != null && _videoController!.value.isInitialized) {
        await _videoController!.play();
        _updateState(PlayerState.playing);
      }
    } catch (e) {}
  }

  Future<void> stop() async {
    try {
      if (_videoController != null) {
        await _videoController!.pause();
        _updateState(PlayerState.stopped);
        WakelockPlus.disable().catchError((_) => null);
      }
    } catch (e) {}
  }

  Future<void> setVolume(double volume) async {
    _currentVolume = volume;
    if (_videoController != null && _videoController!.value.isInitialized) {
      await _videoController!.setVolume(volume);
    }
  }

  void dispose() {
    _disposeControllers();
    _stateController.close();
    _errorController.close();
  }

  void _updateState(PlayerState newState) {
    _currentState = newState;
    _stateController.add(newState);
  }
}
