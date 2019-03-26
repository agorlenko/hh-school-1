package ru.hh.school.homework;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

public class Launcher {

  private static final Logger LOGGER = getLogger(Launcher.class);


  public static void main(String[] args) {
    // Написать код, который, как можно более параллельно:
    // - по заданному пути найдет все "*.java" файлы
    // - для каждого файла вычислит 10 самых популярных слов (см. #naiveCount())
    // - соберет top 10 для каждой папки в которой есть хотя-бы один java файл
    // - для каждого слова сходит в гугл и вернет количество результатов по нему (см. #naiveSearch())
    // - распечатает в консоль результаты в виде:
    // <папка1> - <слово #1> - <кол-во результатов в гугле>
    // <папка1> - <слово #2> - <кол-во результатов в гугле>
    // ...
    // <папка1> - <слово #10> - <кол-во результатов в гугле>
    // <папка2> - <слово #1> - <кол-во результатов в гугле>
    // <папка2> - <слово #2> - <кол-во результатов в гугле>
    // ...
    // <папка2> - <слово #10> - <кол-во результатов в гугле>
    // ...
    //
    // Порядок результатов в консоли не обязательный.
    // При желании naiveSearch и naiveCount можно оптимизировать.

    // test our naive methods:
    // testCount();
    // testSearch();

    Map<String, String> parameters = new HashMap<>();

    for (String arg : args) {
      String[] parts = arg.split("=", 2);
      String key = parts[0];
      if (key.startsWith("--")) {
        key = key.substring(2);
      } else {
        key = key.substring(1);
      }
      String value = parts[1].trim();
      parameters.put(key, value);
    }

    String directoryArg = parameters.get("directory");
    String methodArg = parameters.get("method");
    String limitArg = parameters.get("limit");

    if (directoryArg == null || directoryArg.isEmpty()) {
      LOGGER.error("No parameter value specified '-directory'");
      return;
    }
    if (methodArg == null || methodArg.isEmpty()) {
      LOGGER.error("No parameter value specified '-method'");
      return;
    }
    if (limitArg == null || limitArg.isEmpty()) {
      LOGGER.error("No parameter value specified '-limit'");
      return;
    }

    Path path = Path.of(directoryArg);

    WordsFinderEngine finder = WordsFinder.create(methodArg, Integer.parseInt(limitArg));
    finder.find(path);

  }

}
