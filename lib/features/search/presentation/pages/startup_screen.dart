import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
import '../bloc/search_bloc.dart';
import 'login_screen.dart';
import 'search_screen.dart';

/// Startup gate: routes straight to the search screen when the user
/// previously tapped Continue after signing in, otherwise shows the login
/// screen. No session probing — the user re-checks Bing sign-in in the
/// search tab's browser themselves.
class StartupScreen extends StatefulWidget {
  const StartupScreen({super.key});

  @override
  State<StartupScreen> createState() => _StartupScreenState();
}

class _StartupScreenState extends State<StartupScreen> {
  bool? _loggedIn;

  @override
  void initState() {
    super.initState();
    _checkLoginStatus();
  }

  void _checkLoginStatus() {
    setState(() => _loggedIn = sl<PreferencesService>().loggedIn);
  }

  @override
  Widget build(BuildContext context) {
    if (_loggedIn == null) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return BlocProvider<SearchBloc>(
      create: (_) => sl<SearchBloc>(),
      child: _loggedIn! ? const SearchScreen() : const LoginScreen(),
    );
  }
}