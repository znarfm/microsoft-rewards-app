import 'package:flutter_test/flutter_test.dart';
import 'package:microsoft_automatic_rewards/core/services/preferences_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late PreferencesService service;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    service = PreferencesService(prefs);
  });

  group('PreferencesService Defaults', () {
    test('default loggedIn is false', () {
      expect(service.loggedIn, false);
    });

    test('default searchCount is 22', () {
      expect(service.searchCount, '22');
    });

    test('default searchDelay is 15', () {
      expect(service.searchDelay, '15');
    });

    test('default sendDailyReminder is false', () {
      expect(service.sendDailyReminder, false);
    });

    test('default keepScreenOn is true', () {
      expect(service.keepScreenOn, true);
    });

    test('default showLoginReminderPopup is true', () {
      expect(service.showLoginReminderPopup, true);
    });

    test('default reminder time is 19:00', () {
      expect(service.reminderHour, 19);
      expect(service.reminderMinute, 0);
    });

    test('default themeMode is null', () {
      expect(service.themeMode, isNull);
    });

    test('default amoledOverlay is false', () {
      expect(service.amoledOverlay, false);
    });

    test('default dataSaver is true', () {
      expect(service.dataSaver, true);
    });

    test('default hapticFeedback is true', () {
      expect(service.hapticFeedback, true);
    });

    test('default completionStreak is 0', () {
      expect(service.completionStreak, 0);
    });

    test('default isCompletedToday is false', () {
      expect(service.isCompletedToday, false);
    });
  });

  group('PreferencesService Setters & Actions', () {
    test('setLoggedIn updates loggedIn', () async {
      await service.setLoggedIn(true);
      expect(service.loggedIn, true);
    });

    test('setDataSaver updates dataSaver', () async {
      await service.setDataSaver(false);
      expect(service.dataSaver, false);
    });

    test('setHapticFeedback updates hapticFeedback', () async {
      await service.setHapticFeedback(false);
      expect(service.hapticFeedback, false);
    });

    test('recordSearchCompletion sets streak to 1 on first day', () async {
      final now = DateTime(2026, 8, 16);
      final streak = await service.recordSearchCompletion(now);
      expect(streak, 1);
      expect(service.completionStreak, 1);
      expect(service.lastCompletedDate, '2026-08-16');
    });

    test('recordSearchCompletion increments streak on consecutive day', () async {
      final day1 = DateTime(2026, 8, 15);
      final day2 = DateTime(2026, 8, 16);

      await service.recordSearchCompletion(day1);
      expect(service.completionStreak, 1);

      final streak = await service.recordSearchCompletion(day2);
      expect(streak, 2);
      expect(service.completionStreak, 2);
    });

    test('recordSearchCompletion maintains streak if called multiple times on same day', () async {
      final day1 = DateTime(2026, 8, 16);

      await service.recordSearchCompletion(day1);
      final streak = await service.recordSearchCompletion(day1);

      expect(streak, 1);
      expect(service.completionStreak, 1);
    });

    test('recordSearchCompletion resets streak to 1 if day skipped', () async {
      final day1 = DateTime(2026, 8, 10);
      final day2 = DateTime(2026, 8, 16);

      await service.recordSearchCompletion(day1);
      expect(service.completionStreak, 1);

      final streak = await service.recordSearchCompletion(day2);
      expect(streak, 1);
      expect(service.completionStreak, 1);
    });

    test('saveAppOpenedToday saves formatted date string', () async {
      await service.saveAppOpenedToday();
      final now = DateTime.now();
      final expected = PreferencesService.formatDate(now);
      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString(PreferencesService.keyLastOpenedDate), expected);
    });
  });
}
