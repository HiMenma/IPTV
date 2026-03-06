import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'dart:io';
import 'package:flutter/foundation.dart';

/// Database helper for managing SQLite database
class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('iptv_player.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, filePath);

    debugPrint('Database path: $path');

    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDB,
      onUpgrade: _upgradeDB,
    );
  }

  Future<void> _createDB(Database db, int version) async {
    debugPrint('Creating database tables...');

    // Configurations table
    await db.execute('''
      CREATE TABLE configurations (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        type TEXT NOT NULL,
        credentials TEXT NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL
      )
    ''');

    // Favorites table
    await db.execute('''
      CREATE TABLE favorites (
        channel_id TEXT PRIMARY KEY,
        added_at TEXT NOT NULL
      )
    ''');

    // History table
    await db.execute('''
      CREATE TABLE history (
        channel_id TEXT PRIMARY KEY,
        watched_at TEXT NOT NULL
      )
    ''');

    // Channel cache table
    await db.execute('''
      CREATE TABLE channel_cache (
        config_id TEXT NOT NULL,
        channel_id TEXT NOT NULL,
        name TEXT NOT NULL,
        stream_url TEXT NOT NULL,
        logo_url TEXT,
        category TEXT,
        cached_at TEXT NOT NULL,
        PRIMARY KEY (config_id, channel_id)
      )
    ''');

    // Create indexes for better performance
    await db.execute('CREATE INDEX idx_favorites_added_at ON favorites(added_at)');
    await db.execute('CREATE INDEX idx_history_watched_at ON history(watched_at)');
    await db.execute('CREATE INDEX idx_channel_cache_config ON channel_cache(config_id)');
    await db.execute('CREATE INDEX idx_channel_cache_id ON channel_cache(channel_id)');

    debugPrint('Database tables created successfully');
  }

  Future<void> _upgradeDB(Database db, int oldVersion, int newVersion) async {
    debugPrint('Upgrading database from version $oldVersion to $newVersion');
    
    // Handle future database migrations here
    if (oldVersion < 2) {
      // Example: Add new column
      // await db.execute('ALTER TABLE configurations ADD COLUMN new_field TEXT');
    }
  }

  /// Close the database
  Future<void> close() async {
    final db = await database;
    await db.close();
    _database = null;
    debugPrint('Database closed');
  }

  /// Delete the database (for testing or reset)
  Future<void> deleteDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'iptv_player.db');
    
    if (await File(path).exists()) {
      await File(path).delete();
      debugPrint('Database deleted');
    }
    
    _database = null;
  }

  /// Get database size
  Future<int> getDatabaseSize() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'iptv_player.db');
    
    if (await File(path).exists()) {
      final file = File(path);
      return await file.length();
    }
    
    return 0;
  }

  /// Vacuum database to reclaim space
  Future<void> vacuum() async {
    final db = await database;
    await db.execute('VACUUM');
    debugPrint('Database vacuumed');
  }
}
