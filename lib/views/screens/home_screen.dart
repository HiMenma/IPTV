import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/configuration.dart';
import '../../viewmodels/configuration_viewmodel.dart';
import '../../providers/theme_provider.dart';
import '../widgets/configuration_card.dart';
import 'configuration_screen.dart';
import 'channel_list_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    Future.microtask(
      () => context.read<ConfigurationViewModel>().loadConfigurations(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('IPTV Player'),
        actions: [
          Consumer<ThemeProvider>(
            builder: (context, themeProvider, child) {
              return PopupMenuButton<ThemeMode>(
                icon: Icon(_getThemeIcon(themeProvider.themeMode)),
                tooltip: 'Theme Mode',
                onSelected: (mode) => themeProvider.setThemeMode(mode),
                itemBuilder: (context) => [
                  const PopupMenuItem(value: ThemeMode.system, child: Text('System Default')),
                  const PopupMenuItem(value: ThemeMode.light, child: Text('Light Mode')),
                  const PopupMenuItem(value: ThemeMode.dark, child: Text('Dark Mode')),
                ],
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Add Configuration',
            onPressed: () => _navigateToAddConfiguration(context),
          ),
        ],
      ),
      body: Consumer<ConfigurationViewModel>(
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
                    onPressed: () => viewModel.loadConfigurations(),
                    child: const Text('Retry'),
                  ),
                ],
              ),
            );
          }

          if (viewModel.configurations.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.tv_off, size: 64, color: Theme.of(context).colorScheme.onSurfaceVariant),
                  const SizedBox(height: 16),
                  Text(
                    'No configurations yet',
                    style: TextStyle(fontSize: 18, color: Theme.of(context).colorScheme.onSurfaceVariant),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Add a configuration to get started',
                    style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            );
          }

          // Use ReorderableListView for drag-and-drop support
          return ReorderableListView.builder(
            padding: const EdgeInsets.all(8),
            itemCount: viewModel.configurations.length,
            onReorder: (oldIndex, newIndex) {
              viewModel.reorderConfigurations(oldIndex, newIndex);
            },
            itemBuilder: (context, index) {
              final config = viewModel.configurations[index];
              return ConfigurationCard(
                key: ValueKey(config.id), // Key is required for reordering
                configuration: config,
                onTap: () => _navigateToChannelList(context, config),
                onEdit: () => _navigateToEditConfiguration(context, config),
                onDelete: () => _deleteConfiguration(context, config),
                onRefresh: () => _refreshConfiguration(context, config),
              );
            },
          );
        },
      ),
    );
  }

  void _navigateToChannelList(BuildContext context, Configuration config) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ChannelListScreen(configuration: config),
      ),
    );
  }

  void _navigateToAddConfiguration(BuildContext context) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => const ConfigurationScreen(),
      ),
    );
  }

  void _navigateToEditConfiguration(
      BuildContext context, Configuration config) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ConfigurationScreen(configuration: config),
      ),
    );
  }

  Future<void> _deleteConfiguration(
      BuildContext context, Configuration config) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Delete Configuration'),
        content: Text('Are you sure you want to delete "${config.name}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(
                foregroundColor: Theme.of(context).colorScheme.error),
            child: const Text('Delete'),
          ),
        ],
      ),
    );

    if (confirmed == true && mounted) {
      context.read<ConfigurationViewModel>().deleteConfiguration(config.id);
    }
  }

  Future<void> _refreshConfiguration(
      BuildContext context, Configuration config) async {
    await context.read<ConfigurationViewModel>().refreshConfiguration(config.id);
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Refreshed ${config.name}')),
      );
    }
  }

  IconData _getThemeIcon(ThemeMode mode) {
    switch (mode) {
      case ThemeMode.system: return Icons.brightness_auto;
      case ThemeMode.light: return Icons.light_mode;
      case ThemeMode.dark: return Icons.dark_mode;
    }
  }
}
