import 'package:shared_preferences/shared_preferences.dart';

/// Centralized service for reading and writing app preferences.
/// Encapsulates storage keys, defaults, and type casting.
class PreferencesService {
  final SharedPreferences _prefs;

  PreferencesService(this._prefs);

  static const String keyLoggedIn = 'loggedIn';
  static const String keySearchCount = 'search_count';
  static const String keySearchDelay = 'search_delay';
  static const String keySendDailyReminder = 'send_daily_reminder';
  static const String keyKeepScreenOn = 'keep_screen_on';
  static const String keyShowLoginReminderPopup = 'show_login_reminder_popup';
  static const String keyReminderHour = 'reminder_hour';
  static const String keyReminderMinute = 'reminder_minute';
  static const String keyLastOpenedDate = 'last_opened_date';
  static const String keyThemeMode = 'theme_mode';
  static const String keyAmoledOverlay = 'amoled_screen_off';

  // Theme Mode
  String? get themeMode => _prefs.getString(keyThemeMode);
  Future<bool> setThemeMode(String value) =>
      _prefs.setString(keyThemeMode, value);

  // AMOLED Overlay
  bool get amoledOverlay => _prefs.getBool(keyAmoledOverlay) ?? false;
  Future<bool> setAmoledOverlay(bool value) =>
      _prefs.setBool(keyAmoledOverlay, value);

  // Logged In
  bool get loggedIn => _prefs.getBool(keyLoggedIn) ?? false;
  Future<bool> setLoggedIn(bool value) => _prefs.setBool(keyLoggedIn, value);

  // Search Count
  String get searchCount => _prefs.getString(keySearchCount) ?? '22';
  Future<bool> setSearchCount(String value) =>
      _prefs.setString(keySearchCount, value);

  // Search Delay
  String get searchDelay => _prefs.getString(keySearchDelay) ?? '15';
  Future<bool> setSearchDelay(String value) =>
      _prefs.setString(keySearchDelay, value);

  // Daily Reminder
  bool get sendDailyReminder =>
      _prefs.getBool(keySendDailyReminder) ?? false;
  Future<bool> setSendDailyReminder(bool value) =>
      _prefs.setBool(keySendDailyReminder, value);

  // Keep Screen On
  bool get keepScreenOn => _prefs.getBool(keyKeepScreenOn) ?? true;
  Future<bool> setKeepScreenOn(bool value) =>
      _prefs.setBool(keyKeepScreenOn, value);

  // Show Login Reminder Popup
  bool get showLoginReminderPopup =>
      _prefs.getBool(keyShowLoginReminderPopup) ?? true;
  Future<bool> setShowLoginReminderPopup(bool value) =>
      _prefs.setBool(keyShowLoginReminderPopup, value);

  // Reminder Time
  int get reminderHour => _prefs.getInt(keyReminderHour) ?? 19;
  int get reminderMinute => _prefs.getInt(keyReminderMinute) ?? 0;
  Future<void> setReminderTime(int hour, int minute) async {
    await _prefs.setInt(keyReminderHour, hour);
    await _prefs.setInt(keyReminderMinute, minute);
  }

  // App Opened Today
  Future<void> saveAppOpenedToday() async {
    final today = DateTime.now();
    final formatted = '${today.year}-${today.month}-${today.day}';
    await _prefs.setString(keyLastOpenedDate, formatted);
  }
}
