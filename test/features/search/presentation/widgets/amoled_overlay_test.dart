import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:microsoft_automatic_rewards/features/search/presentation/widgets/amoled_overlay.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('AmoledOverlay does not wake on quick tap', (tester) async {
    bool woken = false;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AmoledOverlay(onWake: () => woken = true),
        ),
      ),
    );

    // Quick tap
    await tester.tap(find.byType(AmoledOverlay));
    await tester.pump(const Duration(milliseconds: 200));

    expect(woken, false);
  });

  testWidgets('AmoledOverlay wakes after holding for 1500ms', (tester) async {
    bool woken = false;

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: AmoledOverlay(onWake: () => woken = true),
        ),
      ),
    );

    final gesture = await tester.startGesture(const Offset(100, 200));
    await tester.pump();

    // Advance 800ms (not yet completed)
    await tester.pump(const Duration(milliseconds: 800));
    expect(woken, false);
    expect(find.text('Hold to wake'), findsOneWidget);

    // Advance past 1500ms
    await tester.pump(const Duration(milliseconds: 800));
    expect(woken, true);

    await gesture.up();
    await tester.pump();
  });
}
