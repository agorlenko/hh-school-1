package ru.hh.school.homework.streams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.school.homework.FindPopularWordsResult;
import ru.hh.school.homework.util.FinderUtils;
import ru.hh.school.homework.WordsFinderEngine;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StreamsExample implements WordsFinderEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamsExample.class);
  private final int limit;
  private final ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();

  public StreamsExample(int limit) {
    this.limit = limit;
  }

  @Override
  public void find(Path path) {

    counts.clear();
    List<Path> directories = FinderUtils.getDirectories(path);

    directories.parallelStream()
            .map(this::findPopularWords)
            .flatMap(result -> splitFindPopularWordsResult(result).stream())
            .map(this::getGoogleCounts)
            .distinct()
            .forEach(result -> FinderUtils.printResult(result, counts));
  }

  private FindPopularWordsResult findPopularWords(Path path) {
     return FinderUtils.getPopularWords(path, limit);
  }

  private FindPopularWordsResult getGoogleCounts(WordResult wordResult) {
    counts.computeIfAbsent(wordResult.word.toLowerCase(), k -> FinderUtils.naiveSearch(k));
    return wordResult.findPopularWordsResult;
  }

  private List<WordResult> splitFindPopularWordsResult(FindPopularWordsResult findPopularWordsResult) {
    return findPopularWordsResult.getWords()
            .stream()
            .map(w -> new WordResult(w, findPopularWordsResult))
            .collect(Collectors.toList());
  }

  private class WordResult {
    private final String word;
    private final FindPopularWordsResult findPopularWordsResult;
    WordResult(String word, FindPopularWordsResult findPopularWordsResult) {
      this.word = word;
      this.findPopularWordsResult = findPopularWordsResult;
    }
  }

}
