import 'package:animated_text_kit/animated_text_kit.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
import '../../../../core/theme/expressive_motion.dart';
import '../../../../core/utils/validators/input_validators.dart';
import '../../../../core/widgets/custom_button.dart';
import '../../../../core/widgets/custom_text_field.dart';
import '../../../../core/widgets/expressive_chip.dart';
import '../../../../core/widgets/wavy_progress_indicator.dart';
import '../bloc/search_bloc.dart';

/// Form component handling count/delay inputs, InAppWebView container,
/// search progress indicators, and start/cancel search actions.
class SearchForm extends StatefulWidget {
  const SearchForm({super.key});

  @override
  State<SearchForm> createState() => SearchFormState();
}

class SearchFormState extends State<SearchForm> {
  static final URLRequest _initialBingRequest =
      URLRequest(url: WebUri('https://www.bing.com'));

  InAppWebViewSettings _getWebSettings() {
    final dataSaver = sl<PreferencesService>().dataSaver;
    return InAppWebViewSettings(
      javaScriptEnabled: true,
      cacheEnabled: true,
      incognito: false,
      clearSessionCache: false,
      clearCache: false,
      blockNetworkImage: dataSaver,
      mediaPlaybackRequiresUserGesture: true,
    );
  }

  final _formKey = GlobalKey<FormState>();
  final TextEditingController _countController = TextEditingController();
  final TextEditingController _delayController = TextEditingController();
  InAppWebViewController? _webViewController;

  @override
  void initState() {
    super.initState();
    saveAppOpenedToday();
    _loadSavedValues();
    _countController.addListener(_onCountTextChanged);
  }

  void _onCountTextChanged() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _countController.removeListener(_onCountTextChanged);
    saveCountValue();
    saveDelayValue();
    _countController.dispose();
    _delayController.dispose();
    _webViewController = null;
    WakelockPlus.disable();
    super.dispose();
  }

  Future<bool> handleBackPress() async {
    if (_webViewController != null && await _webViewController!.canGoBack()) {
      await _webViewController!.goBack();
      return true;
    }
    return false;
  }

  Future<void> saveAppOpenedToday() async {
    await sl<PreferencesService>().saveAppOpenedToday();
  }

  void _loadSavedValues() {
    final prefs = sl<PreferencesService>();
    setState(() {
      _countController.text = prefs.searchCount;
      _delayController.text = prefs.searchDelay;
    });
  }

  Future<void> saveCountValue() async {
    await sl<PreferencesService>().setSearchCount(_countController.text);
  }

  Future<void> saveDelayValue() async {
    await sl<PreferencesService>().setSearchDelay(_delayController.text);
  }

  @override
  Widget build(BuildContext context) {
    final spatialCurve = ExpressiveMotion.defaultSpatialCurve(context);
    final spatialDuration = ExpressiveMotion.defaultSpatialDuration(context);

    return Form(
      key: _formKey,
      child: BlocBuilder<SearchBloc, SearchState>(
        builder: (context, state) {
          final isInProgress = state is SearchInProgress;
          final percent = (isInProgress && state.totalCount > 0)
              ? ((state.currentCount / state.totalCount) * 100).toInt()
              : 0;
          final colorScheme = Theme.of(context).colorScheme;
          final primaryColor = colorScheme.primary;
          final errorColor = colorScheme.error;
          final onErrorColor = colorScheme.onError;
          final currentCountText = _countController.text.trim();

          return Column(
            children: [
              Expanded(
                child: Container(
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: colorScheme.outlineVariant.withAlpha(140),
                      width: 1,
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withAlpha(14),
                        blurRadius: 10,
                        offset: const Offset(0, 3),
                      ),
                    ],
                  ),
                  child: RepaintBoundary(
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(19),
                      child: InAppWebView(
                        initialUrlRequest: _initialBingRequest,
                        onWebViewCreated: (controller) =>
                            _webViewController = controller,
                        initialSettings: _getWebSettings(),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              // M3 Expressive dual-morphing preset search count chips
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  for (final count in [10, 20, 30, 50]) ...[
                    Expanded(
                      child: ExpressiveChoiceChip(
                        label: '$count',
                        expand: true,
                        selected: currentCountText == '$count',
                        onSelected: isInProgress
                            ? null
                            : (_) {
                                _countController.text = count.toString();
                              },
                      ),
                    ),
                    if (count != 50) const SizedBox(width: 8),
                  ],
                ],
              ),
              const SizedBox(height: 10),
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
              AnimatedSize(
                duration: spatialDuration,
                curve: spatialCurve,
                child: AnimatedSwitcher(
                  duration: ExpressiveMotion.defaultEffectsDuration,
                  child: isInProgress
                      ? Column(
                          key: const ValueKey('progress_active'),
                          children: [
                            const SizedBox(height: 12),
                            // Countdown or spinner + animated dots
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                if (state.remainingSeconds > 0)
                                  Text(
                                    'Next search in ${state.remainingSeconds}s…',
                                    style: TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.w700,
                                      color: primaryColor,
                                    ),
                                  )
                                else ...[
                                  Text(
                                    'Searching.',
                                    style: TextStyle(
                                      fontSize: 15.5,
                                      fontWeight: FontWeight.w700,
                                      color: primaryColor,
                                    ),
                                  ),
                                  AnimatedTextKit(
                                    repeatForever: true,
                                    animatedTexts: [
                                      TyperAnimatedText(
                                        '..',
                                        textStyle: TextStyle(
                                          fontSize: 15.5,
                                          fontWeight: FontWeight.w700,
                                          color: primaryColor,
                                        ),
                                        speed:
                                            const Duration(milliseconds: 1000),
                                      ),
                                    ],
                                    isRepeatingAnimation: true,
                                    pause: const Duration(milliseconds: 200),
                                    displayFullTextOnTap: false,
                                  ),
                                ],
                              ],
                            ),
                            const SizedBox(height: 8),

                            // M3 Expressive Wavy Linear Progress Indicator
                            TweenAnimationBuilder<double>(
                              duration: spatialDuration,
                              curve: spatialCurve,
                              tween: Tween<double>(
                                begin: 0,
                                end: state.totalCount > 0
                                    ? state.currentCount / state.totalCount
                                    : 0,
                              ),
                              builder: (context, value, _) {
                                return Padding(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 20,
                                  ),
                                  child: WavyLinearProgressIndicator(
                                    value: value,
                                    color: primaryColor,
                                    trackColor:
                                        colorScheme.surfaceContainerHighest,
                                  ),
                                );
                              },
                            ),

                            const SizedBox(height: 6),

                            // Progress text + percentage badge
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(
                                  '${state.currentCount}/${state.totalCount} completed',
                                  style: TextStyle(
                                    fontSize: 13.5,
                                    fontWeight: FontWeight.w600,
                                    color: colorScheme.onSurface,
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 9,
                                    vertical: 2.5,
                                  ),
                                  decoration: BoxDecoration(
                                    color: colorScheme.primaryContainer,
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Text(
                                    '$percent%',
                                    style: TextStyle(
                                      fontSize: 12,
                                      fontWeight: FontWeight.w800,
                                      color: colorScheme.onPrimaryContainer,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            if (state.totalCount > state.currentCount) ...[
                              const SizedBox(height: 3),
                              Builder(
                                builder: (context) {
                                  final remaining =
                                      state.totalCount - state.currentCount;
                                  final delayVal =
                                      double.tryParse(_delayController.text) ??
                                          15.0;
                                  final totalSec =
                                      (remaining * (delayVal + 0.5) +
                                              state.remainingSeconds)
                                          .round();
                                  final min = totalSec ~/ 60;
                                  final sec = totalSec % 60;
                                  final timeStr = min > 0
                                      ? '~${min}m ${sec}s left'
                                      : '~${sec}s left';
                                  return Text(
                                    '${Strings.estTimeRemaining}$timeStr',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: colorScheme.onSurfaceVariant,
                                    ),
                                  );
                                },
                              ),
                            ],
                            const SizedBox(height: 10),
                          ],
                        )
                      : const SizedBox(
                          key: ValueKey('progress_idle'),
                          height: 10,
                        ),
                ),
              ),
              CustomButton(
                onPressed: isInProgress ? _cancelSearch : _startSearch,
                text: _getButtonText(state),
                icon: isInProgress
                    ? Icons.stop_rounded
                    : Icons.play_arrow_rounded,
                isActiveState: isInProgress,
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
    if (_webViewController == null) return;
    if (!_formKey.currentState!.validate()) return;

    await saveCountValue();
    await saveDelayValue();

    if (sl<PreferencesService>().keepScreenOn) {
      WakelockPlus.enable();
    }

    if (!mounted) return;
    context.read<SearchBloc>().add(StartSearchEvent(
          count: int.parse(_countController.text),
          delay: double.parse(_delayController.text),
          controller: _webViewController!,
        ));
  }

  void triggerSearchAction() {
    final state = context.read<SearchBloc>().state;
    if (state is! SearchInProgress) {
      _startSearch();
    }
  }

  void _cancelSearch() {
    context.read<SearchBloc>().add(CancelSearchEvent());
  }

  void loadUrl(String url) {
    _webViewController?.loadUrl(urlRequest: URLRequest(url: WebUri(url)));
  }
}
