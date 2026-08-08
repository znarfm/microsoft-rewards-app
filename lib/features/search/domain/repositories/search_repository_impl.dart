import 'dart:math';

import 'package:flutter_inappwebview/flutter_inappwebview.dart';
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
      for (int i = 0; i < count; i++) {
        if (cancellationToken.isCancelled) {
          break;
        }
        onProgress(i + 1, count, 0);
        final query = dataSource.randomSentence();
        await SearchHelper.launchSearch(controller: controller, query: query);

        if (i < count - 1) {
          final totalDelayMs = (delay * 1000 + random.nextInt(1001)).toInt();
          final totalSeconds = (totalDelayMs / 1000).ceil();
          for (int sec = totalSeconds; sec > 0; sec--) {
            if (cancellationToken.isCancelled) return;
            onProgress(i + 1, count, sec);
            await Future.delayed(const Duration(seconds: 1));
          }
        } else {
          onProgress(i + 1, count, 0);
        }
      }
    } catch (e) {
      throw Exception('Search failed: ${ErrorHandler.getErrorMessage(e)}');
    }
  }
}