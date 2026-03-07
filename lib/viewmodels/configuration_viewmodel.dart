import 'package:flutter/foundation.dart';
import '../models/configuration.dart';
import '../repositories/configuration_repository.dart';
import '../repositories/channel_cache_repository_sqlite.dart';
import '../utils/app_logger.dart';

class ConfigurationViewModel extends ChangeNotifier {
  final ConfigurationRepository _repository;
  final ChannelCacheRepositorySQLite _cacheRepository;

  List<Configuration> _configurations = [];
  bool _isLoading = false;
  String? _error;

  ConfigurationViewModel({
    ConfigurationRepository? repository,
    ChannelCacheRepositorySQLite? cacheRepository,
  })  : _repository = repository ?? ConfigurationRepository(),
        _cacheRepository = cacheRepository ?? ChannelCacheRepositorySQLite();

  List<Configuration> get configurations => _configurations;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Load all configurations from database
  Future<void> loadConfigurations() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _configurations = await _repository.getAll();
    } catch (e) {
      _error = 'Load Error: $e';
      AppLogger.log('ConfigVM: Load failed: $e');
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Reorder configurations and persist to database
  Future<void> reorderConfigurations(int oldIndex, int newIndex) async {
    if (oldIndex < newIndex) {
      newIndex -= 1;
    }
    
    final List<Configuration> items = List.from(_configurations);
    final Configuration item = items.removeAt(oldIndex);
    items.insert(newIndex, item);
    
    _configurations = items;
    notifyListeners();
    
    try {
      await _repository.updateOrder(items);
    } catch (e) {
      _error = 'Reorder sync failed: $e';
      AppLogger.log('ConfigVM: Reorder failed: $e');
      notifyListeners();
    }
  }

  /// Add a new configuration with explicit SQL error catching
  Future<void> addConfiguration(Configuration config) async {
    try {
      _error = null;
      await _repository.add(config);
      await loadConfigurations();
    } catch (e) {
      _error = 'Database Error: ${e.toString()}';
      AppLogger.log('ConfigVM: Add failed: $e');
      notifyListeners();
      rethrow;
    }
  }

  /// Update an existing configuration
  Future<void> updateConfiguration(Configuration config) async {
    try {
      await _repository.update(config);
      await loadConfigurations();
    } catch (e) {
      _error = 'Update failed: $e';
      AppLogger.log('ConfigVM: Update failed: $e');
      notifyListeners();
      rethrow;
    }
  }

  /// Delete a configuration and its cache
  Future<void> deleteConfiguration(String id) async {
    try {
      await _repository.delete(id);
      await _cacheRepository.clearCache(id);
      await loadConfigurations();
    } catch (e) {
      _error = 'Delete failed: $e';
      AppLogger.log('ConfigVM: Delete failed: $e');
      notifyListeners();
      rethrow;
    }
  }

  /// Refresh configuration channels
  Future<void> refreshConfiguration(String id) async {
    try {
      await _cacheRepository.clearCache(id);
      notifyListeners();
    } catch (e) {
      AppLogger.log('ConfigVM: Refresh failed: $e');
    }
  }
}
