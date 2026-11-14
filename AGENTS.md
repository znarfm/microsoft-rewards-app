# Agent Instructions
## Communication (Caveman — chat responses only; keep docs/comments standard English)
Respond like smart caveman. Cut filler, keep technical substance.
- Drop articles (`a`, `an`, `the`), filler (`just`, `really`, `basically`, `actually`), pleasantries, hedging.
- Fragments fine. Short synonyms. Technical terms exact. Code blocks unchanged.
- Pattern: `[thing] [action] [reason]. [next step].`

# Repository Guidelines
## Project Overview
Native Android app (Kotlin + Compose M3 Expressive), `Bing Rewards Automator` (`com.znarfm.bing`). Embedded MS `WebView` login + configurable Bing searches (count + delay) for Rewards points. In-place upgrade from Flutter — keep compat (see Persistence).

## Architecture & Data Flow
Root: `android/app/src/main/kotlin/com/znarfm/bing/`. Single Activity, single ViewModel, no DI.
- `MainActivity` — Compose host (`LoginScreen` if `!isLoggedIn` else `SearchScreen`); handles start-search shortcut, keep-screen-on, immersive AMOLED, `onStart/onStop` → `viewModel.onAppForegroundChanged`. Manifest `configChanges` keeps Activity + `WebView` alive across rotation/uiMode.
- `ui/screens/LoginScreen` — MS login `WebView`; user taps **Continue** → `setLoggedIn(true)` (no auto URL detection; MS redirects flicker).
- `ui/screens/SearchScreen` — controls, progress card, persistent Bing `WebView` (`remember`). Collects `viewModel.webViewUrlCommands` → `loadUrl`. `WebViewClient` reports main-frame results via `viewModel.onPageResult(url, success, error)` (`onReceivedError`, `onReceivedHttpError >= 400`, `onPageFinished`).
- `ui/SearchViewModel` — `StateFlow<AppUiState>`, `SearchState` (`Idle`/`InProgress`/`Success`/`Cancelled`/`Failure`). `startSearch`:
  1. Validate count/delay; require Bing `_U` cookie (`hasBingAuthCookie`) else `Failure`.
  2. `WordGenerator.distinctQueries(count)`.
  3. Per query: send `bing.com/search?q=` via `Channel` (never dropped), await `PageLoadResult` (`bing.com/search` URLs only) ≤20s, 1 retry, else `Failure` (no streak).
  4. Wait `computeWaitMs(delay)` (±30%, min window 1s, floor 1s) with 1s countdown + notification update.
  5. Complete → `prefs.recordSearchCompletion()` streak, `Success`.
  Cancel = coroutine cancel (`cancelSearch`, notification Stop action).
- `data/WordGenerator` — template natural queries (`randomSentence`, `distinctQueries`); no brands/people/NSFW.
- `data/PreferencesRepository` — prefs + pure streak math (`computeStreakAfterCompletion`).
- `notifications/` — `NotificationHelper` (Live Update progress, daily reminder), `SearchForegroundService` (`dataSync` FGS + partial wakelock), `NotificationReceivers.kt` (`DailyReminderWorker` WorkManager, receivers).

## Persistence
`SharedPreferences` file `FlutterSharedPreferences`, keys `flutter.*`, ints `Long`-encoded — ⚠️ NEVER rename (upgrade compat, asserted in tests). Defaults: count `"22"`, delay `"15"`, daily reminder off @19:00, keep screen on, login popup on, AMOLED overlay off, data saver on, haptics on, theme `system`.

## Commands
No `gradlew` script committed (wrapper props only). Run system `gradle` from `android/`:
```bash
gradle testReleaseUnitTest   # unit tests (CI gate)
gradle assembleRelease       # APKs
gradle bundleRelease         # AAB
```
CI: `.github/workflows/ci.yml` (tests on `main`/`feat/**` + PRs), `release.yml` (tag `v*` → build/release, generates keystore).

## Conventions
- `minSdk 24`, JVM 17 — avoid API > 24 without version check (`Calendar`, not `java.time`).
- State in ViewModel `StateFlow`; screens stateless, call ViewModel methods. One-shot WebView commands via `Channel`, not `SharedFlow`.
- Pure logic in `companion object`/`object` funcs for JVM tests (`hasBingAuthCookie`, `computeWaitMs`, validators, streak math).
- Keep 1-arg `SearchViewModel(Application)` ctor (reflection factory; tested) + ProGuard rules.
- Serial search loop by design (one shared `WebView`).

## Testing
JUnit4 only (`android/app/src/test/.../RewardsUnitTest.kt`). No Robolectric/mocks — test pure functions. Add unit test per new pure function.

## Gotchas
- ⚠️ `app/build.gradle.kts` hardcodes release keystore passwords — NEVER commit new secrets; do not touch keystore config unless asked.
- Default WebView UA (`; wv`) → mobile search bucket (not Bing app). Automation may violate MS Rewards ToS (`README.md` warns).
- Keep `README.md` synced with user-facing features.
