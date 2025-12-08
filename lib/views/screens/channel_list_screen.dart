import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../viewmodels/channel_viewmodel.dart';
import '../../viewmodels/configuration_viewmodel.dart';
import '../../models/configuration.dart';
import '../../models/channel.dart';
import '../widgets/channel_item.dart';
import 'player_screen.dart';

class ChannelListScreen extends StatefulWidget {
  final Configuration configuration;

  const ChannelListScreen({super.key, required this.configuration});

  @override
  State<ChannelListScreen> createState() => _ChannelListScreenState();
}

class _ChannelListScreenState extends State<ChannelListScreen> {
  final _searchController = TextEditingController();
  String _searchQuery = '';
  String? _selectedCategory;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      // Load channels (will use cache if available)
      context.read<ChannelViewModel>().loadChannels(widget.configuration.id, forceRefresh: false);
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.configuration.name),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          // Refresh button for network configurations
          if (widget.configuration.type != ConfigType.m3uLocal)
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: _refreshConfiguration,
              tooltip: 'Refresh',
            ),
          // Export button for Xtream configurations
          if (widget.configuration.type == ConfigType.xtream)
            IconButton(
              icon: const Icon(Icons.download),
              onPressed: _exportToM3U,
              tooltip: 'Export to M3U',
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
                    onPressed: () => viewModel.loadChannels(widget.configuration.id),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          if (viewModel.channels.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.tv_off, size: 64, color: Theme.of(context).colorScheme.onSurfaceVariant),
                  const SizedBox(height: 16),
                  Text(
                    'No channels found',
                    style: TextStyle(fontSize: 18, color: Theme.of(context).colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            );
          }

          final filteredChannels = _filterChannels(viewModel.channels);
          final categories = _getCategories(viewModel.channels);

          return Column(
            children: [
              // Search bar
              Padding(
                padding: const EdgeInsets.all(8.0),
                child: TextField(
                  controller: _searchController,
                  decoration: InputDecoration(
                    hintText: 'Search channels...',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: _searchQuery.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear),
                            onPressed: () {
                              setState(() {
                                _searchController.clear();
                                _searchQuery = '';
                              });
                            },
                          )
                        : null,
                    border: const OutlineInputBorder(),
                  ),
                  onChanged: (value) {
                    setState(() {
                      _searchQuery = value.toLowerCase();
                    });
                  },
                ),
              ),

              // Category filter
              if (categories.isNotEmpty)
                SizedBox(
                  height: 50,
                  child: ListView(
                    scrollDirection: Axis.horizontal,
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    children: [
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 4),
                        child: FilterChip(
                          label: const Text('All'),
                          selected: _selectedCategory == null,
                          onSelected: (selected) {
                            setState(() {
                              _selectedCategory = null;
                            });
                          },
                        ),
                      ),
                      ...categories.map((category) => Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 4),
                            child: FilterChip(
                              label: Text(category),
                              selected: _selectedCategory == category,
                              onSelected: (selected) {
                                setState(() {
                                  _selectedCategory = selected ? category : null;
                                });
                              },
                            ),
                          )),
                    ],
                  ),
                ),

              // Channel list
              Expanded(
                child: filteredChannels.isEmpty
                    ? Center(
                        child: Text(
                          'No channels match your search',
                          style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                        ),
                      )
                    : ListView.builder(
                        itemCount: filteredChannels.length,
                        // Add explicit item extent for better performance
                        itemExtent: 80.0,
                        // Enable caching for better scrolling performance
                        cacheExtent: 500.0,
                        itemBuilder: (context, index) {
                          final channel = filteredChannels[index];
                          return Consumer<ChannelViewModel>(
                            builder: (context, channelViewModel, child) {
                              final isFavorite = channelViewModel.isFavorite(channel.id);
                              return ChannelItem(
                                key: ValueKey(channel.id),
                                channel: channel,
                                onTap: () => _playChannel(context, channel),
                                isFavorite: isFavorite,
                                onToggleFavorite: () => _toggleFavorite(context, channel),
                                showFavoriteButton: true,
                              );
                            },
                          );
                        },
                      ),
              ),
            ],
          );
        },
      ),
    );
  }

  List<Channel> _filterChannels(List<Channel> channels) {
    var filtered = channels;

    // Filter by search query
    if (_searchQuery.isNotEmpty) {
      filtered = filtered
          .where((channel) => channel.name.toLowerCase().contains(_searchQuery))
          .toList();
    }

    // Filter by category
    if (_selectedCategory != null) {
      filtered = filtered
          .where((channel) => channel.category == _selectedCategory)
          .toList();
    }

    return filtered;
  }

  List<String> _getCategories(List<Channel> channels) {
    final categories = channels
        .where((channel) => channel.category != null && channel.category!.isNotEmpty)
        .map((channel) => channel.category!)
        .toSet()
        .toList();
    categories.sort();
    return categories;
  }

  void _playChannel(BuildContext context, Channel channel) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => PlayerScreen(channel: channel),
      ),
    );
  }

  Future<void> _toggleFavorite(BuildContext context, Channel channel) async {
    try {
      final viewModel = context.read<ChannelViewModel>();
      final wasFavorite = viewModel.isFavorite(channel.id);
      
      await viewModel.toggleFavorite(channel.id);
      
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              wasFavorite 
                ? 'Removed "${channel.name}" from favorites'
                : 'Added "${channel.name}" to favorites'
            ),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to update favorite: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    }
  }

  Future<void> _refreshConfiguration() async {
    if (!mounted) return;
    
    final configViewModel = context.read<ConfigurationViewModel>();
    final channelViewModel = context.read<ChannelViewModel>();
    final messenger = ScaffoldMessenger.of(context);
    
    try {
      await configViewModel.refreshConfiguration(widget.configuration.id);
      
      if (mounted) {
        // Force refresh to reload from source and update cache
        await channelViewModel.loadChannels(widget.configuration.id, forceRefresh: true);
        
        messenger.showSnackBar(
          const SnackBar(content: Text('Configuration refreshed from source')),
        );
      }
    } catch (e) {
      if (mounted) {
        messenger.showSnackBar(
          SnackBar(
            content: Text('Failed to refresh: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    }
  }

  Future<void> _exportToM3U() async {
    try {
      final m3uContent = await context
          .read<ConfigurationViewModel>()
          .exportToM3U(widget.configuration.id);

      if (mounted) {
        // Show dialog to save file
        final fileName = '${widget.configuration.name.replaceAll(' ', '_')}.m3u';
        
        showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Export to M3U'),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('File: $fileName'),
                const SizedBox(height: 8),
                const Text('M3U content has been generated.'),
                const SizedBox(height: 16),
                Text(
                  'Content preview:\n${m3uContent.substring(0, m3uContent.length > 200 ? 200 : m3uContent.length)}...',
                  style: const TextStyle(fontSize: 12, fontFamily: 'monospace'),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Close'),
              ),
            ],
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to export: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    }
  }
}


