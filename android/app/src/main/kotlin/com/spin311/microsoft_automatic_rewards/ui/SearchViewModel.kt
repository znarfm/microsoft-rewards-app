package com.spin311.microsoft_automatic_rewards.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spin311.microsoft_automatic_rewards.data.PreferencesRepository
import com.spin311.microsoft_automatic_rewards.data.WordGenerator
import com.spin311.microsoft_automatic_rewards.notifications.NotificationHelper
import com.spin311.microsoft_automatic_rewards.ui.theme.AppThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.spin311.microsoft_automatic_rewards.utils.HapticsHelper
import com.spin311.microsoft_automatic_rewards.utils.InputValidators
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

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
    val searchState: SearchState = SearchState.Idle
)

class SearchViewModel(
    application: Application,
    val prefs: PreferencesRepository = PreferencesRepository(application.applicationContext),
    private val haptics: HapticsHelper = HapticsHelper(application.applicationContext)
) : AndroidViewModel(application) {
    private val appContext: Context = application.applicationContext

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _webViewUrlCommands = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val webViewUrlCommands: SharedFlow<String> = _webViewUrlCommands.asSharedFlow()

    private var searchJob: Job? = null
    private var isInForeground: Boolean = true

    init {
        checkStreakStatus()
        prefs.lastOpenedDate = PreferencesRepository.todayString()
    }

    private fun loadInitialState(): AppUiState {
        return AppUiState(
            isLoggedIn = prefs.isLoggedIn,
            themeMode = prefs.themeMode,
            amoledOverlay = prefs.amoledOverlay,
            keepScreenOn = prefs.keepScreenOn,
            dataSaver = prefs.dataSaver,
            hapticFeedback = prefs.hapticFeedback,
            sendDailyReminder = prefs.sendDailyReminder,
            reminderHour = prefs.reminderHour,
            reminderMinute = prefs.reminderMinute,
            showLoginReminderPopup = prefs.showLoginReminderPopup,
            searchCountText = prefs.searchCount,
            searchDelayText = prefs.searchDelay,
            currentStreak = prefs.completionStreak,
            bestStreak = prefs.bestStreak,
            isCompletedToday = prefs.isCompletedToday()
        )
    }

    fun checkStreakStatus() {
        val lost = prefs.checkAndClearLostStreak()
        _uiState.update { state ->
            state.copy(
                currentStreak = prefs.completionStreak,
                bestStreak = prefs.bestStreak,
                isCompletedToday = prefs.isCompletedToday(),
                statusBannerMessage = if (lost > 0) {
                    "Streak lost! Your $lost-day streak reset to 0."
                } else {
                    state.statusBannerMessage
                }
            )
        }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun markLoginReminderShown(dontShowAgain: Boolean) {
        if (dontShowAgain) {
            prefs.showLoginReminderPopup = false
        }
        _uiState.update {
            it.copy(
                loginReminderShownThisSession = true,
                showLoginReminderPopup = prefs.showLoginReminderPopup
            )
        }
    }

    fun setLoggedIn(value: Boolean) {
        prefs.isLoggedIn = value
        _uiState.update { it.copy(isLoggedIn = value) }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.themeMode = mode
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setAmoledOverlay(enabled: Boolean) {
        prefs.amoledOverlay = enabled
        _uiState.update { it.copy(amoledOverlay = enabled, overlayDismissed = false) }
    }

    fun dismissAmoledOverlay() {
        _uiState.update { it.copy(overlayDismissed = true) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.keepScreenOn = enabled
        _uiState.update { it.copy(keepScreenOn = enabled) }
    }

    fun setDataSaver(enabled: Boolean) {
        prefs.dataSaver = enabled
        _uiState.update { it.copy(dataSaver = enabled) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        prefs.hapticFeedback = enabled
        _uiState.update { it.copy(hapticFeedback = enabled) }
    }

    fun setShowLoginReminderPopup(enabled: Boolean) {
        prefs.showLoginReminderPopup = enabled
        _uiState.update { it.copy(showLoginReminderPopup = enabled) }
    }

    fun setDailyReminder(enabled: Boolean) {
        prefs.sendDailyReminder = enabled
        _uiState.update { it.copy(sendDailyReminder = enabled) }
        if (enabled) {
            NotificationHelper.scheduleDailyReminder(
                appContext,
                _uiState.value.reminderHour,
                _uiState.value.reminderMinute
            )
        } else {
            NotificationHelper.cancelDailyReminder(appContext)
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        prefs.reminderHour = hour
        prefs.reminderMinute = minute
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
        if (_uiState.value.sendDailyReminder) {
            NotificationHelper.scheduleDailyReminder(appContext, hour, minute)
        }
    }

    fun updateSearchCount(text: String) {
        prefs.searchCount = text
        val err = InputValidators.validateSearchCount(text)
        _uiState.update { it.copy(searchCountText = text, countError = err) }
    }

    fun updateSearchDelay(text: String) {
        prefs.searchDelay = text
        val err = InputValidators.validateDelay(text)
        _uiState.update { it.copy(searchDelayText = text, delayError = err) }
    }

    fun openRewardsDashboard() {
        _uiState.update { it.copy(selectedTab = 0) }
        _webViewUrlCommands.tryEmit("https://rewards.bing.com")
    }

    fun onAppForegroundChanged(inForeground: Boolean) {
        isInForeground = inForeground
        if (inForeground) {
            NotificationHelper.cancelPromotedSearchProgress(appContext)
            if (prefs.searchCancelledFromNotification) {
                prefs.searchCancelledFromNotification = false
                if (_uiState.value.searchState is SearchState.InProgress) {
                    cancelSearch()
                }
            }
            checkStreakStatus()
        } else {
            val state = _uiState.value.searchState
            if (state is SearchState.InProgress) {
                NotificationHelper.showPromotedSearchProgress(
                    context = appContext,
                    current = state.current,
                    total = state.total,
                    remainingSeconds = state.remainingSeconds,
                    precomputedPercent = state.percent
                )
            }
        }
    }

    fun startSearch() {
        if (_uiState.value.searchState is SearchState.InProgress) return

        val countStr = _uiState.value.searchCountText.trim()
        val delayStr = _uiState.value.searchDelayText.trim()
        val countErr = InputValidators.validateSearchCount(countStr)
        val delayErr = InputValidators.validateDelay(delayStr)

        if (countErr != null || delayErr != null) {
            _uiState.update {
                it.copy(countError = countErr, delayError = delayErr)
            }
            return
        }

        val count = countStr.toInt()
        val delaySec = delayStr.toDouble()

        prefs.searchCancelledFromNotification = false
        NotificationHelper.dismissedByUser = false

        _uiState.update {
            it.copy(
                selectedTab = 0,
                overlayDismissed = false,
                countError = null,
                delayError = null,
                searchState = SearchState.InProgress(current = 0, total = count, remainingSeconds = 0)
            )
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                for (i in 0 until count) {
                    if (!isActive) throw CancellationException()

                    _uiState.update {
                        it.copy(
                            searchState = SearchState.InProgress(
                                current = i,
                                total = count,
                                remainingSeconds = 0
                            )
                        )
                    }
                    if (!isInForeground) {
                        NotificationHelper.showPromotedSearchProgress(
                            context = appContext,
                            current = i,
                            total = count,
                            remainingSeconds = 0
                        )
                    }

                    val query = WordGenerator.randomSentence(5)
                    val encodedQuery = Uri.encode(query)
                    _webViewUrlCommands.tryEmit("https://www.bing.com/search?q=$encodedQuery")

                    val randomJitterMs = Random.nextLong(0L, 1001L)
                    val totalWaitMs = (delaySec * 1000.0).toLong() + randomJitterMs
                    val totalWaitSec = ((totalWaitMs + 999L) / 1000L).toInt().coerceAtLeast(1)

                    var remainingMs = totalWaitMs
                    for (remSec in totalWaitSec downTo 1) {
                        if (!isActive) throw CancellationException()
                        _uiState.update {
                            it.copy(
                                searchState = SearchState.InProgress(
                                    current = i + 1,
                                    total = count,
                                    remainingSeconds = remSec
                                )
                            )
                        }
                        if (!isInForeground) {
                            NotificationHelper.showPromotedSearchProgress(
                                context = appContext,
                                current = i + 1,
                                total = count,
                                remainingSeconds = remSec
                            )
                        }
                        val stepMs = remainingMs.coerceAtMost(1000L).coerceAtLeast(100L)
                        delay(stepMs)
                        remainingMs -= stepMs
                    }
                }

                val newStreak = prefs.recordSearchCompletion()
                NotificationHelper.cancelPromotedSearchProgress(appContext)

                _uiState.update {
                    it.copy(
                        currentStreak = newStreak,
                        bestStreak = prefs.bestStreak,
                        isCompletedToday = true,
                        searchState = SearchState.Success,
                        statusBannerMessage = "All $count searches completed! 🔥 $newStreak day streak"
                    )
                }

                if (_uiState.value.hapticFeedback) {
                    triggerCompletionHaptics()
                }

                if (!isInForeground) {
                    NotificationHelper.sendImmediateNotification(
                        context = appContext,
                        title = "Searches Completed",
                        body = "All daily Bing searches have finished!"
                    )
                }
            } catch (_: CancellationException) {
                NotificationHelper.cancelPromotedSearchProgress(appContext)
                _uiState.update {
                    it.copy(
                        searchState = SearchState.Cancelled,
                        statusBannerMessage = "Search cancelled"
                    )
                }
            } catch (e: Throwable) {
                NotificationHelper.cancelPromotedSearchProgress(appContext)
                val msg = e.message ?: "Unexpected search error"
                _uiState.update {
                    it.copy(
                        searchState = SearchState.Failure(msg),
                        statusBannerMessage = "Search failed: $msg"
                    )
                }
                if (!isInForeground) {
                    NotificationHelper.sendImmediateNotification(
                        context = appContext,
                        title = "Search Interrupted",
                        body = "Your Bing search session encountered an error."
                    )
                }
            }
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        NotificationHelper.cancelPromotedSearchProgress(appContext)
        _uiState.update {
            it.copy(
                searchState = SearchState.Cancelled,
                statusBannerMessage = "Search cancelled"
            )
        }
    }

    private fun triggerCompletionHaptics() {
        haptics.triggerCompletionPattern()
    }

    companion object {
        fun validateSearchCount(value: String?): String? = InputValidators.validateSearchCount(value)
        fun validateDelay(value: String?): String? = InputValidators.validateDelay(value)
    }
}
