/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import infra.lang.Assert;
import infra.lang.Nullable;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 * Utility methods for working with the file system.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.io.File
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 * @since 4.0 2021/8/21 00:04
 */
public abstract class FileSystemUtils {

  /**
   * Delete the supplied {@link File} - for directories,
   * recursively delete any nested directories or files as well.
   * <p>Note: Like {@link File#delete()}, this method does not throw any
   * exception but rather silently returns {@code false} in case of I/O
   * errors. Consider using {@link #deleteRecursively(Path)} for NIO-style
   * handling of I/O errors, clearly differentiating between non-existence
   * and failure to delete an existing file.
   *
   * @param root the root {@code File} to delete
   * @return {@code true} if the {@code File} was successfully deleted,
   * otherwise {@code false}
   */
  public static boolean deleteRecursively(@Nullable File root) {
    if (root == null) {
      return false;
    }

    try {
      return deleteRecursively(root.toPath());
    }
    catch (IOException ex) {
      return false;
    }
  }

  /**
   * Delete the supplied {@link Path} &mdash; for directories,
   * recursively delete any nested directories or files as well.
   *
   * @param root the root {@code Path} to delete
   * @return {@code true} if the {@code Path} existed and was deleted,
   * or {@code false} if it did not exist
   * @throws IOException in the case of I/O errors
   */
  public static boolean deleteRecursively(@Nullable Path root) throws IOException {
    if (root == null) {
      return false;
    }
    if (!Files.exists(root)) {
      return false;
    }

    Files.walkFileTree(root, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
    return true;
  }

  /**
   * Recursively copy the contents of the {@code src} file/directory
   * to the {@code dest} file/directory.
   *
   * @param src the source directory
   * @param dest the destination directory
   * @throws IOException in the case of I/O errors
   */
  public static void copyRecursively(File src, File dest) throws IOException {
    Assert.notNull(src, "Source File is required");
    Assert.notNull(dest, "Destination File is required");
    copyRecursively(src.toPath(), dest.toPath());
  }

  /**
   * Recursively copy the contents of the {@code src} file/directory
   * to the {@code dest} file/directory.
   *
   * @param src the source directory
   * @param dest the destination directory
   * @throws IOException in the case of I/O errors
   */
  public static void copyRecursively(Path src, Path dest) throws IOException {
    Assert.notNull(src, "Source Path is required");
    Assert.notNull(dest, "Destination Path is required");
    BasicFileAttributes srcAttr = Files.readAttributes(src, BasicFileAttributes.class);

    if (srcAttr.isDirectory()) {
      Files.walkFileTree(src, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          Files.createDirectories(dest.resolve(src.relativize(dir)));
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
          return FileVisitResult.CONTINUE;
        }
      });
    }
    else if (srcAttr.isRegularFile()) {
      Files.copy(src, dest);
    }
    else {
      throw new IllegalArgumentException("Source File must denote a directory or file");
    }
  }

}
