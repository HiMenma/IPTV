import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../../viewmodels/channel_viewmodel.dart';
import '../../models/channel.dart';
import 'player_screen.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    // Load data on first init
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && !_isInitialized) {
        _isInitialized = true;
        _loadData();
      }
    });
  }

  Future<void> _loadData() async {
    try {
      await context.read<ChannelViewModel>().loadHistory();
      debugPrint('History loaded successfully');
    } catch (e) {
      debugPrint('Error loading history: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context); // Required for AutomaticKeepAliveClientMixin
    return Scaffold(
      appBar: AppBar(
        title: const Text('Browse History'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<ChannelViewModel>().loadHistory(),
            tooltip: 'Refresh',
          ),
          Consumer<ChannelViewModel>(
            builder: (context, viewModel, child) {
              if (viewModel.history.isNotEmpty) {
                return IconButton(
                  icon: const Icon(Icons.delete_sweep),
                  onPressed: () => _clearHistory(context),
                  tooltip: 'Clear history',
                );
              }
              return const SizedBox.shrink();
            },
          ),
        ],
      ),
      body: Consumer<ChannelViewModel>(
        builder: (context, viewModel, child) {
          if (viewModel.isLoading) {
            return const Center(child: CircularProgressIndicator());
          }

          if (viewModel.error != null) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.error_outline, size: 48, color: Theme.of(context).colorScheme.error),
                  const SizedBox(height: 16),
                  Text(
                    viewModel.error!,
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Theme.of(context).colorScheme.error),
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton(
                    onPressed: () => viewModel.loadHistory(),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          if (viewModel.history.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.history, size: 64, color: Theme.of(context).colorScheme.onSurfaceVariant),
                  const SizedBox(height: 16),
                  Text(
                    'No history yet',
                    style: TextStyle(fontSize: 18, color: Theme.of(context).colorScheme.onSurfaceVariant),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Channels you watch will appear here',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: () => context.read<ChannelViewModel>().loadHistory(),
                    icon: const Icon(Icons.refresh),
                    label: const Text('Refresh'),
                  ),
                ],
              ),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.all(8),
            itemCount: viewModel.history.length,
            // Add explicit item extent for better performance
            itemExtent: 80.0,
            // Enable caching for better scrolling performance
            cacheExtent: 500.0,
            itemBuilder: (context, index) {
              final channel = viewModel.history[index];
              return _HistoryChannelCard(
                key: ValueKey(channel.id),
                channel: channel,
                onPlay: () => _playChannel(context, channel),
              );
            },
          );
        },
      ),
    );
  }

  void _playChannel(BuildContext context, Channel channel) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => PlayerScreen(channel: channel),
      ),
    );
  }

  Future<void> _clearHistory(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear History'),
        content: const Text('Are you sure you want to clear all browse history?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Theme.of(context).colorScheme.error),
            child: const Text('Clear'),
          ),
        ],
      ),
    );

    if (confirmed == true && context.mounted) {
      try {
        await context.read<ChannelViewModel>().clearHistory();
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('History cleared')),
          );
        }
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Failed to clear history: $e'),
              backgroundColor: Theme.of(context).colorScheme.error,
            ),
          );
        }
      }
    }
  }
}

class _HistoryChannelCard extends StatelessWidget {
  final Channel channel;
  final VoidCallback onPlay;

  const _HistoryChannelCard({
    super.key,
    required this.channel,
    required this.onPlay,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.symmetric(vertical: 4, horizontal: 8),
      child: ListTile(
        leading: channel.logoUrl != null && channel.logoUrl!.isNotEmpty
            ? CachedNetworkImage(
                imageUrl: channel.logoUrl!,
                width: 50,
                height: 50,
                fit: BoxFit.contain,
                placeholder: (context, url) => const SizedBox(
                  width: 50,
                  height: 50,
                  child: Center(
                    child: SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  ),
                ),
                errorWidget: (context, url, error) => const Icon(Icons.tv, size: 40),
                maxHeightDiskCache: 200,
                maxWidthDiskCache: 200,
                memCacheHeight: 200,
                memCacheWidth: 200,
              )
            : const Icon(Icons.tv, size: 40),
        title: Text(
          channel.name,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(fontWeight: FontWeight.w500),
        ),
        subtitle: channel.category != null
            ? Text(
                channel.category!,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontSize: 12),
              )
            : null,
        trailing: IconButton(
          icon: Icon(Icons.play_arrow, color: Theme.of(context).colorScheme.primary),
          onPressed: onPlay,
          tooltip: 'Play',
        ),
        onTap: onPlay,
      ),
    );
  }
}
