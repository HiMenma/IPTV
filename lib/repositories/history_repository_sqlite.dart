import 'package:sqflite/sqflite.dart';
import '../models/browse_history.dart';
import '../database/database_helper.dart';

class HistoryRepositorySQLite {
  final DatabaseHelper _dbHelper = DatabaseHelper.instance;

  Future<List<BrowseHistory>> getAll() async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.query(
        'history',
        orderBy: 'watched_at DESC',
      );

      return maps.map((map) => BrowseHistory(
        channelId: map['channel_id'] as String,
        watchedAt: DateTime.parse(map['watched_at'] as String),
      )).toList();
    } catch (e) {
      print('Error getting history: $e');
      return [];
    }
  }

  Future<void> add(String channelId) async {
    try {
      final db = await _dbHelper.database;
      
      await db.insert(
        'history',
        {
          'channel_id': channelId,
          'watched_at': DateTime.now().toIso8601String(),
        },
        conflictAlgorithm: ConflictAlgorithm.replace,
      );

      print('Added to history: $channelId');
    } catch (e) {
      print('Error adding to history: $e');
      rethrow;
    }
  }

  Future<void> clear() async {
    try {
      final db = await _dbHelper.database;
      await db.delete('history');
      print('Cleared all history');
    } catch (e) {
      print('Error clearing history: $e');
      rethrow;
    }
  }

  Future<int> getCount() async {
    try {
      final db = await _dbHelper.database;
      final result = await db.rawQuery('SELECT COUNT(*) as count FROM history');
      return Sqflite.firstIntValue(result) ?? 0;
    } catch (e) {
      print('Error getting history count: $e');
      return 0;
    }
  }

  Future<void> remove(String channelId) async {
    try {
      final db = await _dbHelper.database;
      await db.delete(
        'history',
        where: 'channel_id = ?',
        whereArgs: [channelId],
      );

      print('Removed from history: $channelId');
    } catch (e) {
      print('Error removing from history: $e');
      rethrow;
    }
  }

  Future<List<BrowseHistory>> getRecent(int limit) async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.query(
        'history',
        orderBy: 'watched_at DESC',
        limit: limit,
      );

      return maps.map((map) => BrowseHistory(
        channelId: map['channel_id'] as String,
        watchedAt: DateTime.parse(map['watched_at'] as String),
      )).toList();
    } catch (e) {
      print('Error getting recent history: $e');
      return [];
    }
  }
}
