import 'dart:io';
import 'package:sqflite/sqflite.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart';
import 'package:flutter/foundation.dart';
import '../utils/app_logger.dart';

class DatabaseHelper {
  static final DatabaseHelper instance = DatabaseHelper._internal();
  static Database? _database;

  factory DatabaseHelper() => instance;

  DatabaseHelper._internal();

  static void initPlatformFactory() {
    debugPrint('Database: Platform factory initialized');
  }

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    await _ensureSchemaConsistency(_database!);
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final path = await _resolveDbPath();
    return await openDatabase(
      path,
      version: 3,
      onCreate: _onCreate,
      onUpgrade: _onUpgrade,
    );
  }

  /// 统一解析数据库文件路径。
  ///
  /// 历史上创建时用 [getApplicationDocumentsDirectory]，而删除/统计用
  /// [getDatabasesPath]，二者在 Android 上指向不同目录，导致删除与体积统计
  /// 作用在不存在的文件上。此处收敛为唯一来源。
  static Future<String> _resolveDbPath() async {
    final documentsDirectory = await getApplicationDocumentsDirectory();
    return join(documentsDirectory.path, 'iptv_player.db');
  }

  /// Check tables and columns consistency on startup
  Future<void> _ensureSchemaConsistency(Database db) async {
    try {
      // 1. Ensure 'history' table exists (Fix for rename bug)
      await db.execute('''
        CREATE TABLE IF NOT EXISTS history (
          channel_id TEXT PRIMARY KEY,
          watched_at TEXT NOT NULL
        )
      ''');

      // 2. Ensure 'configurations' columns exist
      final List<Map<String, dynamic>> configCols = await db.rawQuery('PRAGMA table_info(configurations)');
      final existingConfigCols = configCols.map((c) => c['name'] as String).toSet();

      final requiredConfigCols = {
        'order_index': 'INTEGER DEFAULT 0',
        'last_refreshed': 'TEXT',
        'expiration_date': 'TEXT',
        'account_status': 'TEXT',
      };

      for (var entry in requiredConfigCols.entries) {
        if (!existingConfigCols.contains(entry.key)) {
          AppLogger.log('Database: Fixing missing config column ${entry.key}');
          await db.execute('ALTER TABLE configurations ADD COLUMN ${entry.key} ${entry.value}');
        }
      }

      // 3. Ensure 'favorites' has 'added_at' (if previously called created_at)
      final List<Map<String, dynamic>> favCols = await db.rawQuery('PRAGMA table_info(favorites)');
      final existingFavCols = favCols.map((c) => c['name'] as String).toSet();
      if (!existingFavCols.contains('added_at')) {
        AppLogger.log('Database: Fixing favorites schema');
        // Handle migration if it was 'created_at'
        if (existingFavCols.contains('created_at')) {
          await db.execute('ALTER TABLE favorites RENAME COLUMN created_at TO added_at');
        } else {
          await db.execute('ALTER TABLE favorites ADD COLUMN added_at TEXT');
        }
      }
    } catch (e) {
      AppLogger.log('Database consistency check failed: $e');
    }
  }

  Future<void> _onCreate(Database db, int version) async {
    AppLogger.log('Database: Creating tables...');
    
    await db.execute('''
      CREATE TABLE configurations (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        type TEXT NOT NULL,
        credentials TEXT NOT NULL,
        created_at TEXT NOT NULL,
        updated_at TEXT NOT NULL,
        order_index INTEGER DEFAULT 0,
        last_refreshed TEXT,
        expiration_date TEXT,
        account_status TEXT
      )
    ''');

    await db.execute('''
      CREATE TABLE favorites (
        channel_id TEXT PRIMARY KEY,
        added_at TEXT NOT NULL
      )
    ''');

    // Use 'history' to match HistoryRepositorySQLite
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
    
    await db.execute('CREATE INDEX idx_favorites_id ON favorites(channel_id)');
    await db.execute('CREATE INDEX idx_history_id ON history(channel_id)');
    await db.execute('CREATE INDEX idx_channel_cache_id ON channel_cache(channel_id)');
  }

  Future<void> _onUpgrade(Database db, int oldVersion, int newVersion) async {
    // Migration handled by _ensureSchemaConsistency for more robustness
    await _ensureSchemaConsistency(db);
  }

  /// 删除数据库文件。
  ///
  /// 必须先关闭已打开的连接，否则文件句柄被占用会导致删除失败（Windows 桌面端尤其明显）。
  Future<void> deleteDatabase() async {
    final path = await _resolveDbPath();
    // 先关闭连接并置空，避免下次访问时复用已失效的实例
    if (_database != null) {
      await _database!.close();
      _database = null;
    }
    final file = File(path);
    if (await file.exists()) {
      await file.delete();
      AppLogger.log('Database: Deleted $path');
    }
  }

  Future<int> getDatabaseSize() async {
    final path = await _resolveDbPath();
    final file = File(path);
    if (await file.exists()) {
      return await file.length();
    }
    return 0;
  }

  Future<void> vacuum() async {
    final db = await database;
    await db.execute('VACUUM');
  }

  Future<void> close() async {
    if (_database != null) {
      await _database!.close();
      _database = null;
    }
  }
}
