import 'package:get_it/get_it.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../features/search/data/dataSources/search_words.dart';
import '../../features/search/domain/repositories/search_repository.dart';
import '../../features/search/domain/repositories/search_repository_impl.dart';
import '../../features/search/domain/useCases/perform_search.dart';
import '../../features/search/presentation/bloc/search_bloc.dart';
import '../services/preferences_service.dart';
import '../theme/theme_controller.dart';
import '../utils/helpers/search_helper.dart';

final GetIt sl = GetIt.instance;

Future<void> init() async {
  // Register SharedPreferences & PreferencesService
  final prefs = await SharedPreferences.getInstance();
  final preferencesService = PreferencesService(prefs);
  sl.registerLazySingleton<PreferencesService>(() => preferencesService);

  // Register theme controller (loads persisted mode + overlay flag)
  final themeController = ThemeController(preferencesService);
  await themeController.load();
  sl.registerLazySingleton<ThemeController>(() => themeController);

  // Register BLoCs (singleton so background notification stop action controls active search)
  sl.registerLazySingleton<SearchBloc>(
    () => SearchBloc(performSearch: sl()),
  );

  // Register use cases
  sl.registerLazySingleton(() => PerformSearch(sl()));

  // Register repositories
  sl.registerLazySingleton<SearchRepository>(() => SearchRepositoryImpl(
    dataSource: sl(),
    searchHelper: sl(),
  ));

  // Register data sources
  sl.registerLazySingleton<SearchWordsDataSource>(
    () => SearchWordsDataSourceImpl(),
  );

  // Register core services
  sl.registerLazySingleton(() => const SearchHelper());
}