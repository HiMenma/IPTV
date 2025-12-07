import 'dart:async';
import 'package:flutter/material.dart';
import 'package:chewie/chewie.dart';
import 'package:video_player/video_player.dart';
import 'package:wakelock_plus/wakelock_plus.dart';
import '../utils/error_handler.dart';

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

/// Service wrapper for Chewie video player
/// Provides a simplified interface for video playback operations
class PlayerService {
  VideoPlayerController? _videoController;
  ChewieController? _chewieController;
  final StreamController<PlayerState> _stateController = StreamController<PlayerState>.broadcast();
  final StreamController<String?> _errorController = StreamController<String?>.broadcast();
  
  PlayerState _currentState = PlayerState.idle;
  double _currentVolume = 1.0;

  /// Stream of player state changes
  Stream<PlayerState> get stateStream => _stateController.stream;

  /// Stream of player errors
  Stream<String?> get errorStream => _errorController.stream;

  /// Get current player state
  PlayerState get currentState => _currentState;
  
  /// Get the Chewie controller for UI integration
  ChewieController? get chewieController => _chewieController;
  
  /// Get the video player controller
  VideoPlayerController? get videoController => _videoController;

  /// Initialize the player
  Future<void> initialize() async {
    try {
      if (_videoController != null || _chewieController != null) {
        await dispose();
      }

      _updateState(PlayerState.idle);
    } catch (e) {
      _updateState(PlayerState.error);
      _errorController.add('Failed to initialize player: $e');
      rethrow;
    }
  }

  /// Play a stream from the given URL
  /// Requirements: 5.3
  Future<void> play(String streamUrl) async {
    try {
      // Check if player is initialized (we need to have called initialize at least once)
      if (_currentState == PlayerState.idle && _videoController == null && _chewieController == null) {
        // Auto-initialize if not done yet
        await initialize();
      }

      // Validate stream URL
      if (streamUrl.isEmpty) {
        throw Exception('Invalid stream URL: URL cannot be empty');
      }

      if (!streamUrl.startsWith('http://') && !streamUrl.startsWith('https://')) {
        throw Exception('Invalid stream URL: Must start with http:// or https://');
      }

      _updateState(PlayerState.preparing);

      // Dispose of existing controllers if any
      if (_chewieController != null) {
        await _chewieController!.pause();
        _chewieController!.dispose();
        _chewieController = null;
      }
      if (_videoController != null) {
        await _videoController!.dispose();
        _videoController = null;
      }

      // Create video player controller with network URL
      _videoController = VideoPlayerController.networkUrl(Uri.parse(streamUrl));
      
      // Set up video player listeners
      _setupVideoPlayerListeners();
      
      // Initialize the video player
      await _videoController!.initialize();
      
      // Create Chewie controller
      _chewieController = ChewieController(
        videoPlayerController: _videoController!,
        autoPlay: true,
        looping: false,
        allowFullScreen: true,
        allowMuting: true,
        showControls: true,
        materialProgressColors: ChewieProgressColors(
          playedColor: Color(0xFF2196F3),
          handleColor: Color(0xFF2196F3),
          backgroundColor: Color(0x33FFFFFF),
          bufferedColor: Color(0x66FFFFFF),
        ),
        placeholder: Container(
          color: Color(0xFF000000),
        ),
        autoInitialize: true,
      );
      
      // Set volume
      await _videoController!.setVolume(_currentVolume);
      
      // Enable wakelock during playback (ignore errors in test environment)
      try {
        await WakelockPlus.enable();
      } catch (e) {
        // Wakelock may not be available in test environment, ignore
      }
      
      _updateState(PlayerState.playing);
    } catch (e) {
      _updateState(PlayerState.error);
      final errorMessage = ErrorHandler.getUserFriendlyMessage(e);
      _errorController.add(errorMessage);
      throw Exception(errorMessage);
    }
  }

  /// Pause playback
  Future<void> pause() async {
    try {
      if (_videoController == null) {
        throw Exception('Player not initialized');
      }

      if (_currentState != PlayerState.playing) {
        return;
      }

      await _videoController!.pause();
      _updateState(PlayerState.paused);
    } catch (e) {
      _updateState(PlayerState.error);
      _errorController.add('Failed to pause playback: $e');
      rethrow;
    }
  }

  /// Resume playback
  Future<void> resume() async {
    try {
      if (_videoController == null) {
        throw Exception('Player not initialized');
      }

      if (_currentState != PlayerState.paused) {
        return;
      }

      await _videoController!.play();
      _updateState(PlayerState.playing);
    } catch (e) {
      _updateState(PlayerState.error);
      _errorController.add('Failed to resume playback: $e');
      rethrow;
    }
  }

  /// Stop playback
  Future<void> stop() async {
    try {
      if (_videoController == null) {
        return;
      }

      await _videoController!.pause();
      await _videoController!.seekTo(Duration.zero);
      
      // Disable wakelock when stopping (ignore errors in test environment)
      try {
        await WakelockPlus.disable();
      } catch (e) {
        // Wakelock may not be available in test environment, ignore
      }
      
      _updateState(PlayerState.stopped);
    } catch (e) {
      _updateState(PlayerState.error);
      _errorController.add('Failed to stop playback: $e');
      rethrow;
    }
  }

  /// Set playback volume (0.0 to 1.0)
  Future<void> setVolume(double volume) async {
    try {
      if (volume < 0.0 || volume > 1.0) {
        throw ArgumentError('Volume must be between 0.0 and 1.0');
      }

      _currentVolume = volume;

      if (_videoController != null) {
        await _videoController!.setVolume(volume);
      }
    } catch (e) {
      _errorController.add('Failed to set volume: $e');
      rethrow;
    }
  }

  /// Dispose of player resources
  Future<void> dispose() async {
    try {
      // Disable wakelock (ignore errors in test environment)
      try {
        await WakelockPlus.disable();
      } catch (e) {
        // Wakelock may not be available in test environment, ignore
      }
      
      // Dispose Chewie controller first
      if (_chewieController != null) {
        _chewieController!.dispose();
        _chewieController = null;
      }
      
      // Then dispose video controller
      if (_videoController != null) {
        await _videoController!.dispose();
        _videoController = null;
      }

      _updateState(PlayerState.idle);
    } catch (e) {
      _errorController.add('Error during disposal: $e');
    }
  }

  /// Set up video player event listeners
  void _setupVideoPlayerListeners() {
    if (_videoController == null) return;

    // Listen to player state changes
    _videoController!.addListener(() {
      if (_videoController == null) return;
      
      final value = _videoController!.value;
      
      // Handle errors
      // Requirements: 5.3
      if (value.hasError) {
        _updateState(PlayerState.error);
        
        // Provide user-friendly error messages
        String userFriendlyMessage;
        final errorDescription = value.errorDescription?.toLowerCase() ?? '';
        
        if (errorDescription.contains('404') || errorDescription.contains('not found')) {
          userFriendlyMessage = 'Stream not found. The channel may be offline or the URL is incorrect.';
        } else if (errorDescription.contains('timeout')) {
          userFriendlyMessage = 'Connection timeout. Please check your internet connection and try again.';
        } else if (errorDescription.contains('network') || errorDescription.contains('connection')) {
          userFriendlyMessage = 'Network error. Please check your internet connection.';
        } else if (errorDescription.contains('format') || errorDescription.contains('codec')) {
          userFriendlyMessage = 'Unsupported video format. This stream cannot be played.';
        } else {
          userFriendlyMessage = 'Playback error: ${value.errorDescription ?? "Unknown error"}';
        }
        
        _errorController.add(userFriendlyMessage);
        return;
      }
      
      // Handle state changes
      if (value.isInitialized) {
        if (_currentState == PlayerState.preparing) {
          _updateState(PlayerState.prepared);
        }
        
        if (value.isPlaying) {
          _updateState(PlayerState.playing);
        } else if (_currentState == PlayerState.playing) {
          // Only update to paused if we were playing
          _updateState(PlayerState.paused);
        }
      }
      
      // Handle completion
      if (value.position >= value.duration && value.duration.inMilliseconds > 0) {
        _updateState(PlayerState.stopped);
        try {
          WakelockPlus.disable();
        } catch (e) {
          // Wakelock may not be available in test environment, ignore
        }
      }
    });
  }

  /// Update player state and notify listeners
  void _updateState(PlayerState newState) {
    _currentState = newState;
    _stateController.add(newState);
  }

  /// Clean up stream controllers
  Future<void> closeStreams() async {
    await _stateController.close();
    await _errorController.close();
  }
}
