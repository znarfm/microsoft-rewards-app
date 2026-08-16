import 'package:flutter_test/flutter_test.dart';
import 'package:microsoft_automatic_rewards/core/services/preferences_service.dart';
import 'package:microsoft_automatic_rewards/core/theme/theme_controller.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late PreferencesService prefsService;
  late ThemeController controller;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();
    prefsService = PreferencesService(prefs);
    controller = ThemeController(prefsService);
  });

  group('ThemeController', () {
    test('initial state before load', () {
      expect(controller.mode, AppThemeMode.system);
      expect(controller.amoledOverlay, false);
      expect(controller.isLoaded, false);
    });

    test('load sets isLoaded and reads from PreferencesService', () async {
      await prefsService.setThemeMode('amoled');
      await prefsService.setAmoledOverlay(true);

      await controller.load();

      expect(controller.isLoaded, true);
      expect(controller.mode, AppThemeMode.amoled);
      expect(controller.amoledOverlay, true);
    });

    test('setMode updates mode and persists via PreferencesService', () async {
      bool notified = false;
      controller.addListener(() => notified = true);

      await controller.setMode(AppThemeMode.dark);

      expect(controller.mode, AppThemeMode.dark);
      expect(notified, true);
      expect(prefsService.themeMode, 'dark');
    });

    test('setMode with same value does not notify listeners', () async {
      await controller.setMode(AppThemeMode.system);
      bool notified = false;
      controller.addListener(() => notified = true);

      await controller.setMode(AppThemeMode.system);

      expect(notified, false);
    });

    test('setAmoledOverlay updates state and persists', () async {
      bool notified = false;
      controller.addListener(() => notified = true);

      await controller.setAmoledOverlay(true);

      expect(controller.amoledOverlay, true);
      expect(notified, true);
      expect(prefsService.amoledOverlay, true);
    });

    test('AppThemeMode.fromName handles valid and invalid strings', () {
      expect(AppThemeMode.fromName('system'), AppThemeMode.system);
      expect(AppThemeMode.fromName('light'), AppThemeMode.light);
      expect(AppThemeMode.fromName('dark'), AppThemeMode.dark);
      expect(AppThemeMode.fromName('amoled'), AppThemeMode.amoled);
      expect(AppThemeMode.fromName('unknown'), AppThemeMode.system);
      expect(AppThemeMode.fromName(null), AppThemeMode.system);
    });
  });
}
