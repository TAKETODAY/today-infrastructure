/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.maven;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A file filter using includes/excludes patterns.
 */
public class FileFilter {

  private static final String DEFAULT_INCLUDES = "**";
  private static final String DEFAULT_EXCLUDES = "";

  private final List<String> includes;
  private final List<String> excludes;

  /**
   * Construct a new FileFilter
   *
   * @param includes list of includes patterns
   * @param excludes list of excludes patterns
   */
  public FileFilter(final List<String> includes,
          final List<String> excludes) {
    this.includes = includes;
    this.excludes = excludes;
  }

  /**
   * Returns a list of file names.
   *
   * @param directory the directory to scan
   * @return a list of files
   * @throws IOException if file system access fails
   */
  public List<String> getFileNames(final File directory) throws IOException {
    return FileUtils.getFileNames(directory, getIncludes(), getExcludes(), false);
  }

  /**
   * Returns a list of files.
   *
   * @param directory the directory to scan
   * @return a list of files
   * @throws IOException if file system access fails
   */
  public List<File> getFiles(final File directory) throws IOException {
    return FileUtils.getFiles(directory, getIncludes(), getExcludes());
  }

  /**
   * Get the includes pattern
   *
   * @return the pattern
   */
  public String getIncludes() {
    return this.buildPattern(this.includes, DEFAULT_INCLUDES);
  }

  /**
   * Get the excludes pattern
   *
   * @return the pattern
   */
  public String getExcludes() {
    return this.buildPattern(this.excludes, DEFAULT_EXCLUDES);
  }

  private String buildPattern(final List<String> patterns,
          final String defaultPattern) {
    String pattern = defaultPattern;
    if (patterns != null && !patterns.isEmpty()) {
      pattern = StringUtils.join(patterns.iterator(), ",");
    }
    return pattern;
  }
}
