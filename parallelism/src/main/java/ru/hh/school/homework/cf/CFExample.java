package ru.hh.school.homework.cf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.school.homework.util.FinderUtils;
import ru.hh.school.homework.WordsFinderEngine;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.*;

public class CFExample implements WordsFinderEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(CFExample.class);
  private final int limit;
  private final List<Path> directories = new ArrayList<>();
  private final ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();

  public CFExample(int limit) {
    this.limit = limit;
  }

  @Override
  public void find(Path path) {

    DirectoryVisitor directoryVisitor = new DirectoryVisitor();

    directories.clear();
    counts.clear();
    try {
      Files.walkFileTree(path, directoryVisitor);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }

    ExecutorService executorService = Executors.newCachedThreadPool();
    CountDownLatch latch = new CountDownLatch(directories.size());
    for (Path directory : directories) {
      findPopularWords(directory, executorService)
              .thenCompose(data -> getGoogleCounts(data, executorService))
              .thenAccept(found -> {
                for (String word : found.words) {
                  LOGGER.info("Directory: {}, word: {}, google counts: {}", found.directory, word, counts.get(word.toLowerCase()));
                }
                latch.countDown();
              });
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted during calculations", e);
    }

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
      for (String word : findResult.words) {
        if (!counts.containsKey(word.toLowerCase())) {
          futures.add(CompletableFuture.supplyAsync(() -> {
            try {
              return FinderUtils.naiveSearch(word);
            } catch (IOException e) {
              e.printStackTrace();
              return 0L;
            }
          }, exec)
          .thenAccept(count -> counts.put(word.toLowerCase(), count)));
        }
      }
      exec.shutdown();
      futures.forEach(CompletableFuture::join);
      return findResult;
    }, executorService);
    promise.complete(found);
    return result;
  }

  private class DirectoryVisitor extends SimpleFileVisitor<Path> {

    @Override
    public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes attrs) {
      if (attrs.isDirectory()) {
        directories.add(path);
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(final Path path, final IOException exc) {
      LOGGER.error("Failed to process: " + path.toString()
              + ". Reason: " + exc.toString());
      return FileVisitResult.SKIP_SUBTREE;
    }

  }

  private class FindPopularWordsResult {
    private final Path directory;
    private final List<String> words;

    FindPopularWordsResult(Path directory, List<String> words) {
      this.directory = directory;
      this.words = words;
    }

    @Override
    public String toString() {
      return directory.toString() + ": " + words.toString();
    }
  }

}
