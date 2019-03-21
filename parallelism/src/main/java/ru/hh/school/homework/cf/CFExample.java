package ru.hh.school.homework.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.school.homework.FindPopularWordsResult;
import ru.hh.school.homework.util.FinderUtils;
import ru.hh.school.homework.WordsFinderEngine;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toList;

public class CFExample implements WordsFinderEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(CFExample.class);
  private final int limit;
  private final ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();

  public CFExample(int limit) {
    this.limit = limit;
  }

  @Override
  public void find(Path path) {

    counts.clear();
    List<Path> directories = FinderUtils.getDirectories(path);

    ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (Path directory : directories) {
      futures.add(findPopularWords(directory, executorService)
      .thenCompose(data -> getGoogleCounts(data, executorService))
      .thenAccept(found -> {
        for (String word : found.getWords()) {
          LOGGER.info("Directory: {}, word: {}, google counts: {}", found.getDirectory(), word, counts.get(word.toLowerCase()));
        }
      }));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executorService.shutdown();

  }

  private CompletableFuture<FindPopularWordsResult> findPopularWords(Path path, ExecutorService executorService) {

    return CompletableFuture.supplyAsync(() -> {
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
    }, executorService);
  }

  private CompletableFuture<FindPopularWordsResult> getGoogleCounts(FindPopularWordsResult found, ExecutorService executorService) {
    CompletableFuture<FindPopularWordsResult> promise = new CompletableFuture<>();
    CompletableFuture<FindPopularWordsResult> result = promise.thenApplyAsync(findResult -> {
      ExecutorService exec = Executors.newCachedThreadPool();
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (String word : findResult.getWords()) {
        if (!counts.containsKey(word.toLowerCase())) {
          futures.add(CompletableFuture.supplyAsync(() -> {
            try {
              return FinderUtils.naiveSearch(word);
            } catch (IOException e) {
              LOGGER.error(e.getMessage(), e);
              return 0L;
            }
          }, exec)
          .thenAccept(count -> counts.put(word.toLowerCase(), count)));
        }
      }
      exec.shutdown();
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      return findResult;
    }, executorService);
    promise.complete(found);
    return result;
  }

}
