package com.spin311.microsoft_automatic_rewards.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spin311.microsoft_automatic_rewards.data.PreferencesRepository

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = PreferencesRepository(context)
        if (!prefs.sendDailyReminder) return

        NotificationHelper.sendImmediateNotification(
            context = context,
            title = "Microsoft Rewards Reminder",
            body = "Don't forget to complete your daily Bing searches!",
            notificationId = NotificationHelper.DAILY_REMINDER_NOTIFICATION_ID,
            isReminder = true
        )

        // Schedule next day's reminder
        NotificationHelper.scheduleDailyReminder(
            context = context,
            hour = prefs.reminderHour,
            minute = prefs.reminderMinute
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = PreferencesRepository(context)
        if (prefs.sendDailyReminder) {
            NotificationHelper.scheduleDailyReminder(
                context = context,
                hour = prefs.reminderHour,
                minute = prefs.reminderMinute
            )
        }
    }
}
