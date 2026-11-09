import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
import '../../../../core/theme/expressive_motion.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../core/widgets/expressive_chip.dart';
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
      if (call.method == 'onShortcutTriggered') {
        if (call.arguments == 'start_search') {
          _triggerShortcutSearch();
        } else if (call.arguments == 'stop_search' && mounted) {
          context.read<SearchBloc>().add(CancelSearchEvent());
        }
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

  Widget _buildSubtitle(BuildContext context, SearchState state) {
    final scheme = Theme.of(context).colorScheme;
    final prefs = sl<PreferencesService>();
    final isDoneToday = prefs.isCompletedToday;

    String text;
    Color color;
    IconData icon;

    if (_tabIndex == 1) {
      text = 'Theme, automation & reminders';
      color = scheme.onSurfaceVariant;
      icon = Icons.tune_rounded;
    } else if (state is SearchInProgress) {
      icon = Icons.bolt_rounded;
      color = scheme.primary;
      if (state.remainingSeconds > 0) {
        text =
            '${state.currentCount}/${state.totalCount} • Next in ${state.remainingSeconds}s';
      } else {
        text = 'Searching (${state.currentCount}/${state.totalCount})…';
      }
    } else if (isDoneToday) {
      text = 'Completed today';
      color = Colors.green;
      icon = Icons.check_circle_rounded;
    } else {
      text = 'Pending today';
      color = scheme.onSurfaceVariant;
      icon = Icons.schedule_rounded;
    }

    return AnimatedSwitcher(
      duration: ExpressiveMotion.defaultEffectsDuration,
      child: Row(
        key: ValueKey(text),
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: color),
          const SizedBox(width: 5),
          Flexible(
            child: Text(
              text,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w600,
                color: color,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStreakBadge(BuildContext context) {
    final prefs = sl<PreferencesService>();
    final streak = prefs.completionStreak;
    final bestStreak = prefs.bestStreak;
    final scheme = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    if (streak <= 0 && bestStreak <= 0) {
      return const SizedBox.shrink();
    }

    final isActiveStreak = streak > 0;
    final label = isActiveStreak ? '$streak' : 'Best $bestStreak';
    final emoji = isActiveStreak ? '🔥' : '🏆';

    return AnimatedScale(
      scale: 1.0,
      duration: ExpressiveMotion.fastSpatialDuration(context),
      curve: ExpressiveMotion.fastSpatialCurve(context),
      child: Container(
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(
          color: isActiveStreak
              ? Colors.orange.withAlpha(28)
              : scheme.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: isActiveStreak
                ? Colors.orange.withAlpha(90)
                : scheme.outlineVariant.withAlpha(80),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(emoji, style: const TextStyle(fontSize: 12.5)),
            const SizedBox(width: 4),
            Text(
              label,
              style: TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w800,
                color: isActiveStreak
                    ? (isDark ? Colors.orangeAccent : Colors.deepOrange)
                    : scheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final spatialCurve = ExpressiveMotion.defaultSpatialCurve(context);
    final spatialDuration = ExpressiveMotion.defaultSpatialDuration(context);

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
                  toolbarHeight: 62,
                  title: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text(Strings.appTitle),
                      const SizedBox(height: 2),
                      _buildSubtitle(context, state),
                    ],
                  ),
                  actions: [
                    _buildStreakBadge(context),
                    Padding(
                      padding: const EdgeInsets.only(right: 12),
                      child: ExpressiveIconButton(
                        icon: Icons.military_tech_rounded,
                        tooltip: Strings.rewardsDashboardTitle,
                        onPressed: () {
                          if (_tabIndex != 0) {
                            setState(() => _tabIndex = 0);
                          }
                          _searchFormKey.currentState
                              ?.loadUrl('https://rewards.bing.com');
                        },
                      ),
                    ),
                  ],
                ),
                body: Stack(
                  children: [
                    // Keep SearchTab mounted at all times so WebView preserves state
                    IgnorePointer(
                      ignoring: _tabIndex != 0,
                      child: AnimatedOpacity(
                        opacity: _tabIndex == 0 ? 1.0 : 0.0,
                        duration: ExpressiveMotion.defaultEffectsDuration,
                        child: AnimatedScale(
                          scale: _tabIndex == 0 ? 1.0 : 0.97,
                          duration: spatialDuration,
                          curve: spatialCurve,
                          child: SearchTab(
                            formKey: _searchFormKey,
                            onStatusChanged: () {
                              if (mounted) setState(() {});
                            },
                          ),
                        ),
                      ),
                    ),
                    IgnorePointer(
                      ignoring: _tabIndex != 1,
                      child: AnimatedOpacity(
                        opacity: _tabIndex == 1 ? 1.0 : 0.0,
                        duration: ExpressiveMotion.defaultEffectsDuration,
                        child: AnimatedScale(
                          scale: _tabIndex == 1 ? 1.0 : 0.97,
                          duration: spatialDuration,
                          curve: spatialCurve,
                          child: const ConfigScreen(),
                        ),
                      ),
                    ),
                  ],
                ),
                bottomNavigationBar: SafeArea(
                  top: false,
                  child: Padding(
                    padding: const EdgeInsets.fromLTRB(24, 4, 24, 12),
                    child: _ExpressiveFloatingNavBar(
                      selectedIndex: _tabIndex,
                      onDestinationSelected: (index) {
                        if (index != _tabIndex) {
                          HapticFeedback.selectionClick();
                          setState(() => _tabIndex = index);
                        }
                      },
                    ),
                  ),
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
                        borderRadius: BorderRadius.circular(18.0),
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

/// Material 3 Expressive Floating Pill Bottom Bar with spatial spring indicator
/// and shape-morphing press states.
class _ExpressiveFloatingNavBar extends StatefulWidget {
  final int selectedIndex;
  final ValueChanged<int> onDestinationSelected;

  const _ExpressiveFloatingNavBar({
    required this.selectedIndex,
    required this.onDestinationSelected,
  });

  @override
  State<_ExpressiveFloatingNavBar> createState() =>
      _ExpressiveFloatingNavBarState();
}

class _ExpressiveFloatingNavBarState extends State<_ExpressiveFloatingNavBar> {
  int? _pressedIndex;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final fastCurve = ExpressiveMotion.fastSpatialCurve(context);
    final fastDuration = ExpressiveMotion.fastSpatialDuration(context);

    return Container(
      height: 60,
      padding: const EdgeInsets.all(5),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerHigh,
        borderRadius: BorderRadius.circular(30),
        border: Border.all(
          color: scheme.outlineVariant.withAlpha(110),
          width: 1,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(24),
            blurRadius: 16,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Stack(
        children: [
          // Sliding & shape-morphing active pill indicator
          AnimatedAlign(
            duration: fastDuration,
            curve: fastCurve,
            alignment: widget.selectedIndex == 0
                ? Alignment.centerLeft
                : Alignment.centerRight,
            child: FractionallySizedBox(
              widthFactor: 0.5,
              heightFactor: 1.0,
              child: AnimatedContainer(
                duration: fastDuration,
                curve: fastCurve,
                decoration: BoxDecoration(
                  color: scheme.secondaryContainer,
                  borderRadius: BorderRadius.circular(
                    _pressedIndex == widget.selectedIndex ? 14 : 25,
                  ),
                ),
              ),
            ),
          ),
          Row(
            children: [
              Expanded(
                child: _buildItem(
                  context: context,
                  index: 0,
                  icon: Icons.search_rounded,
                  selectedIcon: Icons.manage_search_rounded,
                  label: Strings.searchTab,
                ),
              ),
              Expanded(
                child: _buildItem(
                  context: context,
                  index: 1,
                  icon: Icons.settings_outlined,
                  selectedIcon: Icons.settings_rounded,
                  label: Strings.configTab,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildItem({
    required BuildContext context,
    required int index,
    required IconData icon,
    required IconData selectedIcon,
    required String label,
  }) {
    final isSelected = widget.selectedIndex == index;
    final isPressed = _pressedIndex == index;
    final scheme = Theme.of(context).colorScheme;
    final fastCurve = ExpressiveMotion.fastSpatialCurve(context);
    final fastDuration = ExpressiveMotion.fastSpatialDuration(context);

    final fgColor = isSelected
        ? scheme.onSecondaryContainer
        : scheme.onSurfaceVariant;

    return Listener(
      onPointerDown: (_) => setState(() => _pressedIndex = index),
      onPointerUp: (_) => setState(() => _pressedIndex = null),
      onPointerCancel: (_) => setState(() => _pressedIndex = null),
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: () => widget.onDestinationSelected(index),
        child: AnimatedScale(
          scale: isPressed ? 0.92 : (isSelected ? 1.02 : 1.0),
          duration: fastDuration,
          curve: fastCurve,
          child: Center(
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  isSelected ? selectedIcon : icon,
                  size: 20,
                  color: fgColor,
                ),
                const SizedBox(width: 8),
                Text(
                  label,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
                    color: fgColor,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}