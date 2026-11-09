import 'package:flutter/material.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:microsoft_automatic_rewards/features/search/presentation/pages/search_screen.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
import '../../../../core/widgets/custom_button.dart';
import '../../../../core/widgets/wavy_progress_indicator.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _showWebView = false;
  double _progress = 0;

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return PopScope(
      canPop: !_showWebView,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        if (_showWebView) {
          setState(() => _showWebView = false);
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text("Login to Microsoft Account"),
          bottom: _showWebView && _progress < 1.0
              ? PreferredSize(
                  preferredSize: const Size.fromHeight(14),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    child: WavyLinearProgressIndicator(
                      value: _progress == 0 ? null : _progress,
                      height: 12,
                      strokeWidth: 3.5,
                      amplitude: 2.5,
                    ),
                  ),
                )
              : null,
        ),
        body: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              if (!_showWebView) ...[
                Card(
                  margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          padding: const EdgeInsets.all(18),
                          decoration: BoxDecoration(
                            color: colorScheme.primaryContainer,
                            borderRadius: BorderRadius.circular(24),
                          ),
                          child: Icon(
                            Icons.stars_rounded,
                            size: 48,
                            color: colorScheme.onPrimaryContainer,
                          ),
                        ),
                        const SizedBox(height: 20),
                        Text(
                          "Automate your daily web searches to collect reward points",
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: colorScheme.onSurface,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 12),
                        Text(
                          "Sign in with your existing rewards account to run automated searches.\n"
                          "Customizable number of searches and delays.\n"
                          "Use your collected points as usual for gift cards, donations, or game credits.",
                          style: TextStyle(
                            fontSize: 14,
                            color: colorScheme.onSurfaceVariant,
                            height: 1.4,
                          ),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 24),
                        CustomButton(
                          onPressed: () {
                            setState(() {
                              _showWebView = true;
                            });
                          },
                          icon: Icons.login_rounded,
                          text: "Sign In",
                        ),
                      ],
                    ),
                  ),
                ),
                const Spacer(),
                Align(
                  alignment: Alignment.bottomRight,
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: TextButton(
                      onPressed: () => showConfirmPopup(context),
                      child: const Text(
                        "Skip for now",
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                      ),
                    ),
                  ),
                ),
              ] else ...[
                Expanded(
                  child: Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: colorScheme.outlineVariant.withAlpha(100),
                      ),
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(19),
                      child: RepaintBoundary(
                        child: InAppWebView(
                          initialUrlRequest: URLRequest(
                            url: WebUri(AppConstants.loginUrl),
                          ),
                          initialSettings: InAppWebViewSettings(
                            javaScriptEnabled: true,
                            cacheEnabled: true,
                            incognito: false,
                            clearSessionCache: false,
                            clearCache: false,
                          ),
                          onProgressChanged: (controller, progress) {
                            setState(() {
                              _progress = progress / 100.0;
                            });
                          },
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                CustomButton(
                  onPressed: _finishLogin,
                  icon: Icons.check_circle_outline_rounded,
                  text: 'Continue',
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _finishLogin() async {
    await sl<PreferencesService>().setLoggedIn(true);
    if (mounted) {
      navigateToSearchScreen(context);
    }
  }

  void navigateToSearchScreen(BuildContext context) {
    if (mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (context) => const SearchScreen(),
        ),
      );
    }
  }

  Future<void> showConfirmPopup(BuildContext context) {
    return showDialog<void>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text("Skip Login for now?"),
          content: const Text(
              "You won't be able to earn points until you login. Are you sure you want to skip?"),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: const Text("Cancel"),
            ),
            FilledButton(
              onPressed: () {
                Navigator.of(context).pop();
                navigateToSearchScreen(context);
              },
              child: const Text("Skip"),
            ),
          ],
        );
      },
      barrierDismissible: true,
    );
  }
}
