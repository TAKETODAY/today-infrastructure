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

package cn.taketoday.aot.hint.support;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;

/**
 * Register the necessary resource hints for loading files from the classpath.
 *
 * <p>Candidates are identified by a file name, a location, and an extension.
 * The location can be the empty string to refer to the root of the classpath.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
public class FilePatternResourceHintsRegistrar {

  private final List<String> names;

  private final List<String> locations;

  private final List<String> extensions;

  /**
   * Create a new instance for the specified file names, locations, and file
   * extensions.
   *
   * @param names the file names
   * @param locations the classpath locations
   * @param extensions the file extensions (starts with a dot)
   */
  public FilePatternResourceHintsRegistrar(List<String> names, List<String> locations,
          List<String> extensions) {
    this.names = validateNames(names);
    this.locations = validateLocations(locations);
    this.extensions = validateExtensions(extensions);
  }

  private static List<String> validateNames(List<String> names) {
    for (String name : names) {
      if (name.contains("*")) {
        throw new IllegalArgumentException("File name '" + name + "' cannot contain '*'");
      }
    }
    return names;
  }

  private static List<String> validateLocations(List<String> locations) {
    Assert.notEmpty(locations, "At least one location should be specified");
    List<String> parsedLocations = new ArrayList<>();
    for (String location : locations) {
      if (location.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX)) {
        location = location.substring(ResourceUtils.CLASSPATH_URL_PREFIX.length());
      }
      if (location.startsWith("/")) {
        location = location.substring(1);
      }
      if (!location.isEmpty() && !location.endsWith("/")) {
        location = location + "/";
      }
      parsedLocations.add(location);
    }
    return parsedLocations;

  }

  private static List<String> validateExtensions(List<String> extensions) {
    for (String extension : extensions) {
      if (!extension.startsWith(".")) {
        throw new IllegalArgumentException("Extension '" + extension + "' should start with '.'");
      }
    }
    return extensions;
  }

  public void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
    ClassLoader classLoaderToUse = (classLoader != null) ? classLoader : getClass().getClassLoader();
    List<String> includes = new ArrayList<>();
    for (String location : this.locations) {
      if (classLoaderToUse.getResource(location) != null) {
        for (String extension : this.extensions) {
          for (String name : this.names) {
            includes.add(location + name + "*" + extension);
          }
        }
      }
    }
    if (!includes.isEmpty()) {
      hints.registerPattern(hint -> hint.includes(includes.toArray(String[]::new)));
    }
  }
}
