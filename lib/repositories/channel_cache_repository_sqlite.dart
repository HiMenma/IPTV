import 'package:sqflite/sqflite.dart';
import '../models/channel.dart';
import '../database/database_helper.dart';

/// SQLite-based repository for caching channel data
class ChannelCacheRepositorySQLite {
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;

  /// Save channels to cache for a specific configuration
  Future<void> saveChannels(String configId, List<Channel> channels) async {
    try {
      final db = await _dbHelper.database;
      final batch = db.batch();

      // Delete existing channels for this config
      batch.delete(
        'channel_cache',
        where: 'config_id = ?',
        whereArgs: [configId],
      );

      // Insert new channels
      final cachedAt = DateTime.now().toIso8601String();
      for (final channel in channels) {
        batch.insert(
          'channel_cache',
          {
            'config_id': configId,
            'channel_id': channel.id,
            'name': channel.name,
            'stream_url': channel.streamUrl,
            'logo_url': channel.logoUrl,
            'category': channel.category,
            'cached_at': cachedAt,
          },
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }

      await batch.commit(noResult: true);
      print('Cached ${channels.length} channels for config $configId');
    } catch (e) {
      print('Error saving channels to cache: $e');
      rethrow;
    }
  }

  /// Load channels from cache for a specific configuration
  Future<List<Channel>?> loadChannels(String configId) async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.query(
        'channel_cache',
        where: 'config_id = ?',
        whereArgs: [configId],
      );

      if (maps.isEmpty) {
        print('No cache found for config $configId');
        return null;
      }

      final channels = maps.map((map) => Channel(
        id: map['channel_id'] as String,
        name: map['name'] as String,
        streamUrl: map['stream_url'] as String,
        configId: configId,
        logoUrl: map['logo_url'] as String?,
        category: map['category'] as String?,
      )).toList();

      print('Loaded ${channels.length} channels from cache for config $configId');
      return channels;
    } catch (e) {
      print('Error loading channels from cache: $e');
      return null;
    }
  }

  /// Check if cache exists for a configuration
  Future<bool> hasCache(String configId) async {
    try {
      final db = await _dbHelper.database;
      final result = await db.rawQuery(
        'SELECT COUNT(*) as count FROM channel_cache WHERE config_id = ?',
        [configId],
      );
      return (Sqflite.firstIntValue(result) ?? 0) > 0;
    } catch (e) {
      print('Error checking cache: $e');
      return false;
    }
  }

  /// Get the timestamp when channels were last cached
  Future<DateTime?> getCacheTimestamp(String configId) async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.query(
        'channel_cache',
        columns: ['cached_at'],
        where: 'config_id = ?',
        whereArgs: [configId],
        limit: 1,
      );

      if (maps.isEmpty) return null;

      return DateTime.parse(maps.first['cached_at'] as String);
    } catch (e) {
      print('Error getting cache timestamp: $e');
      return null;
    }
  }

  /// Clear cache for a specific configuration
  Future<void> clearCache(String configId) async {
    try {
      final db = await _dbHelper.database;
      await db.delete(
        'channel_cache',
        where: 'config_id = ?',
        whereArgs: [configId],
      );
      print('Cleared cache for config $configId');
    } catch (e) {
      print('Error clearing cache: $e');
      rethrow;
    }
  }

  /// Clear all channel caches
  Future<void> clearAllCaches() async {
    try {
      final db = await _dbHelper.database;
      await db.delete('channel_cache');
      print('Cleared all channel caches');
    } catch (e) {
      print('Error clearing all caches: $e');
      rethrow;
    }
  }

  /// Get cache info for debugging
  Future<Map<String, dynamic>> getCacheInfo(String configId) async {
    try {
      final hasCache = await this.hasCache(configId);
      final timestamp = await getCacheTimestamp(configId);
      final channels = await loadChannels(configId);

      return {
        'hasCache': hasCache,
        'timestamp': timestamp?.toIso8601String(),
        'channelCount': channels?.length ?? 0,
      };
    } catch (e) {
      return {
        'hasCache': false,
        'error': e.toString(),
      };
    }
  }

  /// Get total number of cached channels
  Future<int> getTotalCachedChannels() async {
    try {
      final db = await _dbHelper.database;
      final result = await db.rawQuery('SELECT COUNT(*) as count FROM channel_cache');
      return Sqflite.firstIntValue(result) ?? 0;
    } catch (e) {
      print('Error getting total cached channels: $e');
      return 0;
    }
  }

  /// Get list of all cached config IDs
  Future<List<String>> getCachedConfigIds() async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.rawQuery(
        'SELECT DISTINCT config_id FROM channel_cache',
      );

      return maps.map((map) => map['config_id'] as String).toList();
    } catch (e) {
      print('Error getting cached config IDs: $e');
      return [];
    }
  }
}
