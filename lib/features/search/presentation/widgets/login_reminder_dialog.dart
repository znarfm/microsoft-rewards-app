import 'package:flutter/material.dart';

import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';

/// Startup dialog prompting the user to check Bing login status in the embedded webview.
Future<void> showLoginReminderDialog(BuildContext context) async {
  final prefs = sl<PreferencesService>();
  if (!prefs.showLoginReminderPopup) return;

  bool dontShowAgain = false;

  await showDialog<void>(
    context: context,
    builder: (context) {
      return StatefulBuilder(
        builder: (context, setDialogState) {
          final colorScheme = Theme.of(context).colorScheme;
          return AlertDialog(
            icon: Icon(
              Icons.verified_user_rounded,
              color: colorScheme.primary,
              size: 28,
            ),
            title: const Text(
              Strings.verifyLoginTitle,
              textAlign: TextAlign.center,
            ),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  Strings.verifyLoginContent,
                  style: TextStyle(fontSize: 14),
                ),
                const SizedBox(height: 12),
                CheckboxListTile(
                  value: dontShowAgain,
                  onChanged: (val) {
                    setDialogState(() {
                      dontShowAgain = val ?? false;
                    });
                  },
                  title: const Text(
                    Strings.dontShowAgain,
                    style: TextStyle(fontSize: 14),
                  ),
                  controlAffinity: ListTileControlAffinity.leading,
                  contentPadding: EdgeInsets.zero,
                  dense: true,
                ),
              ],
            ),
            actions: [
              FilledButton(
                onPressed: () async {
                  if (dontShowAgain) {
                    await sl<PreferencesService>()
                        .setShowLoginReminderPopup(false);
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
