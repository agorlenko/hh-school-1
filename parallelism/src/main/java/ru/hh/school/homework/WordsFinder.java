package ru.hh.school.homework;

import ru.hh.school.homework.cf.CFExample;
import ru.hh.school.homework.streams.StreamsExample;
import ru.hh.school.homework.tasks.FindPopularWords;

public class WordsFinder {

  public static WordsFinderEngine create(String method, int limit) {
    switch (method) {
      case "cf":
        return new CFExample(limit);
      case "tasks":
        return new FindPopularWords(limit);
      case "stream":
        return new StreamsExample(limit);
      default:
        throw new RuntimeException("Unknown method " + method);
    }
  }

}
