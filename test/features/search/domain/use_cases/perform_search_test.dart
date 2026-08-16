import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:microsoft_automatic_rewards/core/di/search_cancellation_token.dart';
import 'package:microsoft_automatic_rewards/features/search/domain/repositories/search_repository.dart';
import 'package:microsoft_automatic_rewards/features/search/domain/useCases/perform_search.dart';

class FakeSearchRepository implements SearchRepository {
  int? lastCount;
  double? lastDelay;
  bool performSearchesCalled = false;

  @override
  Future<void> performSearches({
    required int count,
    required double delay,
    required SearchCancellationToken cancellationToken,
    required InAppWebViewController controller,
    required void Function(int currentCount, int totalCount, int remainingSeconds) onProgress,
  }) async {
    performSearchesCalled = true;
    lastCount = count;
    lastDelay = delay;
  }
}

class FakeWebViewController implements InAppWebViewController {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late FakeSearchRepository fakeRepository;
  late PerformSearch useCase;
  late FakeWebViewController fakeController;

  setUp(() {
    fakeRepository = FakeSearchRepository();
    useCase = PerformSearch(fakeRepository);
    fakeController = FakeWebViewController();
  });

  group('PerformSearch UseCase', () {
    test('throws ArgumentError when count < 1', () async {
      expect(
        () => useCase(
          count: 0,
          delay: 5,
          cancellationToken: SearchCancellationToken(),
          controller: fakeController,
          onProgress: (_, _, _) {},
        ),
        throwsArgumentError,
      );
    });

    test('throws ArgumentError when delay < 0', () async {
      expect(
        () => useCase(
          count: 10,
          delay: -1,
          cancellationToken: SearchCancellationToken(),
          controller: fakeController,
          onProgress: (_, _, _) {},
        ),
        throwsArgumentError,
      );
    });

    test('delegates to repository when parameters are valid', () async {
      await useCase(
        count: 5,
        delay: 2.5,
        cancellationToken: SearchCancellationToken(),
        controller: fakeController,
        onProgress: (_, _, _) {},
      );

      expect(fakeRepository.performSearchesCalled, true);
      expect(fakeRepository.lastCount, 5);
      expect(fakeRepository.lastDelay, 2.5);
    });
  });
}
