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

/**
 * Register the necessary resource hints for loading files from the classpath,
 * based on file name prefixes and file extensions with convenience to support
 * multiple classpath locations.
 *
 * <p>Only registers hints for matching classpath locations, which allows for
 * several locations to be provided without contributing unnecessary hints.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FilePatternResourceHintsRegistrar {

  private final List<String> classpathLocations;

  private final List<String> filePrefixes;

  private final List<String> fileExtensions;

  /**
   * Create a new instance for the specified file prefixes, classpath locations,
   * and file extensions.
   *
   * @param filePrefixes the file prefixes
   * @param classpathLocations the classpath locations
   * @param fileExtensions the file extensions (starting with a dot)
   */
  public FilePatternResourceHintsRegistrar(List<String> filePrefixes,
          List<String> classpathLocations, List<String> fileExtensions) {

    this.classpathLocations = validateClasspathLocations(classpathLocations);
    this.filePrefixes = validateFilePrefixes(filePrefixes);
    this.fileExtensions = validateFileExtensions(fileExtensions);
  }

  public void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
    ClassLoader classLoaderToUse = (classLoader != null ? classLoader : getClass().getClassLoader());
    List<String> includes = new ArrayList<>();
    for (String location : this.classpathLocations) {
      if (classLoaderToUse.getResource(location) != null) {
        for (String filePrefix : this.filePrefixes) {
          for (String fileExtension : this.fileExtensions) {
            includes.add(location + filePrefix + "*" + fileExtension);
          }
        }
      }
    }
    if (!includes.isEmpty()) {
      hints.registerPattern(hint -> hint.includes(includes.toArray(String[]::new)));
    }
  }

  /**
   * Configure the registrar with the specified
   * {@linkplain Builder#withClasspathLocations(String...) classpath locations}.
   *
   * @param classpathLocations the classpath locations
   * @return a {@link Builder} to further configure the registrar
   * @see #forClassPathLocations(List)
   */
  public static Builder forClassPathLocations(String... classpathLocations) {
    return forClassPathLocations(Arrays.asList(classpathLocations));
  }

  /**
   * Configure the registrar with the specified
   * {@linkplain Builder#withClasspathLocations(List) classpath locations}.
   *
   * @param classpathLocations the classpath locations
   * @return a {@link Builder} to further configure the registrar
   * @see #forClassPathLocations(String...)
   */
  public static Builder forClassPathLocations(List<String> classpathLocations) {
    return new Builder().withClasspathLocations(classpathLocations);
  }

  private static List<String> validateClasspathLocations(List<String> classpathLocations) {
    Assert.notEmpty(classpathLocations, "At least one classpath location must be specified");
    List<String> parsedLocations = new ArrayList<>();
    for (String location : classpathLocations) {
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

  private static List<String> validateFilePrefixes(List<String> filePrefixes) {
    for (String filePrefix : filePrefixes) {
      if (filePrefix.contains("*")) {
        throw new IllegalArgumentException("File prefix '" + filePrefix + "' cannot contain '*'");
      }
    }
    return filePrefixes;
  }

  private static List<String> validateFileExtensions(List<String> fileExtensions) {
    for (String fileExtension : fileExtensions) {
      if (!fileExtension.startsWith(".")) {
        throw new IllegalArgumentException("Extension '" + fileExtension + "' must start with '.'");
      }
    }
    return fileExtensions;
  }

  /**
   * Builder for {@link FilePatternResourceHintsRegistrar}.
   */
  public static final class Builder {

    private final List<String> classpathLocations = new ArrayList<>();

    private final List<String> filePrefixes = new ArrayList<>();

    private final List<String> fileExtensions = new ArrayList<>();

    private Builder() {
      // no-op
    }

    /**
     * Consider the specified classpath locations.
     * <p>A location can either be a special {@value ResourceUtils#CLASSPATH_URL_PREFIX}
     * pseudo location or a standard location, such as {@code com/example/resources}.
     * An empty String represents the root of the classpath.
     *
     * @param classpathLocations the classpath locations to consider
     * @return this builder
     * @see #withClasspathLocations(List)
     */
    public Builder withClasspathLocations(String... classpathLocations) {
      return withClasspathLocations(Arrays.asList(classpathLocations));
    }

    /**
     * Consider the specified classpath locations.
     * <p>A location can either be a special {@value ResourceUtils#CLASSPATH_URL_PREFIX}
     * pseudo location or a standard location, such as {@code com/example/resources}.
     * An empty String represents the root of the classpath.
     *
     * @param classpathLocations the classpath locations to consider
     * @return this builder
     * @see #withClasspathLocations(String...)
     */
    public Builder withClasspathLocations(List<String> classpathLocations) {
      this.classpathLocations.addAll(validateClasspathLocations(classpathLocations));
      return this;
    }

    /**
     * Consider the specified file prefixes. Any file whose name starts with one
     * of the specified prefixes is considered. A prefix cannot contain the {@code *}
     * character.
     *
     * @param filePrefixes the file prefixes to consider
     * @return this builder
     * @see #withFilePrefixes(List)
     */
    public Builder withFilePrefixes(String... filePrefixes) {
      return withFilePrefixes(Arrays.asList(filePrefixes));
    }

    /**
     * Consider the specified file prefixes. Any file whose name starts with one
     * of the specified prefixes is considered. A prefix cannot contain the {@code *}
     * character.
     *
     * @param filePrefixes the file prefixes to consider
     * @return this builder
     * @see #withFilePrefixes(String...)
     */
    public Builder withFilePrefixes(List<String> filePrefixes) {
      this.filePrefixes.addAll(validateFilePrefixes(filePrefixes));
      return this;
    }

    /**
     * Consider the specified file extensions. A file extension must start with a
     * {@code .} character.
     *
     * @param fileExtensions the file extensions to consider
     * @return this builder
     * @see #withFileExtensions(List)
     */
    public Builder withFileExtensions(String... fileExtensions) {
      return withFileExtensions(Arrays.asList(fileExtensions));
    }

    /**
     * Consider the specified file extensions. A file extension must start with a
     * {@code .} character.
     *
     * @param fileExtensions the file extensions to consider
     * @return this builder
     * @see #withFileExtensions(String...)
     */
    public Builder withFileExtensions(List<String> fileExtensions) {
      this.fileExtensions.addAll(validateFileExtensions(fileExtensions));
      return this;
    }

    private FilePatternResourceHintsRegistrar build() {
      return new FilePatternResourceHintsRegistrar(this.filePrefixes,
              this.classpathLocations, this.fileExtensions);
    }

    /**
     * Register resource hints for the current state of this builder. For each
     * classpath location that resolves against the {@code ClassLoader}, files
     * with the configured file prefixes and extensions are registered.
     *
     * @param hints the hints contributed so far for the deployment unit
     * @param classLoader the classloader, or {@code null} if even the system
     * ClassLoader isn't accessible
     */
    public void registerHints(ResourceHints hints, @Nullable ClassLoader classLoader) {
      build().registerHints(hints, classLoader);
    }

  }

}
