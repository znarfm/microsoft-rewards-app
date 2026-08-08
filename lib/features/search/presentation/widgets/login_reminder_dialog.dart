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
