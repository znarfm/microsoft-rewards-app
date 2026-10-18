# Specification: Reward Search Automator Optimization, Fixes, and Quality of Life

## Problem Statement

Users of Reward Search Automator experience several operational issues:
- Tapping the "Stop" action on the ongoing Android background notification does not cancel searches because the background isolate cannot access the main isolate's `SearchBloc`.
- Navigating between login and search can crash or permanently break search dispatch because the singleton `SearchBloc` is closed when its widget-level `BlocProvider` unmounts.
- Manifest configurations carry orphaned permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`), an invalid shortcut category (`conversation`), and a test library dependency (`android.test.runner`), threatening app store acceptance and device compatibility.
- Battery and CPU are wasted by updating Android notifications every 1 second during delays, generating words via new class allocations on every query, and resizing the native WebView surface during progress bar expansion animations.
- The root `MaterialApp` and entire route tree reload whenever the AMOLED overlay preference is toggled.
- Tapping the bottom navigation bar on an already-active "Search" tab accidentally triggers search start or cancellation.
- Users have no quick count presets (10, 20, 30, 50), no estimated time remaining, no direct way to open the Microsoft Rewards dashboard to check earned points, and no loading indicator or back navigation on the login screen.

## Solution

Optimize the application architecture, resolve background notification isolation and bloc lifecycle bugs, clean Android manifests and shortcuts, eliminate battery/CPU waste, and add core Quality of Life (QoL) improvements:
- Establish a reliable cancellation mechanism between background notification isolate and main search loop.
- Preserve `SearchBloc` singleton lifecycle across route transitions using `BlocProvider.value` or scoped lifetime.
- Clean `AndroidManifest.xml` and `shortcuts.xml` of orphaned permissions, test runner dependencies, and improper categories.
- Throttle background notification progress updates to search step completions ($i/N$), avoiding 1-second IPC wakeups.
- Make word generation allocation-free using static reusable generators.
- Decouple AMOLED overlay settings from root theme rebuilding to avoid unnecessary `MaterialApp` rebuilds.
- Avoid native WebView surface resize thrash during progress transitions.
- Fix bottom navigation tab self-tap behavior.
- Add search count preset chips, estimated remaining time display, and an in-app Rewards Dashboard shortcut.
- Improve login flow with page loading progress and back-gesture handling.

## User Stories

1. As a rewards earner, I want the "Stop" button on the background notification to cancel active searches reliably, so that I can stop automation without opening the app.
2. As a user, I want the search screen to dispatch searches consistently after logging in, so that I do not encounter `Cannot add new events after calling close` state errors.
3. As an Android user, I want the app to install and run without crashes on all Android distributions without failing on missing `android.test.runner` libraries.
4. As an Android user, I want the app to adhere to Google Play policies without declaring orphaned foreground service permissions, so that the app installs cleanly and safely.
5. As a mobile user, I want launcher shortcuts on my home screen to function without incorrect messaging/conversation categorization, so that launcher integration works properly.
6. As a battery-conscious user, I want the app not to send platform notification updates every second during delays, so that my phone's battery and CPU are preserved while searches run.
7. As a user, I want the delay countdown to be mathematically precise, so that searches do not sleep longer than the configured delay duration.
8. As a user, I want search query generation to run with minimal memory allocation, so that the garbage collector does not trigger frame stutters.
9. As a user, I want toggling the AMOLED overlay setting to update smoothly without flickering or rebuilding the entire application and route stack.
10. As a user, I want the browser view to stay smooth when search progress appears, so that the native Android WebView does not drop frames while animating layout size changes.
11. As a user, I want tapping the active "Search" tab on the bottom navigation bar not to start or cancel searches, so that I avoid accidental search triggers.
12. As a user, I want to tap preset chips for common search counts (10, 20, 30, 50), so that I do not have to manually type numbers every time.
13. As a user, I want to see an estimated time remaining (ETA) during active searches, so that I know exactly when the automated batch will finish.
14. As a user, I want an action button to quickly open the Microsoft Rewards dashboard (`rewards.bing.com`) in the browser view, so that I can inspect my points and daily streak without leaving the app.
15. As a user, I want a visual progress bar when loading the Microsoft login page in the webview, so that I know the page is loading and not frozen.
16. As a user, I want pressing the back button inside the login webview to return to the sign-in intro card, so that I do not accidentally exit the app.
17. As a user, I want the reminder time chip to be visually disabled when daily reminders are switched off, so that I understand changing the time has no effect until the toggle is enabled.
18. As a user, I want the screen to release wake-locks promptly whenever searches complete, fail, or cancel, so that my battery is not depleted by a display that never sleeps.
19. As an OLED device user, I want the AMOLED screen-off simulation to run with minimum screen brightness, so that power consumption is reduced to the absolute lowest level possible.

## Implementation Decisions

### 1. Inter-Isolate Background Notification Stop Action
- **Files**: [`lib/notifications/notification_service.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/notifications/notification_service.dart), [`lib/core/services/preferences_service.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/core/services/preferences_service.dart), [`lib/features/search/domain/repositories/search_repository_impl.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/domain/repositories/search_repository_impl.dart)
- **Decision**: In `notificationTapBackground`, write an atomic cancellation flag `search_cancelled_from_notification = true` into `SharedPreferences` (since `SharedPreferences` is thread-safe and accessible in background isolates). In `SearchCancellationToken` / `SearchRepositoryImpl` search loop, check both in-memory cancellation token and this shared preference flag before every search iteration and delay tick. In the main isolate `onDidReceiveNotificationResponse`, continue dispatching `CancelSearchEvent` to `sl<SearchBloc>()`.

### 2. `SearchBloc` Lifecycle and Provider Wiring
- **Files**: [`lib/core/di/injection_container.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/core/di/injection_container.dart), [`lib/app.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/app.dart), [`lib/features/search/presentation/pages/login_screen.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/presentation/pages/login_screen.dart)
- **Decision**: Wire `BlocProvider.value(value: sl<SearchBloc>())` at root `MyApp` so `BlocProvider` does not invoke `close()` when child routes or rebuilds occur. Remove redundant `BlocProvider` wrappers in `LoginScreen.navigateToSearchScreen`—simply navigate to `const SearchScreen()`.

### 3. Android Manifest and Shortcuts Cleanup
- **Files**: [`android/app/src/main/AndroidManifest.xml`](file:///home/nard/Projects/microsoft-rewards-app/android/app/src/main/AndroidManifest.xml), [`android/app/src/main/res/xml/shortcuts.xml`](file:///home/nard/Projects/microsoft-rewards-app/android/app/src/main/res/xml/shortcuts.xml)
- **Decision**:
  - Remove `<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />` and `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />`.
  - Remove `<uses-library android:name="android.test.runner" />`.
  - Remove `<meta-data android:name="com.google.firebase.messaging.default_notification_channel_id" ... />` (Firebase not used).
  - In `shortcuts.xml`, remove `<categories android:name="android.shortcut.conversation" />` and replace with `<categories android:name="android.intent.category.DEFAULT" />`.

### 4. Background Notification Update Throttling
- **Files**: [`lib/features/search/presentation/widgets/search_tab.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/presentation/widgets/search_tab.dart), [`lib/notifications/notification_service.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/notifications/notification_service.dart)
- **Decision**: In `SearchTab._handleStateChanges`, when `!_isAppInFocus`, only call `NotificationService.showSearchProgressNotification` when `currentCount` changes, or when delay countdown reaches 0. Suppress per-second countdown updates to notifications.

### 5. Static Generator & Memory Allocation Optimization
- **Files**: [`lib/features/search/data/dataSources/search_words.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/data/dataSources/search_words.dart)
- **Decision**: Convert `SearchWordsDataSourceImpl` internal fields to `static final Random _random = Random();` and `static final WordGenerator _wordGenerator = WordGenerator();`.

### 6. Decouple AMOLED Overlay Setting from Root Theme
- **Files**: [`lib/core/theme/theme_controller.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/core/theme/theme_controller.dart), [`lib/app.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/app.dart)
- **Decision**: In `app.dart`, bind `DynamicColorBuilder` and `MaterialApp` to a scoped listener or value selector that only rebuilds when `themeController.mode` changes, not on `amoledOverlay` changes.

### 7. Layout Jitter Fix for WebView & Progress
- **Files**: [`lib/features/search/presentation/widgets/search_form.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/presentation/widgets/search_form.dart)
- **Decision**: Reserve fixed height for progress panel or overlay progress on the form to avoid continuous resize triggers on the native Android WebView surface during `AnimatedSize` transitions.

### 8. Bottom Navigation Bar Interaction Fix
- **Files**: [`lib/features/search/presentation/pages/search_screen.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/presentation/pages/search_screen.dart)
- **Decision**: In `NavigationBar.onDestinationSelected`:
```dart
onDestinationSelected: (index) {
  if (index != _tabIndex) {
    setState(() => _tabIndex = index);
  }
}
```
Remove `_searchFormKey.currentState?.triggerSearchAction()` on tab re-selection.

### 9. Presets, ETA, and Rewards Dashboard Shortcut
- **Files**: [`lib/features/search/presentation/widgets/search_form.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/presentation/widgets/search_form.dart), [`lib/features/search/presentation/pages/search_screen.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/presentation/pages/search_screen.dart)
- **Decision**:
  - Add preset chips `[10]`, `[20]`, `[30]`, `[50]` above or next to the search count field that populate `_countController`.
  - Calculate ETA: `remainingSecondsTotal = ((totalCount - currentCount) * (delay + 0.5)).round()`. Render formatted ETA string `~Xm Ys remaining` in `SearchForm` progress section.
  - Add an action in the `SearchScreen` `AppBar` (or quick button) to load `https://rewards.bing.com` in `InAppWebViewController`.

### 10. LoginScreen Progress & Navigation UX
- **Files**: [`lib/features/search/presentation/pages/login_screen.dart`](file:///home/nard/Projects/microsoft-rewards-app/lib/features/search/presentation/pages/login_screen.dart)
- **Decision**:
  - Add `onProgressChanged: (controller, progress)` and render a top `LinearProgressIndicator` when `progress < 100`.
  - Wrap screen in `PopScope`: if `_showWebView` is true, intercept back button to set `setState(() => _showWebView = false)` instead of popping or exiting the application.

## Testing Decisions

### What Makes a Good Test
- Tests must verify external behavior and system boundaries, not internal private fields or widget tree nesting details.
- Tests must run deterministically without real network calls or physical platform timing dependencies.

### Modules Tested
1. **`SearchRepositoryImpl`**:
   - Verify cancellation token check interrupts loop immediately.
   - Verify inter-isolate cancellation flag in `PreferencesService` halts search loop.
   - Verify delay calculation adheres to specified duration without drift.
2. **`SearchBloc`**:
   - Verify state progression `SearchInitial` -> `SearchInProgress` -> `SearchSuccess`.
   - Verify cancellation state transition to `SearchCancelled`.
   - Verify error mapping to `SearchFailure`.
3. **`SearchWordsDataSourceImpl`**:
   - Verify generation produces valid multi-word queries repeatedly without memory leak or exception.
4. **`PreferencesService`**:
   - Verify atomic cancellation flag reading and clearing.
5. **Widget Tests**:
   - `SearchForm`: Verify preset chips update text controller; verify ETA calculation renders.
   - `SearchScreen`: Verify bottom nav tab tap does not start search.
   - `LoginScreen`: Verify back press closes webview mode back to intro card.

### Prior Art
- Unit tests in `test/features/search/domain/repositories/search_repository_impl_test.dart` and `test/features/search/presentation/bloc/search_bloc_test.dart`.
- Widget test in `test/features/search/presentation/widgets/amoled_overlay_test.dart`.

## Out of Scope

- **Desktop Mode / User-Agent Switcher**: Excluded per explicit user direction; Bing searches are now unified to a single rewards bucket.
- **Background Execution Engine (WorkManager / Background Service)**: The app runs active searches in the foreground or while screen is simulated off via AMOLED overlay; full background autonomous scheduling without UI is out of scope.
- **Third-Party Rewards Scraping**: Direct API automation bypassing Bing web interface is out of scope to prevent account bans.

## Further Notes

- Unified search bucket on Microsoft Rewards simplifies the search loop and removes the need for multiple WebView instances or user-agent spoofing.
- Keeping AMOLED simulation screen brightness low ensures true power savings while keeping searches running in foreground context on Android.
