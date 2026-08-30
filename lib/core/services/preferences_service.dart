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
  static const String keyDataSaver = 'data_saver';
  static const String keyCompletionStreak = 'completion_streak';
  static const String keyBestStreak = 'best_streak';
  static const String keyLastCompletedDate = 'last_completed_date';
  static const String keyHapticFeedback = 'haptic_feedback';

  // Data Saver (blocks images in WebView to save data & speed up searches)
  bool get dataSaver => _prefs.getBool(keyDataSaver) ?? true;
  Future<bool> setDataSaver(bool value) =>
      _prefs.setBool(keyDataSaver, value);

  // Haptic Feedback
  bool get hapticFeedback => _prefs.getBool(keyHapticFeedback) ?? true;
  Future<bool> setHapticFeedback(bool value) =>
      _prefs.setBool(keyHapticFeedback, value);

  // Completion Streak & History
  int get completionStreak => getCompletionStreak();

  int get bestStreak {
    final savedBest = _prefs.getInt(keyBestStreak) ?? 0;
    final current = getCompletionStreak();
    if (current > savedBest) {
      _prefs.setInt(keyBestStreak, current);
      return current;
    }
    return savedBest;
  }

  int getCompletionStreak([DateTime? customNow]) {
    final last = lastCompletedDate;
    if (last == null) return 0;
    final now = customNow ?? DateTime.now();
    final todayStr = formatDate(now);
    final yesterdayStr = formatDate(DateTime(now.year, now.month, now.day - 1));
    if (last == todayStr || last == yesterdayStr) {
      return _prefs.getInt(keyCompletionStreak) ?? 0;
    }
    return 0;
  }

  int checkAndClearLostStreak([DateTime? customNow]) {
    final last = lastCompletedDate;
    if (last == null) return 0;
    final now = customNow ?? DateTime.now();
    final todayStr = formatDate(now);
    final yesterdayStr = formatDate(DateTime(now.year, now.month, now.day - 1));
    if (last != todayStr && last != yesterdayStr) {
      final lostStreak = _prefs.getInt(keyCompletionStreak) ?? 0;
      if (lostStreak > 0) {
        _prefs.setInt(keyCompletionStreak, 0);
        return lostStreak;
      }
    }
    return 0;
  }

  String? get lastCompletedDate => _prefs.getString(keyLastCompletedDate);

  static String formatDate(DateTime date) =>
      '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

  bool get isCompletedToday => isCompletedTodayOn();

  bool isCompletedTodayOn([DateTime? customNow]) {
    final last = lastCompletedDate;
    if (last == null) return false;
    final now = customNow ?? DateTime.now();
    return last == formatDate(now);
  }

  Future<int> recordSearchCompletion([DateTime? customNow]) async {
    final now = customNow ?? DateTime.now();
    final todayStr = formatDate(now);
    final last = lastCompletedDate;
    final currentStreak = _prefs.getInt(keyCompletionStreak) ?? 0;

    int newStreak;
    if (last == todayStr) {
      newStreak = currentStreak;
    } else {
      final yesterdayStr = formatDate(DateTime(now.year, now.month, now.day - 1));
      if (last == yesterdayStr) {
        newStreak = currentStreak + 1;
      } else {
        newStreak = 1;
      }
      await _prefs.setString(keyLastCompletedDate, todayStr);
      await _prefs.setInt(keyCompletionStreak, newStreak);
    }
    final savedBest = _prefs.getInt(keyBestStreak) ?? 0;
    if (newStreak > savedBest) {
      await _prefs.setInt(keyBestStreak, newStreak);
    }
    return newStreak;
  }

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
    await _prefs.setString(keyLastOpenedDate, formatDate(today));
  }
}
