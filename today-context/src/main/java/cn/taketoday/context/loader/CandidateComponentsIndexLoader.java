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

package cn.taketoday.context.loader;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import cn.taketoday.core.io.PropertiesUtils;
import cn.taketoday.core.io.UrlResource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ConcurrentReferenceHashMap;

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

  private CandidateComponentsIndexLoader() { }

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
  @Nullable
  public static CandidateComponentsIndex loadIndex(@Nullable ClassLoader classLoader) {
    ClassLoader classLoaderToUse = classLoader;
    if (classLoaderToUse == null) {
      classLoaderToUse = CandidateComponentsIndexLoader.class.getClassLoader();
    }
    return cache.computeIfAbsent(classLoaderToUse, CandidateComponentsIndexLoader::doLoadIndex);
  }

  @Nullable
  private static CandidateComponentsIndex doLoadIndex(ClassLoader classLoader) {
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
      throw new IllegalStateException(
              "Unable to load indexes from location [" + COMPONENTS_RESOURCE_LOCATION + "]", ex);
    }
  }

}
