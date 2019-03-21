package ru.hh.school.homework.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hh.school.homework.Launcher;
import ru.hh.school.homework.util.FinderUtils;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class GetGoogleCountsTask implements Callable<Long> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);
  private final String word;
  final ConcurrentHashMap<String, Long> seen;

  public GetGoogleCountsTask(String word, ConcurrentHashMap<String, Long> seen) {
    this.word = word;
    this.seen = seen;
  }

  @Override
  public Long call() {
    return seen.computeIfAbsent(word.toLowerCase(), k -> {
      try {
        return FinderUtils.naiveSearch(k);
      } catch (IOException e) {
        LOGGER.error(e.getMessage(), e);
        return 0L;
      }
    });
  }

}
