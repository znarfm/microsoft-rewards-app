import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:timezone/data/latest_all.dart' as tzdata;
import 'package:timezone/timezone.dart' as tz;

import '../core/di/injection_container.dart';
import '../features/search/presentation/bloc/search_bloc.dart';

@pragma('vm:entry-point')
void notificationTapBackground(NotificationResponse response) {
  if (response.actionId == 'stop_search') {
    try {
      sl<SearchBloc>().add(CancelSearchEvent());
    } catch (e) {
      debugPrint('Background notification stop error: $e');
    }
  }
}

class NotificationService {
  static final _notifications = FlutterLocalNotificationsPlugin();
  static const int _reminderId = 0;

  static Future<void> init() async {
    WidgetsFlutterBinding.ensureInitialized();

    // 1) Load all the TZ database.
    tzdata.initializeTimeZones();

    // 2) Figure out the device’s current IANA zone identifier
    final timezoneInfo = await FlutterTimezone.getLocalTimezone();
    final String nativeName = timezoneInfo.identifier;

    // 3) Tell the timezone package to treat that as “local”
    final tz.Location deviceLocation = tz.getLocation(nativeName);
    debugPrint('Device timezone: $nativeName');
    tz.setLocalLocation(deviceLocation);

    // 4) Continue initializing your notifications plugin…
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    await _notifications.initialize(
      settings: const InitializationSettings(android: androidSettings),
      onDidReceiveNotificationResponse: (response) {
        if (response.actionId == 'stop_search') {
          try {
            sl<SearchBloc>().add(CancelSearchEvent());
          } catch (e) {
            debugPrint('Error stopping search via notification: $e');
          }
        }
      },
      onDidReceiveBackgroundNotificationResponse: notificationTapBackground,
    );

    await _createNotificationChannel();
  }

  static const int _progressNotificationId = 2;

  static Future<void> _createNotificationChannel() async {
    const androidChannel = AndroidNotificationChannel(
      'daily_reminder_channel',
      'Daily Reminders',
      description: 'Channel for daily search reminders',
      importance: Importance.max,
    );

    const progressChannel = AndroidNotificationChannel(
      'search_progress_channel',
      'Search Progress',
      description: 'Ongoing notification while search is running',
      importance: Importance.defaultImportance,
    );

    final androidPlugin = _notifications.resolvePlatformSpecificImplementation<
        AndroidFlutterLocalNotificationsPlugin>();

    await androidPlugin?.createNotificationChannel(androidChannel);
    await androidPlugin?.createNotificationChannel(progressChannel);
  }

  static Future<void> _requestNotificationPermissions() async {
    if (defaultTargetPlatform == TargetPlatform.android) {
      final androidPlugin = _notifications.resolvePlatformSpecificImplementation<
          AndroidFlutterLocalNotificationsPlugin>();

      await androidPlugin?.requestNotificationsPermission();
    }
  }

  static Future<void> scheduleDailyReminder({int hour = 19, int minute = 0}) async {
    await _requestNotificationPermissions();
    await cancelReminder();
    await _notifications.zonedSchedule(
      id: _reminderId,
      title: 'Reminder',
      body: 'Don\'t forget to run your Bing searches today!',
      scheduledDate: _nextInstanceOfTime(hour, minute),
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          'daily_reminder_channel',
          'Daily Reminders',
          channelDescription: 'Daily search reminder at 7 PM',
          importance: Importance.max,
          priority: Priority.high,
        ),
      ),
      matchDateTimeComponents: DateTimeComponents.time,
      androidScheduleMode: AndroidScheduleMode.exactAllowWhileIdle,
    );
  }

  static Future<void> sendImmediateNotification({
    String title = 'Microsoft Automatic Rewards',
    String body = 'Thank you for using our app!',
  }) async {
    await _notifications.show(
      id: 1, // Different ID from reminder
      title: title,
      body: body,
      notificationDetails: const NotificationDetails(
        android: AndroidNotificationDetails(
          'daily_reminder_channel',
          'Daily Reminders',
          channelDescription: 'Immediate notifications',
          importance: Importance.max,
          priority: Priority.high,
        ),
      ),
    );
  }
  static tz.TZDateTime _nextInstanceOfTime(int hour, int minute) {
    final now = tz.TZDateTime.now(tz.local);
    debugPrint('Current time: $now');
    final scheduled = tz.TZDateTime(tz.local, now.year, now.month, now.day, hour, minute);
    debugPrint('Scheduled time: $scheduled');
    return scheduled.isBefore(now) ? scheduled.add(const Duration(days: 1)) : scheduled;
  }

  static Future<void> cancelReminder() async {
    await _notifications.cancel(id: _reminderId);
  }

  static Future<void> showSearchProgressNotification({
    required int current,
    required int total,
    int remainingSeconds = 0,
  }) async {
    await _requestNotificationPermissions();

    final percent = total > 0 ? ((current / total) * 100).round() : 0;

    await _notifications.show(
      id: _progressNotificationId,
      title: 'Bing Search Automation',
      body: '$current/$total completed ($percent%)',
      notificationDetails: NotificationDetails(
        android: AndroidNotificationDetails(
          'search_progress_channel',
          'Search Progress',
          channelDescription: 'Ongoing notification while search is running',
          importance: Importance.defaultImportance,
          priority: Priority.defaultPriority,
          ongoing: true,
          autoCancel: false,
          onlyAlertOnce: true,
          showProgress: true,
          maxProgress: total,
          progress: current,
          subText: '$percent%',
          category: AndroidNotificationCategory.progress,
          icon: '@mipmap/ic_launcher',
          actions: const [
            AndroidNotificationAction(
              'stop_search',
              'Stop',
              cancelNotification: true,
              showsUserInterface: true,
            ),
          ],
        ),
      ),
    );
  }

  static Future<void> cancelSearchProgressNotification() async {
    await _notifications.cancel(id: _progressNotificationId);
  }
}