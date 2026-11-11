# Agent Instructions
## Communication (Caveman)
Respond like smart caveman. Cut filler, keep technical substance.
- Drop articles (a, an, the), filler (just, really, basically, actually), pleasantries, hedging.
- Fragments fine. Short synonyms. Technical terms exact. Code blocks unchanged.
- Pattern: [thing] [action] [reason]. [next step].

## Engineering (Ponytail)
Ship simplest, minimal working solution. YAGNI.
- Reuse ladder: Existing code > stdlib > native platform > installed deps > new code.
- No unrequested abstractions, boilerplate, or extra dependencies.
- Trace flow first; fix root cause at origin, not symptom.
- Code first. Max 1-3 terse lines on skipped items/triggers.
- Keep security, input validation, and data loss prevention intact.

# Repository Guidelines
## Project Overview
Native Android app (Kotlin + Jetpack Compose M3 Expressive), `applicationId = com.spin311.microsoft_automatic_rewards`, v2.3.2 (code 27). Sign in to Microsoft in embedded `WebView`, run configurable Bing searches (count + delay) to earn Rewards points. Extras: streaks, daily reminder, keep-screen-on, AMOLED overlay, data saver, haptics, Android 16 Live Update progress. Upgraded in-place from old Flutter app — keep compat (see Persistence).

## Architecture & Data Flow
Code root: `android/app/src/main/kotlin/com/spin311/microsoft_automatic_rewards/`. Single Activity, single ViewModel, no DI framework.
- `MainActivity` — Compose host; `LoginScreen` if `!isLoggedIn` else `SearchScreen`. Handles start-search shortcut, keep-screen-on flag, immersive AMOLED, `onStart/onStop` → `viewModel.onAppForegroundChanged`. Manifest `configChanges` keeps Activity (and WebView) alive across rotation/uiMode.
- `ui/screens/LoginScreen` — MS login WebView; user taps **Continue** → `setLoggedIn(true)` (no auto URL detection: MS redirects flicker).
- `ui/screens/SearchScreen` — controls, progress card, persistent Bing `WebView` (`remember`). Collects `viewModel.webViewUrlCommands` → `loadUrl`. `WebViewClient` reports main-frame result via `viewModel.onPageResult(url, success, error)` (`onReceivedError`, `onReceivedHttpError >= 400`, `onPageFinished`).
- `ui/SearchViewModel` — `StateFlow<AppUiState>`, `SearchState` (Idle/InProgress/Success/Cancelled/Failure). `startSearch`:
  1. validate count/delay; require Bing `_U` auth cookie (`hasBingAuthCookie`) else `Failure`.
  2. `WordGenerator.distinctQueries(count)`.
  3. per query: send `bing.com/search?q=` on `Channel` (never dropped), await `PageLoadResult` (only `bing.com/search` URLs accepted) ≤20s, 1 retry, then `Failure` (no streak).
  4. wait `computeWaitMs(delay)` (±30%, min window 1s, floor 1s) with per-second countdown + notification update.
  5. all done → `prefs.recordSearchCompletion()` streak, `Success`.
  Cancel = coroutine cancel (`cancelSearch`, notification Stop action).
- `data/WordGenerator` — template-based natural queries (`randomSentence`, `distinctQueries`). No brands/people/NSFW.
- `data/PreferencesRepository` — all prefs + pure streak math (`computeStreakAfterCompletion`).
- `notifications/` — `NotificationHelper` (progress Live Update, daily reminder scheduling), `SearchForegroundService` (dataSync FGS + partial wakelock during run), `NotificationReceivers.kt` (`DailyReminderWorker` WorkManager smart reminder, receivers).
- `ui/components/ExpressiveComponents`, `ui/theme/Theme` (`AppThemeMode` system/light/dark/amoled, dynamic color, seed `#0061B0`), `utils/` (`InputValidators`, `HapticsHelper`).

## Persistence
`SharedPreferences` file `FlutterSharedPreferences`, keys `flutter.*`, ints Long-encoded — ⚠️ never rename (upgrade compat, asserted in tests). Defaults: count `"22"`, delay `"15"`, daily reminder off @19:00, keep screen on, login popup on, AMOLED overlay off, data saver on, haptics on, theme `system`.

## Commands
No `gradlew` script committed (wrapper props only, Gradle 8.14.3). Use system `gradle` from `android/`:
```bash
gradle testReleaseUnitTest   # unit tests (CI gate)
gradle assembleRelease       # APKs
gradle bundleRelease         # AAB
```
CI: `.github/workflows/ci.yml` (tests on main/feat/** + PRs), `release.yml` (tag `v*` → build/release, generates keystore).

## Conventions
- Toolchain: AGP 8.11.1, Kotlin 2.2.20, compileSdk/targetSdk 36, minSdk 24, JVM 17. Avoid API > 24 without version check (e.g. use `Calendar`, not `java.time`).
- State in ViewModel `StateFlow`; screens stateless-ish, call ViewModel methods. One-shot WebView commands via `Channel`, not `SharedFlow`.
- Pure logic in `companion object`/`object` funcs for JVM unit tests (`hasBingAuthCookie`, `computeWaitMs`, validators, streak math).
- Keep 1-arg `SearchViewModel(Application)` ctor (reflection factory; tested) + ProGuard rules.
- Search loop serial by design: one shared WebView.

## Testing
JUnit4 only, `android/app/src/test/.../RewardsUnitTest.kt`. No Robolectric/mocks — test pure functions. Add test with each new pure function.

## Gotchas
- ⚠️ `app/build.gradle.kts` hardcodes release keystore passwords — don't add more secrets; flag for cleanup if touched.
- Default WebView UA (`; wv`) → mobile search bucket; not Bing app. Automation may violate MS Rewards ToS (README warns).
- README.md = user docs; keep in sync with features.
