import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:microsoft_automatic_rewards/features/search/presentation/pages/startup_screen.dart';
import 'core/constants/strings.dart';
import 'core/di/injection_container.dart';
import 'core/theme/app_theme.dart';
import 'core/theme/theme_controller.dart';
import 'features/search/presentation/bloc/search_bloc.dart';

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    final themeController = sl<ThemeController>();

    return ListenableBuilder(
      listenable: themeController,
      builder: (context, _) {
        final AppThemeMode mode = themeController.mode;
        final themeMode = switch (mode) {
          AppThemeMode.light => ThemeMode.light,
          AppThemeMode.dark || AppThemeMode.amoled => ThemeMode.dark,
          AppThemeMode.system => ThemeMode.system,
        };

        // Android 12+ supplies both schemes from the wallpaper; below 12 or
        // on other platforms they are null and AppTheme falls back to the
        // seed color.
        return DynamicColorBuilder(
          builder: (lightDynamic, darkDynamic) {
            final amoled = mode == AppThemeMode.amoled;
            return MaterialApp(
              title: Strings.appTitle,
              theme: AppTheme.light(lightDynamic),
              darkTheme: AppTheme.dark(darkDynamic, amoled: amoled),
              themeMode: themeMode,
              debugShowCheckedModeBanner: false,
              home: BlocProvider<SearchBloc>(
                create: (context) => sl<SearchBloc>(),
                child: const StartupScreen(),
              ),
            );
          },
        );
      },
    );
  }
}