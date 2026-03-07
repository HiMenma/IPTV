import 'package:sqflite/sqflite.dart';
// Only import this package if we are actually compiling for web
// This conditional import is managed by the parent file logic, 
// but here we just export the init function.
import 'package:sqflite_common_ffi_web/sqflite_ffi_web.dart';

void initWebFactory() {
  databaseFactory = databaseFactoryFfiWeb;
}
