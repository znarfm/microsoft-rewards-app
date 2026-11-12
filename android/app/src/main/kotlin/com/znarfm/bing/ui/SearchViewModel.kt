package com.znarfm.bing.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.znarfm.bing.BuildConfig
import com.znarfm.bing.data.PreferencesRepository
import com.znarfm.bing.data.WordGenerator
import com.znarfm.bing.notifications.NotificationHelper
import com.znarfm.bing.ui.theme.AppThemeMode
import com.znarfm.bing.utils.HapticsHelper
import com.znarfm.bing.utils.InputValidators
import com.znarfm.bing.utils.UpdateChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToLong
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
    val searchState: SearchState = SearchState.Idle,
    val availableUpdate: com.znarfm.bing.utils.UpdateChecker.ReleaseInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val isDownloadingUpdate: Boolean = false,
    val updateDownloadProgress: Float = 0f
)

class SearchViewModel(
    application: Application,
    val prefs: PreferencesRepository,
    private val haptics: HapticsHelper
) : AndroidViewModel(application) {

    // Default 1-arg constructor invoked via reflection by ViewModelProvider.AndroidViewModelFactory
    constructor(application: Application) : this(
        application = application,
        prefs = PreferencesRepository(application.applicationContext),
        haptics = HapticsHelper(application.applicationContext)
    )

    private val appContext: Context = application.applicationContext

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private val _webViewUrlCommands = Channel<String>(Channel.BUFFERED)
    val webViewUrlCommands: Flow<String> = _webViewUrlCommands.receiveAsFlow()

    @Volatile
    private var pendingPageResult: CompletableDeferred<PageLoadResult>? = null

    private var searchJob: Job? = null
    private var isInForeground: Boolean = true

    fun onPageResult(url: String?, success: Boolean, error: String? = null) {
        if (url?.contains("bing.com/search") != true) return
        pendingPageResult?.complete(PageLoadResult(url = url, success = success, error = error))
    }

    init {
        checkStreakStatus()
        prefs.lastOpenedDate = PreferencesRepository.todayString()
        checkForUpdates(isManual = false)
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
        _webViewUrlCommands.trySend("https://rewards.bing.com")
    }

    fun onAppForegroundChanged(inForeground: Boolean) {
        isInForeground = inForeground
        if (inForeground) {
            if (_uiState.value.searchState !is SearchState.InProgress) {
                NotificationHelper.cancelPromotedSearchProgress(appContext)
            }
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

        // Verify signed-in via Bing auth cookie before starting run
        val cookieHeader = try {
            CookieManager.getInstance().getCookie("https://www.bing.com")
        } catch (_: Throwable) {
            null
        }
        if (!hasBingAuthCookie(cookieHeader)) {
            val notSignedInMsg = "Not signed in to Bing. Sign in via the browser below."
            _uiState.update {
                it.copy(
                    searchState = SearchState.Failure(notSignedInMsg),
                    statusBannerMessage = notSignedInMsg
                )
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
        NotificationHelper.showPromotedSearchProgress(
            context = appContext,
            current = 0,
            total = count,
            remainingSeconds = 0
        )

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                val queries = WordGenerator.distinctQueries(count)

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
                    NotificationHelper.showPromotedSearchProgress(
                        context = appContext,
                        current = i,
                        total = count,
                        remainingSeconds = 0
                    )

                    val query = queries.getOrElse(i) { WordGenerator.randomSentence() }
                    val encodedQuery = Uri.encode(query)
                    val searchUrl = buildSearchUrl(encodedQuery)

                    // Execute page load with 1 retry policy on failure
                    var attempt = 0
                    var pageSucceeded = false
                    var lastError: String? = null

                    while (attempt < 2 && !pageSucceeded) {
                        if (!isActive) throw CancellationException()
                        attempt++

                        val deferred = CompletableDeferred<PageLoadResult>()
                        pendingPageResult = deferred

                        _webViewUrlCommands.send(searchUrl)

                        val result = withTimeoutOrNull(20_000L) {
                            deferred.await()
                        }

                        if (result != null && result.success) {
                            pageSucceeded = true
                        } else {
                            lastError = when {
                                result == null -> "Page load timed out (20s)"
                                else -> result.error ?: "Page load failed"
                            }
                            if (attempt < 2) {
                                // Short pause before retry
                                delay(1000L)
                            }
                        }
                    }

                    if (!pageSucceeded) {
                        val failMsg = "Search ${i + 1} failed: ${lastError ?: "Unknown error"}"
                        throw SearchFailedException(failMsg)
                    }

                    // Start delay countdown only after successful page load
                    val totalWaitMs = computeWaitMs(delaySec)
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
                        NotificationHelper.showPromotedSearchProgress(
                            context = appContext,
                            current = i + 1,
                            total = count,
                            remainingSeconds = remSec
                        )
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
            } finally {
                pendingPageResult = null
            }
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        pendingPageResult?.cancel()
        pendingPageResult = null
        NotificationHelper.cancelPromotedSearchProgress(appContext)
        _uiState.update {
            it.copy(
                searchState = SearchState.Cancelled,
                statusBannerMessage = "Search cancelled"
            )
        }
    }

    fun checkForUpdates(isManual: Boolean = true) {
        if (_uiState.value.isCheckingUpdate) return
        _uiState.update { it.copy(isCheckingUpdate = true) }
        viewModelScope.launch {
            val result = UpdateChecker.fetchLatestRelease()
            result.onSuccess { release ->
                if (release != null) {
                    _uiState.update {
                        it.copy(isCheckingUpdate = false, availableUpdate = release)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isCheckingUpdate = false,
                            statusBannerMessage = if (isManual) {
                                "You're on the latest version (${BuildConfig.VERSION_NAME})"
                            } else {
                                it.statusBannerMessage
                            }
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isCheckingUpdate = false,
                        statusBannerMessage = if (isManual) {
                            "Update check failed: ${err.message ?: "Network error"}"
                        } else {
                            it.statusBannerMessage
                        }
                    )
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(availableUpdate = null) }
    }

    fun downloadAndInstallUpdate() {
        val release = _uiState.value.availableUpdate ?: return
        if (_uiState.value.isDownloadingUpdate) return

        _uiState.update {
            it.copy(isDownloadingUpdate = true, updateDownloadProgress = 0f)
        }

        viewModelScope.launch {
            val result = UpdateChecker.downloadApk(appContext, release.downloadUrl) { progress ->
                _uiState.update { it.copy(updateDownloadProgress = progress) }
            }
            result.onSuccess { apkFile ->
                _uiState.update {
                    it.copy(isDownloadingUpdate = false, availableUpdate = null)
                }
                val launched = UpdateChecker.installApk(appContext, apkFile)
                if (!launched) {
                    _uiState.update {
                        it.copy(
                            statusBannerMessage = "Allow 'Install unknown apps' then tap Check for Updates again."
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isDownloadingUpdate = false,
                        statusBannerMessage = "Update download failed: ${err.message ?: "Error"}"
                    )
                }
            }
        }
    }

    private fun triggerCompletionHaptics() {
        haptics.triggerCompletionPattern()
    }

    companion object {
        fun validateSearchCount(value: String?): String? = InputValidators.validateSearchCount(value)
        fun validateDelay(value: String?): String? = InputValidators.validateDelay(value)

        fun hasBingAuthCookie(cookieHeader: String?): Boolean {
            if (cookieHeader.isNullOrBlank()) return false
            val cookies = cookieHeader.split(";")
            for (cookie in cookies) {
                val trimmed = cookie.trim()
                val eqIndex = trimmed.indexOf('=')
                if (eqIndex <= 0) continue
                val name = trimmed.substring(0, eqIndex).trim()
                val value = trimmed.substring(eqIndex + 1).trim()
                if (name == "_U" && value.isNotEmpty()) {
                    return true
                }
            }
            return false
        }

        fun computeWaitMs(delaySec: Double, random: Random = Random.Default): Long {
            val baseMs = (delaySec * 1000.0).roundToLong().coerceAtLeast(0L)
            val maxJitterMs = (baseMs * 0.3).roundToLong().coerceAtLeast(1000L)
            val jitterMs = random.nextLong(-maxJitterMs, maxJitterMs + 1L)
            return (baseMs + jitterMs).coerceAtLeast(1000L)
        }

        fun buildSearchUrl(encodedQuery: String): String =
            "https://www.bing.com/search?q=$encodedQuery&qs=n&form=QBLH&sp=-1&pq="

        fun sanitizeUserAgent(ua: String): String =
            ua.replace("; wv", "")
                .replace(Regex("""Version/\d+\.\d+\s*"""), "")
    }
}

private class SearchFailedException(message: String) : Exception(message)

data class PageLoadResult(
    val url: String?,
    val success: Boolean,
    val error: String? = null
)
