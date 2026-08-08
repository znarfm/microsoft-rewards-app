import 'package:flutter/material.dart';
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

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) async {
        if (didPop) return;
        final state = context.read<SearchBloc>().state;
        final controller = sl<ThemeController>();
        if (state is SearchInProgress && controller.amoledOverlay) {
          await controller.setAmoledOverlay(false);
        } else {
          Navigator.of(context).pop();
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: Builder(
            builder: (context) {
              final isSmallScreen = MediaQuery.of(context).size.width < 365;
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
          onDestinationSelected: (index) => setState(() => _tabIndex = index),
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
    );
  }
}

/// Search tab content: inputs, WebView, progress, and the AMOLED
/// screen-off simulation overlay (pure black while a search is running and
/// the overlay toggle is on).
class _SearchTab extends StatefulWidget {
  const _SearchTab();

  @override
  State<_SearchTab> createState() => _SearchTabState();
}

class _SearchTabState extends State<_SearchTab> {
  final GlobalKey<SearchFormState> _searchFormKey = GlobalKey();

  @override
  Widget build(BuildContext context) {
    final isKeyboardVisible = MediaQuery.of(context).viewInsets.bottom > 0;

    return BlocBuilder<SearchBloc, SearchState>(
      builder: (context, state) {
        final showOverlay = state is SearchInProgress &&
            sl<ThemeController>().amoledOverlay;
        return Stack(
          children: [
            Padding(
              padding: const EdgeInsets.all(AppConstants.defaultPadding),
              child: Column(
                children: [
                  Expanded(
                    child: BlocListener<SearchBloc, SearchState>(
                      listener: _handleStateChanges,
                      child: SearchForm(key: _searchFormKey),
                    ),
                  ),
                  if (!isKeyboardVisible) const Divider(),
                  const SizedBox(height: 48), // Reserve space for the bottom row
                ],
              ),
            ),
            // Bottom row positioned
            if (!isKeyboardVisible)
              Positioned(
                bottom: 12,
                left: 0,
                right: 0,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    TextButton(
                      onPressed: () => openInWebView(
                          'https://svitspindler.com/microsoft-automatic-rewards'),
                      child: const Text('Help'),
                    ),
                    TextButton(
                      onPressed: () =>
                          openInWebView('https://rewards.bing.com/'),
                      child: const Text('Rewards'),
                    ),
                  ],
                ),
              ),
            if (showOverlay)
              Positioned.fill(
                child: GestureDetector(
                  behavior: HitTestBehavior.opaque,
                  onTap: () => sl<ThemeController>().setAmoledOverlay(false),
                  child: const ColoredBox(color: Colors.black),
                ),
              ),
          ],
        );
      },
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
    if (state is SearchFailure || state is SearchSuccess) {
      WakelockPlus.disable();
    }
  }

  void openInWebView(String url) {
    if (_searchFormKey.currentState != null) {
      _searchFormKey.currentState!.openInWebView(url);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(Strings.browserNotReady)),
      );
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
      _delayController.text = prefs.getString('search_delay') ?? '20';
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
      child: Column(
        children: [
          CustomTextField(
            controller: _countController,
            labelText: Strings.searchCountLabel,
            hintText: Strings.searchCountHint,
            keyboardType: TextInputType.number,
            validator: InputValidators.validateSearchCount,
          ),
          const SizedBox(height: AppConstants.defaultPadding),
          CustomTextField(
            controller: _delayController,
            labelText: Strings.delayLabel,
            hintText: Strings.delayHint,
            keyboardType: TextInputType.number,
            validator: InputValidators.validateDelay,
          ),
          const SizedBox(height: AppConstants.defaultPadding),
          BlocBuilder<SearchBloc, SearchState>(
            builder: (context, state) {
              final isInProgress = state is SearchInProgress;
              return Column(
                children: [
                  CustomButton(
                    onPressed: isInProgress ? _cancelSearch : _startSearch,
                    text: _getButtonText(state),
                  ),
                  if (isInProgress) ...[
                    const SizedBox(height: 12),

                    // Spinner + animated dots
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text(
                          'Searching.',
                          style: TextStyle(fontSize: 16, color: Colors.blue),
                        ),
                        AnimatedTextKit(
                          repeatForever: true,
                          animatedTexts: [
                            TyperAnimatedText('..',
                                textStyle: const TextStyle(
                                    fontSize: 16, color: Colors.blue),
                                speed: const Duration(milliseconds: 1000)),
                          ],
                          isRepeatingAnimation: true,
                          pause: const Duration(milliseconds: 200),
                          displayFullTextOnTap: false,
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),

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
                              color: Colors.blue,
                            ),
                          ),
                        );
                      },
                    ),

                    const SizedBox(height: 8),

                    // Progress text (e.g. 4/20)
                    Text(
                      '${state.currentCount}/${state.totalCount} completed',
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                        color: Colors.black87,
                      ),
                    ),
                  ],
                  const SizedBox(height: AppConstants.defaultPadding * 2),
                  SizedBox(
                    height: MediaQuery.of(context).size.height *
                        (state is SearchInProgress ? 0.3 : 0.4),
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
                  const SizedBox(height: 16), // Bottom margin
                ],
              );
            },
          ),
        ],
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

  void openInWebView(String url) {
    if (_webViewController != null) {
      _webViewController!.loadUrl(urlRequest: URLRequest(url: WebUri(url)));
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text(Strings.browserNotReady)),
      );
    }
  }

  void _cancelSearch() {
    context.read<SearchBloc>().add(CancelSearchEvent());
  }
}