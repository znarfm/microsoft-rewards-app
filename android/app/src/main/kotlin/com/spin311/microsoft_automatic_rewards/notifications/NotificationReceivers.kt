package com.spin311.microsoft_automatic_rewards.notifications
 
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.spin311.microsoft_automatic_rewards.data.PreferencesRepository
import java.util.Calendar

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val prefs = PreferencesRepository(applicationContext)
        if (!prefs.sendDailyReminder) return Result.success()

        if (!prefs.isCompletedToday()) {
            NotificationHelper.sendImmediateNotification(
                context = applicationContext,
                title = "Microsoft Rewards Reminder",
                body = "Don't forget to complete your daily Bing searches!",
                notificationId = NotificationHelper.DAILY_REMINDER_NOTIFICATION_ID,
                isReminder = true
            )
        }

        // Reschedule next day's reminder
        NotificationHelper.scheduleDailyReminder(
            context = applicationContext,
            hour = prefs.reminderHour,
            minute = prefs.reminderMinute
        )

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_rewards_reminder"

        fun shouldSendReminder(sendDailyReminder: Boolean, isCompletedToday: Boolean): Boolean {
            return sendDailyReminder && !isCompletedToday
        }

        fun computeInitialDelayMillis(
            hour: Int,
            minute: Int,
            now: Calendar = Calendar.getInstance()
        ): Long {
            val target = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
        }
    }
}

