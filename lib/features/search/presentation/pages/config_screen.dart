import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
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
  bool _showLoginReminderPopup = true;
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
      _selectedTime = TimeOfDay(
        hour: prefs.reminderHour,
        minute: prefs.reminderMinute,
      );
    });
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
    _showLoginReminderPopup = sl<PreferencesService>().showLoginReminderPopup;
    final themeController = sl<ThemeController>();
    return ListenableBuilder(
      listenable: themeController,
      builder: (context, _) {
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Card(
              elevation: 1,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12)),
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
                              Expanded(child: _buildThemeChip(themeController, AppThemeMode.system)),
                              const SizedBox(width: 8),
                              Expanded(child: _buildThemeChip(themeController, AppThemeMode.light)),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Expanded(child: _buildThemeChip(themeController, AppThemeMode.dark)),
                              const SizedBox(width: 8),
                              Expanded(child: _buildThemeChip(themeController, AppThemeMode.amoled)),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const Divider(height: 24),
                    AnimatedSize(
                      duration: const Duration(milliseconds: 250),
                      curve: Curves.easeInOutCubic,
                      child: AnimatedOpacity(
                        duration: const Duration(milliseconds: 200),
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
              elevation: 1,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12)),
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
                          ActionChip(
                            label: Text(_selectedTime.format(context)),
                            avatar: const Icon(Icons.access_time, size: 16),
                            onPressed: _selectTime,
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
              elevation: 1,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12)),
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

  IconData _themeIcon(AppThemeMode mode) {
    return switch (mode) {
      AppThemeMode.system => Icons.brightness_auto,
      AppThemeMode.light => Icons.light_mode,
      AppThemeMode.dark => Icons.dark_mode,
      AppThemeMode.amoled => Icons.contrast,
    };
  }

  Widget _buildThemeChip(ThemeController controller, AppThemeMode mode) {
    final isSelected = controller.mode == mode;
    return ChoiceChip(
      showCheckmark: false,
      avatar: Icon(_themeIcon(mode), size: 18),
      label: Center(
        child: Text(
          _themeLabel(mode),
          overflow: TextOverflow.ellipsis,
        ),
      ),
      selected: isSelected,
      onSelected: (selected) {
        if (selected) controller.setMode(mode);
      },
    );
  }
}