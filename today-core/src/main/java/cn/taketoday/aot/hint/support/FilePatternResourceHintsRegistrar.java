/*
 * Copyright 2017 - 2023 the original author or authors.
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
import java.util.Arrays;
import java.util.List;

import cn.taketoday.aot.hint.ResourceHints;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * Register the necessary resource hints for loading files from the classpath.
 *
 * <p>Candidates are identified by a file name, a location, and an extension.
 * The location can be the empty string to refer to the root of the classpath.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
  public FilePatternResourceHintsRegistrar(List<String> names, List<String> locations, List<String> extensions) {
    this.names = Builder.validateFilePrefixes(StringUtils.toStringArray(names));
    this.extensions = Builder.validateFileExtensions(StringUtils.toStringArray(extensions));
    this.locations = Builder.validateClasspathLocations(StringUtils.toStringArray(locations));
  }

  /**
   * Configure the registrar with the specified
   * {@linkplain Builder#withClasspathLocations(String...) classpath locations}.
   *
   * @param locations the classpath locations
   * @return a {@link Builder} to further configure the registrar
   */
  public static Builder forClassPathLocations(String... locations) {
    Assert.notEmpty(locations, "At least one classpath location should be specified");
    return new Builder().withClasspathLocations(locations);
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

  /**
   * Builder for {@link FilePatternResourceHintsRegistrar}.
   */
  public static final class Builder {

    private final List<String> classpathLocations = new ArrayList<>();

    private final List<String> filePrefixes = new ArrayList<>();

    private final List<String> fileExtensions = new ArrayList<>();

    /**
     * Consider the specified classpath locations. A location can either be
     * a special "classpath" pseudo location or a standard location, such as
     * {@code com/example/resources}. An empty String represents the root of
     * the classpath.
     *
     * @param classpathLocations the classpath locations to consider
     * @return this builder
     */
    public Builder withClasspathLocations(String... classpathLocations) {
      this.classpathLocations.addAll(validateClasspathLocations(classpathLocations));
      return this;
    }

    /**
     * Consider the specified file prefixes. Any file whose name starts with one
     * of the specified prefix is considered. A prefix cannot contain the {@code *}
     * character.
     *
     * @param filePrefixes the file prefixes to consider
     * @return this builder
     */
    public Builder withFilePrefixes(String... filePrefixes) {
      this.filePrefixes.addAll(validateFilePrefixes(filePrefixes));
      return this;
    }

    /**
     * Consider the specified file extensions. A file extension must starts with a
     * {@code .} character..
     *
     * @param fileExtensions the file extensions to consider
     * @return this builder
     */
    public Builder withFileExtensions(String... fileExtensions) {
      this.fileExtensions.addAll(validateFileExtensions(fileExtensions));
      return this;
    }

    FilePatternResourceHintsRegistrar build() {
      Assert.notEmpty(this.classpathLocations, "At least one location should be specified");
      return new FilePatternResourceHintsRegistrar(this.filePrefixes,
              this.classpathLocations, this.fileExtensions);
    }

    /**
     * Register resource hints for the current state of this builder. For each
     * classpath location that resolves against the {@code ClassLoader}, file
     * starting with the configured file prefixes and extensions are registered.
     *
     * @param hints the hints contributed so far for the deployment unit
     * @param classLoader the classloader, or {@code null} if even the system ClassLoader isn't accessible
     */
    public void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
      build().registerHints(hints, classLoader);
    }

    private static List<String> validateClasspathLocations(String... locations) {
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

    private static List<String> validateFilePrefixes(String... fileNames) {
      for (String name : fileNames) {
        if (name.contains("*")) {
          throw new IllegalArgumentException("File prefix '" + name + "' cannot contain '*'");
        }
      }
      return Arrays.asList(fileNames);
    }

    private static List<String> validateFileExtensions(String... fileExtensions) {
      for (String fileExtension : fileExtensions) {
        if (!fileExtension.startsWith(".")) {
          throw new IllegalArgumentException("Extension '" + fileExtension + "' should start with '.'");
        }
      }
      return Arrays.asList(fileExtensions);
    }

  }
}
