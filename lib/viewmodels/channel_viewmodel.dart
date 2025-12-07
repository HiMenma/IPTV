import 'package:flutter/foundation.dart';
import '../models/channel.dart';
import '../models/favorite.dart';
import '../models/browse_history.dart';
import '../repositories/favorite_repository.dart';
import '../repositories/history_repository.dart';
import '../repositories/configuration_repository.dart';
import '../services/xtream_service.dart';
import '../services/m3u_service.dart';
import '../models/configuration.dart';

class ChannelViewModel extends ChangeNotifier {
  final FavoriteRepository _favoriteRepository;
  final HistoryRepository _historyRepository;
  final ConfigurationRepository _configRepository;
  final XtreamService _xtreamService;
  final M3UService _m3uService;

  List<Channel> _channels = [];
  List<Channel> _favorites = [];
  List<Channel> _history = [];
  bool _isLoading = false;
  String? _error;

  ChannelViewModel({
    FavoriteRepository? favoriteRepository,
    HistoryRepository? historyRepository,
    ConfigurationRepository? configRepository,
    XtreamService? xtreamService,
    M3UService? m3uService,
  })  : _favoriteRepository = favoriteRepository ?? FavoriteRepository(),
        _historyRepository = historyRepository ?? HistoryRepository(),
        _configRepository = configRepository ?? ConfigurationRepository(),
        _xtreamService = xtreamService ?? XtreamService(),
        _m3uService = m3uService ?? M3UService();

  List<Channel> get channels => List.unmodifiable(_channels);
  List<Channel> get favorites => List.unmodifiable(_favorites);
  List<Channel> get history => List.unmodifiable(_history);
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Load channels from a specific configuration
  Future<void> loadChannels(String configId) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final config = await _configRepository.getById(configId);
      if (config == null) {
        throw Exception('Configuration not found');
      }

      switch (config.type) {
        case ConfigType.xtream:
          final serverUrl = config.credentials['serverUrl'] as String;
          final username = config.credentials['username'] as String;
          final password = config.credentials['password'] as String;
          _channels = await _xtreamService.getChannels(
            serverUrl,
            username,
            password,
            config.id,
          );
          break;

        case ConfigType.m3uNetwork:
          final url = config.credentials['url'] as String;
          _channels = await _m3uService.parseNetworkFile(url, config.id);
          break;

        case ConfigType.m3uLocal:
          final filePath = config.credentials['filePath'] as String;
          _channels = await _m3uService.parseLocalFile(filePath, config.id);
          break;
      }
    } catch (e) {
      _error = 'Failed to load channels: $e';
      _channels = [];
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Load favorite channels
  Future<void> loadFavorites() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final favoriteRecords = await _favoriteRepository.getAll();
      
      // Get all configurations to find channels
      final configs = await _configRepository.getAll();
      final allChannels = <Channel>[];
      
      // Load channels from all configurations
      for (final config in configs) {
        try {
          List<Channel> configChannels;
          
          switch (config.type) {
            case ConfigType.xtream:
              final serverUrl = config.credentials['serverUrl'] as String;
              final username = config.credentials['username'] as String;
              final password = config.credentials['password'] as String;
              configChannels = await _xtreamService.getChannels(
                serverUrl,
                username,
                password,
                config.id,
              );
              break;

            case ConfigType.m3uNetwork:
              final url = config.credentials['url'] as String;
              configChannels = await _m3uService.parseNetworkFile(url, config.id);
              break;

            case ConfigType.m3uLocal:
              final filePath = config.credentials['filePath'] as String;
              configChannels = await _m3uService.parseLocalFile(filePath, config.id);
              break;
          }
          
          allChannels.addAll(configChannels);
        } catch (e) {
          // Skip configurations that fail to load
          continue;
        }
      }
      
      // Filter channels that are in favorites
      _favorites = allChannels
          .where((channel) => favoriteRecords.any((fav) => fav.channelId == channel.id))
          .toList();
    } catch (e) {
      _error = 'Failed to load favorites: $e';
      _favorites = [];
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Load browse history
  Future<void> loadHistory() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final historyRecords = await _historyRepository.getAll();
      
      // Get all configurations to find channels
      final configs = await _configRepository.getAll();
      final allChannels = <Channel>[];
      
      // Load channels from all configurations
      for (final config in configs) {
        try {
          List<Channel> configChannels;
          
          switch (config.type) {
            case ConfigType.xtream:
              final serverUrl = config.credentials['serverUrl'] as String;
              final username = config.credentials['username'] as String;
              final password = config.credentials['password'] as String;
              configChannels = await _xtreamService.getChannels(
                serverUrl,
                username,
                password,
                config.id,
              );
              break;

            case ConfigType.m3uNetwork:
              final url = config.credentials['url'] as String;
              configChannels = await _m3uService.parseNetworkFile(url, config.id);
              break;

            case ConfigType.m3uLocal:
              final filePath = config.credentials['filePath'] as String;
              configChannels = await _m3uService.parseLocalFile(filePath, config.id);
              break;
          }
          
          allChannels.addAll(configChannels);
        } catch (e) {
          // Skip configurations that fail to load
          continue;
        }
      }
      
      // Build a map of channels by ID for quick lookup
      final channelMap = {for (var channel in allChannels) channel.id: channel};
      
      // Filter and order channels by history (already sorted by repository)
      _history = historyRecords
          .map((record) => channelMap[record.channelId])
          .where((channel) => channel != null)
          .cast<Channel>()
          .toList();
    } catch (e) {
      _error = 'Failed to load history: $e';
      _history = [];
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Toggle favorite status of a channel
  Future<void> toggleFavorite(String channelId) async {
    try {
      final isFavorite = await _favoriteRepository.isFavorite(channelId);
      
      if (isFavorite) {
        await _favoriteRepository.remove(channelId);
      } else {
        await _favoriteRepository.add(channelId);
      }
      
      // Reload favorites to update the list
      await loadFavorites();
    } catch (e) {
      _error = 'Failed to toggle favorite: $e';
      notifyListeners();
      rethrow;
    }
  }

  /// Clear all browse history
  Future<void> clearHistory() async {
    try {
      await _historyRepository.clear();
      _history = [];
      notifyListeners();
    } catch (e) {
      _error = 'Failed to clear history: $e';
      notifyListeners();
      rethrow;
    }
  }
}
