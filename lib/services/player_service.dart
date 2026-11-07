import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:chewie/chewie.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../utils/app_logger.dart';
import '../utils/log_redactor.dart';

enum PlayerState { idle, preparing, prepared, playing, paused, stopped, error, retrying }

class PlayerService {
  VideoPlayerController? _videoController;
  ChewieController? _chewieController;
  PlayerState _currentState = PlayerState.idle;
  double _currentVolume = 1.0;
  
  Timer? _stallTimer;
  static const Duration _stallThreshold = Duration(seconds: 15);

  /// 代次守卫：用于防止并发的多次 play() 互相dispose 控制器
  int _playGeneration = 0;

  final _stateController = StreamController<PlayerState>.broadcast();
  final _errorController = StreamController<String?>.broadcast();

  Stream<PlayerState> get stateStream => _stateController.stream;
  Stream<String?> get errorStream => _errorController.stream;
  PlayerState get currentState => _currentState;
  ChewieController? get chewieController => _chewieController;

  Future<void> initialize() async => _updateState(PlayerState.idle);

  /// 对可能包含账号密码的 URL 做日志脱敏。
  ///
  /// Xtream 线路形如 `http://host/live/{username}/{password}/{id}.m3u8`，
  /// 直接写日志等同于泄露凭证，故统一走 [redactUrl] 处理。
  String _redactUrl(String url) => redactUrl(url);

  Future<void> play(String streamUrl, {bool isRetry = false}) async {
    // 每次 play 递增代次；后发起的调用会使先前的调用立即放弃
    final int generation = ++_playGeneration;
    try {
      AppLogger.log('Player: Playing ${_redactUrl(streamUrl)} (isRetry: $isRetry)');
      _updateState(isRetry ? PlayerState.retrying : PlayerState.preparing);

      await _disposeControllers();
      // 等待期间可能已发生新的 play()，此时不再创建控制器，避免争抢资源
      if (generation != _playGeneration) return;
      _stopStallTimer();

      final controller = VideoPlayerController.networkUrl(
        Uri.parse(streamUrl),
        httpHeaders: {
          'User-Agent': 'VLC/3.0.12 LibVLC/3.0.12',
          'Accept': '*/*',
        },
      );
      _videoController = controller;

      _setupVideoPlayerListeners(controller);

      // Extended timeout for slow IPTV servers
      await controller.initialize().timeout(
        const Duration(seconds: 35),
        onTimeout: () => throw Exception('Connection timed out after 35s'),
      );
      // 初始化期间若已被新的 play() 取代，直接返回且不触碰控制器
      if (generation != _playGeneration) {
        if (identical(_videoController, controller)) {
          _videoController = null;
        }
        await controller.dispose();
        return;
      }

      _chewieController = ChewieController(
        videoPlayerController: controller,
        autoPlay: true,
        looping: false,
        isLive: true,
        allowFullScreen: true,
        allowMuting: true,
        showControls: true,
        autoInitialize: true,
        placeholder: Container(color: Colors.black),
        aspectRatio: (controller.value.aspectRatio > 0)
            ? controller.value.aspectRatio
            : 16 / 9,
      );

      await controller.setVolume(_currentVolume);

      if (Platform.isMacOS) {
        await Future.delayed(const Duration(seconds: 1));
        if (identical(_videoController, controller) && !controller.value.isPlaying) {
          await controller.play();
        }
      }

      // 上述 await 之后再次确认本次调用仍然有效
      if (generation != _playGeneration) return;

      try {
        await WakelockPlus.enable();
      } catch (_) {
        // Wakelock not available on this platform
      }
      _updateState(PlayerState.playing);
    } catch (e) {
      // 被新调用取代时，旧调用不再污染状态与错误流
      if (generation != _playGeneration) return;
      _updateState(PlayerState.error);
      _errorController.add(e.toString());
      rethrow;
    }
  }

  /// 为指定控制器绑定播放状态监听器（不再依赖全局字段，避免跨代次误回调）
  void _setupVideoPlayerListeners(VideoPlayerController controller) {
    controller.addListener(() {
      // 该控制器若已被替换/释放，则不再产生状态副作用
      if (!identical(_videoController, controller)) return;
      final value = controller.value;

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

  /// 释放当前控制器。
  ///
  /// 先把字段置空再await dispose，确保并发的两次调用不会对同一个控制器重复 dispose
  /// （await 期间字段仍指向旧实例会导致重复释放）。
  Future<void> _disposeControllers() async {
    final chewie = _chewieController;
    final video = _videoController;
    _chewieController = null;
    _videoController = null;
    try {
      chewie?.dispose();
    } catch (_) {
      // 释放失败不应阻断后续流程
    }
    if (video != null) {
      try {
        await video.dispose();
      } catch (_) {
        // 同上
      }
    }
  }

  Future<void> stop() async {
    _stopStallTimer();
    // 递增代次，使仍在执行中的 play() 立即放弃，避免停止后又被拉起
    _playGeneration++;
    await _disposeControllers();
    _updateState(PlayerState.stopped);
    try {
      await WakelockPlus.disable();
    } catch (_) {
      // Wakelock not available on this platform
    }
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
    // 递增代次，确保销毁过程中仍在执行的 play() 不再触碰已释放的控制器
    _playGeneration++;
    _disposeControllers();
    _stateController.close();
    _errorController.close();
  }

  void _updateState(PlayerState newState) {
    _currentState = newState;
    _stateController.add(newState);
  }
}
