import 'package:flutter/foundation.dart';
import '../models/configuration.dart';
import '../repositories/configuration_repository.dart';
import '../repositories/channel_cache_repository_sqlite.dart';
import '../services/xtream_service.dart';
import '../services/m3u_service.dart';
import '../utils/app_logger.dart';

class ConfigurationViewModel extends ChangeNotifier {
  final ConfigurationRepository _repository;
  final ChannelCacheRepositorySQLite _cacheRepository;
  final XtreamService _xtreamService;
  final M3UService _m3uService;

  List<Configuration> _configurations = [];
  bool _isLoading = false;
  String? _error;

  ConfigurationViewModel({
    ConfigurationRepository? repository,
    ChannelCacheRepositorySQLite? cacheRepository,
    XtreamService? xtreamService,
    M3UService? m3uService,
  })  : _repository = repository ?? ConfigurationRepository(),
        _cacheRepository = cacheRepository ?? ChannelCacheRepositorySQLite(),
        _xtreamService = xtreamService ?? XtreamService(),
        _m3uService = m3uService ?? M3UService();

  List<Configuration> get configurations => _configurations;
  bool get isLoading => _isLoading;
  String? get error => _error;

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

  /// 清除指定配置的全部缓存。
  ///
  /// 仅清数据库缓存是不够的：Service 层还有一层内存缓存
  /// （Xtream 10 分钟 / M3U 5 分钟），不清掉会让"刷新配置"看起来完全无效。
  Future<void> refreshConfiguration(String id) async {
    try {
      await _cacheRepository.clearCache(id);
      // Xtream 缓存 key 为 'channels:$configId'，可精确清除
      _xtreamService.clearCache(id);
      // M3U 缓存 key 含 url/filePath 而非 configId，无法按配置精确匹配，只能全清
      _m3uService.clearAllCache();
      AppLogger.log('ConfigVM: Cache cleared for $id (db + service memory)');
      notifyListeners();
    } catch (e) {
      AppLogger.log('ConfigVM: Refresh failed: $e');
    }
  }
}
