import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:chewie/chewie.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../utils/error_handler.dart';
import '../utils/app_logger.dart';

enum PlayerState { idle, preparing, prepared, playing, paused, stopped, error, retrying }

class PlayerService {
  VideoPlayerController? _videoController;
  ChewieController? _chewieController;
  PlayerState _currentState = PlayerState.idle;
  double _currentVolume = 1.0;
  
  Timer? _stallTimer;
  static const Duration _stallThreshold = Duration(seconds: 15);

  final _stateController = StreamController<PlayerState>.broadcast();
  final _errorController = StreamController<String?>.broadcast();

  Stream<PlayerState> get stateStream => _stateController.stream;
  Stream<String?> get errorStream => _errorController.stream;
  PlayerState get currentState => _currentState;
  ChewieController? get chewieController => _chewieController;

  Future<void> initialize() async => _updateState(PlayerState.idle);

  Future<void> play(String streamUrl, {bool isRetry = false}) async {
    try {
      AppLogger.log('Player: Playing $streamUrl (isRetry: $isRetry)');
      _updateState(isRetry ? PlayerState.retrying : PlayerState.preparing);
      
      await _disposeControllers();
      _stopStallTimer();

      _videoController = VideoPlayerController.networkUrl(
        Uri.parse(streamUrl),
        httpHeaders: {
          'User-Agent': 'VLC/3.0.12 LibVLC/3.0.12',
          'Accept': '*/*',
        },
      );
      
      _setupVideoPlayerListeners();
      
      // Extended timeout for slow IPTV servers
      await _videoController!.initialize().timeout(
        const Duration(seconds: 35),
        onTimeout: () => throw Exception('Connection timed out after 35s'),
      );
      
      _chewieController = ChewieController(
        videoPlayerController: _videoController!,
        autoPlay: true,
        looping: false,
        isLive: true, 
        allowFullScreen: true,
        allowMuting: true,
        showControls: true, // REVERT: Restore native controls for stability
        autoInitialize: true,
        placeholder: Container(color: Colors.black),
        aspectRatio: (_videoController!.value.aspectRatio > 0) 
            ? _videoController!.value.aspectRatio 
            : 16 / 9,
      );
      
      await _videoController!.setVolume(_currentVolume);
      
      if (Platform.isMacOS) {
        await Future.delayed(const Duration(milliseconds: 1000));
        if (_videoController != null && !_videoController!.value.isPlaying) {
          await _videoController!.play();
        }
      }

      try { await WakelockPlus.enable(); } catch (e) {}
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
      
      if (value.isBuffering && _currentState == PlayerState.playing) {
        _startStallTimer();
      } else if (!value.isBuffering) {
        _stopStallTimer();
      }

      if (value.isInitialized && value.isPlaying && _currentState != PlayerState.playing && _currentState != PlayerState.retrying) {
        _updateState(PlayerState.playing);
      }
    });
  }

  void _startStallTimer() {
    if (_stallTimer != null) return;
    _stallTimer = Timer(_stallThreshold, () {
      AppLogger.log('Player: Buffering stall detected.');
      _updateState(PlayerState.error);
      _errorController.add('Playback stalled');
    });
  }

  void _stopStallTimer() { _stallTimer?.cancel(); _stallTimer = null; }

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

  Future<void> stop() async {
    _stopStallTimer();
    await _disposeControllers();
    _updateState(PlayerState.stopped);
    try { await WakelockPlus.disable(); } catch (e) {}
  }

  Future<void> pause() async {
    if (_videoController?.value.isInitialized ?? false) {
      await _videoController!.pause();
      _updateState(PlayerState.paused);
    }
  }

  Future<void> resume() async {
    if (_videoController?.value.isInitialized ?? false) {
      await _videoController!.play();
      _updateState(PlayerState.playing);
    }
  }

  Future<void> setVolume(double volume) async {
    _currentVolume = volume;
    if (_videoController?.value.isInitialized ?? false) await _videoController!.setVolume(volume);
  }

  void dispose() {
    _stopStallTimer();
    _disposeControllers();
    _stateController.close();
    _errorController.close();
  }

  void _updateState(PlayerState newState) {
    _currentState = newState;
    _stateController.add(newState);
  }
}
