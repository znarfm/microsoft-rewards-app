package com.spin311.microsoft_automatic_rewards.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.spin311.microsoft_automatic_rewards.MainActivity
import com.spin311.microsoft_automatic_rewards.R
import java.util.Calendar

object NotificationHelper {
    const val STOP_SEARCH_ACTION = "com.spin311.microsoft_automatic_rewards.STOP_SEARCH"
    const val DISMISS_PROGRESS_ACTION = "com.spin311.microsoft_automatic_rewards.DISMISS_PROGRESS"

    private const val DAILY_REMINDER_CHANNEL_ID = "daily_reminder_channel"
    private const val SEARCH_STATUS_CHANNEL_ID = "search_completion_channel"
    private const val PROGRESS_CHANNEL_ID = "search_progress_channel"

    const val DAILY_REMINDER_NOTIFICATION_ID = 0
    const val SEARCH_STATUS_NOTIFICATION_ID = 1
    const val PROGRESS_NOTIFICATION_ID = 2
    private const val DAILY_REMINDER_REQUEST_CODE = 100

    @Volatile
    var dismissedByUser: Boolean = false

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminderChannel = NotificationChannel(
            DAILY_REMINDER_CHANNEL_ID,
            "Daily Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminder to run Microsoft Rewards searches"
        }

        val statusChannel = NotificationChannel(
            SEARCH_STATUS_CHANNEL_ID,
            "Search Status",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for search completion or errors"
        }

        val progressChannel = NotificationChannel(
            PROGRESS_CHANNEL_ID,
            "Search Progress",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Ongoing notification while search is running"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(statusChannel)
        manager.createNotificationChannel(progressChannel)
    }

    fun showPromotedSearchProgress(
        context: Context,
        current: Int,
        total: Int,
        remainingSeconds: Int
    ) {
        if (current <= 1 && remainingSeconds == 0) {
            dismissedByUser = false
        }
        if (dismissedByUser) return

        ensureChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val percent = if (total > 0) ((current.toDouble() / total.toDouble()) * 100).toInt() else 0
        val bodyText = if (remainingSeconds > 0) {
            "$current/$total completed ($percent%) • Next in ${remainingSeconds}s"
        } else {
            "$current/$total completed ($percent%) • Searching…"
        }
        // Status bar chip text (< 7 chars required by Android 16 to display full text in chip)
        val chipText = "$current/$total"

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(STOP_SEARCH_ACTION).apply {
            setPackage(context.packageName)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(DISMISS_PROGRESS_ACTION).apply {
            setPackage(context.packageName)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, PROGRESS_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val stopAction = Notification.Action.Builder(
            null,
            "Stop",
            stopPendingIntent
        ).build()

        builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bing Search Automation")
            .setContentText(bodyText)
            .setSubText("$percent%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(stopAction)
            .setProgress(total.coerceAtLeast(1), current.coerceAtLeast(0), false)

        val extras = Bundle().apply {
            putBoolean("android.requestPromotedOngoing", true)
            putCharSequence("android.shortCriticalText", chipText)
        }
        builder.addExtras(extras)

        if (Build.VERSION.SDK_INT >= 36) {
            applyAndroid16LiveUpdateFeatures(builder, chipText, percent)
        }

        try {
            manager.notify(PROGRESS_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    private fun applyAndroid16LiveUpdateFeatures(
        builder: Notification.Builder,
        chipText: String,
        percent: Int
    ) {
        try {
            builder.javaClass
                .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                .invoke(builder, true)
        } catch (_: Throwable) {
        }

        try {
            builder.javaClass
                .getMethod("setShortCriticalText", CharSequence::class.java)
                .invoke(builder, chipText)
        } catch (_: Throwable) {
        }

        try {
            val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
            val progressStyle = progressStyleClass.getDeclaredConstructor().newInstance()
            progressStyleClass
                .getMethod("setStyledByProgress", Boolean::class.javaPrimitiveType)
                .invoke(progressStyle, true)
            progressStyleClass
                .getMethod("setProgress", Int::class.javaPrimitiveType)
                .invoke(progressStyle, percent.coerceIn(0, 100))
            progressStyleClass
                .getMethod("setProgressIndeterminate", Boolean::class.javaPrimitiveType)
                .invoke(progressStyle, false)

            val styleClass = Class.forName("android.app.Notification\$Style")
            builder.javaClass
                .getMethod("setStyle", styleClass)
                .invoke(builder, progressStyle)
        } catch (_: Throwable) {
        }
    }

    fun cancelPromotedSearchProgress(context: Context) {
        dismissedByUser = false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(PROGRESS_NOTIFICATION_ID)
    }

    fun sendImmediateNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = SEARCH_STATUS_NOTIFICATION_ID,
        isReminder: Boolean = false
    ) {
        ensureChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (isReminder) DAILY_REMINDER_CHANNEL_ID else SEARCH_STATUS_CHANNEL_ID

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        val notification = builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    target.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                target.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
