import 'dart:io';
import 'package:path_provider/path_provider.dart';
import 'package:intl/intl.dart';
import 'package:flutter/foundation.dart';

class AppLogger {
  static File? _logFile;
  static bool _initialized = false;

  static Future<void> init() async {
    try {
      final directory = await getApplicationDocumentsDirectory();
      _logFile = File('${directory.path}/iptv_debug.log');
      
      if (await _logFile!.exists()) {
        await _logFile!.delete();
      }
      await _logFile!.create();
      
      _initialized = true;
      log('--- Logger Initialized at ${DateTime.now()} ---');
      log('Platform: ${Platform.operatingSystem} ${Platform.operatingSystemVersion}');
    } catch (e) {
      debugPrint('Failed to initialize logger: $e');
    }
  }

  static void log(String message) {
    final timestamp = DateFormat('HH:mm:ss.SSS').format(DateTime.now());
    final logLine = '[$timestamp] $message';
    debugPrint(logLine);
    if (_initialized && _logFile != null) {
      try {
        _logFile!.writeAsStringSync('$logLine\n', mode: FileMode.append, flush: true);
      } catch (e) {
        // Ignore write errors
      }
    }
  }
}
