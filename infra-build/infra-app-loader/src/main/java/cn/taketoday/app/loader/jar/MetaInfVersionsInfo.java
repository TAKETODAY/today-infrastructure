/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.app.loader.jar;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.IntFunction;

import cn.taketoday.app.loader.zip.ZipContent;

/**
 * Info obtained from a {@link ZipContent} instance relating to the directories listed
 * under {@code META-INF/versions/}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class MetaInfVersionsInfo {

  static final MetaInfVersionsInfo NONE = new MetaInfVersionsInfo(Collections.emptySet());

  private static final String META_INF_VERSIONS = NestedJarFile.META_INF_VERSIONS;

  private final int[] versions;

  private final String[] directories;

  private MetaInfVersionsInfo(Set<Integer> versions) {
    this.versions = versions.stream().mapToInt(Integer::intValue).toArray();
    this.directories = versions.stream().map((version) -> META_INF_VERSIONS + version + "/").toArray(String[]::new);
  }

  /**
   * Return the versions listed under {@code META-INF/versions/} in ascending order.
   *
   * @return the versions
   */
  int[] versions() {
    return this.versions;
  }

  /**
   * Return the version directories in the same order as {@link #versions()}.
   *
   * @return the version directories
   */
  String[] directories() {
    return this.directories;
  }

  /**
   * Get {@link MetaInfVersionsInfo} for the given {@link ZipContent}.
   *
   * @param zipContent the zip content
   * @return the {@link MetaInfVersionsInfo}.
   */
  static MetaInfVersionsInfo get(ZipContent zipContent) {
    return get(zipContent.size(), zipContent::getEntry);
  }

  /**
   * Get {@link MetaInfVersionsInfo} for the given details.
   *
   * @param size the number of entries
   * @param entries a function to get an entry from an index
   * @return the {@link MetaInfVersionsInfo}.
   */
  static MetaInfVersionsInfo get(int size, IntFunction<ZipContent.Entry> entries) {
    Set<Integer> versions = new TreeSet<>();
    for (int i = 0; i < size; i++) {
      ZipContent.Entry contentEntry = entries.apply(i);
      if (contentEntry.hasNameStartingWith(META_INF_VERSIONS) && !contentEntry.isDirectory()) {
        String name = contentEntry.getName();
        int slash = name.indexOf('/', META_INF_VERSIONS.length());
        String version = name.substring(META_INF_VERSIONS.length(), slash);
        try {
          int versionNumber = Integer.parseInt(version);
          if (versionNumber >= NestedJarFile.BASE_VERSION) {
            versions.add(versionNumber);
          }
        }
        catch (NumberFormatException ex) {
          // Ignore
        }
      }
    }
    return (!versions.isEmpty()) ? new MetaInfVersionsInfo(versions) : NONE;

  }

}
