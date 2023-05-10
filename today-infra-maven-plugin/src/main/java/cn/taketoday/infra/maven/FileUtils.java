/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.infra.maven;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for manipulating files and directories in Infra tooling.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.0
 */
public abstract class FileUtils {

  /**
   * Utility to remove duplicate files from an "output" directory if they already exist
   * in an "origin". Recursively scans the origin directory looking for files (not
   * directories) that exist in both places and deleting the copy.
   *
   * @param outputDirectory the output directory
   * @param originDirectory the origin directory
   */
  public static void removeDuplicatesFromOutputDirectory(File outputDirectory, File originDirectory) {
    if (originDirectory.isDirectory()) {
      for (String name : originDirectory.list()) {
        File targetFile = new File(outputDirectory, name);
        if (targetFile.exists() && targetFile.canWrite()) {
          if (!targetFile.isDirectory()) {
            targetFile.delete();
          }
          else {
            FileUtils.removeDuplicatesFromOutputDirectory(targetFile, new File(originDirectory, name));
          }
        }
      }
    }
  }

  /**
   * Generate a SHA-1 Hash for a given file.
   *
   * @param file the file to hash
   * @return the hash value as a String
   * @throws IOException if the file cannot be read
   */
  public static String sha1Hash(File file) throws IOException {
    return Digest.sha1(InputStreamSupplier.forFile(file));
  }

}
