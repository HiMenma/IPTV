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
  PlayerViewModel? _playerViewModel;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _playerViewModel = context.read<PlayerViewModel>();
        _playerViewModel!.playChannel(widget.channel);
      }
    });
  }

  @override
  void dispose() {
    try { _playerViewModel?.stop(); } catch (e) {}
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<PlayerViewModel>(
      builder: (context, viewModel, child) {
        return Scaffold(
          backgroundColor: Colors.black,
          appBar: AppBar(
            title: Text(widget.channel.name, style: const TextStyle(color: Colors.white)),
            backgroundColor: Colors.black,
            elevation: 0,
            iconTheme: const IconThemeData(color: Colors.white),
            leading: IconButton(
              icon: const Icon(Icons.arrow_back),
              onPressed: () => Navigator.pop(context),
            ),
          ),
          body: Stack(
            children: [
              // Main Player Area
              Center(
                child: _buildPlayerWidget(viewModel),
              ),
              
              // Loading
              if (viewModel.state == PlayerState.preparing || viewModel.state == PlayerState.idle)
                const Center(child: CircularProgressIndicator(color: Colors.white)),
              
              // Reconnecting Overlay (Non-blocking)
              if (viewModel.isRetrying)
                _buildReconnectOverlay(viewModel),

              // Fatal Error Overlay (With Escape Buttons)
              if (viewModel.error != null && viewModel.state == PlayerState.error && !viewModel.isRetrying) 
                _buildErrorOverlay(viewModel),
            ],
          ),
        );
      },
    );
  }

  Widget _buildPlayerWidget(PlayerViewModel viewModel) {
    if (viewModel.chewieController != null && 
        (viewModel.state == PlayerState.playing || viewModel.state == PlayerState.paused || viewModel.state == PlayerState.retrying)) {
      return Chewie(controller: viewModel.chewieController!);
    }
    return const SizedBox.shrink();
  }

  Widget _buildReconnectOverlay(PlayerViewModel viewModel) {
    return Container(
      color: Colors.black54,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircularProgressIndicator(color: Colors.orange),
            const SizedBox(height: 16),
            Text(viewModel.error ?? 'Reconnecting...', style: const TextStyle(color: Colors.white)),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorOverlay(PlayerViewModel viewModel) {
    return Container(
      color: Colors.black.withOpacity(0.9),
      width: double.infinity,
      height: double.infinity,
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(32),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline, size: 72, color: Colors.redAccent),
              const SizedBox(height: 24),
              const Text('Playback Error', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 12),
              Text(
                viewModel.error ?? 'Connection failed',
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white70),
              ),
              const SizedBox(height: 40),
              
              // PRIMARY ESCAPE PATH
              ElevatedButton.icon(
                onPressed: () => viewModel.playChannel(widget.channel),
                icon: const Icon(Icons.refresh),
                label: const Text('Retry Playback'),
                style: ElevatedButton.styleFrom(
                  minimumSize: const Size(200, 50),
                  backgroundColor: Theme.of(context).colorScheme.primary,
                  foregroundColor: Colors.white,
                ),
              ),
              const SizedBox(height: 16),
              OutlinedButton.icon(
                onPressed: () => Navigator.pop(context),
                icon: const Icon(Icons.arrow_back),
                label: const Text('Back to List'),
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size(200, 50),
                  foregroundColor: Colors.white,
                  side: const BorderSide(color: Colors.white54),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
