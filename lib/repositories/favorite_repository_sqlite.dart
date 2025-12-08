import 'package:sqflite/sqflite.dart';
import '../models/favorite.dart';
import '../database/database_helper.dart';

/// SQLite-based repository for managing favorites
class FavoriteRepositorySQLite {
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;

  /// Get all favorites from database
  Future<List<Favorite>> getAll() async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.query(
        'favorites',
        orderBy: 'added_at DESC',
      );

      return maps.map((map) => Favorite(
        channelId: map['channel_id'] as String,
        addedAt: DateTime.parse(map['added_at'] as String),
      )).toList();
    } catch (e) {
      print('Error getting favorites: $e');
      return [];
    }
  }

  /// Check if a channel is favorited
  Future<bool> isFavorite(String channelId) async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.query(
        'favorites',
        where: 'channel_id = ?',
        whereArgs: [channelId],
        limit: 1,
      );

      return maps.isNotEmpty;
    } catch (e) {
      print('Error checking favorite: $e');
      return false;
    }
  }

  /// Add a channel to favorites
  Future<void> add(String channelId) async {
    try {
      final db = await _dbHelper.database;
      
      // Check if already exists
      if (await isFavorite(channelId)) {
        return;
      }

      await db.insert(
        'favorites',
        {
          'channel_id': channelId,
          'added_at': DateTime.now().toIso8601String(),
        },
        conflictAlgorithm: ConflictAlgorithm.replace,
      );

      print('Added favorite: $channelId');
    } catch (e) {
      print('Error adding favorite: $e');
      rethrow;
    }
  }

  /// Remove a channel from favorites
  Future<void> remove(String channelId) async {
    try {
      final db = await _dbHelper.database;
      await db.delete(
        'favorites',
        where: 'channel_id = ?',
        whereArgs: [channelId],
      );

      print('Removed favorite: $channelId');
    } catch (e) {
      print('Error removing favorite: $e');
      rethrow;
    }
  }

  /// Clear all favorites
  Future<void> clear() async {
    try {
      final db = await _dbHelper.database;
      await db.delete('favorites');
      print('Cleared all favorites');
    } catch (e) {
      print('Error clearing favorites: $e');
      rethrow;
    }
  }

  /// Get count of favorites
  Future<int> getCount() async {
    try {
      final db = await _dbHelper.database;
      final result = await db.rawQuery('SELECT COUNT(*) as count FROM favorites');
      return Sqflite.firstIntValue(result) ?? 0;
    } catch (e) {
      print('Error getting favorites count: $e');
      return 0;
    }
  }
}
