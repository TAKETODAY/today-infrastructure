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

package infra.core.io;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Set;

import infra.lang.Modifiable;
import infra.util.ResourceUtils;

/**
 * Strategy interface for resolving a location pattern (for example,
 * an Ant-style path pattern) into {@link Resource} objects.
 *
 * <p>This is an extension to the {@link ResourceLoader}
 * interface. A passed-in {@code ResourceLoader} (for example, an
 * {@link infra.context.ApplicationContext} passed in via
 * {@link infra.context.ResourceLoaderAware} when running in a context)
 * can be checked whether it implements this extended interface too.
 *
 * <p>{@link PathMatchingPatternResourceLoader} is a standalone implementation
 * that is usable outside an {@code ApplicationContext}.
 *
 * <p>Can be used with any sort of location pattern (e.g. "/WEB-INF/*-context.xml"):
 * Input patterns have to match the strategy implementation. This interface just
 * specifies the conversion method rather than a specific pattern format.
 *
 * <p>This interface also defines a {@value #CLASSPATH_ALL_URL_PREFIX} resource
 * prefix for all matching resources from the module path and the class path. Note
 * that the resource location may also contain placeholders &mdash; for example
 * {@code "/beans-*.xml"}. JAR files or different directories in the module path
 * or class path can contain multiple files of the same name.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Resource
 * @see ResourceLoader
 * @see infra.context.ApplicationContext
 * @see infra.context.ResourceLoaderAware
 * @since 4.0 2021/10/7 17:26
 */
public interface PatternResourceLoader extends ResourceLoader {

  /**
   * Pseudo URL prefix for all matching resources from the class path: {@code "classpath*:"}.
   * <p>This differs from ResourceLoader's {@code "classpath:"} URL prefix in
   * that it retrieves all matching resources for a given path &mdash; for
   * example, to locate all "beans.xml" files in the root of all deployed JAR
   * files you can use the location pattern {@code "classpath*:/beans.xml"}.
   * <p>As of 4.0, the semantics for the {@code "classpath*:"}
   * prefix have been expanded to include the module path as well as the class path.
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
  @Modifiable
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
   * Scan the given location pattern into {@code Resource} objects.
   * <p>Overlapping resource entries that point to the same physical
   * resource should be avoided, as far as possible. The result should
   * have set semantics.
   *
   * @param locationPattern the location pattern to resolve
   * @throws IOException in case of I/O errors
   * @since 5.0
   */
  void scan(String locationPattern, SmartResourceConsumer consumer) throws IOException;

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
   * {@code PatternResourceLoader} extension, or a default
   * {@link PathMatchingPatternResourceLoader} built on the given {@code ResourceLoader}.
   *
   * @param resourceLoader the ResourceLoader to build a pattern resolver for
   * (may be {@code null} to indicate a default ResourceLoader)
   * @return the PatternResourceLoader
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
