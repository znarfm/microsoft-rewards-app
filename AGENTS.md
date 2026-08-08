# Token efficiency
Respond like smart caveman. Cut all filler, keep technical substance.
- Drop articles (a, an, the), filler (just, really, basically, actually).
- Drop pleasantries (sure, certainly, happy to).
- No hedging. Fragments fine. Short synonyms.
- Technical terms stay exact. Code blocks unchanged.
- Pattern: [thing] [action] [reason]. [next step].

# Repository Guidelines

## Project Overview
Mobile/desktop/web Flutter app `microsoft_automatic_rewards` (v1.2.0+14) automates Microsoft Rewards: login to Microsoft account in embedded `InAppWebView`, run configurable Bing searches (count + delay) to earn points. Optional daily reminder (timezone-aware) + keep-screen-on.

## Architecture & Data Flow
Feature-first, clean-architecture shape. Feature code in `lib/features/`, shared in `lib/core/`.

- `main.dart` — entrypoint. Order matters: `await init()` (GetIt DI) → `await NotificationService.init()` → set `Bloc.observer` (`_AppBlocObserver`) → `runApp(const MyApp())`.
- `app.dart` — `MyApp`: `DynamicColorBuilder` (wallpaper dynamic scheme Android 12+, seed `AppConstants.primary` fallback) → `AppTheme` light/dark/amoled themes → `BlocProvider`-wrapped `StartupScreen`. Mode from `ThemeController` via `ListenableBuilder`.
- `StartupScreen` reads `loggedIn` (`SharedPreferences`) → `SearchScreen` or `LoginScreen`. No session probing; the search tab shows a hint to verify Bing sign-in in its browser.
- `LoginScreen` — Microsoft sign-in in WebView; the user taps **Continue** once signed in (auto URL detection is unreliable: MS redirects flicker error URLs even on success) → write `loggedIn = true`, go to search.
- `SearchScreen` — scaffold w/ bottom `NavigationBar`, 2 tabs: Search (`_SearchTab` + `SearchForm` — count/delay, WebView, progress) + Config (`ConfigScreen` — theme, toggles, account). Dispatches `StartSearchEvent` / `CancelSearchEvent` to `SearchBloc`.
- `SearchBloc` (`lib/features/search/presentation/bloc/search_bloc.dart`) → `PerformSearch` use case → `SearchRepositoryImpl`.
- `SearchRepositoryImpl.performSearches` — loop `count` iterations: cancel check (`SearchCancellationToken.isCancelled`), `onProgress(i, count)`, query from `randomSentence()`, `SearchHelper.launchSearch(controller, query)` in WebView, random delay `delay ± 0..1000ms` between. Errors rethrown `Exception('Search failed: …')`.
- `SearchHelper` (`lib/core/utils/helpers/search_helper.dart`) — static `launchSearch` opens Bing query in given `InAppWebViewController`.
- AMOLED overlay: pure black `ColoredBox` over search tab while `SearchInProgress` + `ThemeController.amoledOverlay`. Tap or back dismisses. Search keeps running under it.

Per feature: `data/dataSources`, `domain/repositories` (+ `_impl`), `domain/useCases`, `presentation/bloc`, `presentation/pages`.

Persistence: `shared_preferences` — keys in Important Files.

## Key Directories
- `lib/core/` — shared, feature-agnostic: `constants/` (app constants, strings, string ext), `di/` (GetIt container + `SearchCancellationToken`, file `search_cancellation_token.dart`), `theme/` (`AppTheme` themes, `ThemeController` ChangeNotifier), `utils/` (error mapping, helpers, validators), `widgets/` (`CustomButton`, `CustomTextField`, `LoadingIndicator`).
- `lib/features/search/` — only feature: data/domain/presentation.
- `lib/notifications/` — `NotificationService`: local notifications + timezone.
- `assets/images/` — `auto_search.png` (also launcher icon source).
- `android/ ios/ linux/ macos/ windows/ web/` — all six platform shells present.

## Development Commands
```bash
flutter pub get          # install deps
flutter run              # run on default device (-d chrome / -d linux / etc.)
flutter analyze          # static analysis (flutter_lints)
flutter test             # run tests
flutter build apk        # Android release
dart run flutter_launcher_icons  # regenerate launcher icons from pubspec config
```
No scripts, CI, Makefile, justfile. README.md = real project docs, keep in sync with features.

## Code Conventions & Common Patterns
- **Naming**: snake_case files, PascalCase classes/events/states, camelCase members, `I`-free repository style (`SearchRepository` interface, `SearchRepositoryImpl`). Types registered against abstract type.
- **State management**: `flutter_bloc`. Events/states = `XxxEvent`/`XxxState` extending abstract base with `EquatableMixin`, override `props`. New event → new `on<Event>` handler in `Bloc` constructor.
- **Theming**: `ThemeController` (ChangeNotifier, `sl`-singleton) holds `AppThemeMode` (system/light/dark/amoled) + `amoledOverlay`; persisted. `app.dart` listens via `ListenableBuilder`. New themes → edit `AppTheme` (`lib/core/theme/app_theme.dart`). Radio groups: use `RadioGroup` + `RadioListTile` (no `groupValue`/`onChanged` on tile).
- **Use cases**: one `call({ … })`, validate early (`throw ArgumentError('Count must be positive')`), delegate to repo. Name mirrors action: `PerformSearch`.
- **Dependency injection**: GetIt. Add to `init()` in `lib/core/di/injection_container.dart`. `registerLazySingleton` for repos, data sources, use cases, services; `registerFactory` for BLoCs. Access via global `sl`. Wire: `BlocProvider(create: (_) => sl<SearchBloc>())`. `ThemeController` created + `await load()` in `init()` before registrations.
- **Cancellation**: cooperative — `SearchCancellationToken` (in `lib/core/di/`), checked per loop iteration; set by `CancelSearchEvent`. Keep cancel checks inside long loops.
- **Error handling**: `ErrorHandler.getErrorMessage(e)` maps errors to user copy; `ErrorHandler.logError(label, e)` from `BlocObserver.onError` + helpers. Bloc catch → `emit(SearchFailure(message))`. UI reacts per state (progress vs failure vs cancel).
- **Persistence**: read/write `SharedPreferences` directly (no persistence layer); store raw `TextEditingController` strings for count/delay. Defaults: count `'22'`, delay `'20'`, send_daily_reminder `false`, keep_screen_on `true`, reminder 19:00, theme_mode `system`, amoled_screen_off `false`.
- **Notifications**: `NotificationService` static methods: `init()`, `scheduleDailyReminder(hour:, minute:)` (daily; id 0), `sendImmediateNotification(...)`, `cancelReminder()`. Run `init()` in `main()` before use. Timezone from `FlutterTimezone.getLocalTimezone()` + `timezone` package.

## Important Files
- `lib/main.dart` — bootstrap order (DI → notifications → observer → runApp).
- `lib/app.dart` — `MyApp`, dynamic theme wiring, top-level `BlocProvider`.
- `lib/core/di/injection_container.dart` — all registrations + global `sl`.
- `lib/core/theme/theme_controller.dart` — `AppThemeMode`, persisted mode + overlay flag (ChangeNotifier).
- `lib/core/theme/app_theme.dart` — light/dark/amoled `ThemeData` builders, AMOLED black-out.
- `lib/features/search/presentation/pages/search_screen.dart` — tab shell, search tab, overlay.
- `lib/features/search/presentation/pages/config_screen.dart` — theme selector, toggles, account.
- `lib/features/search/presentation/bloc/search_bloc.dart` — events/states, flow, cancellation.
- `lib/features/search/domain/repositories/search_repository_impl.dart` — core search loop.
- `lib/notifications/notification_service.dart` — scheduling/timezone.
- `pubspec.yaml` — deps: `flutter_bloc`, `get_it`, `equatable`, `word_generator`, `shared_preferences`, `flutter_inappwebview` (v6), `flutter_local_notifications`, `permission_handler`, `flutter_timezone`, `timezone`, `wakelock_plus`, `animated_text_kit`, `dynamic_color`, `flutter_launcher_icons`.

## Runtime/Tooling Preferences
- Flutter **stable** (pinned in `.metadata`) + Dart SDK `>=3.7.0 <4.0.0`. Pub = package manager. No Flutter bump without real migration.
- Lints: `package:flutter_lints/flutter.yaml` in `analysis_options.yaml`; no custom rules.
- Android (`android/settings.gradle.kts` + `app/build.gradle.kts`): AGP 8.7.0, Kotlin 1.8.22, JVM target 11, NDK 27.0.12077973; `namespace`/`applicationId` = `com.spin311.microsoft_automatic_rewards` (⚠ differs from iOS bundle id `com.example.microsoftAutomaticRewards` — template origin; don't "fix" silently). ⚠️ Release `signingConfig` hardcodes keystore path + passwords in `app/build.gradle.kts` — don't add similar secrets; flag for cleanup if touched.
- `android/build.gradle.kts` workaround: `namespace com.lyokone.flutter_native_timezone` for deprecated `flutter_native_timezone` module — keep, or migrate `NotificationService` to `flutter_timezone` only.
- Icons via `flutter_launcher_icons` config in pubspec (`assets/images/auto_search.png`, Android+iOS, `remove_alpha_ios: true`). Run `dart run flutter_launcher_icons` after changing source image.

## Testing & QA
- Framework: `flutter_test` (SDK) + `bloc_test` (dev). No mocking package — write fakes/manual doubles (none yet).
- Tests in `test/`.
- ⚠️ `test/widget_test.dart` = **unmodified Flutter template counter test**, broken against this app: pumps `MyApp` without GetIt `init()` (crashes on `sl<SearchBloc>()` + `sl<ThemeController>()`), expects nonexistent counter. Not a reference; suite has no meaningful coverage yet. Replace with: (1) call `init()` in `setUp`/`setUpAll` or mock repository, (2) verify real behavior, e.g. bloc `StartSearchEvent` → `SearchInProgress` → `SearchSuccess`, or mid-loop cancel via fake `SearchCancellationToken`.
- No coverage gates, no CI. `flutter analyze` is gate for lint/format.
- Test search behavior at `SearchRepositoryImpl.performSearches` layer. Loop serial by design — iterations share one WebView controller; parallel search would interleave navigation.
