import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/theme/theme_controller.dart';
import '../bloc/search_bloc.dart';
import '../widgets/amoled_overlay.dart';
import '../widgets/login_reminder_dialog.dart';
import '../widgets/search_form.dart';
import '../widgets/search_tab.dart';
import 'config_screen.dart';

/// Main screen after login. Two tabs: Search (search form + WebView +
/// progress) and Config (theme, toggles, account).
class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  int _tabIndex = 0;
  // Session-only: dismissing the overlay (hold/back) must NOT flip the
  // persisted toggle — it comes back on the next search.
  bool _overlayDismissed = false;
  bool _systemUiHidden = false;
  DateTime? _lastBackPressTime;
  final GlobalKey<SearchFormState> _searchFormKey = GlobalKey();
  static const _shortcutChannel =
      MethodChannel('com.spin311.microsoft_automatic_rewards/shortcuts');

  @override
  void initState() {
    super.initState();
    _initShortcutHandling();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      showLoginReminderDialog(context);
    });
  }

  void _initShortcutHandling() {
    _shortcutChannel.setMethodCallHandler((call) async {
      if (call.method == 'onShortcutTriggered' &&
          call.arguments == 'start_search') {
        _triggerShortcutSearch();
      }
    });

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      try {
        final initial =
            await _shortcutChannel.invokeMethod<String>('getInitialShortcut');
        if (initial == 'start_search') {
          _triggerShortcutSearch();
        }
      } catch (e) {
        debugPrint('Error getting initial shortcut: $e');
      }
    });
  }

  void _triggerShortcutSearch() {
    if (!mounted) return;
    if (_tabIndex != 0) {
      setState(() => _tabIndex = 0);
    }
    Future.delayed(const Duration(milliseconds: 300), () {
      if (mounted) {
        _searchFormKey.currentState?.triggerSearchAction();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return BlocListener<SearchBloc, SearchState>(
      listener: (context, state) {
        // A brand-new search (0/0 progress) re-arms the overlay.
        if (state is SearchInProgress &&
            state.currentCount == 0 &&
            state.totalCount == 0 &&
            _overlayDismissed) {
          setState(() => _overlayDismissed = false);
        }
        final themeCtrl = sl<ThemeController>();
        final overlayOn = themeCtrl.mode == AppThemeMode.amoled &&
            themeCtrl.amoledOverlay &&
            !_overlayDismissed;
        final showOverlay = state is SearchInProgress && overlayOn;
        _applySystemUi(showOverlay);
      },
      child: BlocBuilder<SearchBloc, SearchState>(
        builder: (context, state) {
          final themeCtrl = sl<ThemeController>();
          final overlayOn = themeCtrl.mode == AppThemeMode.amoled &&
              themeCtrl.amoledOverlay &&
              !_overlayDismissed;
          final showOverlay = state is SearchInProgress && overlayOn;

          final contentStack = Stack(
            children: [
              Scaffold(
                appBar: AppBar(
                  title: const Text(Strings.appTitle),
                ),
                body: IndexedStack(
                  index: _tabIndex,
                  children: [
                    SearchTab(formKey: _searchFormKey),
                    const ConfigScreen(),
                  ],
                ),
                bottomNavigationBar: NavigationBar(
                  selectedIndex: _tabIndex,
                  onDestinationSelected: (index) {
                    if (index == 0 && _tabIndex == 0) {
                      _searchFormKey.currentState?.triggerSearchAction();
                    } else {
                      setState(() => _tabIndex = index);
                    }
                  },
                  destinations: const [
                    NavigationDestination(
                      icon: Icon(Icons.search),
                      label: Strings.searchTab,
                    ),
                    NavigationDestination(
                      icon: Icon(Icons.settings),
                      label: Strings.configTab,
                    ),
                  ],
                ),
              ),

              // Pure-black AMOLED overlay with 1.5s hold-to-wake gesture.
              if (showOverlay)
                Positioned.fill(
                  child: AmoledOverlay(onWake: _hideOverlay),
                ),
            ],
          );

          return PopScope(
            canPop: false,
            onPopInvokedWithResult: (didPop, result) async {
              if (didPop) return;
              if (showOverlay) {
                final now = DateTime.now();
                if (_lastBackPressTime != null &&
                    now.difference(_lastBackPressTime!) <
                        const Duration(seconds: 2)) {
                  _hideOverlay();
                } else {
                  _lastBackPressTime = now;
                  if (!context.mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content:
                          const Text('Press back again to exit AMOLED mode'),
                      duration: const Duration(seconds: 2),
                      behavior: SnackBarBehavior.floating,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8.0),
                      ),
                    ),
                  );
                }
                return;
              }

              if (_tabIndex == 0) {
                final handled =
                    await _searchFormKey.currentState?.handleBackPress() ??
                        false;
                if (handled) return;
              }

              if (!context.mounted) return;
              final navigator = Navigator.of(context);
              if (navigator.canPop()) {
                navigator.pop();
              } else {
                SystemNavigator.pop();
              }
            },
            child: contentStack,
          );
        },
      ),
    );
  }

  void _hideOverlay() {
    setState(() => _overlayDismissed = true);
    _applySystemUi(false);
  }

  void _applySystemUi(bool hide) {
    if (hide == _systemUiHidden) return;
    _systemUiHidden = hide;

    if (hide) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
    } else {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    }
  }

  @override
  void dispose() {
    if (_systemUiHidden) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    }
    super.dispose();
  }
}