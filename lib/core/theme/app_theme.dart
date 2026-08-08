import 'package:dynamic_color/dynamic_color.dart';
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
    final scheme = (dynamicScheme ??
            ColorScheme.fromSeed(seedColor: AppConstants.primary))
        .harmonized();
    return _build(scheme);
  }

  static ThemeData dark(ColorScheme? dynamicScheme, {bool amoled = false}) {
    final base = (dynamicScheme ??
            ColorScheme.fromSeed(
              seedColor: AppConstants.primary,
              brightness: Brightness.dark,
            ))
        .harmonized();
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
        backgroundColor: scheme.primaryContainer,
        foregroundColor: scheme.onPrimaryContainer,
        elevation: 0,
        centerTitle: false,
      ),
      cardTheme: CardThemeData(
        color: scheme.surfaceContainerLow,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: BorderSide(
            color: scheme.outlineVariant.withAlpha(120),
            width: 1,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: scheme.surfaceContainerHighest.withAlpha(120),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(
            color: scheme.outlineVariant.withAlpha(180),
            width: 1,
          ),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(
            color: scheme.outlineVariant.withAlpha(180),
            width: 1,
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(
            color: scheme.primary,
            width: 2,
          ),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(
            color: scheme.error,
            width: 1.5,
          ),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: scheme.primary,
          foregroundColor: scheme.onPrimary,
          elevation: 1,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
          ),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          textStyle: const TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.2,
          ),
        ),
      ),
      chipTheme: ChipThemeData(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
        ),
        side: BorderSide.none,
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: scheme.primary,
          textStyle: const TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: scheme.surfaceContainerHigh,
        elevation: 3,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(28),
        ),
      ),
      snackBarTheme: SnackBarThemeData(
        backgroundColor: scheme.inverseSurface,
        contentTextStyle: TextStyle(color: scheme.onInverseSurface),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        behavior: SnackBarBehavior.floating,
      ),
      progressIndicatorTheme: ProgressIndicatorThemeData(
        color: scheme.primary,
        linearTrackColor: scheme.surfaceContainerHighest,
      ),
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: scheme.surfaceContainer,
        indicatorColor: scheme.secondaryContainer,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
      ),
      checkboxTheme: CheckboxThemeData(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(5)),
        fillColor: WidgetStateProperty.resolveWith(
          (states) => states.contains(WidgetState.selected)
              ? scheme.primary
              : null,
        ),
      ),
      switchTheme: SwitchThemeData(
        thumbIcon: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return const Icon(Icons.check, size: 14);
          }
          return null;
        }),
      ),
    );
  }
}