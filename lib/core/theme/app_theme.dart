import 'package:flutter/material.dart';

import '../constants/app_constants.dart';
import 'theme_controller.dart';

/// Builds the app's Material 3 themes.
///
/// Schemes come from the device's wallpaper colors on Android 12+ (supplied
/// by `DynamicColorBuilder` in `app.dart`); plain seeds are the fallback.
/// AMOLED reuses the chosen dark scheme but forces every surface to pure
/// black so OLED pixels truly switch off.
abstract class AppTheme {
  static const Color _amoledBlack = Color(0xFF000000);

  static ThemeData light(ColorScheme? dynamicScheme) {
    final scheme =
        dynamicScheme ?? ColorScheme.fromSeed(seedColor: AppConstants.primary);
    return _build(scheme);
  }

  static ThemeData dark(ColorScheme? dynamicScheme, {bool amoled = false}) {
    final base = dynamicScheme ??
        ColorScheme.fromSeed(
          seedColor: AppConstants.primary,
          brightness: Brightness.dark,
        );
    final scheme = amoled ? _blackOut(base) : base;
    return _build(scheme, scaffoldBackground: amoled ? _amoledBlack : null);
  }

  static ThemeData forMode(
    AppThemeMode mode,
    ColorScheme? lightDynamic,
    ColorScheme? darkDynamic,
  ) {
    switch (mode) {
      case AppThemeMode.light:
        return light(lightDynamic);
      case AppThemeMode.amoled:
        return dark(darkDynamic, amoled: true);
      case AppThemeMode.dark:
      case AppThemeMode.system:
        return dark(darkDynamic);
    }
  }

  static ColorScheme _blackOut(ColorScheme scheme) {
    return scheme.copyWith(
      surface: _amoledBlack,
      surfaceContainerLowest: _amoledBlack,
      surfaceContainerLow: _amoledBlack,
      surfaceContainer: _amoledBlack,
      surfaceContainerHigh: _amoledBlack,
      surfaceContainerHighest: _amoledBlack,
      surfaceTint: _amoledBlack,
    );
  }

  static ThemeData _build(ColorScheme scheme, {Color? scaffoldBackground}) {
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: scaffoldBackground ?? scheme.surface,
      appBarTheme: AppBarTheme(
        backgroundColor: scheme.primary,
        foregroundColor: scheme.onPrimary,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: scheme.primary,
          foregroundColor: scheme.onPrimary,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(foregroundColor: scheme.primary),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: scheme.primary,
        contentTextStyle: const TextStyle(color: Colors.white),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        behavior: SnackBarBehavior.floating,
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: scheme.primary,
        linearTrackColor: scheme.surfaceContainerHighest,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: scheme.surfaceContainer,
        indicatorColor: scheme.secondaryContainer,
      ),
      checkboxTheme: CheckboxThemeData(
        fillColor: WidgetStateProperty.resolveWith(
          (states) => states.contains(WidgetState.selected)
              ? scheme.primary
              : null,
        ),
      ),
    );
  }
}