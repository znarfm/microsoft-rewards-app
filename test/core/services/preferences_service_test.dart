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
  });

  group('PreferencesService Setters', () {
    test('setLoggedIn updates loggedIn', () async {
      await service.setLoggedIn(true);
      expect(service.loggedIn, true);
    });

    test('setSearchCount updates searchCount', () async {
      await service.setSearchCount('30');
      expect(service.searchCount, '30');
    });

    test('setSearchDelay updates searchDelay', () async {
      await service.setSearchDelay('10');
      expect(service.searchDelay, '10');
    });

    test('setSendDailyReminder updates sendDailyReminder', () async {
      await service.setSendDailyReminder(true);
      expect(service.sendDailyReminder, true);
    });

    test('setKeepScreenOn updates keepScreenOn', () async {
      await service.setKeepScreenOn(false);
      expect(service.keepScreenOn, false);
    });

    test('setShowLoginReminderPopup updates showLoginReminderPopup', () async {
      await service.setShowLoginReminderPopup(false);
      expect(service.showLoginReminderPopup, false);
    });

    test('setReminderTime updates reminderHour and reminderMinute', () async {
      await service.setReminderTime(8, 30);
      expect(service.reminderHour, 8);
      expect(service.reminderMinute, 30);
    });

    test('setThemeMode updates themeMode', () async {
      await service.setThemeMode('dark');
      expect(service.themeMode, 'dark');
    });

    test('setAmoledOverlay updates amoledOverlay', () async {
      await service.setAmoledOverlay(true);
      expect(service.amoledOverlay, true);
    });

    test('saveAppOpenedToday saves formatted date string', () async {
      await service.saveAppOpenedToday();
      final now = DateTime.now();
      final expected = '${now.year}-${now.month}-${now.day}';
      final prefs = await SharedPreferences.getInstance();
      expect(prefs.getString(PreferencesService.keyLastOpenedDate), expected);
    });
  });
}
