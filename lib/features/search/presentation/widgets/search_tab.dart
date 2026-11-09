import 'dart:async';

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

/// Search tab container wrapping SearchForm and listening for SearchBloc state changes.
class SearchTab extends StatefulWidget {
  final GlobalKey<SearchFormState>? formKey;
  final VoidCallback? onStatusChanged;

  const SearchTab({
    super.key,
    this.formKey,
    this.onStatusChanged,
  });

  @override
  State<SearchTab> createState() => _SearchTabState();
}

class _SearchTabState extends State<SearchTab> with WidgetsBindingObserver {
  bool _isAppInFocus = true;
  int _lastTotalCount = 0;
  Timer? _midnightTimer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _scheduleMidnightRefresh();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _checkAndNotifyLostStreak();
    });
  }

  @override
  void dispose() {
    _midnightTimer?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    NotificationService.cancelSearchProgressNotification();
    WakelockPlus.disable();
    super.dispose();
  }

  void _scheduleMidnightRefresh() {
    _midnightTimer?.cancel();
    final now = DateTime.now();
    final nextMidnight = DateTime(now.year, now.month, now.day + 1);
    final delay = nextMidnight.difference(now) + const Duration(milliseconds: 500);
    _midnightTimer = Timer(delay, () {
      if (mounted) {
        _checkAndNotifyLostStreak();
        setState(() {});
        widget.onStatusChanged?.call();
        _scheduleMidnightRefresh();
      }
    });
  }

  void _checkAndNotifyLostStreak() {
    if (!mounted) return;
    final prefs = sl<PreferencesService>();
    final lostStreak = prefs.checkAndClearLostStreak();
    if (lostStreak > 0) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.sentiment_dissatisfied_outlined, color: Colors.white),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  'You lost your $lostStreak day streak! Complete searches today to start fresh.',
                  style: const TextStyle(color: Colors.white),
                ),
              ),
            ],
          ),
          backgroundColor: Colors.orange.shade800,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16.0),
          ),
          duration: const Duration(seconds: 4),
        ),
      );
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _isAppInFocus = (state == AppLifecycleState.resumed);
    if (state == AppLifecycleState.resumed) {
      _scheduleMidnightRefresh();
      _checkAndNotifyLostStreak();
      sl<PreferencesService>().setNotificationCancelled(false);
      if (mounted) {
        setState(() {});
        widget.onStatusChanged?.call();
      }
    }
    final searchBloc = context.read<SearchBloc>();
    final currentSearchState = searchBloc.state;

    if (currentSearchState is SearchInProgress) {
      if (!_isAppInFocus) {
        _lastTotalCount = currentSearchState.currentCount;
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
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        AppConstants.defaultPadding,
        8,
        AppConstants.defaultPadding,
        AppConstants.defaultPadding,
      ),
      child: BlocListener<SearchBloc, SearchState>(
        listener: _handleStateChanges,
        child: SearchForm(key: widget.formKey),
      ),
    );
  }

  Future<void> _handleStateChanges(BuildContext _, SearchState state) async {
    if (state is SearchFailure ||
        state is SearchSuccess ||
        state is SearchCancelled) {
      WakelockPlus.disable();
    }

    if (state is SearchInProgress) {
      _lastTotalCount = state.currentCount;
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
            borderRadius: BorderRadius.circular(18.0),
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
      if (mounted) {
        setState(() {});
        widget.onStatusChanged?.call();
      }

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
            borderRadius: BorderRadius.circular(18.0),
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
            borderRadius: BorderRadius.circular(18.0),
          ),
          duration: const Duration(seconds: 2),
        ),
      );
    }
  }
}
