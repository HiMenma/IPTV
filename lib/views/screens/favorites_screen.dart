import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../viewmodels/channel_viewmodel.dart';
import '../../models/channel.dart';
import '../widgets/channel_item.dart';
import 'player_screen.dart';

class FavoritesScreen extends StatefulWidget {
  const FavoritesScreen({super.key});

  @override
  State<FavoritesScreen> createState() => _FavoritesScreenState();
}

class _FavoritesScreenState extends State<FavoritesScreen> with AutomaticKeepAliveClientMixin {
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
      await context.read<ChannelViewModel>().loadFavorites();
      debugPrint('Favorites loaded successfully');
    } catch (e) {
      debugPrint('Error loading favorites: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context); // Required for AutomaticKeepAliveClientMixin
    return Scaffold(
      appBar: AppBar(
        title: const Text('Favorites'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => context.read<ChannelViewModel>().loadFavorites(),
            tooltip: 'Refresh',
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
                    onPressed: () => viewModel.loadFavorites(),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          if (viewModel.favorites.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.favorite_border, size: 64, color: Theme.of(context).colorScheme.onSurfaceVariant),
                  const SizedBox(height: 16),
                  Text(
                    'No favorites yet',
                    style: TextStyle(fontSize: 18, color: Theme.of(context).colorScheme.onSurfaceVariant),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Mark channels as favorites to see them here',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 16),
                  ElevatedButton.icon(
                    onPressed: () => context.read<ChannelViewModel>().loadFavorites(),
                    icon: const Icon(Icons.refresh),
                    label: const Text('Refresh'),
                  ),
                ],
              ),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.all(8),
            itemCount: viewModel.favorites.length,
            // Add explicit item extent for better performance
            itemExtent: 80.0,
            // Enable caching for better scrolling performance
            cacheExtent: 500.0,
            itemBuilder: (context, index) {
              final channel = viewModel.favorites[index];
              return ChannelItem(
                key: ValueKey(channel.id),
                channel: channel,
                onTap: () => _playChannel(context, channel),
                isFavorite: true,
                onToggleFavorite: () => _removeFavorite(context, channel),
                showFavoriteButton: true,
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

  Future<void> _removeFavorite(BuildContext context, Channel channel) async {
    try {
      final viewModel = context.read<ChannelViewModel>();
      
      // Remove from favorites without reloading everything
      await viewModel.toggleFavorite(channel.id);
      
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Removed "${channel.name}" from favorites')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to remove favorite: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    }
  }
}


