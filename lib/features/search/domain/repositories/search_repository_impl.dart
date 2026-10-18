import 'dart:math';

import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:microsoft_automatic_rewards/features/search/domain/repositories/search_repository.dart';
import '../../../../core/di/search_cancellation_token.dart';
import '../../../../core/utils/error_handler.dart';
import '../../../../core/utils/helpers/search_helper.dart';
import '../../data/dataSources/search_words.dart';

class SearchRepositoryImpl implements SearchRepository {
  final SearchWordsDataSource dataSource;
  final SearchHelper searchHelper;

  SearchRepositoryImpl({
    required this.dataSource,
    required this.searchHelper,
  });

  @override
  Future<void> performSearches({
    required int count,
    required double delay,
    required SearchCancellationToken cancellationToken,
    required InAppWebViewController controller,
    required void Function(int currentCount, int totalCount, int remainingSeconds) onProgress,
  }) async {
    try {
      final random = Random();
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('search_cancelled_from_notification', false);
      for (int i = 0; i < count; i++) {
        if (cancellationToken.isCancelled) {
          break;
        }
        try {
          final notificationCancelled = prefs.getBool('search_cancelled_from_notification') ?? false;
          if (notificationCancelled) {
            cancellationToken.cancel();
            break;
          }
        } catch (_) {}
        onProgress(i + 1, count, 0);
        final query = dataSource.randomSentence();
        await searchHelper.launchSearch(controller: controller, query: query);

        if (i < count - 1 && delay > 0) {
          final totalDelayMs = (delay * 1000 + random.nextInt(1001)).toInt();
          final stopwatch = Stopwatch()..start();
          for (int msElapsed = 0; msElapsed < totalDelayMs;) {
            if (cancellationToken.isCancelled) return;
            try {
              if (prefs.getBool('search_cancelled_from_notification') == true) {
                cancellationToken.cancel();
                return;
              }
            } catch (_) {}

            final remainingMs = totalDelayMs - msElapsed;
            final remainingSec = (remainingMs / 1000).ceil();
            onProgress(i + 1, count, remainingSec);

            final sleepChunk = remainingMs >= 1000 ? 1000 : remainingMs;
            await Future.delayed(Duration(milliseconds: sleepChunk));
            msElapsed = stopwatch.elapsedMilliseconds;
          }
        } else {
          onProgress(i + 1, count, 0);
        }
      }
    } catch (e) {
      if (cancellationToken.isCancelled) return;
      throw Exception('Search failed: ${ErrorHandler.getErrorMessage(e)}');
    }
  }
}