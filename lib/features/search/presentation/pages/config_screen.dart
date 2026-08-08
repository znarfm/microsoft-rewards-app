import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../notifications/notification_service.dart';

/// Second tab of the main screen. Holds the theme selector, daily-reminder
/// and keep-screen-on toggles, the AMOLED screen-off simulation, and app
/// actions (exit).
class ConfigScreen extends StatefulWidget {
  const ConfigScreen({super.key});

  @override
  State<ConfigScreen> createState() => _ConfigScreenState();
}

class _ConfigScreenState extends State<ConfigScreen> {
  bool _sendDailyReminder = false;
  bool _keepScreenOn = true;
  TimeOfDay _selectedTime = const TimeOfDay(hour: 19, minute: 0);

  @override
  void initState() {
    super.initState();
    _loadSavedValues();
  }

  Future<void> _loadSavedValues() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _sendDailyReminder = prefs.getBool('send_daily_reminder') ?? false;
      _keepScreenOn = prefs.getBool('keep_screen_on') ?? true;
      _selectedTime = TimeOfDay(
        hour: prefs.getInt('reminder_hour') ?? 19,
        minute: prefs.getInt('reminder_minute') ?? 0,
      );
    });
  }

  Future<void> _onDailyReminderChanged(bool value) async {
    setState(() => _sendDailyReminder = value);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('send_daily_reminder', value);

    if (value) {
      await NotificationService.scheduleDailyReminder(
        hour: _selectedTime.hour,
        minute: _selectedTime.minute,
      );
    } else {
      await NotificationService.cancelReminder();
    }
  }

  Future<void> _selectTime() async {
    final pickedTime = await showTimePicker(
      context: context,
      initialTime: _selectedTime,
    );
    if (pickedTime == null || pickedTime == _selectedTime) return;

    setState(() => _selectedTime = pickedTime);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setInt('reminder_hour', pickedTime.hour);
    await prefs.setInt('reminder_minute', pickedTime.minute);

    if (_sendDailyReminder) {
      await NotificationService.scheduleDailyReminder(
        hour: pickedTime.hour,
        minute: pickedTime.minute,
      );
    }
  }

  Future<void> _onKeepScreenOnChanged(bool value) async {
    setState(() => _keepScreenOn = value);
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('keep_screen_on', value);
  }

  @override
  Widget build(BuildContext context) {
    final themeController = sl<ThemeController>();
        return ListenableBuilder(
      listenable: themeController,
      builder: (context, _) {
        return ListView(
          padding: const EdgeInsets.all(16),
children: [
            _sectionTitle(Strings.themeLabel),
            RadioGroup<AppThemeMode>(
              groupValue: themeController.mode,
              onChanged: (value) => themeController.setMode(value!),
              child: Column(
                children: AppThemeMode.values.map((mode) {
                  return RadioListTile<AppThemeMode>(
                    value: mode,
                    title: Text(_themeLabel(mode)),
                    dense: true,
                    contentPadding: EdgeInsets.zero,
                  );
                }).toList(),
              ),
            ),
            const Divider(height: 32),
            _sectionTitle(Strings.remindersLabel),
            SwitchListTile(
              value: themeController.amoledOverlay,
              onChanged: (value) => themeController.setAmoledOverlay(value),
              title: const Text(Strings.amoledOverlayTitle),
              subtitle: const Text(Strings.amoledOverlayHint),
              contentPadding: EdgeInsets.zero,
            ),
            CheckboxListTile(
              value: _sendDailyReminder,
              onChanged: (value) => _onDailyReminderChanged(value ?? false),
              title: const Text(Strings.dailyReminderAt),
              secondary: Text(
                _selectedTime.format(context),
                style: TextStyle(color: Theme.of(context).colorScheme.primary),
              ),
              controlAffinity: ListTileControlAffinity.leading,
              contentPadding: EdgeInsets.zero,
            ),
            TextButton.icon(
              onPressed: _selectTime,
              icon: const Icon(Icons.access_time),
              label: const Text('Change time'),
            ),
            CheckboxListTile(
              value: _keepScreenOn,
              onChanged: (value) => _onKeepScreenOnChanged(value ?? false),
              title: const Text(Strings.keepScreenOnLabel),
              controlAffinity: ListTileControlAffinity.leading,
              contentPadding: EdgeInsets.zero,
            ),
            const Divider(height: 32),
            _sectionTitle(Strings.appLabel),
            ListTile(
              leading: const Icon(Icons.power_settings_new),
              title: const Text(Strings.exitApp),
              onTap: () => SystemNavigator.pop(),
              contentPadding: EdgeInsets.zero,
            ),
          ],
        );
      },
    );
  }

  Widget _sectionTitle(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.8,
          color: Theme.of(context).colorScheme.primary,
        ),
      ),
    );
  }

  String _themeLabel(AppThemeMode mode) {
    return switch (mode) {
      AppThemeMode.system => Strings.themeSystem,
      AppThemeMode.light => Strings.themeLight,
      AppThemeMode.dark => Strings.themeDark,
      AppThemeMode.amoled => Strings.themeAmoled,
    };
  }
}