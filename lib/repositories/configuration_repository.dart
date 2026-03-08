import 'package:sqflite/sqflite.dart';
import 'package:flutter/foundation.dart';
import '../models/configuration.dart';
import '../database/database_helper.dart';

/// Repository for managing IPTV configurations in SQLite
class ConfigurationRepository {
  final DatabaseHelper _dbHelper;

  ConfigurationRepository({DatabaseHelper? dbHelper})
      : _dbHelper = dbHelper ?? DatabaseHelper.instance;

  /// Get all configurations sorted by order_index
  Future<List<Configuration>> getAll() async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.query(
        'configurations',
        orderBy: 'order_index ASC, created_at DESC',
      );

      return List.generate(maps.length, (i) {
        return Configuration.fromMap(maps[i]);
      });
    } catch (e) {
      debugPrint('Error getting configurations: $e');
      return [];
    }
  }

  /// Get a configuration by ID
  Future<Configuration?> getById(String id) async {
    try {
      final db = await _dbHelper.database;
      final List<Map<String, dynamic>> maps = await db.query(
        'configurations',
        where: 'id = ?',
        whereArgs: [id],
      );

      if (maps.isNotEmpty) {
        return Configuration.fromMap(maps.first);
      }
      return null;
    } catch (e) {
      debugPrint('Error getting configuration by ID: $e');
      return null;
    }
  }

  /// Add a new configuration
  Future<void> add(Configuration config) async {
    try {
      final db = await _dbHelper.database;
      
      final List<Map<String, dynamic>> result = await db.rawQuery('SELECT MAX(order_index) as max_index FROM configurations');
      int nextIndex = 0;
      if (result.isNotEmpty && result.first['max_index'] != null) {
        nextIndex = (result.first['max_index'] as int) + 1;
      }

      final configWithIndex = config.copyWith(orderIndex: nextIndex);

      await db.insert(
        'configurations',
        configWithIndex.toMap(),
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
    } catch (e) {
      debugPrint('Error adding configuration: $e');
      rethrow;
    }
  }

  /// Update an existing configuration
  Future<void> update(Configuration config) async {
    try {
      final db = await _dbHelper.database;
      await db.update(
        'configurations',
        config.toMap(),
        where: 'id = ?',
        whereArgs: [config.id],
      );
    } catch (e) {
      debugPrint('Error updating configuration: $e');
      rethrow;
    }
  }

  /// Delete a configuration
  Future<void> delete(String id) async {
    try {
      final db = await _dbHelper.database;
      await db.delete(
        'configurations',
        where: 'id = ?',
        whereArgs: [id],
      );
    } catch (e) {
      debugPrint('Error deleting configuration: $e');
      rethrow;
    }
  }

  /// Update the order index of multiple configurations
  Future<void> updateOrder(List<Configuration> configs) async {
    try {
      final db = await _dbHelper.database;
      final batch = db.batch();
      
      for (int i = 0; i < configs.length; i++) {
        batch.update(
          'configurations',
          {'order_index': i},
          where: 'id = ?',
          whereArgs: [configs[i].id],
        );
      }
      
      await batch.commit(noResult: true);
    } catch (e) {
      debugPrint('Error updating configurations order: $e');
    }
  }
}
