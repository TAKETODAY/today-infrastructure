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

package cn.taketoday.test.context.util;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cn.taketoday.core.io.FileSystemResource;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * Utility methods for working with resources within the <em>Spring TestContext
 * Framework</em>. Mainly for internal use within the framework.
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @see cn.taketoday.util.ResourceUtils
 * @see cn.taketoday.core.io.Resource
 * @see cn.taketoday.core.io.ClassPathResource
 * @see FileSystemResource
 * @see cn.taketoday.core.io.UrlBasedResource
 * @see cn.taketoday.core.io.ResourceLoader
 * @since 4.0
 */
public abstract class TestContextResourceUtils {

  private static final String SLASH = "/";

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(".*\\$\\{[^}]+\\}.*");

  /**
   * Convert the supplied paths to classpath resource paths.
   * <p>Delegates to {@link #convertToClasspathResourcePaths(Class, boolean, String...)}
   * with {@code false} supplied for the {@code preservePlaceholders} flag.
   *
   * @param clazz the class with which the paths are associated
   * @param paths the paths to be converted
   * @return a new array of converted resource paths
   * @see #convertToResources
   */
  public static String[] convertToClasspathResourcePaths(Class<?> clazz, String... paths) {
    return convertToClasspathResourcePaths(clazz, false, paths);
  }

  /**
   * Convert the supplied paths to classpath resource paths.
   *
   * <p>For each of the supplied paths:
   * <ul>
   * <li>A plain path &mdash; for example, {@code "context.xml"} &mdash; will
   * be treated as a classpath resource that is relative to the package in
   * which the specified class is defined. Such a path will be prepended with
   * the {@code classpath:} prefix and the path to the package for the class.
   * <li>A path starting with a slash will be treated as an absolute path
   * within the classpath, for example: {@code "/org/example/schema.sql"}.
   * Such a path will be prepended with the {@code classpath:} prefix.
   * <li>A path which is already prefixed with a URL protocol (e.g.,
   * {@code classpath:}, {@code file:}, {@code http:}, etc.) will not have its
   * protocol modified.
   * </ul>
   * <p>Each path will then be {@linkplain StringUtils#cleanPath cleaned},
   * unless the {@code preservePlaceholders} flag is {@code true} and the path
   * contains one or more placeholders in the form <code>${placeholder.name}</code>.
   *
   * @param clazz the class with which the paths are associated
   * @param preservePlaceholders {@code true} if placeholders should be preserved
   * @param paths the paths to be converted
   * @return a new array of converted resource paths
   * @see #convertToResources
   * @see ResourceLoader#CLASSPATH_URL_PREFIX
   * @see ResourceUtils#FILE_URL_PREFIX
   */
  public static String[] convertToClasspathResourcePaths(Class<?> clazz, boolean preservePlaceholders, String... paths) {
    String[] convertedPaths = new String[paths.length];
    for (int i = 0; i < paths.length; i++) {
      String path = paths[i];

      // Absolute path
      if (path.startsWith(SLASH)) {
        convertedPaths[i] = ResourceLoader.CLASSPATH_URL_PREFIX + path;
      }
      // Relative path
      else if (!PatternResourceLoader.isUrl(path)) {
        convertedPaths[i] = ResourceLoader.CLASSPATH_URL_PREFIX + SLASH +
                ClassUtils.classPackageAsResourcePath(clazz) + SLASH + path;
      }
      // URL
      else {
        convertedPaths[i] = path;
      }

      if (!(preservePlaceholders && PLACEHOLDER_PATTERN.matcher(convertedPaths[i]).matches())) {
        convertedPaths[i] = StringUtils.cleanPath(convertedPaths[i]);
      }
    }
    return convertedPaths;
  }

  /**
   * Convert the supplied paths to an array of {@link Resource} handles using
   * the given {@link ResourceLoader}.
   *
   * @param resourceLoader the {@code ResourceLoader} to use to convert the paths
   * @param paths the paths to be converted
   * @return a new array of resources
   * @see #convertToResourceList(ResourceLoader, String...)
   * @see #convertToClasspathResourcePaths
   */
  public static Resource[] convertToResources(ResourceLoader resourceLoader, String... paths) {
    return stream(resourceLoader, paths).toArray(Resource[]::new);
  }

  /**
   * Convert the supplied paths to a list of {@link Resource} handles using
   * the given {@link ResourceLoader}.
   *
   * @param resourceLoader the {@code ResourceLoader} to use to convert the paths
   * @param paths the paths to be converted
   * @return a new list of resources
   * @see #convertToResources(ResourceLoader, String...)
   * @see #convertToClasspathResourcePaths
   */
  public static List<Resource> convertToResourceList(ResourceLoader resourceLoader, String... paths) {
    return stream(resourceLoader, paths).collect(Collectors.toList());
  }

  private static Stream<Resource> stream(ResourceLoader resourceLoader, String... paths) {
    return Arrays.stream(paths).map(resourceLoader::getResource);
  }

}
