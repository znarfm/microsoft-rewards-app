<div align="center">

<img src="assets/images/auto_search.png" alt="Auto Search logo" height="96" />

# Microsoft Rewards Automator

Automate your daily Bing searches to collect Microsoft Rewards points.

[![Flutter](https://img.shields.io/badge/Flutter-3.x-02569B?style=flat-square&logo=flutter&logoColor=white)](https://flutter.dev)
[![Dart](https://img.shields.io/badge/Dart-%3E%3D3.7-0175C2?style=flat-square&logo=dart&logoColor=white)](https://dart.dev)
[![Platform](https://img.shields.io/badge/Platform-Android%20/%20iOS%20/%20Desktop-blue?style=flat-square)](#)

[Features](#features) • [Getting started](#getting-started) • [Usage](#usage) • [Configuration](#configuration) • [Build](#build) • [Project structure](#project-structure) • [Troubleshooting](#troubleshooting)

</div>

An Android-first Flutter app that signs you into your Microsoft account inside an embedded web view, then runs configurable Bing searches to earn Microsoft Rewards points hands-free.

> [!WARNING]
> Automating searches may violate [Microsoft Rewards terms](https://www.microsoft.com/servicesagreement). Use responsibly and at your own risk.

## Features

- **Automated Bing searches** - configurable count and delay between queries, with live progress and cancel support
- **Microsoft sign-in** - login through the official Microsoft account flow in an embedded web view, session persisted across restarts
- **Material You theming** - dynamic wallpaper colors on Android 12+, with a classic seed-color fallback on older devices
- **AMOLED dark mode** - a pure-black theme for OLED displays, where off pixels save battery
- **Simulated screen off** - cover the screen with pure black while a search runs, so the display looks off on OLED panels; tap or back to wake it
- **Daily reminders** - timezone-aware local notification at a time you pick, per day
- **Keep screen on** - optional wake lock during searches so the device does not sleep mid-run
- **Settings persisted** - theme, reminders, and search preferences survive restarts

## Getting started

### Prerequisites

- [Flutter SDK](https://docs.flutter.dev/get-started/install) (stable channel) with Dart `>=3.7.0`
- A device or emulator (Android recommended for Material You and AMOLED features)

### Run the app

```bash
# Install dependencies
flutter pub get

# Run on a connected device or emulator
flutter run
```

The app opens to a login screen. Sign in with your Microsoft account (`login.live.com`) and you are taken to the search screen.

## Usage

1. Log in with your Microsoft Rewards account through the embedded sign-in page.
2. Set the number of searches (1–50) and the delay between searches.
3. Press **Start Search** — Bing queries open in the embedded web view, with a progress bar and current count.
4. Press **Cancel** or the back button any time to stop mid-run.

> [!TIP]
> Use lower count and longer delay values to behave more like a human and reduce point-flagging risk.

## Configuration

All settings live in the **Config** tab (bottom-right of the search screen).

| Setting | Options | Description |
| --- | --- | --- |
| Theme | System / Light / Dark / AMOLED | `AMOLED` turns every surface pure black (`#000000`) for OLED screens. `System` follows the device's dark/light preference |
| Simulate screen off | On/Off | While on, starts a search‑time pure-black overlay that hides the UI — looks like a powered-off display on OLED; tap or press back to wake it for the current run (the toggle stays on, so the next search dims again) |
| Daily reminder | On/Off + time | Schedules a daily local notification at the chosen time, timezone-aware |
| Keep screen on | On (default) | Acquires a wake lock while searching so the screen stays awake |
| Account | Log in / Log out | Signs you in or clears accounts and cookies (cookies and local storage are wiped on log out) |

Theme and overlay choices are persisted via `SharedPreferences` under `theme_mode` and `amoled_screen_off` keys (defaults: `system` / `false`).

## Build

```bash
flutter build apk --release     # Android release APK
flutter build ios               # iOS (requires macOS + Xcode)
```

Icons are generated from `assets/images/auto_search.png` via `flutter_launcher_icons`:

```bash
dart run flutter_launcher_icons
```

## Project structure

The codebase follows a feature-first clean-architecture layout (layers per feature: `data`, `domain`, `presentation`):

```
lib/
├── main.dart                  # Bootstrap: DI → notifications → Bloc observer → runApp
├── app.dart                   # MaterialApp, Material You wiring, theme mode
├── core/
│   ├── di/                    # GetIt container + search cancellation token
│   ├── theme/                 # AppTheme (light/dark/AMOLED) + ThemeController
│   ├── constants/             # App constants, string copy
│   ├── utils/                 # Error mapping, search helper, validators
│   └── widgets/               # Reusable CustomButton, CustomTextField, ...
├── features/
│   └── search/
│       ├── data/              # Random query generation
│       ├── domain/            # SearchRepository contract, impl, PerformSearch use case
│       └── presentation/      # Bloc (events/states) + pages (login, search, config)
└── notifications/             # Local notifications + timezone setup
```

State management uses `flutter_bloc` (search flow: `SearchInProgress` → `SearchSuccess` / `SearchFailure`, cooperative cancellation via token) and `get_it` for dependency injection.

## Troubleshooting

- **Search does not start** - make sure you are logged in on the search screen (`Config` tab → account row) and that the web view loaded Bing before pressing Start.
- **No points awarded after a run** - Bing may batch or delay rewards, or duplicate queries may not count. Increase delay and vary counts.
- **Login stuck** - log out from the Config tab (clears cookies), then sign in again.
- **Dynamic colors missing** - Material You only works on Android 12+; older devices fall back to the branded seed color.