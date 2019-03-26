package ru.hh.school.homework.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DirectoryVisitor extends SimpleFileVisitor<Path> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryVisitor.class);

  private final List<Path> directories = new ArrayList<>();

  @Override
  public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes attrs) {
    if (attrs.isDirectory()) {
      directories.add(path);
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(final Path path, final IOException exc) {
    LOGGER.error("Failed to process: " + path.toString()
            + ". Reason: " + exc.toString());
    return FileVisitResult.SKIP_SUBTREE;
  }

  public List<Path> getDirectories() {
    return directories;
  }
}

