package ru.hh.school.homework.tasks;

import ru.hh.school.homework.util.FinderUtils;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class GetGoogleCountsTask implements Callable<Long> {

  private final String word;
  final ConcurrentHashMap<String, Long> seen;

  public GetGoogleCountsTask(String word, ConcurrentHashMap<String, Long> seen) {
    this.word = word;
    this.seen = seen;
  }

  @Override
  public Long call() throws Exception {
    if (!seen.containsKey(word.toLowerCase())) {
      seen.put(word.toLowerCase(), FinderUtils.naiveSearch(word));
    }
    return seen.get(word.toLowerCase());
  }

}
