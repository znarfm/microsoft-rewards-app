import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
import '../../../../core/theme/expressive_motion.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../core/widgets/expressive_chip.dart';
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
  bool _showLoginReminderPopup = true;
  bool _dataSaver = true;
  bool _hapticFeedback = true;
  TimeOfDay _selectedTime = const TimeOfDay(hour: 19, minute: 0);

  @override
  void initState() {
    super.initState();
    _loadSavedValues();
  }

  void _loadSavedValues() {
    final prefs = sl<PreferencesService>();
    setState(() {
      _sendDailyReminder = prefs.sendDailyReminder;
      _keepScreenOn = prefs.keepScreenOn;
      _showLoginReminderPopup = prefs.showLoginReminderPopup;
      _dataSaver = prefs.dataSaver;
      _hapticFeedback = prefs.hapticFeedback;
      _selectedTime = TimeOfDay(
        hour: prefs.reminderHour,
        minute: prefs.reminderMinute,
      );
    });
  }

  Future<void> _onDataSaverChanged(bool value) async {
    setState(() => _dataSaver = value);
    await sl<PreferencesService>().setDataSaver(value);
  }

  Future<void> _onHapticFeedbackChanged(bool value) async {
    setState(() => _hapticFeedback = value);
    await sl<PreferencesService>().setHapticFeedback(value);
  }

  Future<void> _onShowLoginReminderPopupChanged(bool value) async {
    setState(() => _showLoginReminderPopup = value);
    await sl<PreferencesService>().setShowLoginReminderPopup(value);
  }

  Future<void> _onDailyReminderChanged(bool value) async {
    setState(() => _sendDailyReminder = value);
    await sl<PreferencesService>().setSendDailyReminder(value);

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
    await sl<PreferencesService>()
        .setReminderTime(pickedTime.hour, pickedTime.minute);

    if (_sendDailyReminder) {
      await NotificationService.scheduleDailyReminder(
        hour: pickedTime.hour,
        minute: pickedTime.minute,
      );
    }
  }

  Future<void> _onKeepScreenOnChanged(bool value) async {
    setState(() => _keepScreenOn = value);
    await sl<PreferencesService>().setKeepScreenOn(value);
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
            Card(
              margin: const EdgeInsets.only(bottom: 16),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _sectionTitle(Strings.displayLabel),
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              Expanded(
                                  child: _buildThemeChip(
                                      context, themeController, AppThemeMode.system)),
                              const SizedBox(width: 8),
                              Expanded(
                                  child: _buildThemeChip(
                                      context, themeController, AppThemeMode.light)),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Expanded(
                                  child: _buildThemeChip(
                                      context, themeController, AppThemeMode.dark)),
                              const SizedBox(width: 8),
                              Expanded(
                                  child: _buildThemeChip(
                                      context, themeController, AppThemeMode.amoled)),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const Divider(height: 24),
                    AnimatedSize(
                      duration: ExpressiveMotion.defaultSpatialDuration(context),
                      curve: ExpressiveMotion.defaultSpatialCurve(context),
                      child: AnimatedOpacity(
                        duration: ExpressiveMotion.defaultEffectsDuration,
                        opacity: themeController.mode == AppThemeMode.amoled
                            ? 1.0
                            : 0.0,
                        child: themeController.mode == AppThemeMode.amoled
                            ? SwitchListTile(
                                value: themeController.amoledOverlay,
                                onChanged: (value) =>
                                    themeController.setAmoledOverlay(value),
                                title: const Text(Strings.amoledOverlayTitle),
                                subtitle: const Text(Strings.amoledOverlayHint),
                                contentPadding: EdgeInsets.zero,
                              )
                            : const SizedBox.shrink(),
                      ),
                    ),
                    SwitchListTile(
                      value: _keepScreenOn,
                      onChanged: (value) => _onKeepScreenOnChanged(value),
                      title: const Text(Strings.keepScreenOnLabel),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ],
                ),
              ),
            ),
            Card(
              margin: const EdgeInsets.only(bottom: 16),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _sectionTitle('Streak & Activity'),
                    ListTile(
                      leading: const Text('🔥', style: TextStyle(fontSize: 22)),
                      title: const Text('Current Streak'),
                      trailing: Text(
                        '${sl<PreferencesService>().completionStreak} days',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                      ),
                      contentPadding: EdgeInsets.zero,
                    ),
                    ListTile(
                      leading: const Text('🏆', style: TextStyle(fontSize: 22)),
                      title: const Text('Best Streak'),
                      trailing: Text(
                        '${sl<PreferencesService>().bestStreak} days',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                      ),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ],
                ),
              ),
            ),
            Card(
              margin: const EdgeInsets.only(bottom: 16),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _sectionTitle('Optimization & Automation'),
                    SwitchListTile(
                      value: _dataSaver,
                      onChanged: (value) => _onDataSaverChanged(value),
                      title: const Text('Data Saver Mode'),
                      subtitle: const Text(
                        'Block web images to save data and speed up searches',
                      ),
                      contentPadding: EdgeInsets.zero,
                    ),
                    SwitchListTile(
                      value: _hapticFeedback,
                      onChanged: (value) => _onHapticFeedbackChanged(value),
                      title: const Text('Completion Haptics'),
                      subtitle: const Text(
                        'Vibrate when searches finish',
                      ),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ],
                ),
              ),
            ),
            Card(
              margin: const EdgeInsets.only(bottom: 16),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _sectionTitle(Strings.remindersLabel),
                    SwitchListTile(
                      value: _sendDailyReminder,
                      onChanged: (value) => _onDailyReminderChanged(value),
                      title: Row(
                        children: [
                          const Text(Strings.dailyReminderAt),
                          const SizedBox(width: 8),
                          ExpressiveChoiceChip(
                            label: _selectedTime.format(context),
                            icon: Icons.access_time_rounded,
                            selected: _sendDailyReminder,
                            onSelected: _sendDailyReminder
                                ? (_) => _selectTime()
                                : null,
                          ),
                        ],
                      ),
                      contentPadding: EdgeInsets.zero,
                    ),
                    SwitchListTile(
                      value: _showLoginReminderPopup,
                      onChanged: (value) =>
                          _onShowLoginReminderPopupChanged(value),
                      title: const Text(Strings.loginReminderPopupLabel),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ],
                ),
              ),
            ),
            Card(
              margin: const EdgeInsets.only(bottom: 16),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _sectionTitle(Strings.appLabel),
                    ListTile(
                      leading: const Icon(Icons.code),
                      title: const Text('Developer (@znarfm)'),
                      subtitle: const Text('https://github.com/znarfm'),
                      trailing: const Icon(Icons.open_in_new, size: 18),
                      onTap: () => InAppBrowser.openWithSystemBrowser(
                        url: WebUri('https://github.com/znarfm'),
                      ),
                      contentPadding: EdgeInsets.zero,
                    ),
                    ListTile(
                      leading: const Icon(Icons.info_outline),
                      title: const Text('Version'),
                      subtitle: const Text('2.0.0'),
                      contentPadding: EdgeInsets.zero,
                    ),
                    ListTile(
                      leading: const Icon(Icons.power_settings_new),
                      title: const Text(Strings.exitApp),
                      onTap: () => SystemNavigator.pop(),
                      contentPadding: EdgeInsets.zero,
                    ),
                  ],
                ),
              ),
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
          fontWeight: FontWeight.w700,
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

  IconData _themeIcon(AppThemeMode mode) {
    return switch (mode) {
      AppThemeMode.system => Icons.brightness_auto_rounded,
      AppThemeMode.light => Icons.light_mode_rounded,
      AppThemeMode.dark => Icons.dark_mode_rounded,
      AppThemeMode.amoled => Icons.contrast_rounded,
    };
  }

  Widget _buildThemeChip(
      BuildContext context, ThemeController controller, AppThemeMode mode) {
    final isSelected = controller.mode == mode;
    return ExpressiveChoiceChip(
      label: _themeLabel(mode),
      icon: _themeIcon(mode),
      selected: isSelected,
      expand: true,
      onSelected: (_) => controller.setMode(mode),
    );
  }
}