package com.spin311.microsoft_automatic_rewards

import com.spin311.microsoft_automatic_rewards.data.PreferencesRepository
import com.spin311.microsoft_automatic_rewards.data.WordGenerator
import com.spin311.microsoft_automatic_rewards.ui.SearchViewModel
import com.spin311.microsoft_automatic_rewards.ui.theme.AppThemeMode
import org.junit.Assert.assertEquals
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
    fun `WordGenerator generates 5-word search query`() {
        val query = WordGenerator.randomSentence(5)
        val words = query.split(" ").filter { it.isNotBlank() }
        assertEquals(5, words.size)
        assertTrue(query.isNotBlank())
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
        val state0 = com.spin311.microsoft_automatic_rewards.ui.SearchState.InProgress(current = 0, total = 20)
        assertEquals(0f, state0.fraction, 0.001f)
        assertEquals(0, state0.percent)

        val stateHalf = com.spin311.microsoft_automatic_rewards.ui.SearchState.InProgress(current = 10, total = 20)
        assertEquals(0.5f, stateHalf.fraction, 0.001f)
        assertEquals(50, stateHalf.percent)

        val stateFull = com.spin311.microsoft_automatic_rewards.ui.SearchState.InProgress(current = 20, total = 20)
        assertEquals(1.0f, stateFull.fraction, 0.001f)
        assertEquals(100, stateFull.percent)
    }

    @Test
    fun `InputValidators enforces count and delay limits directly`() {
        assertNull(com.spin311.microsoft_automatic_rewards.utils.InputValidators.validateSearchCount("30"))
        assertNotNull(com.spin311.microsoft_automatic_rewards.utils.InputValidators.validateSearchCount("0"))
        assertNotNull(com.spin311.microsoft_automatic_rewards.utils.InputValidators.validateSearchCount("51"))
        assertNull(com.spin311.microsoft_automatic_rewards.utils.InputValidators.validateDelay("10"))
        assertNotNull(com.spin311.microsoft_automatic_rewards.utils.InputValidators.validateDelay("0.4"))
    }

    @Test
    fun `PRESET_SEARCH_COUNTS contains valid standard preset values`() {
        assertEquals(listOf(10, 20, 30, 50), PreferencesRepository.PRESET_SEARCH_COUNTS)
    }

    @Test
    fun `SearchViewModel has 1-arg Application constructor for ViewModelProvider`() {
        val constructor = com.spin311.microsoft_automatic_rewards.ui.SearchViewModel::class.java
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
}
