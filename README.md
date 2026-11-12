<div align="center">

<img src="android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Bing Rewards Automator logo" height="96" />

# Bing Rewards Automator

Automate your daily Bing searches to collect Microsoft Rewards points.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203%20Expressive-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024--36)-3DDC84?style=flat-square&logo=android&logoColor=white)](#)

[Features](#features) • [Upgrade Compatibility](#upgrade-compatibility) • [Build](#build) • [Project Structure](#project-structure)

</div>

A pure Native Android app built with **Kotlin** and **Jetpack Compose (Material 3 Expressive)** that signs you into your Microsoft account inside an embedded `WebView`, then runs configurable Bing searches to earn Microsoft Rewards points hands-free.

> [!WARNING]
> Automating searches may violate [Microsoft Rewards terms](https://www.microsoft.com/servicesagreement). Use responsibly and at your own risk.

## Features

- **Automated Bing searches** - configurable count (`1..50`), preset count chips (`10, 20, 30, 50`), and randomized delay between queries with live countdown and cancel support
- **Material 3 Expressive UI** - spatial spring physics (`dampingRatio = 0.6f, stiffness = 500f`), dual shape-morphing buttons & chips (pill $\leftrightarrow$ squircle), animated sinusoidal `WavyLinearProgressIndicator`, and floating pill bottom navigation dock
- **Vector Adaptive & Material You Monochrome Icon** - vector adaptive launcher icon (`drawable/ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `roundIcon`) with themed Material You monochrome dynamic tinting on Android 13+ (`drawable/ic_launcher_monochrome.xml`)
- **In-App Self-Updater** - checks GitHub Releases for new updates (`utils/UpdateChecker.kt`), auto-checks on launch with manual check option in ConfigScreen, ABI-aware APK download, and seamless install via `FileProvider`
- **Android 16 Live Updates (Promoted Ongoing Notifications)** - `POST_PROMOTED_NOTIFICATIONS`, `Notification.ProgressStyle`, status-bar chip `shortCriticalText` (`x/y`), `Stop` action button, and swipe-dismiss guard when running in background
- **Material You dynamic color** - dynamic wallpaper palette on Android 12+ (`API 31+`) with Microsoft Blue (`#0061B0`) seed fallback
- **Pure-black AMOLED theme & simulated screen off** - `AMOLED` mode blacks out all surfaces (`#000000`) and optionally covers the display in sticky immersive black during active searches (1.5s hold-to-wake ring or double-back to wake)
- **Daily streak tracking & haptics** - tracks current and best daily completion streaks with celebratory completion haptics
- **Data Saver & Keep Screen On** - blocks WebView network images/media for lighter searches and keeps screen awake during active runs
- **Daily reminders** - `WorkManager` & `AlarmManager` smart daily notifications surviving device reboots (`BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`)

## Upgrade Compatibility

Upgrading in-place from the previous Flutter release (`v2.0.0` and earlier) works seamlessly without uninstalling:
- **Package ID & Signing Key**: Uses `applicationId = "com.znarfm.bing"` (namespace `com.znarfm.bing`) and the release `upload-keystore.jks` (`alias = "upload"`) certificate.
- **Preserved Settings & Streaks**: `PreferencesRepository` reads and writes `FlutterSharedPreferences` with `flutter.*` keys and `Long`-encoded integers, preserving existing login state, streaks, theme, and reminder settings automatically.

## Build

```bash
cd android
./gradlew testReleaseUnitTest   # Run JUnit unit tests
./gradlew assembleRelease       # Build Universal + ABI-split Release APKs
./gradlew bundleRelease         # Build Release App Bundle (AAB)
```

## Project Structure

```
android/app/src/main/kotlin/com/znarfm/bing/
├── MainActivity.kt                  # Edge-to-edge Compose host, shortcuts, system bars, notification receiver
├── data/
│   ├── PreferencesRepository.kt     # FlutterSharedPreferences-compatible storage + streak calculation
│   └── WordGenerator.kt             # Template-based natural search queries (distinct per run)
├── notifications/
│   ├── NotificationHelper.kt        # Android 16 Live Updates (ProgressStyle + chip) & AlarmManager scheduling
│   ├── NotificationReceivers.kt     # DailyReminderWorker & BootReceiver
│   └── SearchForegroundService.kt   # DataSync foreground service & partial wakelock during active search runs
├── ui/
│   ├── SearchViewModel.kt           # StateFlow state, search coroutine loop, validation, haptics
│   ├── components/
│   │   └── ExpressiveComponents.kt  # ExpressiveButton, ExpressiveChoiceChip, WavyLinearProgressIndicator, FloatingNavBar
│   ├── screens/
│   │   ├── AmoledOverlay.kt         # Pure-black hold-to-wake (1.5s) & double-back overlay
│   │   ├── ConfigScreen.kt          # Theme selector, AMOLED toggle, streaks, data saver, reminders, update check
│   │   ├── LoginScreen.kt           # Microsoft account sign-in flow & skip dialog
│   │   └── SearchScreen.kt          # Search controls, preset chips, progress card, persistent Bing WebView
│   └── theme/
│       └── Theme.kt                 # Material 3 Expressive dynamic color + pure-black AMOLED theme
└── utils/
    ├── HapticsHelper.kt             # Completion and interaction haptics
    ├── InputValidators.kt           # Validation for search count and delay inputs
    └── UpdateChecker.kt             # GitHub Releases update check, ABI-aware download & FileProvider installer
```
