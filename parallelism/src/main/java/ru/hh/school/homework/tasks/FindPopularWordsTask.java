package ru.hh.school.homework.tasks;

import ru.hh.school.homework.FindPopularWordsResult;
import ru.hh.school.homework.util.FinderUtils;

import java.nio.file.Path;
import java.util.concurrent.Callable;

public class FindPopularWordsTask implements Callable<FindPopularWordsResult> {

  private final Path directoryPath;
  private final int limit;

  FindPopularWordsTask(Path directoryPath, int limit) {
    this.directoryPath = directoryPath;
    this.limit = limit;
  }

  @Override
  public FindPopularWordsResult call() throws Exception {
    return FinderUtils.getPopularWords(directoryPath, limit);
  }
}
