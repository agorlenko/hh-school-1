package ru.hh.school.homework;

import java.nio.file.Path;
import java.util.List;

public class FindPopularWordsResult {
  private final Path directory;
  private final List<String> words;

  public FindPopularWordsResult(Path directory, List<String> words) {
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
