import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';
import 'dart:io' show File, Platform;
import 'package:flutter/foundation.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';

// Conditional imports to prevent macOS build from failing on Web-only packages
import 'db_stub.dart' if (dart.library.html) 'db_web.dart' as platform_db;

/// Database helper for managing SQLite database
class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._init();
  static Database? _database;

  DatabaseHelper._init();

  /// Initialize the database factory for the current platform.
  static void initPlatformFactory() {
    if (kIsWeb) {
      platform_db.initWebFactory();
      debugPrint('Database: Global factory set to FfiWeb');
    } else if (Platform.isWindows || Platform.isLinux) {
      sqfliteFfiInit();
      databaseFactory = databaseFactoryFfi;
      debugPrint('Database: Global factory set to Ffi (Desktop)');
    }
  }

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB('iptv_player.db');
    return _database!;
  }

  Future<Database> _initDB(String filePath) async {
    String path;
    
    if (kIsWeb) {
      platform_db.initWebFactory();
      path = filePath;
    } else {
      if (Platform.isWindows || Platform.isLinux) {
        databaseFactory = databaseFactoryFfi;
        sqfliteFfiInit();
      }
      final dbPath = await getDatabasesPath();
      path = join(dbPath, filePath);
    }

    return await openDatabase(
      path,
      version: 1,
      onCreate: _createDB,
      onUpgrade: _upgradeDB,
    );
  }

  Future<void> _createDB(Database db, int version) async {
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

    await db.execute('''
      CREATE TABLE favorites (
        channel_id TEXT PRIMARY KEY,
        added_at TEXT NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE history (
        channel_id TEXT PRIMARY KEY,
        watched_at TEXT NOT NULL
      )
    ''');

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

    await db.execute('CREATE INDEX idx_favorites_added_at ON favorites(added_at)');
    await db.execute('CREATE INDEX idx_history_watched_at ON history(watched_at)');
    await db.execute('CREATE INDEX idx_channel_cache_config ON channel_cache(config_id)');
    await db.execute('CREATE INDEX idx_channel_cache_id ON channel_cache(channel_id)');
  }

  Future<void> _upgradeDB(Database db, int oldVersion, int newVersion) async {}

  Future<void> close() async {
    final db = await database;
    await db.close();
    _database = null;
  }

  Future<void> deleteDatabase() async {
    if (kIsWeb) {
      await databaseFactory.deleteDatabase('iptv_player.db');
    } else {
      final dbPath = await getDatabasesPath();
      final path = join(dbPath, 'iptv_player.db');
      if (await File(path).exists()) {
        await File(path).delete();
      }
    }
    _database = null;
  }

  Future<int> getDatabaseSize() async {
    if (kIsWeb) return 0;
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'iptv_player.db');
    if (await File(path).exists()) {
      return await File(path).length();
    }
    return 0;
  }

  Future<void> vacuum() async {
    final db = await database;
    await db.execute('VACUUM');
  }
}
