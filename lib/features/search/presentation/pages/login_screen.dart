import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:microsoft_automatic_rewards/features/search/presentation/pages/search_screen.dart';
import '../../../../core/constants/app_constants.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
import '../bloc/search_bloc.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  bool _showWebView = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Login to Microsoft Account")),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            if (!_showWebView) ...[
              Card(
                elevation: 4,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                margin: const EdgeInsets.symmetric(horizontal: 16),
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Image.asset(
                        'assets/images/auto_search.png',
                        height: 40,
                      ),
                      const SizedBox(height: 16),
                      const Text(
                        "Automate your daily web searches to collect reward points",
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 12),
                      const Text(
                        "Sign in with your existing rewards account to run automated searches.\n"
                            "Customizable number of searches and delays.\n"
                            "Use your collected points as usual for gift cards, donations, or game credits.",
                      ),
                      const SizedBox(height: 24),
                      ElevatedButton.icon(
                        onPressed: () {
                          setState(() {
                            _showWebView = true;
                          });
                        },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: Colors.blueAccent,
                          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                        label: const Text(
                          "Sign In",
                          style: TextStyle(fontSize: 16),
                        ),
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
                      style: TextStyle(fontSize: 16, color: Colors.blue)
                  ),
                ),
              ),
            ),
            ] else ...[
              Expanded(
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
                ),
              ),
              const SizedBox(height: 12),
              // Sign-in success is confirmed by the user: they sign in inside
              // the WebView (which returns to Bing) and tap Continue. No URL
              // heuristics — Microsoft's redirect chain flickers error URLs
              // even on successful logins.
              SizedBox(
                width: double.infinity,
                child: ElevatedButton.icon(
                  onPressed: _finishLogin,
                  icon: const Icon(Icons.check_circle_outline),
                  label: const Text('Continue'),
                ),
              ),
            ],
          ],
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
          builder: (context) => BlocProvider<SearchBloc>(
            create: (context) => sl<SearchBloc>(),
            child: const SearchScreen(),
          ),
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
          content: const Text("You won't be able to earn points until you login. Are you sure you want to skip?"),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
              },
              child: const Text("Cancel"),
            ),
            TextButton(
              onPressed: () {
                // Handle logout logic here
                Navigator.of(context).pop();
                navigateToSearchScreen(context);
              },
              child: const Text("Skip"),
            ),
          ],
        );
      },
      barrierDismissible: true
    );
  }
}
