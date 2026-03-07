import 'package:sqflite/sqflite.dart';
import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:flutter/foundation.dart';

/// Stub function to avoid compilation errors on non-web platforms.
/// The actual implementation is provided by conditional exports.
void initWebFactory() {
  // Do nothing on native platforms
}

DatabaseFactory get webFactory => throw UnsupportedError('Web factory not supported');
