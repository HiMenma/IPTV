import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/configuration.dart';

class ConfigurationRepository {
  static const String _storageKey = 'configurations';
  static const String _backupKey = 'configurations_backup';

  /// Get all configurations from storage
  /// Requirements: 4.4
  Future<List<Configuration>> getAll() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final jsonString = prefs.getString(_storageKey);
      
      if (jsonString == null) {
        return [];
      }

      try {
        final Map<String, dynamic> data = json.decode(jsonString);
        final List<dynamic> configList = data['configurations'] ?? [];
        return configList
            .map((item) => Configuration.fromJson(item as Map<String, dynamic>))
            .toList();
      } on FormatException catch (e) {
        // Corrupted data - try to restore from backup
        print('Configuration data corrupted: $e. Attempting to restore from backup.');
        return await _restoreFromBackup(prefs);
      } catch (e) {
        // Other parsing errors - try backup
        print('Error parsing configurations: $e. Attempting to restore from backup.');
        return await _restoreFromBackup(prefs);
      }
    } catch (e) {
      print('Error accessing storage: $e');
      return [];
    }
  }

  /// Restore configurations from backup
  Future<List<Configuration>> _restoreFromBackup(SharedPreferences prefs) async {
    try {
      final backupString = prefs.getString(_backupKey);
      
      if (backupString == null) {
        print('No backup available. Returning empty list.');
        return [];
      }

      final Map<String, dynamic> data = json.decode(backupString);
      final List<dynamic> configList = data['configurations'] ?? [];
      final configurations = configList
          .map((item) => Configuration.fromJson(item as Map<String, dynamic>))
          .toList();
      
      // Restore the main storage from backup
      await prefs.setString(_storageKey, backupString);
      print('Successfully restored ${configurations.length} configurations from backup.');
      
      return configurations;
    } catch (e) {
      print('Failed to restore from backup: $e. Returning empty list.');
      // Clear corrupted data and start fresh
      await prefs.remove(_storageKey);
      await prefs.remove(_backupKey);
      return [];
    }
  }

  Future<Configuration?> getById(String id) async {
    final configurations = await getAll();
    try {
      return configurations.firstWhere((config) => config.id == id);
    } catch (e) {
      return null;
    }
  }

  Future<void> save(Configuration config) async {
    final configurations = await getAll();
    configurations.add(config);
    await _saveAll(configurations);
  }

  Future<void> update(Configuration config) async {
    final configurations = await getAll();
    final index = configurations.indexWhere((c) => c.id == config.id);
    
    if (index != -1) {
      configurations[index] = config;
      await _saveAll(configurations);
    }
  }

  Future<void> delete(String id) async {
    final configurations = await getAll();
    configurations.removeWhere((config) => config.id == id);
    await _saveAll(configurations);
  }

  /// Save all configurations to storage with backup
  /// Requirements: 4.4
  Future<void> _saveAll(List<Configuration> configurations) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = {
        'configurations': configurations.map((c) => c.toJson()).toList(),
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
        throw Exception('Failed to save configurations. Storage may be full.');
      }
    } on Exception catch (e) {
      // Check if it's a disk full error
      if (e.toString().contains('full') || e.toString().contains('space')) {
        throw Exception('Storage is full. Please free up space and try again.');
      }
      throw Exception('Failed to save configurations: $e');
    }
  }
}
