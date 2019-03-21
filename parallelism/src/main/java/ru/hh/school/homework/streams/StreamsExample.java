package ru.hh.school.homework.streams;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.school.homework.FindPopularWordsResult;
import ru.hh.school.homework.util.FinderUtils;
import ru.hh.school.homework.WordsFinderEngine;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toList;

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
            .forEach(result -> {
              for (String word : result.getWords()) {
                LOGGER.info("Directory: {}, word: {}, google counts: {}", result.getDirectory(), word, counts.get(word.toLowerCase()));
              }
            });
  }

  private FindPopularWordsResult findPopularWords(Path path) {

      Map<String, Long> counter = new HashMap<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, "*.java")) {
        for (Path entry : stream) {
          if (Files.isRegularFile(entry)) {
            FinderUtils.naiveCount(entry).forEach((k, v) -> counter.merge(k, v, Long::sum));
          }
        }
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
      }
      List<String> popularWords = counter.entrySet()
              .stream()
              .sorted(comparingByValue(reverseOrder()))
              .limit(limit)
              .map(HashMap.Entry::getKey)
              .collect(toList());
      return new FindPopularWordsResult(path, popularWords);
  }

  private FindPopularWordsResult getGoogleCounts(WordResult wordResult) {
    counts.computeIfAbsent(wordResult.word.toLowerCase(), k -> {
      try {
        return FinderUtils.naiveSearch(k);
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
        return 0L;
      }
    });
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
