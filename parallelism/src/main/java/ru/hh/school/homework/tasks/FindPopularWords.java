package ru.hh.school.homework.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.school.homework.Launcher;
import ru.hh.school.homework.WordsFinderEngine;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FindPopularWords implements WordsFinderEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
  private final int limit;

  public FindPopularWords(int limit) {
    this.limit = limit;
  }

  @Override
  public void find(Path path) {

    ExecutorService executorService = Executors.newCachedThreadPool();

    List<Future<FindPopularWordsTask.Result>> findPopularWordsResults = new ArrayList<>();
    List<Future<Long>> googleCountsResult = new ArrayList<>();

    DirectoryVisitor directoryVisitor = new DirectoryVisitor(executorService, findPopularWordsResults);

    ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();
    List<FindPopularWordsTask.Result> results = new ArrayList<>();

    try {
      Files.walkFileTree(path, directoryVisitor);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
    for (Future<FindPopularWordsTask.Result> future : findPopularWordsResults) {
      try {
        FindPopularWordsTask.Result currentResult = future.get();
        results.add(currentResult);
        for (String word : currentResult.getWords()) {
          googleCountsResult.add(executorService.submit(new GetGoogleCountsTask(word, counts)));
        }
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }

    executorService.shutdown();

    for (Future<Long> future : googleCountsResult) {
      try {
        future.get();
      } catch (InterruptedException | ExecutionException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }

    for (FindPopularWordsTask.Result result : results) {
      result.getWords().forEach(word -> System.out.println(String.format("Directory: %s, word: %s, google counts: %s",
              result.getDirectory(), word, counts.get(word.toLowerCase()))));
    }

  }

  private class DirectoryVisitor extends SimpleFileVisitor<Path> {

    final ExecutorService executorService;
    final List<Future<FindPopularWordsTask.Result>> results;

    DirectoryVisitor(ExecutorService executorService, List<Future<FindPopularWordsTask.Result>> results) {
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
