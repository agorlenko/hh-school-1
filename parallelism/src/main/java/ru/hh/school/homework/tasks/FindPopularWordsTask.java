package ru.hh.school.homework.tasks;

import ru.hh.school.homework.util.FinderUtils;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.*;

public class FindPopularWordsTask implements Callable<FindPopularWordsTask.Result> {

  private final Path directoryPath;
  private final int limit;

  FindPopularWordsTask(Path directoryPath, int limit) {
    this.directoryPath = directoryPath;
    this.limit = limit;
  }

  @Override
  public Result call() throws Exception {

    Map<String, Long> counter = new HashMap<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.java")) {
      for (Path entry : stream) {
        if (Files.isRegularFile(entry)) {
          FinderUtils.naiveCount(entry).forEach((k, v) -> counter.merge(k, v, Long::sum));
        }
      }
    }
    List<String> popularWords = counter.entrySet()
            .stream()
            .sorted(comparingByValue(reverseOrder()))
            .limit(limit)
            .map(HashMap.Entry::getKey)
            .collect(toList());
    return new Result(directoryPath, popularWords);
  }

  public class Result {
    private final Path directory;
    private final List<String> words;

    Result(Path directory, List<String> words) {
      this.directory = directory;
      this.words = words;
    }

    public Path getDirectory() {
      return directory;
    }

    public List<String> getWords() {
      return words;
    }

    @Override
    public String toString() {
      return directory.toString() + ": " + words.toString();
    }
  }

}
