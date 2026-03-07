import 'dart:io';
import 'package:sqflite/sqflite.dart';
import 'package:path/provider.dart';
import 'package:path/path.dart';
import '../utils/app_logger.dart';

class DatabaseHelper {
  static final DatabaseHelper _instance = DatabaseHelper._internal();
  static Database? _database;

  factory DatabaseHelper() => _instance;

  DatabaseHelper._internal();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final documentsDirectory = await getApplicationDocumentsDirectory();
    final path = join(documentsDirectory.path, 'iptv_player.db');
    AppLogger.log('Database: Opening database at $path');

    return await openDatabase(
      path,
      version: 3,
      onCreate: _onCreate,
      onUpgrade: _onUpgrade,
    );
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
        created_at TEXT NOT NULL
      )
    ''');

    await db.execute('''
      CREATE TABLE browse_history (
        channel_id TEXT PRIMARY KEY,
        watched_at TEXT NOT NULL
      )
    ''');
    
    // Index for faster lookups
    await db.execute('CREATE INDEX idx_favorites_id ON favorites(channel_id)');
  }

  Future<void> _onUpgrade(Database db, int oldVersion, int newVersion) async {
    AppLogger.log('Database: Upgrading from $oldVersion to $newVersion');
    if (oldVersion < 2) {
      await db.execute('ALTER TABLE configurations ADD COLUMN order_index INTEGER DEFAULT 0');
    }
    if (oldVersion < 3) {
      // Add missing fields for Xtream info
      await _addColumnIfNotExists(db, 'configurations', 'last_refreshed', 'TEXT');
      await _addColumnIfNotExists(db, 'configurations', 'expiration_date', 'TEXT');
      await _addColumnIfNotExists(db, 'configurations', 'account_status', 'TEXT');
    }
  }

  Future<void> _addColumnIfNotExists(Database db, String table, String column, String type) async {
    try {
      final columns = await db.rawQuery('PRAGMA table_info($table)');
      final exists = columns.any((c) => c['name'] == column);
      if (!exists) {
        await db.execute('ALTER TABLE $table ADD COLUMN $column $type');
        AppLogger.log('Database: Added column $column to $table');
      }
    } catch (e) {
      AppLogger.log('Database Error: Failed to add column $column: $e');
    }
  }

  /// Self-healing check to ensure schema is consistent across platforms
  Future<void> ensureSchemaConsistency() async {
    final db = await database;
    AppLogger.log('Database: Running self-healing schema check...');
    await _addColumnIfNotExists(db, 'configurations', 'order_index', 'INTEGER DEFAULT 0');
    await _addColumnIfNotExists(db, 'configurations', 'last_refreshed', 'TEXT');
    await _addColumnIfNotExists(db, 'configurations', 'expiration_date', 'TEXT');
    await _addColumnIfNotExists(db, 'configurations', 'account_status', 'TEXT');
  }

  Future<void> close() async {
    if (_database != null) {
      await _database!.close();
      _database = null;
    }
  }
}
