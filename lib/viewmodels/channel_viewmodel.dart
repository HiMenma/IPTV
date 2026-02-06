import 'package:flutter/foundation.dart';
import '../models/channel.dart';
import '../models/favorite.dart';
import '../models/browse_history.dart';
import '../repositories/favorite_repository_sqlite.dart';
import '../repositories/history_repository_sqlite.dart';
import '../repositories/configuration_repository.dart';
import '../repositories/channel_cache_repository_sqlite.dart';
import '../services/xtream_service.dart';
import '../services/m3u_service.dart';
import '../models/configuration.dart';

class ChannelViewModel extends ChangeNotifier {
  final FavoriteRepositorySQLite _favoriteRepository;
  final HistoryRepositorySQLite _historyRepository;
  final ConfigurationRepository _configRepository;
  final ChannelCacheRepositorySQLite _cacheRepository;
  final XtreamService _xtreamService;
  final M3UService _m3uService;

  List<Channel> _channels = [];
  List<Channel> _favorites = [];
  List<Channel> _history = [];
  Set<String> _favoriteIds = {};
  bool _isLoading = false;
  String? _error;

  ChannelViewModel({
    FavoriteRepositorySQLite? favoriteRepository,
    HistoryRepositorySQLite? historyRepository,
    ConfigurationRepository? configRepository,
    ChannelCacheRepositorySQLite? cacheRepository,
    XtreamService? xtreamService,
    M3UService? m3uService,
  })  : _favoriteRepository = favoriteRepository ?? FavoriteRepositorySQLite(),
        _historyRepository = historyRepository ?? HistoryRepositorySQLite(),
        _configRepository = configRepository ?? ConfigurationRepository(),
        _cacheRepository = cacheRepository ?? ChannelCacheRepositorySQLite(),
        _xtreamService = xtreamService ?? XtreamService(),
        _m3uService = m3uService ?? M3UService();

  List<Channel> get channels => List.unmodifiable(_channels);
  List<Channel> get favorites => List.unmodifiable(_favorites);
  List<Channel> get history => List.unmodifiable(_history);
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Load channels from a specific configuration
  /// Uses cache if available, otherwise loads from source
  Future<void> loadChannels(String configId, {bool forceRefresh = false}) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final config = await _configRepository.getById(configId);
      if (config == null) {
        throw Exception('Configuration not found');
      }

      // Try to load from cache first (unless force refresh)
      if (!forceRefresh) {
        final cachedChannels = await _cacheRepository.loadChannels(configId);
        if (cachedChannels != null && cachedChannels.isNotEmpty) {
          _channels = cachedChannels;
          debugPrint('Loaded ${_channels.length} channels from cache for $configId');
          
          // Load favorite IDs for quick lookup
          await _loadFavoriteIds();
          
          _isLoading = false;
          notifyListeners();
          return;
        }
      }

      // Load from source
      debugPrint('Loading channels from source for $configId (forceRefresh: $forceRefresh)');
      
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
      
      // Save to cache
      await _cacheRepository.saveChannels(configId, _channels);
      debugPrint('Saved ${_channels.length} channels to cache for $configId');
      
      // Load favorite IDs for quick lookup
      await _loadFavoriteIds();
    } catch (e) {
      _error = 'Failed to load channels: $e';
      _channels = [];
      debugPrint('Error loading channels: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Load favorite IDs into memory for quick lookup
  Future<void> _loadFavoriteIds() async {
    try {
      final favoriteRecords = await _favoriteRepository.getAll();
      _favoriteIds = favoriteRecords.map((f) => f.channelId).toSet();
    } catch (e) {
      _favoriteIds = {};
    }
  }

  /// Load favorite channels
  /// Loads ONLY from SQLite cache, never from network
  Future<void> loadFavorites() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final favoriteRecords = await _favoriteRepository.getAll();
      _favoriteIds = favoriteRecords.map((f) => f.channelId).toSet();
      
      debugPrint('Loading favorites: ${favoriteRecords.length} favorite records found');
      debugPrint('Favorite IDs: $_favoriteIds');
      
      // Get all configurations to find channels
      final configs = await _configRepository.getAll();
      debugPrint('Found ${configs.length} configurations');
      final allChannels = <Channel>[];
      
      // Load channels ONLY from cache (never from network)
      for (final config in configs) {
        try {
          // Load from cache only
          final configChannels = await _cacheRepository.loadChannels(config.id);
          
          if (configChannels != null && configChannels.isNotEmpty) {
            debugPrint('Loaded ${configChannels.length} channels from cache for config ${config.id}');
            allChannels.addAll(configChannels);
          } else {
            debugPrint('No cache found for config ${config.id}');
          }
        } catch (e) {
          debugPrint('Failed to load cache for config ${config.id}: $e');
          continue;
        }
      }
      
      // Filter channels that are in favorites
      _favorites = allChannels
          .where((channel) => _favoriteIds.contains(channel.id))
          .toList();
      
      debugPrint('Loaded ${_favorites.length} favorite channels from cache');
    } catch (e) {
      _error = 'Failed to load favorites: $e';
      _favorites = [];
      debugPrint('Error loading favorites: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Load browse history
  /// Loads ONLY from SQLite cache, never from network
  Future<void> loadHistory() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final historyRecords = await _historyRepository.getAll();
      debugPrint('Loading history: ${historyRecords.length} history records found');
      
      // Get all configurations to find channels
      final configs = await _configRepository.getAll();
      debugPrint('Found ${configs.length} configurations');
      final allChannels = <Channel>[];
      
      // Load channels ONLY from cache (never from network)
      for (final config in configs) {
        try {
          // Load from cache only
          final configChannels = await _cacheRepository.loadChannels(config.id);
          
          if (configChannels != null && configChannels.isNotEmpty) {
            debugPrint('Loaded ${configChannels.length} channels from cache for config ${config.id}');
            allChannels.addAll(configChannels);
          } else {
            debugPrint('No cache found for config ${config.id}');
          }
        } catch (e) {
          debugPrint('Failed to load cache for config ${config.id}: $e');
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
      
      debugPrint('Loaded ${_history.length} history channels from cache');
    } catch (e) {
      _error = 'Failed to load history: $e';
      _history = [];
      debugPrint('Error loading history: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Check if a channel is favorited (synchronous, uses cached data)
  bool isFavorite(String channelId) {
    return _favoriteIds.contains(channelId);
  }

  /// Toggle favorite status of a channel
  Future<void> toggleFavorite(String channelId) async {
    try {
      final wasFavorite = _favoriteIds.contains(channelId);
      
      if (wasFavorite) {
        await _favoriteRepository.remove(channelId);
        _favoriteIds.remove(channelId);
        
        // Remove from favorites list without reloading
        _favorites.removeWhere((channel) => channel.id == channelId);
      } else {
        await _favoriteRepository.add(channelId);
        _favoriteIds.add(channelId);
        
        // Note: When adding a favorite, we don't add to _favorites list here
        // because we don't have the Channel object. It will be loaded next time
        // the favorites screen is opened.
      }
      
      // Notify listeners to update UI
      notifyListeners();
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
