package com.znarfm.bing

import com.znarfm.bing.data.PreferencesRepository
import com.znarfm.bing.data.WordGenerator
import com.znarfm.bing.ui.SearchViewModel
import com.znarfm.bing.ui.theme.AppThemeMode
import com.znarfm.bing.utils.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardsUnitTest {

    @Test
    fun `FlutterSharedPreferences keys preserve exact backward compatibility`() {
        assertEquals("FlutterSharedPreferences", PreferencesRepository.PREFS_FILE_NAME)
        assertEquals("flutter.loggedIn", PreferencesRepository.KEY_LOGGED_IN)
        assertEquals("flutter.search_count", PreferencesRepository.KEY_SEARCH_COUNT)
        assertEquals("flutter.search_delay", PreferencesRepository.KEY_SEARCH_DELAY)
        assertEquals("flutter.theme_mode", PreferencesRepository.KEY_THEME_MODE)
        assertEquals("flutter.amoled_screen_off", PreferencesRepository.KEY_AMOLED_SCREEN_OFF)
        assertEquals("flutter.completion_streak", PreferencesRepository.KEY_COMPLETION_STREAK)
        assertEquals("flutter.best_streak", PreferencesRepository.KEY_BEST_STREAK)
    }

    @Test
    fun `streak increments on consecutive days and updates best streak`() {
        val r1 = PreferencesRepository.computeStreakAfterCompletion(
            currentStreak = 0,
            bestStreak = 0,
            lastCompletedDate = null,
            todayStr = "2026-04-10",
            yesterdayStr = "2026-04-09"
        )
        assertEquals(1, r1.newStreak)
        assertEquals(1, r1.newBestStreak)
        assertEquals("2026-04-10", r1.lastCompletedDate)

        // Same day completion is idempotent
        val rSame = PreferencesRepository.computeStreakAfterCompletion(
            currentStreak = r1.newStreak,
            bestStreak = r1.newBestStreak,
            lastCompletedDate = r1.lastCompletedDate,
            todayStr = "2026-04-10",
            yesterdayStr = "2026-04-09"
        )
        assertEquals(1, rSame.newStreak)
        assertEquals(1, rSame.newBestStreak)

        // Next day increments streak
        val r2 = PreferencesRepository.computeStreakAfterCompletion(
            currentStreak = r1.newStreak,
            bestStreak = r1.newBestStreak,
            lastCompletedDate = r1.lastCompletedDate,
            todayStr = "2026-04-11",
            yesterdayStr = "2026-04-10"
        )
        assertEquals(2, r2.newStreak)
        assertEquals(2, r2.newBestStreak)
    }

    @Test
    fun `streak resets when day is missed`() {
        val lost = PreferencesRepository.computeLostStreak(
            currentStreak = 5,
            lastCompletedDate = "2026-04-01",
            todayStr = "2026-04-03",
            yesterdayStr = "2026-04-02"
        )
        assertEquals(5, lost)

        val notLost = PreferencesRepository.computeLostStreak(
            currentStreak = 5,
            lastCompletedDate = "2026-04-02",
            todayStr = "2026-04-03",
            yesterdayStr = "2026-04-02"
        )
        assertEquals(0, notLost)
    }

    @Test
    fun `WordGenerator generates non-empty realistic search query`() {
        val query = WordGenerator.randomSentence(5)
        assertTrue(query.isNotBlank())
        assertEquals(query.trim(), query)
        assertTrue(!query.contains("  "))
        assertEquals(query.lowercase(), query)
        val words = query.split(" ")
        assertTrue(words.size in 2..7)
    }

    @Test
    fun `WordGenerator returns empty string for non-positive wordCount`() {
        assertEquals("", WordGenerator.randomSentence(0))
        assertEquals("", WordGenerator.randomSentence(-1))
    }

    @Test
    fun `WordGenerator achieves high uniqueness across 100 generations`() {
        val seed = 42L
        val rng = kotlin.random.Random(seed)
        val queries = (1..100).map { WordGenerator.randomSentence(5, rng) }
        val uniqueQueries = queries.toSet()

        queries.forEach { query ->
            assertTrue(query.isNotBlank())
            assertEquals(query.trim(), query)
            assertTrue(!query.contains("  "))
        }

        assertTrue("Expected at least 85 unique queries out of 100, got ${uniqueQueries.size}", uniqueQueries.size >= 85)
    }

    @Test
    fun `WordGenerator distinctQueries returns count unique queries`() {
        val queries = WordGenerator.distinctQueries(60, kotlin.random.Random(7))
        assertEquals(60, queries.size)
        assertEquals(60, queries.toSet().size)
    }

    @Test
    fun `hasBingAuthCookie detects non-empty _U cookie only`() {
        assertTrue(SearchViewModel.hasBingAuthCookie("MUID=abc; _U=token123; SRCHD=AF"))
        assertTrue(!SearchViewModel.hasBingAuthCookie("MUID=abc; _U=; SRCHD=AF"))
        assertTrue(!SearchViewModel.hasBingAuthCookie("MUID=abc; X_U=token"))
        assertTrue(!SearchViewModel.hasBingAuthCookie(null))
        assertTrue(!SearchViewModel.hasBingAuthCookie(""))
    }

    @Test
    fun `computeWaitMs jitters symmetrically within 30 percent and floors at 1s`() {
        val rng = kotlin.random.Random(1)
        val waits = (1..500).map { SearchViewModel.computeWaitMs(15.0, rng) }
        assertTrue(waits.all { it in 10_500L..19_500L })
        assertTrue(waits.any { it < 15_000L } && waits.any { it > 15_000L })
        assertTrue((1..100).all { SearchViewModel.computeWaitMs(0.0, rng) >= 1_000L })
    }

    @Test
    fun `input validators enforce search count and delay limits`() {
        assertNull(SearchViewModel.validateSearchCount("22"))
        assertNull(SearchViewModel.validateSearchCount("50"))
        assertNotNull(SearchViewModel.validateSearchCount("0"))
        assertNotNull(SearchViewModel.validateSearchCount("51"))
        assertNotNull(SearchViewModel.validateSearchCount("abc"))

        assertNull(SearchViewModel.validateDelay("0.5"))
        assertNull(SearchViewModel.validateDelay("15"))
        assertNotNull(SearchViewModel.validateDelay("0.2"))
        assertNotNull(SearchViewModel.validateDelay("xyz"))
    }

    @Test
    fun `SearchState InProgress computes accurate progress fraction and percent`() {
        val state0 = com.znarfm.bing.ui.SearchState.InProgress(current = 0, total = 20)
        assertEquals(0f, state0.fraction, 0.001f)
        assertEquals(0, state0.percent)

        val stateHalf = com.znarfm.bing.ui.SearchState.InProgress(current = 10, total = 20)
        assertEquals(0.5f, stateHalf.fraction, 0.001f)
        assertEquals(50, stateHalf.percent)

        val stateFull = com.znarfm.bing.ui.SearchState.InProgress(current = 20, total = 20)
        assertEquals(1.0f, stateFull.fraction, 0.001f)
        assertEquals(100, stateFull.percent)
    }

    @Test
    fun `InputValidators enforces count and delay limits directly`() {
        assertNull(com.znarfm.bing.utils.InputValidators.validateSearchCount("30"))
        assertNotNull(com.znarfm.bing.utils.InputValidators.validateSearchCount("0"))
        assertNotNull(com.znarfm.bing.utils.InputValidators.validateSearchCount("51"))
        assertNull(com.znarfm.bing.utils.InputValidators.validateDelay("10"))
        assertNotNull(com.znarfm.bing.utils.InputValidators.validateDelay("0.4"))
    }

    @Test
    fun `DailyReminderWorker shouldSendReminder verifies smart reminder conditions`() {
        // Disabled reminder never sends
        org.junit.Assert.assertFalse(
            com.znarfm.bing.notifications.DailyReminderWorker.shouldSendReminder(
                sendDailyReminder = false,
                isCompletedToday = false
            )
        )
        org.junit.Assert.assertFalse(
            com.znarfm.bing.notifications.DailyReminderWorker.shouldSendReminder(
                sendDailyReminder = false,
                isCompletedToday = true
            )
        )
        // Enabled reminder skips if already completed today
        org.junit.Assert.assertFalse(
            com.znarfm.bing.notifications.DailyReminderWorker.shouldSendReminder(
                sendDailyReminder = true,
                isCompletedToday = true
            )
        )
        // Enabled reminder sends if not completed today
        assertTrue(
            com.znarfm.bing.notifications.DailyReminderWorker.shouldSendReminder(
                sendDailyReminder = true,
                isCompletedToday = false
            )
        )
    }

    @Test
    fun `DailyReminderWorker computeInitialDelayMillis handles later today and next day rollover`() {
        val baseCalendar = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.APRIL, 10, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // Target 14:30 same day -> exactly 4.5 hours = 16,200,000 ms
        val delaySameDay = com.znarfm.bing.notifications.DailyReminderWorker.computeInitialDelayMillis(
            hour = 14,
            minute = 30,
            now = baseCalendar
        )
        assertEquals(16_200_000L, delaySameDay)

        // Target 08:15 next day -> 22 hours + 15 min = 80,100,000 ms
        val delayNextDay = com.znarfm.bing.notifications.DailyReminderWorker.computeInitialDelayMillis(
            hour = 8,
            minute = 15,
            now = baseCalendar
        )
        assertEquals(80_100_000L, delayNextDay)
    }

    @Test
    fun `SearchForegroundService and DailyReminderWorker constants match expectations`() {
        assertEquals(
            "com.znarfm.bing.FGS_START_OR_UPDATE",
            com.znarfm.bing.notifications.SearchForegroundService.ACTION_START_OR_UPDATE
        )
        assertEquals(
            "com.znarfm.bing.FGS_STOP",
            com.znarfm.bing.notifications.SearchForegroundService.ACTION_STOP
        )
        assertEquals(
            "daily_rewards_reminder",
            com.znarfm.bing.notifications.DailyReminderWorker.WORK_NAME
        )
    }

    @Test
    fun `SearchViewModel has 1-arg Application constructor for ViewModelProvider`() {
        val constructor = com.znarfm.bing.ui.SearchViewModel::class.java
            .getConstructor(android.app.Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun `AppThemeMode maps storage keys accurately`() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStorageKey("system"))
        assertEquals(AppThemeMode.LIGHT, AppThemeMode.fromStorageKey("light"))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromStorageKey("dark"))
        assertEquals(AppThemeMode.AMOLED, AppThemeMode.fromStorageKey("amoled"))
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStorageKey(null))
    }

    @Test
    fun `buildSearchUrl includes form=QBLH, qs=n, and sp=-1 params`() {
        val url = SearchViewModel.buildSearchUrl("test%20query")
        assertEquals(
            "https://www.bing.com/search?q=test%20query&qs=n&form=QBLH&sp=-1&pq=",
            url
        )
        assertTrue(url.contains("form=QBLH"))
        assertTrue(url.contains("qs=n"))
        assertTrue(url.contains("sp=-1"))
    }

    @Test
    fun `sanitizeUserAgent strips wv token and Version prefix`() {
        val webViewUa = "Mozilla/5.0 (Linux; Android 14; Pixel 7 Build/UQ1A) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/120.0.6099.230 Mobile Safari/537.36; wv"
        val cleaned = SearchViewModel.sanitizeUserAgent(webViewUa)
        assertEquals(
            "Mozilla/5.0 (Linux; Android 14; Pixel 7 Build/UQ1A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36",
            cleaned
        )
        assertTrue(!cleaned.contains("; wv"))
        assertTrue(!cleaned.contains("Version/4.0"))
    }

    @Test
    fun `UpdateChecker isNewerVersion compares semantic versions accurately`() {
        assertTrue(UpdateChecker.isNewerVersion("2.4.0", "v2.5.0"))
        assertFalse(UpdateChecker.isNewerVersion("2.4.0", "v2.4.0"))
        assertFalse(UpdateChecker.isNewerVersion("2.4.0", "v2.3.2"))
        assertTrue(UpdateChecker.isNewerVersion("2.4.0", "v2.4.1-beta"))
        assertFalse(UpdateChecker.isNewerVersion("2.4", "v2.4.0"))
    }

    @Test
    fun `UpdateChecker selectBestApkAsset prefers matching device ABI then universal`() {
        val assets = listOf(
            "app-release.aab" to "https://github.com/.../app-release.aab",
            "microsoft-rewards-automator-arm64-v8a.apk" to "https://github.com/.../arm64-v8a.apk",
            "microsoft-rewards-automator-armeabi-v7a.apk" to "https://github.com/.../armeabi-v7a.apk",
            "microsoft-rewards-automator-universal.apk" to "https://github.com/.../universal.apk",
            "microsoft-rewards-automator-x86_64.apk" to "https://github.com/.../x86_64.apk"
        )

        // Matching device ABI prefers arm64-v8a
        val matchAbi = UpdateChecker.selectBestApkAsset(assets, listOf("arm64-v8a", "armeabi-v7a"))
        assertNotNull(matchAbi)
        assertEquals("microsoft-rewards-automator-arm64-v8a.apk", matchAbi!!.first)
        assertEquals("https://github.com/.../arm64-v8a.apk", matchAbi.second)

        // Fallback to universal when ABI is mips (unmatched), ignoring .aab
        val fallbackUniversal = UpdateChecker.selectBestApkAsset(assets, listOf("mips"))
        assertNotNull(fallbackUniversal)
        assertEquals("microsoft-rewards-automator-universal.apk", fallbackUniversal!!.first)
        assertEquals("https://github.com/.../universal.apk", fallbackUniversal.second)
    }

    @Test
    fun `UpdateChecker parseLatestReleaseJson extracts tag notes and best APK asset`() {
        val jsonPayload = """
            {
              "tag_name": "v2.5.0",
              "html_url": "https://github.com/znarfm/microsoft-rewards-app/releases/tag/v2.5.0",
              "body": "## What's Changed\r\n- Bug fixes and improvements\r\n- Faster search delays",
              "assets": [
                {
                  "name": "microsoft-rewards-automator.aab",
                  "browser_download_url": "https://github.com/znarfm/microsoft-rewards-app/releases/download/v2.5.0/microsoft-rewards-automator.aab"
                },
                {
                  "name": "microsoft-rewards-automator-arm64-v8a.apk",
                  "browser_download_url": "https://github.com/znarfm/microsoft-rewards-app/releases/download/v2.5.0/microsoft-rewards-automator-arm64-v8a.apk"
                },
                {
                  "name": "microsoft-rewards-automator-universal.apk",
                  "browser_download_url": "https://github.com/znarfm/microsoft-rewards-app/releases/download/v2.5.0/microsoft-rewards-automator-universal.apk"
                }
              ]
            }
        """.trimIndent()

        val release = UpdateChecker.parseLatestReleaseJson(
            jsonStr = jsonPayload,
            currentVersion = "2.4.0",
            supportedAbis = listOf("arm64-v8a")
        )

        assertNotNull(release)
        assertEquals("v2.5.0", release!!.tagName)
        assertEquals("2.5.0", release.versionName)
        assertEquals("https://github.com/znarfm/microsoft-rewards-app/releases/tag/v2.5.0", release.htmlUrl)
        assertEquals("## What's Changed\r\n- Bug fixes and improvements\r\n- Faster search delays", release.releaseNotes)
        assertEquals("microsoft-rewards-automator-arm64-v8a.apk", release.assetName)
        assertEquals("https://github.com/znarfm/microsoft-rewards-app/releases/download/v2.5.0/microsoft-rewards-automator-arm64-v8a.apk", release.downloadUrl)
    }
}

