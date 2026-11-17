package com.znarfm.bing.ui

import com.znarfm.bing.ui.theme.AppThemeMode
import com.znarfm.bing.utils.UpdateChecker

sealed interface SearchState {
    data object Idle : SearchState
    data class InProgress(
        val current: Int,
        val total: Int,
        val remainingSeconds: Int = 0
    ) : SearchState {
        val fraction: Float
            get() = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
        val percent: Int
            get() = (fraction * 100).toInt()
    }
    data object Success : SearchState
    data object Cancelled : SearchState
    data class Failure(val message: String) : SearchState
}

data class AppUiState(
    val isLoggedIn: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val amoledOverlay: Boolean = false,
    val overlayDismissed: Boolean = false,
    val keepScreenOn: Boolean = true,
    val dataSaver: Boolean = true,
    val hapticFeedback: Boolean = true,
    val sendDailyReminder: Boolean = false,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val showLoginReminderPopup: Boolean = true,
    val loginReminderShownThisSession: Boolean = false,
    val searchCountText: String = "22",
    val searchDelayText: String = "15",
    val countError: String? = null,
    val delayError: String? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val isCompletedToday: Boolean = false,
    val statusBannerMessage: String? = null,
    val selectedTab: Int = 0,
    val searchState: SearchState = SearchState.Idle,
    val autoCheckUpdates: Boolean = true,
    val availableUpdate: UpdateChecker.ReleaseInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Float = 0f
)

data class PageLoadResult(
    val url: String?,
    val success: Boolean,
    val error: String? = null
)
