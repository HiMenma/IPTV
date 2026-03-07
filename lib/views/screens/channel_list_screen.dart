import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'dart:io';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:path_provider/path_provider.dart';
import '../../models/configuration.dart';
import '../../models/channel.dart';
import '../../viewmodels/channel_viewmodel.dart';
import '../../services/m3u_service.dart';
import '../widgets/channel_list_item.dart';
import '../widgets/channel_grid_item.dart';
import 'player_screen.dart';

class ChannelListScreen extends StatefulWidget {
  final Configuration configuration;

  const ChannelListScreen({super.key, required this.configuration});

  @override
  State<ChannelListScreen> createState() => _ChannelListScreenState();
}

class _ChannelListScreenState extends State<ChannelListScreen> {
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';
  String? _selectedCategory;
  
  // Selection mode state
  bool _isSelectionMode = false;
  final Set<String> _selectedChannelIds = {};

  // View mode state
  bool _isGridView = false;

  @override
  void initState() {
    super.initState();
    Future.microtask(
      () => context.read<ChannelViewModel>().loadChannels(widget.configuration.id),
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<ChannelViewModel>(
      builder: (context, viewModel, child) {
        final filteredChannels = _getFilteredChannels(viewModel.channels);
        final categories = _getCategories(viewModel.channels);

        return Scaffold(
          appBar: AppBar(
            title: _isSelectionMode 
              ? Text('${_selectedChannelIds.length} Selected')
              : Text(widget.configuration.name),
            leading: _isSelectionMode 
              ? IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () => setState(() {
                    _isSelectionMode = false;
                    _selectedChannelIds.clear();
                  }),
                )
              : null,
            actions: [
              if (_isSelectionMode) ...[
                IconButton(
                  icon: const Icon(Icons.select_all),
                  onPressed: () => setState(() {
                    if (_selectedChannelIds.length == filteredChannels.length) {
                      _selectedChannelIds.clear();
                    } else {
                      _selectedChannelIds.addAll(filteredChannels.map((c) => c.id));
                    }
                  }),
                ),
                IconButton(
                  icon: const Icon(Icons.download),
                  tooltip: 'Export to M3U',
                  onPressed: _selectedChannelIds.isEmpty ? null : () => _exportSelected(filteredChannels),
                ),
              ] else ...[
                IconButton(
                  icon: Icon(_isGridView ? Icons.view_list : Icons.grid_view),
                  tooltip: _isGridView ? 'List View' : 'Grid View',
                  onPressed: () => setState(() => _isGridView = !_isGridView),
                ),
                IconButton(
                  icon: const Icon(Icons.refresh),
                  onPressed: () => viewModel.loadChannels(widget.configuration.id, forceRefresh: true),
                ),
              ],
            ],
          ),
          body: Column(
            children: [
              if (!_isSelectionMode) _buildSearchAndFilter(categories),
              Expanded(
                child: _buildChannelContainer(viewModel, filteredChannels),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildSearchAndFilter(List<String> categories) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Column(
        children: [
          TextField(
            controller: _searchController,
            decoration: InputDecoration(
              hintText: 'Search channels...',
              prefixIcon: const Icon(Icons.search),
              suffixIcon: _searchQuery.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear),
                      onPressed: () {
                        _searchController.clear();
                        setState(() => _searchQuery = '');
                      },
                    )
                  : null,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
            ),
            onChanged: (value) => setState(() => _searchQuery = value),
          ),
          if (categories.isNotEmpty) ...[
            const SizedBox(height: 8),
            SizedBox(
              height: 40,
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: categories.length + 1,
                itemBuilder: (context, index) {
                  final category = index == 0 ? null : categories[index - 1];
                  final isSelected = _selectedCategory == category;
                  return Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: FilterChip(
                      label: Text(category ?? 'All'),
                      selected: isSelected,
                      onSelected: (selected) {
                        setState(() => _selectedCategory = selected ? category : null);
                      },
                    ),
                  );
                },
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildChannelContainer(ChannelViewModel viewModel, List<Channel> channels) {
    if (viewModel.isLoading) return const Center(child: CircularProgressIndicator());
    if (viewModel.error != null) return Center(child: Text(viewModel.error!, style: const TextStyle(color: Colors.red)));
    if (channels.isEmpty) return const Center(child: Text('No channels found'));

    if (_isGridView) {
      return GridView.builder(
        padding: const EdgeInsets.all(12),
        gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
          maxCrossAxisExtent: 150,
          childAspectRatio: 0.8,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
        ),
        itemCount: channels.length,
        itemBuilder: (context, index) {
          final channel = channels[index];
          final isSelected = _selectedChannelIds.contains(channel.id);
          return ChannelGridItem(
            channel: channel,
            isFavorite: viewModel.isFavorite(channel.id),
            isSelected: _isSelectionMode ? isSelected : false,
            onTap: () {
              if (_isSelectionMode) {
                _toggleSelection(channel.id);
              } else {
                _navigateToPlayer(context, channel);
              }
            },
            onLongPress: () {
              if (!_isSelectionMode) {
                setState(() {
                  _isSelectionMode = true;
                  _selectedChannelIds.add(channel.id);
                });
              }
            },
            onFavoriteToggle: () => viewModel.toggleFavorite(channel.id),
          );
        },
      );
    }

    return ListView.builder(
      padding: const EdgeInsets.symmetric(vertical: 8),
      itemCount: channels.length,
      itemBuilder: (context, index) {
        final channel = channels[index];
        final isSelected = _selectedChannelIds.contains(channel.id);

        return ChannelListItem(
          channel: channel,
          isFavorite: viewModel.isFavorite(channel.id),
          isSelected: _isSelectionMode ? isSelected : false,
          onTap: () {
            if (_isSelectionMode) {
              _toggleSelection(channel.id);
            } else {
              _navigateToPlayer(context, channel);
            }
          },
          onLongPress: () {
            if (!_isSelectionMode) {
              setState(() {
                _isSelectionMode = true;
                _selectedChannelIds.add(channel.id);
              });
            }
          },
          onFavoriteToggle: () => viewModel.toggleFavorite(channel.id),
        );
      },
    );
  }

  void _toggleSelection(String id) {
    setState(() {
      if (_selectedChannelIds.contains(id)) {
        _selectedChannelIds.remove(id);
        if (_selectedChannelIds.isEmpty) _isSelectionMode = false;
      } else {
        _selectedChannelIds.add(id);
      }
    });
  }

  List<Channel> _getFilteredChannels(List<Channel> allChannels) {
    return allChannels.where((channel) {
      final matchesSearch = channel.name.toLowerCase().contains(_searchQuery.toLowerCase());
      final matchesCategory = _selectedCategory == null || channel.category == _selectedCategory;
      return matchesSearch && matchesCategory;
    }).toList();
  }

  List<String> _getCategories(List<Channel> allChannels) {
    final categories = allChannels
        .map((c) => c.category)
        .where((cat) => cat != null && cat.isNotEmpty)
        .cast<String>()
        .toSet()
        .toList();
    categories.sort();
    return categories;
  }

  void _navigateToPlayer(BuildContext context, Channel channel) {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => PlayerScreen(channel: channel)),
    );
  }

  Future<void> _exportSelected(List<Channel> allFiltered) async {
    final selectedChannels = allFiltered.where((c) => _selectedChannelIds.contains(c.id)).toList();
    if (selectedChannels.isEmpty) return;

    final m3uContent = M3UService().exportToM3U(selectedChannels);
    
    try {
      if (kIsWeb) {
        await Clipboard.setData(ClipboardData(text: m3uContent));
        if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('M3U content copied to clipboard (Web)')));
      } else {
        final directory = await getTemporaryDirectory();
        final file = File('${directory.path}/exported_channels.m3u');
        await file.writeAsString(m3uContent);
        
        await Clipboard.setData(ClipboardData(text: m3uContent));
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Exported ${selectedChannels.length} channels. Content copied to clipboard.'))
          );
        }
      }
      setState(() {
        _isSelectionMode = false;
        _selectedChannelIds.clear();
      });
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Export failed: $e')));
    }
  }
}
