import 'package:flutter/material.dart';
import '../../services/player_service.dart';

/// A reusable widget for displaying video player controls.
/// Includes play/pause button, volume slider, fullscreen toggle,
/// and optional progress bar.
///
/// Requirements: 5.5
class PlayerControls extends StatelessWidget {
  final PlayerState playerState;
  final bool isFullscreen;
  final double volume;
  final VoidCallback onPlayPause;
  final VoidCallback onToggleFullscreen;
  final ValueChanged<double> onVolumeChanged;
  final VoidCallback? onPrevious;
  final VoidCallback? onNext;
  final bool showProgressBar;
  final double? progress;

  const PlayerControls({
    super.key,
    required this.playerState,
    required this.isFullscreen,
    required this.volume,
    required this.onPlayPause,
    required this.onToggleFullscreen,
    required this.onVolumeChanged,
    this.onPrevious,
    this.onNext,
    this.showProgressBar = false,
    this.progress,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.black.withValues(alpha: 0.7),
            Colors.transparent,
            Colors.black.withValues(alpha: 0.7),
          ],
        ),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // Top controls
          _buildTopControls(),
          // Center controls
          _buildCenterControls(),
          // Bottom controls
          _buildBottomControls(),
        ],
      ),
    );
  }

  Widget _buildTopControls() {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          IconButton(
            icon: Icon(
              isFullscreen ? Icons.fullscreen_exit : Icons.fullscreen,
              color: Colors.white,
            ),
            onPressed: onToggleFullscreen,
            tooltip: isFullscreen ? 'Exit fullscreen' : 'Enter fullscreen',
          ),
        ],
      ),
    );
  }

  Widget _buildCenterControls() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        // Previous button
        IconButton(
          icon: const Icon(Icons.skip_previous, color: Colors.white, size: 40),
          onPressed: onPrevious,
          tooltip: 'Previous',
        ),
        const SizedBox(width: 24),
        // Play/Pause button
        IconButton(
          icon: Icon(
            _getPlayPauseIcon(),
            color: Colors.white,
            size: 64,
          ),
          onPressed: _canPlayPause() ? onPlayPause : null,
          tooltip: _getPlayPauseTooltip(),
        ),
        const SizedBox(width: 24),
        // Next button
        IconButton(
          icon: const Icon(Icons.skip_next, color: Colors.white, size: 40),
          onPressed: onNext,
          tooltip: 'Next',
        ),
      ],
    );
  }

  Widget _buildBottomControls() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        // Progress bar (if applicable)
        if (showProgressBar && progress != null)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: LinearProgressIndicator(
              value: progress,
              backgroundColor: Colors.white24,
              valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
            ),
          ),
        // Volume controls
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            children: [
              Icon(
                _getVolumeIcon(),
                color: Colors.white,
              ),
              Expanded(
                child: Slider(
                  value: volume,
                  min: 0.0,
                  max: 1.0,
                  onChanged: onVolumeChanged,
                  activeColor: Colors.white,
                  inactiveColor: Colors.white38,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  IconData _getPlayPauseIcon() {
    switch (playerState) {
      case PlayerState.playing:
        return Icons.pause_circle_filled;
      case PlayerState.paused:
        return Icons.play_circle_filled;
      case PlayerState.preparing:
      case PlayerState.prepared:
        return Icons.hourglass_empty;
      case PlayerState.idle:
      case PlayerState.stopped:
      case PlayerState.error:
        return Icons.play_circle_filled;
    }
  }

  String _getPlayPauseTooltip() {
    switch (playerState) {
      case PlayerState.playing:
        return 'Pause';
      case PlayerState.paused:
        return 'Resume';
      case PlayerState.preparing:
      case PlayerState.prepared:
        return 'Loading...';
      case PlayerState.idle:
      case PlayerState.stopped:
      case PlayerState.error:
        return 'Play';
    }
  }

  bool _canPlayPause() {
    return playerState == PlayerState.playing || playerState == PlayerState.paused;
  }

  IconData _getVolumeIcon() {
    if (volume == 0) {
      return Icons.volume_off;
    } else if (volume < 0.5) {
      return Icons.volume_down;
    } else {
      return Icons.volume_up;
    }
  }
}
