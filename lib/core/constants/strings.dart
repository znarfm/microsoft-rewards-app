// lib/core/constants/strings.dart
class Strings {
  static const String appTitle = 'Reward Search Automator';
  static const String searchFailed = 'Search failed: ';
  static const String searchCompleted = 'Search completed successfully!';
  static const String searchCountLabel = 'Search Count';
  static const String searchCountHint = 'Enter the number of searches';
  static const String delayLabel = 'Delay (seconds)';
  static const String delayHint = 'Enter the delay between searches';
  static const String searchInProgress = 'Cancel Search';
  static const String startSearch = 'Start Search';
  static const String progress = 'Progress:';

  static const String invalidNumberError = 'Please enter a valid number between 1 and 100.';
  static const String invalidDelayError = 'Please enter a valid delay of at least 0.5 seconds.';

  // Tabs
  static const String searchTab = 'Search';
  static const String configTab = 'Config';

  // Config screen
  static const String themeLabel = 'Theme';
  static const String themeSystem = 'System';
  static const String themeLight = 'Light';
  static const String themeDark = 'Dark';
  static const String themeAmoled = 'AMOLED';
  static const String amoledOverlayTitle = 'Simulate screen off (AMOLED)';
  static const String amoledOverlayHint =
      'Shows a pure black screen during search. Tap to dismiss. Use on OLED displays only.';
  static const String keepScreenOnLabel = 'Keep screen on during search';
  static const String dailyReminderAt = 'Daily reminder at ';
  static const String remindersLabel = 'Reminders';
  static const String accountLabel = 'Account';
  static const String appLabel = 'App';
  static const String exitApp = 'Exit';
  static const String logInToEarn = 'Log in to earn points.';
  static const String logIn = 'Log in';
  static const String logOut = 'Log out';
  static const String browserNotReady = 'Browser is not ready yet';
}