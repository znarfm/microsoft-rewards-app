import 'package:flutter/foundation.dart';
import '../services/preferences_service.dart';

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
/// search tab can react without re-reading storage on every frame.
class ThemeController extends ChangeNotifier {
  final PreferencesService _preferencesService;

  AppThemeMode _mode = AppThemeMode.system;
  bool _amoledOverlay = false;
  bool _loaded = false;

  ThemeController(this._preferencesService);

  AppThemeMode get mode => _mode;
  bool get amoledOverlay => _amoledOverlay;
  bool get isLoaded => _loaded;

  Future<void> load() async {
    _mode = AppThemeMode.fromName(_preferencesService.themeMode);
    _amoledOverlay = _preferencesService.amoledOverlay;
    _loaded = true;
    notifyListeners();
  }

  Future<void> setMode(AppThemeMode mode) async {
    if (mode == _mode) return;
    _mode = mode;
    notifyListeners();
    await _preferencesService.setThemeMode(mode.name);
  }

  Future<void> setAmoledOverlay(bool enabled) async {
    if (enabled == _amoledOverlay) return;
    _amoledOverlay = enabled;
    notifyListeners();
    await _preferencesService.setAmoledOverlay(enabled);
  }
}