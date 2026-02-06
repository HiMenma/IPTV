import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/favorite.dart';

class FavoriteRepository {
  static const String _storageKey = 'favorites';
  static const String _backupKey = 'favorites_backup';

  /// Get all favorites from storage
  /// Requirements: 6.3
  Future<List<Favorite>> getAll() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final jsonString = prefs.getString(_storageKey);
      
      if (jsonString == null) {
        return [];
      }

      try {
        final Map<String, dynamic> data = json.decode(jsonString);
        final List<dynamic> favoriteList = data['favorites'] ?? [];
        return favoriteList
            .map((item) => Favorite.fromJson(item as Map<String, dynamic>))
            .toList();
      } on FormatException catch (e) {
        // Corrupted data - try to restore from backup
        print('Favorites data corrupted: $e. Attempting to restore from backup.');
        return await _restoreFromBackup(prefs);
      } catch (e) {
        // Other parsing errors - try backup
        print('Error parsing favorites: $e. Attempting to restore from backup.');
        return await _restoreFromBackup(prefs);
      }
    } catch (e) {
      print('Error accessing storage: $e');
      return [];
    }
  }

  /// Restore favorites from backup
  Future<List<Favorite>> _restoreFromBackup(SharedPreferences prefs) async {
    try {
      final backupString = prefs.getString(_backupKey);
      
      if (backupString == null) {
        print('No backup available. Returning empty list.');
        return [];
      }

      final Map<String, dynamic> data = json.decode(backupString);
      final List<dynamic> favoriteList = data['favorites'] ?? [];
      final favorites = favoriteList
          .map((item) => Favorite.fromJson(item as Map<String, dynamic>))
          .toList();
      
      // Restore the main storage from backup
      await prefs.setString(_storageKey, backupString);
      print('Successfully restored ${favorites.length} favorites from backup.');
      
      return favorites;
    } catch (e) {
      print('Failed to restore from backup: $e. Returning empty list.');
      // Clear corrupted data and start fresh
      await prefs.remove(_storageKey);
      await prefs.remove(_backupKey);
      return [];
    }
  }

  Future<bool> isFavorite(String channelId) async {
    final favorites = await getAll();
    return favorites.any((favorite) => favorite.channelId == channelId);
  }

  Future<void> add(String channelId) async {
    final favorites = await getAll();
    
    // Don't add if already exists
    if (favorites.any((f) => f.channelId == channelId)) {
      return;
    }
    
    favorites.add(Favorite(
      channelId: channelId,
      addedAt: DateTime.now(),
    ));
    
    await _saveAll(favorites);
  }

  Future<void> remove(String channelId) async {
    final favorites = await getAll();
    favorites.removeWhere((favorite) => favorite.channelId == channelId);
    await _saveAll(favorites);
  }

  /// Save all favorites to storage with backup
  /// Requirements: 6.3
  Future<void> _saveAll(List<Favorite> favorites) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = {
        'favorites': favorites.map((f) => f.toJson()).toList(),
      };
      final jsonString = json.encode(data);
      
      // Create backup of current data before overwriting
      final currentData = prefs.getString(_storageKey);
      if (currentData != null) {
        try {
          await prefs.setString(_backupKey, currentData);
        } catch (e) {
          print('Warning: Failed to create backup: $e');
          // Continue with save even if backup fails
        }
      }
      
      // Save new data
      final success = await prefs.setString(_storageKey, jsonString);
      
      if (!success) {
        throw Exception('Failed to save favorites. Storage may be full.');
      }
    } on Exception catch (e) {
      // Check if it's a disk full error
      if (e.toString().contains('full') || e.toString().contains('space')) {
        throw Exception('Storage is full. Please free up space and try again.');
      }
      throw Exception('Failed to save favorites: $e');
    }
  }
}
