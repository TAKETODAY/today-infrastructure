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

package cn.taketoday.core.io;

import java.io.IOException;
import java.util.Set;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ResourceUtils;

/**
 * Strategy interface for resolving a location pattern (for example,
 * an Ant-style path pattern) into {@link Resource} objects.
 *
 * <p>This is an extension to the {@link cn.taketoday.core.io.ResourceLoader}
 * interface. A passed-in {@code ResourceLoader} (for example, an
 * {@link cn.taketoday.context.ApplicationContext} passed in via
 * {@link cn.taketoday.context.aware.ResourceLoaderAware} when running in a context)
 * can be checked whether it implements this extended interface too.
 *
 * <p>{@link PathMatchingPatternResourceLoader} is a standalone implementation
 * that is usable outside an {@code ApplicationContext}.
 *
 * <p>Can be used with any sort of location pattern (e.g. "/WEB-INF/*-context.xml"):
 * Input patterns have to match the strategy implementation. This interface just
 * specifies the conversion method rather than a specific pattern format.
 *
 * <p>This interface also suggests a new resource prefix "classpath*:" for all
 * matching resources from the class path. Note that the resource location is
 * expected to be a path without placeholders in this case (e.g. "/test.xml");
 * JAR files or different directories in the class path can contain multiple files
 * of the same name.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/7 17:26
 * @see Resource
 * @see ResourceLoader
 * @see cn.taketoday.context.ApplicationContext
 * @see cn.taketoday.context.aware.ResourceLoaderAware
 * @since 4.0
 */
public interface PatternResourceLoader extends ResourceLoader {

  /**
   * Pseudo URL prefix for all matching resources from the class path: "classpath*:"
   * <p>This differs from ResourceLoader's classpath URL prefix in that it
   * retrieves all matching resources for a given name (e.g. "/test.xml"),
   * for example in the root of all deployed JAR files.
   *
   * @see ResourceLoader#CLASSPATH_URL_PREFIX
   */
  String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

  /**
   * Resolve the given location pattern into {@code Resource} objects.
   * <p>Overlapping resource entries that point to the same physical
   * resource should be avoided, as far as possible. The result should
   * have set semantics.
   *
   * @param locationPattern the location pattern to resolve
   * @return a mutable Set of the corresponding {@code Resource} instances
   * @throws IOException in case of I/O errors
   */
  Set<Resource> getResources(String locationPattern) throws IOException;

  /**
   * Scan the given location pattern into {@code Resource} objects.
   * <p>Overlapping resource entries that point to the same physical
   * resource should be avoided, as far as possible. The result should
   * have set semantics.
   *
   * @param locationPattern the location pattern to resolve
   * @throws IOException in case of I/O errors
   */
  void scan(String locationPattern, ResourceConsumer consumer) throws IOException;

  /**
   * Resolve the given location pattern into {@code Resource} objects.
   * <p>Overlapping resource entries that point to the same physical
   * resource should be avoided, as far as possible. The result should
   * have set semantics.
   *
   * @param locationPattern the location pattern to resolve
   * @return the corresponding {@code Resource} objects
   * @throws IOException in case of I/O errors
   */
  default Resource[] getResourcesArray(String locationPattern) throws IOException {
    return getResources(locationPattern).toArray(Resource.EMPTY_ARRAY);
  }

  /**
   * Return whether the given resource location is a URL: either a
   * special "classpath" or "classpath*" pseudo URL or a standard URL.
   *
   * @param resourceLocation the location String to check
   * @return whether the location qualifies as a URL
   * @see PatternResourceLoader#CLASSPATH_ALL_URL_PREFIX
   * @see ResourceLoader#CLASSPATH_URL_PREFIX
   * @see ResourceUtils#isUrl(String)
   * @see java.net.URL
   */
  static boolean isUrl(@Nullable String resourceLocation) {
    return resourceLocation != null && (resourceLocation.startsWith(CLASSPATH_ALL_URL_PREFIX) || ResourceUtils.isUrl(resourceLocation));
  }

  /**
   * Return a default {@link PatternResourceLoader} for the given {@link ResourceLoader}.
   * <p>This might be the {@code ResourceLoader} itself, if it implements the
   * {@code ResourcePatternResolver} extension, or a default
   * {@link PathMatchingPatternResourceLoader} built on the given {@code ResourceLoader}.
   *
   * @param resourceLoader the ResourceLoader to build a pattern resolver for
   * (may be {@code null} to indicate a default ResourceLoader)
   * @return the ResourcePatternResolver
   * @see PathMatchingPatternResourceLoader
   */
  static PatternResourceLoader fromResourceLoader(@Nullable ResourceLoader resourceLoader) {
    if (resourceLoader instanceof PatternResourceLoader) {
      return (PatternResourceLoader) resourceLoader;
    }
    else if (resourceLoader != null) {
      return new PathMatchingPatternResourceLoader(resourceLoader);
    }
    else {
      return new PathMatchingPatternResourceLoader();
    }
  }

}
