import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ThemeProvider extends ChangeNotifier {
  ThemeMode _themeMode = ThemeMode.system;
  final SharedPreferences? _prefs;

  ThemeProvider({SharedPreferences? prefs}) : _prefs = prefs {
    _loadThemeMode();
  }

  ThemeMode get themeMode => _themeMode;

  void setThemeMode(ThemeMode mode) {
    _themeMode = mode;
    _saveThemeMode(mode);
    notifyListeners();
  }

  Future<void> _loadThemeMode() async {
    final prefs = _prefs ?? await SharedPreferences.getInstance();
    final modeIndex = prefs.getInt('theme_mode');
    if (modeIndex != null) {
      _themeMode = ThemeMode.values[modeIndex];
      notifyListeners();
    }
  }

  Future<void> _saveThemeMode(ThemeMode mode) async {
    final prefs = _prefs ?? await SharedPreferences.getInstance();
    await prefs.setInt('theme_mode', mode.index);
  }

  ThemeData get lightTheme => ThemeData(
        useMaterial3: true,
        colorSchemeSeed: Colors.blue,
        brightness: Brightness.light,
        appBarTheme: const AppBarTheme(centerTitle: true),
      );

  ThemeData get darkTheme => ThemeData(
        useMaterial3: true,
        colorSchemeSeed: Colors.blue,
        brightness: Brightness.dark,
        appBarTheme: const AppBarTheme(centerTitle: true),
      );
}
