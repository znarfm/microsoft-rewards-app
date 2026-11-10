package com.spin311.microsoft_automatic_rewards.data

import android.content.Context
import android.content.SharedPreferences
import com.spin311.microsoft_automatic_rewards.ui.theme.AppThemeMode
import java.util.Calendar

data class StreakUpdateResult(
    val newStreak: Int,
    val newBestStreak: Int,
    val lastCompletedDate: String
)

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    private fun getIntCompat(key: String, default: Int): Int {
        val raw = prefs.all[key]
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: default
            else -> default
        }
    }

    private fun getBooleanCompat(key: String, default: Boolean): Boolean {
        val raw = prefs.all[key]
        return when (raw) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.toBooleanStrictOrNull() ?: default
            else -> default
        }
    }

    private fun getStringCompat(key: String, default: String? = null): String? {
        val raw = prefs.all[key]
        return when (raw) {
            is String -> raw
            null -> default
            else -> raw.toString()
        }
    }

    private fun putIntCompat(key: String, value: Int) {
        // Store as Long to remain 100% compatible with FlutterSharedPreferences
        prefs.edit().putLong(key, value.toLong()).apply()
    }

    var isLoggedIn: Boolean
        get() = getBooleanCompat(KEY_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    var searchCount: String
        get() = getStringCompat(KEY_SEARCH_COUNT, DEFAULT_SEARCH_COUNT) ?: DEFAULT_SEARCH_COUNT
        set(value) = prefs.edit().putString(KEY_SEARCH_COUNT, value).apply()

    var searchDelay: String
        get() = getStringCompat(KEY_SEARCH_DELAY, DEFAULT_SEARCH_DELAY) ?: DEFAULT_SEARCH_DELAY
        set(value) = prefs.edit().putString(KEY_SEARCH_DELAY, value).apply()

    var sendDailyReminder: Boolean
        get() = getBooleanCompat(KEY_SEND_DAILY_REMINDER, DEFAULT_SEND_DAILY_REMINDER)
        set(value) = prefs.edit().putBoolean(KEY_SEND_DAILY_REMINDER, value).apply()

    var keepScreenOn: Boolean
        get() = getBooleanCompat(KEY_KEEP_SCREEN_ON, DEFAULT_KEEP_SCREEN_ON)
        set(value) = prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    var showLoginReminderPopup: Boolean
        get() = getBooleanCompat(KEY_SHOW_LOGIN_REMINDER_POPUP, DEFAULT_SHOW_LOGIN_REMINDER_POPUP)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_LOGIN_REMINDER_POPUP, value).apply()

    var reminderHour: Int
        get() = getIntCompat(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
        set(value) = putIntCompat(KEY_REMINDER_HOUR, value)

    var reminderMinute: Int
        get() = getIntCompat(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE)
        set(value) = putIntCompat(KEY_REMINDER_MINUTE, value)

    var lastOpenedDate: String?
        get() = getStringCompat(KEY_LAST_OPENED_DATE, null)
        set(value) = prefs.edit().putString(KEY_LAST_OPENED_DATE, value).apply()

    var themeMode: AppThemeMode
        get() = AppThemeMode.fromStorageKey(getStringCompat(KEY_THEME_MODE, null))
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.storageKey).apply()

    var amoledOverlay: Boolean
        get() = getBooleanCompat(KEY_AMOLED_SCREEN_OFF, DEFAULT_AMOLED_SCREEN_OFF)
        set(value) = prefs.edit().putBoolean(KEY_AMOLED_SCREEN_OFF, value).apply()

    var dataSaver: Boolean
        get() = getBooleanCompat(KEY_DATA_SAVER, DEFAULT_DATA_SAVER)
        set(value) = prefs.edit().putBoolean(KEY_DATA_SAVER, value).apply()

    var hapticFeedback: Boolean
        get() = getBooleanCompat(KEY_HAPTIC_FEEDBACK, DEFAULT_HAPTIC_FEEDBACK)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var completionStreak: Int
        get() = getIntCompat(KEY_COMPLETION_STREAK, 0)
        set(value) = putIntCompat(KEY_COMPLETION_STREAK, value)

    var bestStreak: Int
        get() = getIntCompat(KEY_BEST_STREAK, 0)
        set(value) = putIntCompat(KEY_BEST_STREAK, value)

    var lastCompletedDate: String?
        get() = getStringCompat(KEY_LAST_COMPLETED_DATE, null)
        set(value) = prefs.edit().putString(KEY_LAST_COMPLETED_DATE, value).apply()

    var searchCancelledFromNotification: Boolean
        get() = getBooleanCompat(KEY_SEARCH_CANCELLED_FROM_NOTIFICATION, false)
        set(value) = prefs.edit().putBoolean(KEY_SEARCH_CANCELLED_FROM_NOTIFICATION, value).apply()

    fun isCompletedToday(now: Calendar = Calendar.getInstance()): Boolean {
        return lastCompletedDate == todayString(now)
    }

    fun checkAndClearLostStreak(now: Calendar = Calendar.getInstance()): Int {
        val today = todayString(now)
        val yesterday = yesterdayString(now)
        val lost = computeLostStreak(
            currentStreak = completionStreak,
            lastCompletedDate = lastCompletedDate,
            todayStr = today,
            yesterdayStr = yesterday
        )
        if (lost > 0) {
            completionStreak = 0
        }
        return lost
    }

    fun recordSearchCompletion(now: Calendar = Calendar.getInstance()): Int {
        val today = todayString(now)
        val yesterday = yesterdayString(now)
        val result = computeStreakAfterCompletion(
            currentStreak = completionStreak,
            bestStreak = bestStreak,
            lastCompletedDate = lastCompletedDate,
            todayStr = today,
            yesterdayStr = yesterday
        )
        completionStreak = result.newStreak
        bestStreak = result.newBestStreak
        lastCompletedDate = result.lastCompletedDate
        return result.newStreak
    }

    companion object {
        const val PREFS_FILE_NAME = "FlutterSharedPreferences"

        const val KEY_LOGGED_IN = "flutter.loggedIn"
        const val KEY_SEARCH_COUNT = "flutter.search_count"
        const val KEY_SEARCH_DELAY = "flutter.search_delay"
        const val KEY_SEND_DAILY_REMINDER = "flutter.send_daily_reminder"
        const val KEY_KEEP_SCREEN_ON = "flutter.keep_screen_on"
        const val KEY_SHOW_LOGIN_REMINDER_POPUP = "flutter.show_login_reminder_popup"
        const val KEY_REMINDER_HOUR = "flutter.reminder_hour"
        const val KEY_REMINDER_MINUTE = "flutter.reminder_minute"
        const val KEY_LAST_OPENED_DATE = "flutter.last_opened_date"
        const val KEY_THEME_MODE = "flutter.theme_mode"
        const val KEY_AMOLED_SCREEN_OFF = "flutter.amoled_screen_off"
        const val KEY_DATA_SAVER = "flutter.data_saver"
        const val KEY_COMPLETION_STREAK = "flutter.completion_streak"
        const val KEY_BEST_STREAK = "flutter.best_streak"
        const val KEY_LAST_COMPLETED_DATE = "flutter.last_completed_date"
        const val KEY_HAPTIC_FEEDBACK = "flutter.haptic_feedback"
        const val KEY_SEARCH_CANCELLED_FROM_NOTIFICATION = "flutter.search_cancelled_from_notification"

        const val DEFAULT_SEARCH_COUNT = "22"
        const val DEFAULT_SEARCH_DELAY = "15"
        const val DEFAULT_SEND_DAILY_REMINDER = false
        const val DEFAULT_KEEP_SCREEN_ON = true
        const val DEFAULT_SHOW_LOGIN_REMINDER_POPUP = true
        const val DEFAULT_REMINDER_HOUR = 19
        const val DEFAULT_REMINDER_MINUTE = 0
        const val DEFAULT_AMOLED_SCREEN_OFF = false
        const val DEFAULT_DATA_SAVER = true
        const val DEFAULT_HAPTIC_FEEDBACK = true

        fun formatDate(year: Int, monthOneBased: Int, day: Int): String {
            return "%04d-%02d-%02d".format(year, monthOneBased, day)
        }

        fun todayString(now: Calendar = Calendar.getInstance()): String {
            return formatDate(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun yesterdayString(now: Calendar = Calendar.getInstance()): String {
            val cal = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            return formatDate(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun computeLostStreak(
            currentStreak: Int,
            lastCompletedDate: String?,
            todayStr: String,
            yesterdayStr: String
        ): Int {
            if (currentStreak <= 0 || lastCompletedDate == null) return 0
            if (lastCompletedDate != todayStr && lastCompletedDate != yesterdayStr) {
                return currentStreak
            }
            return 0
        }

        fun computeStreakAfterCompletion(
            currentStreak: Int,
            bestStreak: Int,
            lastCompletedDate: String?,
            todayStr: String,
            yesterdayStr: String
        ): StreakUpdateResult {
            if (lastCompletedDate == todayStr) {
                val safeBest = maxOf(bestStreak, currentStreak)
                return StreakUpdateResult(currentStreak, safeBest, todayStr)
            }
            val nextStreak = if (lastCompletedDate == yesterdayStr) {
                currentStreak + 1
            } else {
                1
            }
            val nextBest = maxOf(bestStreak, nextStreak)
            return StreakUpdateResult(nextStreak, nextBest, todayStr)
        }
    }
}
