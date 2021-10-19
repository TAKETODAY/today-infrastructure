/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 * Utility methods for working with the file system.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author TODAY 2021/8/21 00:04
 * @see java.io.File
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 * @since 4.0
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
   * Delete the supplied {@link File} &mdash; for directories,
   * recursively delete any nested directories or files as well.
   *
   * @param root the root {@code File} to delete
   * @return {@code true} if the {@code File} existed and was deleted,
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

    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
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
    Assert.notNull(src, "Source File must not be null");
    Assert.notNull(dest, "Destination File must not be null");
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
    Assert.notNull(src, "Source Path must not be null");
    Assert.notNull(dest, "Destination Path must not be null");
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
