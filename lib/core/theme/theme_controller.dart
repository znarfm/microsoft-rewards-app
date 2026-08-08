import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// User-selectable theme modes.
enum AppThemeMode {
  system,
  light,
  dark,
  amoled;

  static AppThemeMode fromName(String? name) {
    return AppThemeMode.values.firstWhere(
      (mode) => mode.name == name,
      orElse: () => AppThemeMode.system,
    );
  }
}

/// Holds persisted theme + AMOLED overlay state so the app root and the
/// search tab can react without re-reading SharedPreferences on every frame.
class ThemeController extends ChangeNotifier {
  static const String modePrefKey = 'theme_mode';
  static const String overlayPrefKey = 'amoled_screen_off';

  AppThemeMode _mode = AppThemeMode.system;
  bool _amoledOverlay = false;
  bool _loaded = false;

  AppThemeMode get mode => _mode;
  bool get amoledOverlay => _amoledOverlay;
  bool get isLoaded => _loaded;

  Future<void> load() async {
    final prefs = await SharedPreferences.getInstance();
    _mode = AppThemeMode.fromName(prefs.getString(modePrefKey));
    _amoledOverlay = prefs.getBool(overlayPrefKey) ?? false;
    _loaded = true;
    notifyListeners();
  }

  Future<void> setMode(AppThemeMode mode) async {
    if (mode == _mode) return;
    _mode = mode;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(modePrefKey, mode.name);
  }

  Future<void> setAmoledOverlay(bool enabled) async {
    if (enabled == _amoledOverlay) return;
    _amoledOverlay = enabled;
    notifyListeners();
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(overlayPrefKey, enabled);
  }
}