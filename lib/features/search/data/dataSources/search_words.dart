import 'dart:math';

import 'package:word_generator/word_generator.dart';

abstract class SearchWordsDataSource {
  String randomSentence();
}

class SearchWordsDataSourceImpl implements SearchWordsDataSource {
  static final Random _random = Random();
  static final WordGenerator _wordGenerator = WordGenerator();

  @override
  String randomSentence() {
    return _wordGenerator.randomSentence(2 + _random.nextInt(3));
  }
}