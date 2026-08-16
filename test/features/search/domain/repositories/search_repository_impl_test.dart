import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:microsoft_automatic_rewards/core/di/search_cancellation_token.dart';
import 'package:microsoft_automatic_rewards/core/utils/helpers/search_helper.dart';
import 'package:microsoft_automatic_rewards/features/search/data/dataSources/search_words.dart';
import 'package:microsoft_automatic_rewards/features/search/domain/repositories/search_repository_impl.dart';

class FakeSearchWordsDataSource implements SearchWordsDataSource {
  int sentenceCount = 0;

  @override
  String randomSentence() {
    sentenceCount++;
    return 'test query $sentenceCount';
  }
}

class FakeSearchHelper extends SearchHelper {
  final List<String> launchedQueries = [];
  bool shouldThrow = false;

  @override
  Future<void> launchSearch({
    required InAppWebViewController controller,
    required String query,
  }) async {
    if (shouldThrow) {
      throw Exception('Network error');
    }
    launchedQueries.add(query);
  }
}

class FakeWebViewController implements InAppWebViewController {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late FakeSearchWordsDataSource fakeDataSource;
  late FakeSearchHelper fakeSearchHelper;
  late SearchRepositoryImpl repository;
  late FakeWebViewController fakeController;

  setUp(() {
    fakeDataSource = FakeSearchWordsDataSource();
    fakeSearchHelper = FakeSearchHelper();
    repository = SearchRepositoryImpl(
      dataSource: fakeDataSource,
      searchHelper: fakeSearchHelper,
    );
    fakeController = FakeWebViewController();
  });

  group('SearchRepositoryImpl', () {
    test('performs correct number of searches with 0 delay', () async {
      final progressUpdates = <int>[];
      final token = SearchCancellationToken();

      await repository.performSearches(
        count: 3,
        delay: 0,
        cancellationToken: token,
        controller: fakeController,
        onProgress: (current, total, remaining) {
          progressUpdates.add(current);
        },
      );

      expect(fakeSearchHelper.launchedQueries.length, 3);
      expect(fakeSearchHelper.launchedQueries[0], 'test query 1');
      expect(fakeSearchHelper.launchedQueries[1], 'test query 2');
      expect(fakeSearchHelper.launchedQueries[2], 'test query 3');
    });

    test('stops immediately when cancellationToken is cancelled before start', () async {
      final token = SearchCancellationToken();
      token.cancel();

      await repository.performSearches(
        count: 5,
        delay: 0,
        cancellationToken: token,
        controller: fakeController,
        onProgress: (_, _, _) {},
      );

      expect(fakeSearchHelper.launchedQueries, isEmpty);
    });

    test('stops iteration when cancellationToken is cancelled during progress', () async {
      final token = SearchCancellationToken();

      await repository.performSearches(
        count: 5,
        delay: 0,
        cancellationToken: token,
        controller: fakeController,
        onProgress: (current, total, remaining) {
          if (current == 2) {
            token.cancel();
          }
        },
      );

      expect(fakeSearchHelper.launchedQueries.length, 2);
    });

    test('wraps launch error into Exception when not cancelled', () async {
      fakeSearchHelper.shouldThrow = true;
      final token = SearchCancellationToken();

      expect(
        () => repository.performSearches(
          count: 1,
          delay: 0,
          cancellationToken: token,
          controller: fakeController,
          onProgress: (_, _, _) {},
        ),
        throwsA(isA<Exception>()),
      );
    });
  });
}
