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

package cn.taketoday.test.context.cache;

import cn.taketoday.core.SpringProperties;
import cn.taketoday.util.StringUtils;

/**
 * Collection of utilities for working with {@link ContextCache ContextCaches}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class ContextCacheUtils {

  /**
   * Retrieve the maximum size of the {@link ContextCache}.
   * <p>Uses {@link SpringProperties} to retrieve a system property or Spring
   * property named {@code spring.test.context.cache.maxSize}.
   * <p>Falls back to the value of the {@link ContextCache#DEFAULT_MAX_CONTEXT_CACHE_SIZE}
   * if no such property has been set or if the property is not an integer.
   *
   * @return the maximum size of the context cache
   * @see ContextCache#MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME
   */
  public static int retrieveMaxCacheSize() {
    try {
      String maxSize = SpringProperties.getProperty(ContextCache.MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME);
      if (StringUtils.hasText(maxSize)) {
        return Integer.parseInt(maxSize.trim());
      }
    }
    catch (Exception ex) {
      // ignore
    }

    // Fallback
    return ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
  }

}
