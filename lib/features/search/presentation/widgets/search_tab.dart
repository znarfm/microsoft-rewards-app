import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:wakelock_plus/wakelock_plus.dart';

import '../../../../core/constants/app_constants.dart';
import '../../../../core/constants/strings.dart';
import '../../../../core/di/injection_container.dart';
import '../../../../core/services/preferences_service.dart';
import '../../../../notifications/notification_service.dart';
import '../bloc/search_bloc.dart';
import 'search_form.dart';

/// Search tab container wrapping SearchForm, streak indicator, and listening for SearchBloc state changes.
class SearchTab extends StatefulWidget {
  final GlobalKey<SearchFormState>? formKey;
  const SearchTab({super.key, this.formKey});

  @override
  State<SearchTab> createState() => _SearchTabState();
}

class _SearchTabState extends State<SearchTab> with WidgetsBindingObserver {
  bool _isAppInFocus = true;
  int _lastTotalCount = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    NotificationService.cancelSearchProgressNotification();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _isAppInFocus = (state == AppLifecycleState.resumed);
    final searchBloc = context.read<SearchBloc>();
    final currentSearchState = searchBloc.state;

    if (currentSearchState is SearchInProgress) {
      if (!_isAppInFocus) {
        NotificationService.showSearchProgressNotification(
          current: currentSearchState.currentCount,
          total: currentSearchState.totalCount,
          remainingSeconds: currentSearchState.remainingSeconds,
        );
      } else {
        NotificationService.cancelSearchProgressNotification();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final prefs = sl<PreferencesService>();
    final isDoneToday = prefs.isCompletedToday;
    final streak = prefs.completionStreak;

    return Padding(
      padding: const EdgeInsets.all(AppConstants.defaultPadding),
      child: BlocListener<SearchBloc, SearchState>(
        listener: _handleStateChanges,
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: isDoneToday
                          ? Colors.green.withAlpha(25)
                          : Theme.of(context).colorScheme.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: isDoneToday
                            ? Colors.green.withAlpha(70)
                            : Theme.of(context).colorScheme.outlineVariant.withAlpha(60),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          isDoneToday ? Icons.check_circle : Icons.schedule,
                          size: 14,
                          color: isDoneToday ? Colors.green : Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                        const SizedBox(width: 6),
                        Text(
                          isDoneToday ? 'Completed today' : 'Pending today',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: isDoneToday ? Colors.green : Theme.of(context).colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (streak > 0)
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: Colors.orange.withAlpha(25),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(color: Colors.orange.withAlpha(80)),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Text('🔥', style: TextStyle(fontSize: 12)),
                          const SizedBox(width: 4),
                          Text(
                            '$streak day streak',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: Theme.of(context).brightness == Brightness.dark
                                  ? Colors.orangeAccent
                                  : Colors.deepOrange,
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
            Expanded(
              child: SearchForm(key: widget.formKey),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _handleStateChanges(BuildContext _, SearchState state) async {
    if (state is SearchInProgress) {
      _lastTotalCount = state.totalCount;
      if (!_isAppInFocus) {
        NotificationService.showSearchProgressNotification(
          current: state.currentCount,
          total: state.totalCount,
          remainingSeconds: state.remainingSeconds,
        );
      } else {
        NotificationService.cancelSearchProgressNotification();
      }
    }
    if (state is SearchFailure) {
      if (!_isAppInFocus) {
        NotificationService.showSearchFailedNotification(
          message: '${Strings.searchFailed}${state.message}',
        );
      } else {
        NotificationService.cancelSearchProgressNotification();
      }
      if (!mounted) return;
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
      final prefs = sl<PreferencesService>();
      await prefs.recordSearchCompletion();
      if (prefs.hapticFeedback) {
        HapticFeedback.mediumImpact();
      }
      if (mounted) setState(() {});

      if (!_isAppInFocus) {
        NotificationService.showSearchCompletedNotification(
          total: _lastTotalCount,
        );
      } else {
        NotificationService.cancelSearchProgressNotification();
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Row(
            children: [
              Icon(Icons.check_circle_outline, color: Colors.white),
              SizedBox(width: 12),
              Text(
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
          duration: const Duration(seconds: 4),
        ),
      );
    }
    if (state is SearchCancelled) {
      NotificationService.cancelSearchProgressNotification();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Row(
            children: [
              Icon(Icons.cancel_outlined, color: Colors.white),
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
