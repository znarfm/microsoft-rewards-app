import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:microsoft_automatic_rewards/features/search/presentation/pages/startup_screen.dart';
import 'core/constants/strings.dart';
import 'core/di/injection_container.dart';
import 'core/theme/app_theme.dart';
import 'core/theme/theme_controller.dart';
import 'features/search/presentation/bloc/search_bloc.dart';

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  late final ThemeController _themeController;
  late AppThemeMode _currentMode;

  @override
  void initState() {
    super.initState();
    _themeController = sl<ThemeController>();
    _currentMode = _themeController.mode;
    _themeController.addListener(_onThemeChanged);
  }

  @override
  void dispose() {
    _themeController.removeListener(_onThemeChanged);
    super.dispose();
  }

  void _onThemeChanged() {
    if (_currentMode != _themeController.mode) {
      setState(() => _currentMode = _themeController.mode);
    }
  }

  @override
  Widget build(BuildContext context) {
    final themeMode = switch (_currentMode) {
      AppThemeMode.light => ThemeMode.light,
      AppThemeMode.dark || AppThemeMode.amoled => ThemeMode.dark,
      AppThemeMode.system => ThemeMode.system,
    };

    return BlocProvider<SearchBloc>.value(
      value: sl<SearchBloc>(),
      child: DynamicColorBuilder(
        builder: (lightDynamic, darkDynamic) {
          final amoled = _currentMode == AppThemeMode.amoled;
          return MaterialApp(
            title: Strings.appTitle,
            theme: AppTheme.light(lightDynamic),
            darkTheme: AppTheme.dark(darkDynamic, amoled: amoled),
            themeMode: themeMode,
            debugShowCheckedModeBanner: false,
            home: const StartupScreen(),
          );
        },
      ),
    );
  }
}