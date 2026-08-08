import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/theme/theme_controller.dart';
import '../bloc/search_bloc.dart';
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
  // Session-only: dismissing the overlay (tap/back) must NOT flip the
  // persisted toggle — it comes back on the next search.
  bool _overlayDismissed = false;
  bool _systemUiHidden = false;
  final GlobalKey<SearchFormState> _searchFormKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      showLoginReminderDialog(context);
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
      },
      child: BlocBuilder<SearchBloc, SearchState>(
        builder: (context, state) {
          final themeCtrl = sl<ThemeController>();
          final overlayOn = themeCtrl.mode == AppThemeMode.amoled &&
              themeCtrl.amoledOverlay &&
              !_overlayDismissed;
          final showOverlay = state is SearchInProgress && overlayOn;
          _applySystemUi(showOverlay);

          return PopScope(
            canPop: false,
            onPopInvokedWithResult: (didPop, result) async {
              if (didPop) return;
              if (showOverlay) {
                _hideOverlay();
              } else if (Navigator.of(context).canPop()) {
                Navigator.of(context).pop();
              } else {
                SystemNavigator.pop();
              }
            },
            child: Stack(
              children: [
                Scaffold(
                  appBar: AppBar(
                    title: Builder(
                      builder: (context) {
                        final isSmallScreen =
                            MediaQuery.of(context).size.width < 365;
                        return isSmallScreen
                            ? const Text(Strings.appTitle)
                            : Row(
                                children: [
                                  Image.asset(
                                    'assets/images/auto_search.png',
                                    height: 32,
                                  ),
                                  const SizedBox(width: 10),
                                  const Text(Strings.appTitle),
                                ],
                              );
                      },
                    ),
                  ),
                  body: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 250),
                    switchInCurve: Curves.easeOutCubic,
                    switchOutCurve: Curves.easeInCubic,
                    transitionBuilder: (child, animation) {
                      return FadeTransition(
                        opacity: animation,
                        child: SlideTransition(
                          position: Tween<Offset>(
                            begin: const Offset(0, 0.02),
                            end: Offset.zero,
                          ).animate(animation),
                          child: child,
                        ),
                      );
                    },
                    child: _tabIndex == 0
                        ? SearchTab(
                            key: const ValueKey(0),
                            formKey: _searchFormKey,
                          )
                        : const ConfigScreen(key: ValueKey(1)),
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

                // Pure-black AMOLED overlay while search runs (if enabled).
                if (showOverlay)
                  Positioned.fill(
                    child: GestureDetector(
                      onTap: _hideOverlay,
                      behavior: HitTestBehavior.opaque,
                      child: const ColoredBox(color: Colors.black),
                    ),
                  ),
              ],
            ),
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