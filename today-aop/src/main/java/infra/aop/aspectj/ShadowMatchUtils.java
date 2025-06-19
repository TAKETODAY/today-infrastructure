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

package infra.aop.aspectj;

import org.aspectj.weaver.tools.ShadowMatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.lang.Nullable;

/**
 * Internal {@link ShadowMatch} utilities.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public abstract class ShadowMatchUtils {

  private static final Map<Object, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(256);

  /**
   * Find a {@link ShadowMatch} for the specified key.
   *
   * @param key the key to use
   * @return the {@code ShadowMatch} to use for the specified key,
   * or {@code null} if none found
   */
  @Nullable
  static ShadowMatch getShadowMatch(Object key) {
    return shadowMatchCache.get(key);
  }

  /**
   * Associate the {@link ShadowMatch} with the specified key.
   * If an entry already exists, the given {@code shadowMatch} is ignored.
   *
   * @param key the key to use
   * @param shadowMatch the shadow match to use for this key
   * if none already exists
   * @return the shadow match to use for the specified key
   */
  static ShadowMatch setShadowMatch(Object key, ShadowMatch shadowMatch) {
    ShadowMatch existing = shadowMatchCache.putIfAbsent(key, shadowMatch);
    return (existing != null ? existing : shadowMatch);
  }

  /**
   * Clear the cache of computed {@link ShadowMatch} instances.
   */
  public static void clearCache() {
    shadowMatchCache.clear();
  }

}
