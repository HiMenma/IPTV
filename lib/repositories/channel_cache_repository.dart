import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/channel.dart';

/// Repository for caching channel data locally
/// Channels are cached permanently until manually refreshed
class ChannelCacheRepository {
  static const String _prefix = 'channels_cache_';
  static const String _timestampPrefix = 'channels_timestamp_';

  /// Save channels to cache for a specific configuration
  Future<void> saveChannels(String configId, List<Channel> channels) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = {
        'channels': channels.map((c) => c.toJson()).toList(),
      };
      final jsonString = json.encode(data);
      
      // Save channels
      await prefs.setString('$_prefix$configId', jsonString);
      
      // Save timestamp
      await prefs.setString(
        '$_timestampPrefix$configId',
        DateTime.now().toIso8601String(),
      );
      
      print('Cached ${channels.length} channels for config $configId');
    } catch (e) {
      print('Error saving channels to cache: $e');
      rethrow;
    }
  }

  /// Load channels from cache for a specific configuration
  /// Returns null if no cache exists
  Future<List<Channel>?> loadChannels(String configId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final jsonString = prefs.getString('$_prefix$configId');
      
      if (jsonString == null) {
        print('No cache found for config $configId');
        return null;
      }

      final data = json.decode(jsonString);
      final List<dynamic> channelList = data['channels'] ?? [];
      final channels = channelList
          .map((item) => Channel.fromJson(item as Map<String, dynamic>))
          .toList();
      
      print('Loaded ${channels.length} channels from cache for config $configId');
      return channels;
    } catch (e) {
      print('Error loading channels from cache: $e');
      // If cache is corrupted, return null to trigger fresh load
      return null;
    }
  }

  /// Check if cache exists for a configuration
  Future<bool> hasCache(String configId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.containsKey('$_prefix$configId');
    } catch (e) {
      return false;
    }
  }

  /// Get the timestamp when channels were last cached
  Future<DateTime?> getCacheTimestamp(String configId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final timestamp = prefs.getString('$_timestampPrefix$configId');
      
      if (timestamp == null) return null;
      
      return DateTime.parse(timestamp);
    } catch (e) {
      return null;
    }
  }

  /// Clear cache for a specific configuration
  Future<void> clearCache(String configId) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('$_prefix$configId');
      await prefs.remove('$_timestampPrefix$configId');
      print('Cleared cache for config $configId');
    } catch (e) {
      print('Error clearing cache: $e');
    }
  }

  /// Clear all channel caches
  Future<void> clearAllCaches() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final keys = prefs.getKeys();
      
      for (final key in keys) {
        if (key.startsWith(_prefix) || key.startsWith(_timestampPrefix)) {
          await prefs.remove(key);
        }
      }
      
      print('Cleared all channel caches');
    } catch (e) {
      print('Error clearing all caches: $e');
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
}
