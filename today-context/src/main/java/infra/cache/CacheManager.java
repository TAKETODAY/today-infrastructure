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
package infra.cache;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Framework's central cache manager SPI.
 *
 * <p>Allows for retrieving named {@link Cache} regions.
 *
 * @author Costin Leau
 * @author Sam Brannen
 * @author TODAY
 * @since 2019-01-02 22:44
 */
public interface CacheManager {

  /**
   * Get the cache associated with the given name.
   * <p>Note that the cache may be lazily created at runtime if the
   * native provider supports it.
   *
   * @param name the cache identifier (must not be {@code null})
   * @return the associated cache, or {@code null} if such a cache
   * does not exist or could be not created
   */
  @Nullable
  Cache getCache(String name);

  /**
   * Get a collection of the cache names known by this manager.
   *
   * @return the names of all caches known by the cache manager
   */
  Collection<String> getCacheNames();

  /**
   * Remove all registered caches from this cache manager if possible,
   * re-creating them on demand. After this call, {@link #getCacheNames()}
   * will possibly be empty and the cache provider will have dropped all
   * cache management state.
   * <p>Alternatively, an implementation may perform an equivalent reset
   * on fixed existing cache regions without actually dropping the cache.
   * This behavior will be indicated by {@link #getCacheNames()} still
   * exposing a non-empty set of names, whereas the corresponding cache
   * regions will not contain cache entries anymore.
   * <p>The default implementation calls {@link Cache#clear} on all
   * registered caches, retaining all caches as registered, satisfying
   * the alternative implementation path above. Custom implementations
   * may either drop the actual caches (re-creating them on demand) or
   * perform a more exhaustive reset at the actual cache provider level.
   *
   * @see Cache#clear()
   * @since 5.0
   */
  default void resetCaches() {
    for (String cacheName : getCacheNames()) {
      Cache cache = getCache(cacheName);
      if (cache != null) {
        cache.clear();
      }
    }
  }
}
