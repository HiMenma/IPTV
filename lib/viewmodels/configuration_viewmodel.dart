import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';
import '../models/configuration.dart';
import '../models/channel.dart';
import '../repositories/configuration_repository.dart';
import '../services/xtream_service.dart';
import '../services/m3u_service.dart';

class ConfigurationViewModel extends ChangeNotifier {
  final ConfigurationRepository _configRepository;
  final XtreamService _xtreamService;
  final M3UService _m3uService;
  final Uuid _uuid;

  List<Configuration> _configurations = [];
  bool _isLoading = false;
  String? _error;

  ConfigurationViewModel({
    ConfigurationRepository? configRepository,
    XtreamService? xtreamService,
    M3UService? m3uService,
    Uuid? uuid,
  })  : _configRepository = configRepository ?? ConfigurationRepository(),
        _xtreamService = xtreamService ?? XtreamService(),
        _m3uService = m3uService ?? M3UService(),
        _uuid = uuid ?? const Uuid();

  List<Configuration> get configurations => List.unmodifiable(_configurations);
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Load all configurations from storage
  Future<void> loadConfigurations() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _configurations = await _configRepository.getAll();
    } catch (e) {
      _error = 'Failed to load configurations: $e';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Create a new configuration
  Future<void> createConfiguration(
    String name,
    ConfigType type,
    Map<String, dynamic> credentials,
  ) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final config = Configuration(
        id: _uuid.v4(),
        name: name,
        type: type,
        credentials: credentials,
        createdAt: DateTime.now(),
      );

      await _configRepository.save(config);
      _configurations.add(config);
    } catch (e) {
      _error = 'Failed to create configuration: $e';
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Rename an existing configuration
  Future<void> renameConfiguration(String id, String newName) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final config = await _configRepository.getById(id);
      if (config == null) {
        throw Exception('Configuration not found');
      }

      final updatedConfig = Configuration(
        id: config.id,
        name: newName,
        type: config.type,
        credentials: config.credentials,
        createdAt: config.createdAt,
        lastRefreshed: config.lastRefreshed,
      );

      await _configRepository.update(updatedConfig);

      final index = _configurations.indexWhere((c) => c.id == id);
      if (index != -1) {
        _configurations[index] = updatedConfig;
      }
    } catch (e) {
      _error = 'Failed to rename configuration: $e';
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Delete a configuration
  Future<void> deleteConfiguration(String id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      await _configRepository.delete(id);
      _configurations.removeWhere((c) => c.id == id);
    } catch (e) {
      _error = 'Failed to delete configuration: $e';
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Refresh a configuration (fetch latest channels)
  Future<List<Channel>> refreshConfiguration(String id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final config = await _configRepository.getById(id);
      if (config == null) {
        throw Exception('Configuration not found');
      }

      List<Channel> channels;

      switch (config.type) {
        case ConfigType.xtream:
          final serverUrl = config.credentials['serverUrl'] as String;
          final username = config.credentials['username'] as String;
          final password = config.credentials['password'] as String;
          channels = await _xtreamService.getChannels(
            serverUrl,
            username,
            password,
            config.id,
            forceRefresh: true, // Force refresh to invalidate cache
          );
          break;

        case ConfigType.m3uNetwork:
          final url = config.credentials['url'] as String;
          channels = await _m3uService.parseNetworkFile(url, config.id, forceRefresh: true);
          break;

        case ConfigType.m3uLocal:
          final filePath = config.credentials['filePath'] as String;
          channels = await _m3uService.parseLocalFile(filePath, config.id, forceRefresh: true);
          break;
      }

      // Update lastRefreshed timestamp
      final updatedConfig = Configuration(
        id: config.id,
        name: config.name,
        type: config.type,
        credentials: config.credentials,
        createdAt: config.createdAt,
        lastRefreshed: DateTime.now(),
      );

      await _configRepository.update(updatedConfig);

      final index = _configurations.indexWhere((c) => c.id == id);
      if (index != -1) {
        _configurations[index] = updatedConfig;
      }

      return channels;
    } catch (e) {
      _error = 'Failed to refresh configuration: $e';
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Export configuration channels to M3U format
  Future<String> exportToM3U(String id) async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      final config = await _configRepository.getById(id);
      if (config == null) {
        throw Exception('Configuration not found');
      }

      // Get channels from the configuration
      final channels = await refreshConfiguration(id);

      // Export to M3U format
      return _m3uService.exportToM3U(channels);
    } catch (e) {
      _error = 'Failed to export to M3U: $e';
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
