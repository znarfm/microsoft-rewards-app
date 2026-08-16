import 'package:bloc/bloc.dart';
import 'package:equatable/equatable.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import '../../../../core/di/search_cancellation_token.dart';
import '../../../../core/utils/error_handler.dart';
import '../../domain/useCases/perform_search.dart';

class SearchBloc extends Bloc<SearchEvent, SearchState> {
  SearchCancellationToken? _cancelToken;
  final PerformSearch performSearch;

  SearchBloc({required this.performSearch}) : super(SearchInitial()) {
    on<StartSearchEvent>(_onStartSearch);
    on<CancelSearchEvent>(_onCancelSearch);
  }

  Future<void> _onStartSearch(
    StartSearchEvent event,
    Emitter<SearchState> emit,
  ) async {
    emit(const SearchInProgress());
    final cancelToken = SearchCancellationToken();
    _cancelToken = cancelToken;
    try {
      await performSearch(
        count: event.count,
        delay: event.delay,
        cancellationToken: cancelToken,
        controller: event.controller,
        onProgress: (currentCount, totalCount, remainingSeconds) {
          if (!cancelToken.isCancelled) {
            emit(SearchInProgress(
              currentCount: currentCount,
              totalCount: event.count,
              remainingSeconds: remainingSeconds,
            ));
          }
        },
      );
      if (cancelToken.isCancelled) return;
      emit(SearchSuccess());
    } catch (e) {
      if (cancelToken.isCancelled) return;
      emit(SearchFailure(ErrorHandler.getErrorMessage(e)));
    }
  }

  void _onCancelSearch(
    CancelSearchEvent event,
    Emitter<SearchState> emit,
  ) {
    _cancelToken?.cancel();
    emit(SearchCancelled());
  }
}

abstract class SearchEvent extends Equatable {
  const SearchEvent();

  @override
  List<Object?> get props => [];
}

class StartSearchEvent extends SearchEvent {
  final int count;
  final double delay;
  final InAppWebViewController controller;

  const StartSearchEvent({
    required this.count,
    required this.delay,
    required this.controller,
  });

  @override
  List<Object?> get props => [count, delay, controller];
}

class CancelSearchEvent extends SearchEvent {}

abstract class SearchState extends Equatable {
  const SearchState();

  @override
  List<Object?> get props => [];
}

class SearchInitial extends SearchState {}

class SearchInProgress extends SearchState {
  final bool isCancelled;
  final int currentCount;
  final int totalCount;
  final int remainingSeconds;

  const SearchInProgress({
    this.isCancelled = false,
    this.currentCount = 0,
    this.totalCount = 0,
    this.remainingSeconds = 0,
  });

  @override
  List<Object?> get props => [
        isCancelled,
        currentCount,
        totalCount,
        remainingSeconds,
      ];
}

class SearchCancelled extends SearchState {}

class SearchSuccess extends SearchState {}

class SearchFailure extends SearchState {
  final String message;

  const SearchFailure(this.message);

  @override
  List<Object?> get props => [message];
}