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

package cn.taketoday.test.context.cache;

import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.test.context.CacheAwareContextLoaderDelegate;
import cn.taketoday.util.StringUtils;

/**
 * Collection of utilities for working with {@link ContextCache ContextCaches}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ContextCacheUtils {

  /**
   * Retrieve the maximum size of the {@link ContextCache}.
   * <p>Uses {@link TodayStrategies} to retrieve a system property or Infra
   * property named {@value ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME}.
   * <p>Defaults to {@value ContextCache#DEFAULT_MAX_CONTEXT_CACHE_SIZE}
   * if no such property has been set or if the property is not an integer.
   *
   * @return the maximum size of the context cache
   * @see ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
   */
  public static int retrieveMaxCacheSize() {
    String propertyName = ContextCache.MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME;
    int defaultValue = ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
    return retrieveProperty(propertyName, defaultValue);
  }

  /**
   * Retrieve the <em>failure threshold</em> for application context loading.
   * <p>Uses {@link TodayStrategies} to retrieve a system property or Infra
   * property named {@value CacheAwareContextLoaderDelegate#CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME}.
   * <p>Defaults to {@value CacheAwareContextLoaderDelegate#DEFAULT_CONTEXT_FAILURE_THRESHOLD}
   * if no such property has been set or if the property is not an integer.
   *
   * @return the failure threshold
   * @see CacheAwareContextLoaderDelegate#CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME
   * @see CacheAwareContextLoaderDelegate#DEFAULT_CONTEXT_FAILURE_THRESHOLD
   */
  public static int retrieveContextFailureThreshold() {
    String propertyName = CacheAwareContextLoaderDelegate.CONTEXT_FAILURE_THRESHOLD_PROPERTY_NAME;
    int defaultValue = CacheAwareContextLoaderDelegate.DEFAULT_CONTEXT_FAILURE_THRESHOLD;
    return retrieveProperty(propertyName, defaultValue);
  }

  private static int retrieveProperty(String key, int defaultValue) {
    try {
      String value = TodayStrategies.getProperty(key);
      if (StringUtils.hasText(value)) {
        return Integer.parseInt(value.trim());
      }
    }
    catch (Exception ex) {
      // ignore
    }

    // Fallback
    return defaultValue;
  }

}
