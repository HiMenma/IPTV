import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/browse_history.dart';

class HistoryRepository {
  static const String _storageKey = 'history';
  static const String _backupKey = 'history_backup';

  /// Get all history entries from storage
  /// Requirements: 7.2
  Future<List<BrowseHistory>> getAll() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final jsonString = prefs.getString(_storageKey);
      
      if (jsonString == null) {
        return [];
      }

      try {
        final Map<String, dynamic> data = json.decode(jsonString);
        final List<dynamic> historyList = data['history'] ?? [];
        final history = historyList
            .map((item) => BrowseHistory.fromJson(item as Map<String, dynamic>))
            .toList();
        
        // Sort in reverse chronological order (most recent first)
        history.sort((a, b) => b.watchedAt.compareTo(a.watchedAt));
        
        return history;
      } on FormatException catch (e) {
        // Corrupted data - try to restore from backup
        print('History data corrupted: $e. Attempting to restore from backup.');
        return await _restoreFromBackup(prefs);
      } catch (e) {
        // Other parsing errors - try backup
        print('Error parsing history: $e. Attempting to restore from backup.');
        return await _restoreFromBackup(prefs);
      }
    } catch (e) {
      print('Error accessing storage: $e');
      return [];
    }
  }

  /// Restore history from backup
  Future<List<BrowseHistory>> _restoreFromBackup(SharedPreferences prefs) async {
    try {
      final backupString = prefs.getString(_backupKey);
      
      if (backupString == null) {
        print('No backup available. Returning empty list.');
        return [];
      }

      final Map<String, dynamic> data = json.decode(backupString);
      final List<dynamic> historyList = data['history'] ?? [];
      final history = historyList
          .map((item) => BrowseHistory.fromJson(item as Map<String, dynamic>))
          .toList();
      
      // Sort in reverse chronological order
      history.sort((a, b) => b.watchedAt.compareTo(a.watchedAt));
      
      // Restore the main storage from backup
      await prefs.setString(_storageKey, backupString);
      print('Successfully restored ${history.length} history entries from backup.');
      
      return history;
    } catch (e) {
      print('Failed to restore from backup: $e. Returning empty list.');
      // Clear corrupted data and start fresh
      await prefs.remove(_storageKey);
      await prefs.remove(_backupKey);
      return [];
    }
  }

  Future<void> add(String channelId) async {
    final history = await getAll();
    
    history.add(BrowseHistory(
      channelId: channelId,
      watchedAt: DateTime.now(),
    ));
    
    await _saveAll(history);
  }

  Future<void> clear() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_storageKey);
  }

  /// Save all history entries to storage with backup
  /// Requirements: 7.2
  Future<void> _saveAll(List<BrowseHistory> history) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = {
        'history': history.map((h) => h.toJson()).toList(),
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
        throw Exception('Failed to save history. Storage may be full.');
      }
    } on Exception catch (e) {
      // Check if it's a disk full error
      if (e.toString().contains('full') || e.toString().contains('space')) {
        throw Exception('Storage is full. Please free up space and try again.');
      }
      throw Exception('Failed to save history: $e');
    }
  }
}
