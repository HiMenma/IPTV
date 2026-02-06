import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';

class StorageInspector {
  static Future<void> printAllData() async {
    final prefs = await SharedPreferences.getInstance();
    final keys = prefs.getKeys();
    
    print('=== SharedPreferences Data ===');
    print('Total keys: ${keys.length}');
    print('');
    
    for (final key in keys) {
      final value = prefs.get(key);
      print('Key: $key');
      print('Type: ${value.runtimeType}');
      
      if (value is String) {
        try {
          final decoded = json.decode(value);
          print('Value (decoded): ${json.encode(decoded)}');
        } catch (e) {
          print('Value: $value');
        }
      } else {
        print('Value: $value');
      }
      print('---');
    }
    print('=== End of Data ===');
  }
  
  static Future<Map<String, dynamic>> getFavoritesData() async {
    final prefs = await SharedPreferences.getInstance();
    final favoritesStr = prefs.getString('favorites');
    
    if (favoritesStr == null) {
      return {'exists': false, 'data': null};
    }
    
    try {
      final data = json.decode(favoritesStr);
      return {
        'exists': true,
        'data': data,
        'count': (data['favorites'] as List?)?.length ?? 0,
      };
    } catch (e) {
      return {'exists': true, 'data': null, 'error': e.toString()};
    }
  }
  
  static Future<Map<String, dynamic>> getHistoryData() async {
    final prefs = await SharedPreferences.getInstance();
    final historyStr = prefs.getString('history');
    
    if (historyStr == null) {
      return {'exists': false, 'data': null};
    }
    
    try {
      final data = json.decode(historyStr);
      return {
        'exists': true,
        'data': data,
        'count': (data['history'] as List?)?.length ?? 0,
      };
    } catch (e) {
      return {'exists': true, 'data': null, 'error': e.toString()};
    }
  }
  
  static Future<void> clearAll() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    print('All SharedPreferences data cleared');
  }
}
