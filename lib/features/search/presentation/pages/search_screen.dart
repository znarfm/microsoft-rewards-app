import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:animated_text_kit/animated_text_kit.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/theme/theme_controller.dart';
import '../../../../core/utils/validators/input_validators.dart';
import '../../../../core/widgets/custom_button.dart';
import '../../../../core/widgets/custom_text_field.dart';
import '../bloc/search_bloc.dart';
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

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkAndShowLoginReminder();
    });
  }

  Future<void> _checkAndShowLoginReminder() async {
    final prefs = await SharedPreferences.getInstance();
    final showPopup = prefs.getBool('show_login_reminder_popup') ?? true;
    if (!showPopup || !mounted) return;

    bool dontShowAgain = false;

    showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Row(
                children: [
                  Icon(Icons.verified_user_outlined, color: Colors.blue),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      Strings.verifyLoginTitle,
                      style: TextStyle(fontSize: 18),
                    ),
                  ),
                ],
              ),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(Strings.verifyLoginContent),
                  const SizedBox(height: 12),
                  CheckboxListTile(
                    value: dontShowAgain,
                    onChanged: (val) {
                      setDialogState(() {
                        dontShowAgain = val ?? false;
                      });
                    },
                    title: const Text(Strings.dontShowAgain),
                    controlAffinity: ListTileControlAffinity.leading,
                    contentPadding: EdgeInsets.zero,
                    dense: true,
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () async {
                    if (dontShowAgain) {
                      final prefs = await SharedPreferences.getInstance();
                      await prefs.setBool('show_login_reminder_popup', false);
                    }
                    if (context.mounted) {
                      Navigator.of(context).pop();
                    }
                  },
                  child: const Text('OK'),
                ),
              ],
            );
          },
        );
      },
    );
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
          final overlayOn =
              sl<ThemeController>().amoledOverlay && !_overlayDismissed;
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
                  body: IndexedStack(
                      index: _tabIndex,
                      children: const [
                        _SearchTab(),
                        ConfigScreen(),
                      ],
                    ),
                    bottomNavigationBar: NavigationBar(
                      selectedIndex: _tabIndex,
                      onDestinationSelected: (index) =>
                          setState(() => _tabIndex = index),
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
                // Full-screen AMOLED "screen off" simulation: covers the
                // entire UI (app bar, nav bar, content). Tap or back wakes
                // it for this search only; the persisted toggle stays on.
                if (showOverlay)
                  Positioned.fill(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: _hideOverlay,
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
    FocusManager.instance.primaryFocus?.unfocus();
    _applySystemUi(false);
    setState(() => _overlayDismissed = true);
  }

  void _applySystemUi(bool visible) {
    if (visible == _systemUiHidden) return;
    _systemUiHidden = visible;
    Future(() => SystemChrome.setEnabledSystemUIMode(
        visible ? SystemUiMode.immersiveSticky : SystemUiMode.edgeToEdge));
  }

  @override
  void dispose() {
    if (_systemUiHidden) {
      SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    }
    super.dispose();
  }
}

/// Search tab content: inputs, WebView, and progress. The AMOLED
/// screen-off overlay lives at the shell level (covers the full UI).
class _SearchTab extends StatefulWidget {
  const _SearchTab();

  @override
  State<_SearchTab> createState() => _SearchTabState();
}

class _SearchTabState extends State<_SearchTab> {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(AppConstants.defaultPadding),
      child: BlocListener<SearchBloc, SearchState>(
        listener: _handleStateChanges,
        child: const SearchForm(),
      ),
    );
  }

  void _handleStateChanges(BuildContext context, SearchState state) {
    if (state is SearchFailure) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.error_outline, color: Colors.white),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  '${Strings.searchFailed}${state.message}',
                  style: const TextStyle(color: Colors.white),
                ),
              ),
            ],
          ),
          backgroundColor: Colors.redAccent,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8.0),
          ),
          duration: const Duration(seconds: 4),
        ),
      );
    }
    if (state is SearchSuccess) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.check_circle_outline, color: Colors.white),
              const SizedBox(width: 12),
              const Text(
                Strings.searchCompleted,
                style: TextStyle(color: Colors.white),
              ),
            ],
          ),
          backgroundColor: Colors.green,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8.0),
          ),
          duration: const Duration(seconds: 3),
        ),
      );
    }
    if (state is SearchCancelled) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Row(
            children: [
              Icon(Icons.info_outline, color: Colors.white),
              SizedBox(width: 12),
              Text(
                Strings.searchCancelled,
                style: TextStyle(color: Colors.white),
              ),
            ],
          ),
          backgroundColor: Colors.orange,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8.0),
          ),
          duration: const Duration(seconds: 2),
        ),
      );
    }
    if (state is SearchFailure || state is SearchSuccess || state is SearchCancelled) {
      WakelockPlus.disable();
    }
  }
}

class SearchForm extends StatefulWidget {
  const SearchForm({super.key});

  @override
  State<SearchForm> createState() => SearchFormState();
}

class SearchFormState extends State<SearchForm> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _countController = TextEditingController();
  final TextEditingController _delayController = TextEditingController();
  InAppWebViewController? _webViewController;

  @override
  void initState() {
    super.initState();
    saveAppOpenedToday();
    _loadSavedValues();
    _countController.addListener(saveCountValue);
    _delayController.addListener(saveDelayValue);
  }

  @override
  void dispose() {
    _countController.dispose();
    _delayController.dispose();
    _webViewController?.dispose();
    super.dispose();
  }

  Future<void> saveAppOpenedToday() async {
    final prefs = await SharedPreferences.getInstance();
    final today = DateTime.now();
    final formatted = '${today.year}-${today.month}-${today.day}'; // simple Y-M-D string
    await prefs.setString('last_opened_date', formatted);
  }

  Future<void> _loadSavedValues() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _countController.text = prefs.getString('search_count') ?? '22';
      _delayController.text = prefs.getString('search_delay') ?? '15';
    });
  }

  Future<void> saveCountValue() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('search_count', _countController.text);
  }

  Future<void> saveDelayValue() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('search_delay', _delayController.text);
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: BlocBuilder<SearchBloc, SearchState>(
        builder: (context, state) {
          final isInProgress = state is SearchInProgress;
          final primaryColor = Theme.of(context).colorScheme.primary;
          final errorColor = Theme.of(context).colorScheme.error;
          final onErrorColor = Theme.of(context).colorScheme.onError;

          return Column(
            children: [
              Expanded(
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: Theme.of(context).colorScheme.outlineVariant,
                      width: 1,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withAlpha(12),
                        blurRadius: 8,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(11),
                    child: InAppWebView(
                      initialUrlRequest:
                          URLRequest(url: WebUri("https://www.bing.com")),
                      onWebViewCreated: (controller) =>
                          _webViewController = controller,
                      initialSettings: InAppWebViewSettings(
                        javaScriptEnabled: true,
                        cacheEnabled: true,
                        incognito: false,
                        clearSessionCache: false,
                        clearCache: false,
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: CustomTextField(
                      controller: _countController,
                      labelText: Strings.searchCountLabel,
                      hintText: Strings.searchCountHint,
                      keyboardType: TextInputType.number,
                      validator: InputValidators.validateSearchCount,
                      enabled: !isInProgress,
                    ),
                  ),
                  const SizedBox(width: AppConstants.defaultPadding),
                  Expanded(
                    child: CustomTextField(
                      controller: _delayController,
                      labelText: Strings.delayLabel,
                      hintText: Strings.delayHint,
                      keyboardType: TextInputType.number,
                      validator: InputValidators.validateDelay,
                      enabled: !isInProgress,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              if (isInProgress) ...[
                // Spinner + animated dots
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      'Searching.',
                      style: TextStyle(fontSize: 16, color: primaryColor),
                    ),
                    AnimatedTextKit(
                      repeatForever: true,
                      animatedTexts: [
                        TyperAnimatedText('..',
                            textStyle: TextStyle(
                                fontSize: 16, color: primaryColor),
                            speed: const Duration(milliseconds: 1000)),
                      ],
                      isRepeatingAnimation: true,
                      pause: const Duration(milliseconds: 200),
                      displayFullTextOnTap: false,
                    ),
                  ],
                ),
                const SizedBox(height: 8),

                // Animated linear progress bar
                TweenAnimationBuilder<double>(
                  duration: const Duration(milliseconds: 300),
                  tween: Tween<double>(
                    begin: 0,
                    end: state.totalCount > 0
                        ? state.currentCount / state.totalCount
                        : 0,
                  ),
                  builder: (context, value, _) {
                    return Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 32),
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(10),
                        child: LinearProgressIndicator(
                          value: value,
                          minHeight: 10,
                          backgroundColor: Colors.grey.shade300,
                          color: primaryColor,
                        ),
                      ),
                    );
                  },
                ),

                const SizedBox(height: 6),

                // Progress text + percentage badge
                Builder(
                  builder: (context) {
                    final percent = (state.totalCount > 0)
                        ? ((state.currentCount / state.totalCount) * 100).toInt()
                        : 0;
                    return Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          '${state.currentCount}/${state.totalCount} completed',
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w500,
                            color: Theme.of(context).colorScheme.onSurface,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: primaryColor.withAlpha(38),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            '$percent%',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: primaryColor,
                            ),
                          ),
                        ),
                      ],
                    );
                  },
                ),
                const SizedBox(height: 12),
              ],
              CustomButton(
                onPressed: isInProgress ? _cancelSearch : _startSearch,
                text: _getButtonText(state),
                backgroundColor: isInProgress ? errorColor : null,
                foregroundColor: isInProgress ? onErrorColor : null,
              ),
            ],
          );
        },
      ),
    );
  }

  String _getButtonText(SearchState state) {
    if (state is SearchInProgress) return Strings.searchInProgress;
    return Strings.startSearch;
  }

  Future<void> _startSearch() async {
    if (!_formKey.currentState!.validate()) return;

    final prefs = await SharedPreferences.getInstance();
    if (prefs.getBool('keep_screen_on') ?? true) {
      WakelockPlus.enable();
    }

    if (!mounted) return;
    context.read<SearchBloc>().add(StartSearchEvent(
          count: int.parse(_countController.text),
          delay: double.parse(_delayController.text),
          controller: _webViewController!,
        ));
  }

  void _cancelSearch() {
    context.read<SearchBloc>().add(CancelSearchEvent());
  }
}