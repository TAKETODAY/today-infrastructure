/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core.io;

import org.jspecify.annotations.Nullable;

import infra.util.ClassUtils;

/**
 * Strategy interface for resolving a location pattern (for example, an
 * Ant-style path pattern) into Resource objects.
 *
 * <p>
 * Can be used with any sort of location pattern (e.g.
 * "/WEB-INF/*-context.xml"): Input patterns have to match the strategy
 * implementation. This interface just specifies the conversion method rather
 * than a specific pattern format.
 *
 * <p>
 * This interface also suggests a new resource prefix "classpath*:" for all
 * matching resources from the class path. Note that the resource location is
 * expected to be a path without placeholders in this case (e.g. "/beans.xml");
 * JAR files or classes directories can contain multiple files of the same name.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PatternResourceLoader
 * @since 2.1.7 2019-12-05 12:52
 */
public interface ResourceLoader {

  /**
   * Pseudo URL prefix for loading from the class path: "classpath:".
   */
  String CLASSPATH_URL_PREFIX = "classpath:";

  /**
   * Return a Resource handle for the specified resource location.
   * <p>
   * The handle should always be a reusable resource descriptor, allowing for
   * multiple {@link Resource#getInputStream()} calls.
   * <p>
   * <ul>
   * <li>Must support fully qualified URLs, e.g. "file:C:/test.dat".
   * <li>Must support classpath pseudo-URLs, e.g. "classpath:test.dat".
   * <li>Should support relative file paths, e.g. "WEB-INF/test.dat". (This will
   * be implementation-specific, typically provided by an ApplicationContext
   * implementation.)
   * </ul>
   * <p>
   * Note that a Resource handle does not imply an existing resource; you need to
   * invoke {@link Resource#exists} to check for existence.
   *
   * @param location the resource location
   * @return a corresponding Resource handle (never {@code null})
   * @see #CLASSPATH_URL_PREFIX
   * @see Resource#exists()
   * @see Resource#getInputStream()
   */
  Resource getResource(String location);

  /**
   * Expose the {@link ClassLoader} used by this {@code ResourceLoader}.
   * <p>Clients which need to access the {@code ClassLoader} directly can do so
   * in a uniform manner with the {@code ResourceLoader}, rather than relying
   * on the thread context {@code ClassLoader}.
   *
   * @return the {@code ClassLoader}
   * (only {@code null} if even the system {@code ClassLoader} isn't accessible)
   * @see ClassUtils#getDefaultClassLoader()
   * @see ClassUtils#forName(String, ClassLoader)
   */
  @Nullable
  ClassLoader getClassLoader();

}
