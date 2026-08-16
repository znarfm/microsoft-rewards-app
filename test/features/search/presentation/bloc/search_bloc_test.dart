import 'package:bloc_test/bloc_test.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:microsoft_automatic_rewards/core/di/search_cancellation_token.dart';
import 'package:microsoft_automatic_rewards/features/search/domain/repositories/search_repository.dart';
import 'package:microsoft_automatic_rewards/features/search/domain/useCases/perform_search.dart';
import 'package:microsoft_automatic_rewards/features/search/presentation/bloc/search_bloc.dart';

class FakeSearchRepository implements SearchRepository {
  bool shouldThrow = false;
  String errorMessage = 'Failed to execute search';

  @override
  Future<void> performSearches({
    required int count,
    required double delay,
    required SearchCancellationToken cancellationToken,
    required InAppWebViewController controller,
    required void Function(int currentCount, int totalCount, int remainingSeconds) onProgress,
  }) async {
    if (shouldThrow) {
      throw Exception(errorMessage);
    }
    for (int i = 1; i <= count; i++) {
      if (cancellationToken.isCancelled) return;
      onProgress(i, count, 0);
    }
  }
}

class FakeWebViewController implements InAppWebViewController {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late FakeSearchRepository fakeRepository;
  late PerformSearch performSearch;
  late FakeWebViewController fakeController;

  setUp(() {
    fakeRepository = FakeSearchRepository();
    performSearch = PerformSearch(fakeRepository);
    fakeController = FakeWebViewController();
  });

  group('SearchBloc', () {
    test('initial state is SearchInitial', () {
      final bloc = SearchBloc(performSearch: performSearch);
      expect(bloc.state, isA<SearchInitial>());
      bloc.close();
    });

    blocTest<SearchBloc, SearchState>(
      'CancelSearchEvent before start emits SearchCancelled safely',
      build: () => SearchBloc(performSearch: performSearch),
      act: (bloc) => bloc.add(CancelSearchEvent()),
      expect: () => [isA<SearchCancelled>()],
    );

    blocTest<SearchBloc, SearchState>(
      'StartSearchEvent emits SearchInProgress and SearchSuccess on success',
      build: () => SearchBloc(performSearch: performSearch),
      act: (bloc) => bloc.add(StartSearchEvent(
        count: 2,
        delay: 0,
        controller: fakeController,
      )),
      expect: () => [
        const SearchInProgress(currentCount: 0, totalCount: 0, remainingSeconds: 0),
        const SearchInProgress(currentCount: 1, totalCount: 2, remainingSeconds: 0),
        const SearchInProgress(currentCount: 2, totalCount: 2, remainingSeconds: 0),
        isA<SearchSuccess>(),
      ],
    );

    blocTest<SearchBloc, SearchState>(
      'StartSearchEvent emits SearchFailure when repository throws',
      build: () {
        fakeRepository.shouldThrow = true;
        return SearchBloc(performSearch: performSearch);
      },
      act: (bloc) => bloc.add(StartSearchEvent(
        count: 1,
        delay: 0,
        controller: fakeController,
      )),
      expect: () => [
        const SearchInProgress(currentCount: 0, totalCount: 0, remainingSeconds: 0),
        isA<SearchFailure>(),
      ],
    );
  });
}
