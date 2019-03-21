package ru.hh.school.homework.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class FinderUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(FinderUtils.class);
  public static Map<String, Long> naiveCount(Path path) {
    try {
      return Files.lines(path)
              .flatMap(line -> Stream.of(line.split("[^a-zA-Z0-9]")))
              .filter(word -> word.length() > 3)
              .collect(groupingBy(identity(), counting()))
              .entrySet()
              .stream()
              .sorted(comparingByValue(reverseOrder()))
              .limit(10)
              .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static long naiveSearch(String query) throws IOException {
    Document document = Jsoup //
            .connect("https://www.google.com/search?q=" + query) //
            .userAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.110 Safari/537.36 Viv/2.3.1440.48") //
            .get();

    Element divResultStats = document.select("div#resultStats").first();
    if (divResultStats == null) {
      return 0;
    } else {
      return Long.valueOf(divResultStats.text().replaceAll("[^0-9]", ""));
    }
  }

  public static List<Path> getDirectories(Path path) {
    DirectoryVisitor directoryVisitor = new DirectoryVisitor();
    try {
      Files.walkFileTree(path, directoryVisitor);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
    return directoryVisitor.getDirectories();
  }

}
