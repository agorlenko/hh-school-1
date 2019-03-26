package ru.hh.school.homework.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.school.homework.FindPopularWordsResult;
import ru.hh.school.homework.Launcher;
import ru.hh.school.homework.WordsFinderEngine;
import ru.hh.school.homework.util.FinderUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FindPopularWords implements WordsFinderEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
  private final int limit;

  public FindPopularWords(int limit) {
    this.limit = limit;
  }

  @Override
  public void find(Path path) {

    ExecutorService executorService = Executors.newCachedThreadPool();

    List<Future<FindPopularWordsResult>> findPopularWordsResults = new ArrayList<>();
    List<Future<Long>> googleCountsResult = new ArrayList<>();

    DirectoryVisitor directoryVisitor = new DirectoryVisitor(executorService, findPopularWordsResults);

    ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();
    List<FindPopularWordsResult> results = new ArrayList<>();

    try {
      Files.walkFileTree(path, directoryVisitor);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
    for (Future<FindPopularWordsResult> future : findPopularWordsResults) {
      FindPopularWordsResult currentResult = getFuture(future);
      results.add(currentResult);
      for (String word : currentResult.getWords()) {
        googleCountsResult.add(executorService.submit(new GetGoogleCountsTask(word, counts)));
      }
    }

    executorService.shutdown();

    googleCountsResult.forEach(this::getFuture);

    for (FindPopularWordsResult result : results) {
      FinderUtils.printResult(result, counts);
    }

  }

  private <T> T getFuture(Future<T> future) {
    try {
      return future.get();
    } catch (InterruptedException e) {
      LOGGER.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      LOGGER.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  private class DirectoryVisitor extends SimpleFileVisitor<Path> {

    final ExecutorService executorService;
    final List<Future<FindPopularWordsResult>> results;

    DirectoryVisitor(ExecutorService executorService, List<Future<FindPopularWordsResult>> results) {
      this.executorService = executorService;
      this.results = results;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes attrs) {
      if (attrs.isDirectory()) {
        results.add(executorService.submit(new FindPopularWordsTask(path, limit)));
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

}
