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

package infra.context.index;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import infra.core.io.PropertiesUtils;
import infra.core.io.UrlResource;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ConcurrentReferenceHashMap;

/**
 * Candidate components index loading mechanism for internal use within the framework.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/9 21:35
 */
public final class CandidateComponentsIndexLoader {

  /**
   * The location to look for components.
   * <p>Can be present in multiple JAR files.
   */
  public static final String COMPONENTS_RESOURCE_LOCATION = "META-INF/today.components";

  /**
   * System property that instructs framework to ignore the components index, i.e.
   * to always return {@code null} from {@link #loadIndex(ClassLoader)}.
   * <p>The default is "false", allowing for regular use of the index. Switching this
   * flag to {@code true} fulfills a corner case scenario when an index is partially
   * available for some libraries (or use cases) but couldn't be built for the whole
   * application. In this case, the application context fallbacks to a regular
   * classpath arrangement (i.e. as though no index were present at all).
   */
  public static final String IGNORE_INDEX = "today.index.ignore";

  private static final boolean shouldIgnoreIndex = TodayStrategies.getFlag(IGNORE_INDEX);

  private static final Logger log = LoggerFactory.getLogger(CandidateComponentsIndexLoader.class);

  private static final ConcurrentMap<ClassLoader, CandidateComponentsIndex> cache =
          new ConcurrentReferenceHashMap<>();

  private CandidateComponentsIndexLoader() {
  }

  /**
   * Load and instantiate the {@link CandidateComponentsIndex} from
   * {@value #COMPONENTS_RESOURCE_LOCATION}, using the given class loader. If no
   * index is available, return {@code null}.
   *
   * @param classLoader the ClassLoader to use for loading (can be {@code null} to use the default)
   * @return the index to use or {@code null} if no index was found
   * @throws IllegalArgumentException if any module index cannot
   * be loaded or if an error occurs while creating {@link CandidateComponentsIndex}
   */
  public static @Nullable CandidateComponentsIndex loadIndex(@Nullable ClassLoader classLoader) {
    ClassLoader classLoaderToUse = classLoader;
    if (classLoaderToUse == null) {
      classLoaderToUse = CandidateComponentsIndexLoader.class.getClassLoader();
    }
    return cache.computeIfAbsent(classLoaderToUse, CandidateComponentsIndexLoader::doLoadIndex);
  }

  private static @Nullable CandidateComponentsIndex doLoadIndex(ClassLoader classLoader) {
    if (shouldIgnoreIndex) {
      return null;
    }

    try {
      Enumeration<URL> urls = classLoader.getResources(COMPONENTS_RESOURCE_LOCATION);
      if (!urls.hasMoreElements()) {
        return null;
      }
      List<Properties> result = new ArrayList<>();
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        Properties properties = PropertiesUtils.loadProperties(new UrlResource(url));
        result.add(properties);
      }
      if (log.isDebugEnabled()) {
        log.debug("Loaded {} index(es)", result.size());
      }
      int totalCount = result.stream().mapToInt(Properties::size).sum();
      return totalCount > 0 ? new CandidateComponentsIndex(result) : null;
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to load indexes from location [%s]".formatted(COMPONENTS_RESOURCE_LOCATION), ex);
    }
  }

  /**
   * Programmatically add the given index instance for the given ClassLoader,
   * replacing a file-determined index with a programmatically composed index.
   * <p>The index instance will usually be pre-populated for AOT runtime setups
   * or test scenarios with pre-configured results for runtime-attempted scans.
   * Alternatively, it may be empty for it to get populated during AOT processing
   * or a test run, for subsequent introspection the index-recorded candidate types.
   *
   * @param classLoader the ClassLoader to add the index for
   * @param index the associated CandidateComponentsIndex instance
   * @since 5.0
   */
  public static void addIndex(ClassLoader classLoader, CandidateComponentsIndex index) {
    cache.put(classLoader, index);
  }

  /**
   * Clear the runtime index cache.
   *
   * @since 5.0
   */
  public static void clearCache() {
    cache.clear();
  }

}
