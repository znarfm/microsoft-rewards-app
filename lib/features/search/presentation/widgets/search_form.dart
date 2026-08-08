import 'package:animated_text_kit/animated_text_kit.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
import '../../../../core/utils/validators/input_validators.dart';
import '../../../../core/widgets/custom_button.dart';
import '../../../../core/widgets/custom_text_field.dart';
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
  static final InAppWebViewSettings _webSettings = InAppWebViewSettings(
    javaScriptEnabled: true,
    cacheEnabled: true,
    incognito: false,
    clearSessionCache: false,
    clearCache: false,
  );

  final _formKey = GlobalKey<FormState>();
  final TextEditingController _countController = TextEditingController();
  final TextEditingController _delayController = TextEditingController();
  InAppWebViewController? _webViewController;

  @override
  void initState() {
    super.initState();
    saveAppOpenedToday();
    _loadSavedValues();
  }

  @override
  void dispose() {
    saveCountValue();
    saveDelayValue();
    _countController.dispose();
    _delayController.dispose();
    _webViewController?.dispose();
    super.dispose();
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
    return Form(
      key: _formKey,
      child: BlocBuilder<SearchBloc, SearchState>(
        builder: (context, state) {
          final isInProgress = state is SearchInProgress;
          final percent = (isInProgress && state.totalCount > 0)
              ? ((state.currentCount / state.totalCount) * 100).toInt()
              : 0;
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
                      initialUrlRequest: _initialBingRequest,
                      onWebViewCreated: (controller) =>
                          _webViewController = controller,
                      initialSettings: _webSettings,
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
                Row(
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
}
