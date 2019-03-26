package ru.hh.school.homework.cf;

import ru.hh.school.homework.FindPopularWordsResult;
import ru.hh.school.homework.util.FinderUtils;
import ru.hh.school.homework.WordsFinderEngine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CFExample implements WordsFinderEngine {

  private final int limit;
  private final ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();

  public CFExample(int limit) {
    this.limit = limit;
  }

  @Override
  public void find(Path path) {

    counts.clear();
    List<Path> directories = FinderUtils.getDirectories(path);

    ExecutorService findingExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    ExecutorService countingExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (Path directory : directories) {
      futures.add(findPopularWords(directory, findingExecutorService)
      .thenCompose(data -> getGoogleCounts(data, countingExecutorService))
      .thenAccept(found -> FinderUtils.printResult(found, counts)));
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    findingExecutorService.shutdown();
    countingExecutorService.shutdown();
  }

  private CompletableFuture<FindPopularWordsResult> findPopularWords(Path path, ExecutorService executorService) {
    return CompletableFuture.supplyAsync(() -> FinderUtils.getPopularWords(path, limit), executorService);
  }

  private CompletableFuture<FindPopularWordsResult> getGoogleCounts(FindPopularWordsResult found, ExecutorService executorService) {
    return CompletableFuture.supplyAsync(() -> {
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (String word : found.getWords()) {
        if (!counts.containsKey(word.toLowerCase())) {
          futures.add(CompletableFuture.supplyAsync(() -> FinderUtils.naiveSearch(word), executorService)
                  .thenAccept(count -> counts.put(word.toLowerCase(), count)));
        }
      }
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      return found;
    }, executorService);
  }
}
