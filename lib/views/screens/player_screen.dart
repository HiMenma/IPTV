import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:chewie/chewie.dart';
import '../../viewmodels/player_viewmodel.dart';
import '../../models/channel.dart';
import '../../services/player_service.dart';

class PlayerScreen extends StatefulWidget {
  final Channel channel;

  const PlayerScreen({super.key, required this.channel});

  @override
  State<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends State<PlayerScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      context.read<PlayerViewModel>().playChannel(widget.channel);
    });
  }

  @override
  void dispose() {
    // Stop playback when leaving the screen
    context.read<PlayerViewModel>().stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<PlayerViewModel>(
      builder: (context, viewModel, child) {
        return Scaffold(
          appBar: AppBar(
            title: Text(widget.channel.name),
            backgroundColor: Theme.of(context).colorScheme.inversePrimary,
          ),
          body: Column(
            children: [
              // Video player area
              Expanded(
                child: _buildPlayerArea(viewModel),
              ),
              // Channel information
              _buildChannelInfo(),
            ],
          ),
        );
      },
    );
  }

  Widget _buildPlayerArea(PlayerViewModel viewModel) {
    return Container(
      color: Colors.black,
      child: Stack(
        children: [
          // Chewie player widget
          Center(
            child: _buildPlayerWidget(viewModel),
          ),
          // Error overlay
          if (viewModel.error != null) _buildErrorOverlay(viewModel),
        ],
      ),
    );
  }

  Widget _buildPlayerWidget(PlayerViewModel viewModel) {
    switch (viewModel.state) {
      case PlayerState.idle:
        return const Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.tv, size: 64, color: Colors.white54),
            SizedBox(height: 16),
            Text(
              'Initializing player...',
              style: TextStyle(color: Colors.white54),
            ),
          ],
        );
      case PlayerState.preparing:
      case PlayerState.prepared:
        return const Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(color: Colors.white),
            SizedBox(height: 16),
            Text(
              'Loading stream...',
              style: TextStyle(color: Colors.white54),
            ),
          ],
        );
      case PlayerState.playing:
      case PlayerState.paused:
        // Use Chewie widget for playback
        final chewieController = viewModel.chewieController;
        if (chewieController != null && chewieController.videoPlayerController.value.isInitialized) {
          return AspectRatio(
            aspectRatio: chewieController.videoPlayerController.value.aspectRatio,
            child: Chewie(controller: chewieController),
          );
        } else {
          return const Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(color: Colors.white),
              SizedBox(height: 16),
              Text(
                'Loading stream...',
                style: TextStyle(color: Colors.white54),
              ),
            ],
          );
        }
      case PlayerState.stopped:
        return const Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.stop_circle, size: 64, color: Colors.white54),
            SizedBox(height: 16),
            Text(
              'Playback stopped',
              style: TextStyle(color: Colors.white54),
            ),
          ],
        );
      case PlayerState.error:
        return Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 64, color: Theme.of(context).colorScheme.error),
            const SizedBox(height: 16),
            Text(
              'Playback error',
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
          ],
        );
    }
  }

  Widget _buildErrorOverlay(PlayerViewModel viewModel) {
    return Container(
      color: Colors.black.withValues(alpha: 0.8),
      child: Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.error_outline, size: 64, color: Theme.of(context).colorScheme.error),
              const SizedBox(height: 16),
              const Text(
                'Playback Error',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                viewModel.error ?? 'Unknown error occurred',
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white70),
              ),
              const SizedBox(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ElevatedButton.icon(
                    onPressed: () => viewModel.playChannel(widget.channel),
                    icon: const Icon(Icons.refresh),
                    label: const Text('Retry'),
                  ),
                  const SizedBox(width: 16),
                  OutlinedButton.icon(
                    onPressed: () => Navigator.pop(context),
                    icon: const Icon(Icons.arrow_back),
                    label: const Text('Back'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.white,
                      side: const BorderSide(color: Colors.white),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildChannelInfo() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.1),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        children: [
          if (widget.channel.logoUrl != null && widget.channel.logoUrl!.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Image.network(
                widget.channel.logoUrl!,
                width: 60,
                height: 60,
                fit: BoxFit.contain,
                errorBuilder: (context, error, stackTrace) =>
                    const Icon(Icons.tv, size: 60),
              ),
            ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.channel.name,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                if (widget.channel.category != null)
                  Text(
                    widget.channel.category!,
                    style: TextStyle(
                      fontSize: 14,
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
