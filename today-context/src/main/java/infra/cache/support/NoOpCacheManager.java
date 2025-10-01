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

package infra.cache.support;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import infra.cache.Cache;
import infra.cache.CacheManager;

/**
 * A basic, no operation {@link CacheManager} implementation suitable
 * for disabling caching, typically used for backing cache declarations
 * without an actual backing store.
 *
 * <p>Will simply accept any items into the cache not actually storing them.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/26 14:18
 */
public class NoOpCacheManager implements CacheManager {

  private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>(16);

  private final Set<String> cacheNames = new LinkedHashSet<>(16);

  /**
   * This implementation always returns a {@link Cache} implementation that will not store items.
   * Additionally, the request cache will be remembered by the manager for consistency.
   */
  @Override
  @Nullable
  public Cache getCache(String name) {
    Cache cache = this.caches.get(name);
    if (cache == null) {
      this.caches.computeIfAbsent(name, key -> new NoOpCache(name));
      synchronized(this.cacheNames) {
        this.cacheNames.add(name);
      }
    }

    return this.caches.get(name);
  }

  /**
   * This implementation returns the name of the caches previously requested.
   */
  @Override
  public Collection<String> getCacheNames() {
    synchronized(this.cacheNames) {
      return Collections.unmodifiableSet(this.cacheNames);
    }
  }

}
