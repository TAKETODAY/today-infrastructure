/*
 * Copyright 2017 - 2024 the original author or authors.
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
package cn.taketoday.cache.support;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base class implementing the common {@link CacheManager} methods.
 * Useful for 'static' environments where the backing caches do not change.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2020-08-15 19:18
 */
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {

  private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

  private volatile Set<String> cacheNames = Collections.emptySet();

  // Early cache initialization on startup

  @Override
  public void afterPropertiesSet() {
    initializeCaches();
  }

  /**
   * Initialize the static configuration of caches.
   * <p>Triggered on startup through {@link #afterPropertiesSet()};
   * can also be called to re-initialize at runtime.
   *
   * @see #loadCaches()
   * @since 4.0
   */
  public void initializeCaches() {
    Collection<? extends Cache> caches = loadCaches();

    synchronized(this.cacheMap) {
      this.cacheNames = Collections.emptySet();
      this.cacheMap.clear();
      Set<String> cacheNames = new LinkedHashSet<>(caches.size());
      for (Cache cache : caches) {
        String name = cache.getName();
        this.cacheMap.put(name, decorateCache(cache));
        cacheNames.add(name);
      }
      this.cacheNames = Collections.unmodifiableSet(cacheNames);
    }
  }

  /**
   * Load the initial caches for this cache manager.
   * <p>Called by {@link #afterPropertiesSet()} on startup.
   * The returned collection may be empty but must not be {@code null}.
   */
  protected abstract Collection<? extends Cache> loadCaches();

  // Lazy cache initialization on access

  @Override
  @Nullable
  public Cache getCache(String name) {
    // Quick check for existing cache...
    Cache cache = this.cacheMap.get(name);
    if (cache != null) {
      return cache;
    }

    // The provider may support on-demand cache creation...
    Cache missingCache = getMissingCache(name);
    if (missingCache != null) {
      // Fully synchronize now for missing cache registration
      synchronized(this.cacheMap) {
        cache = this.cacheMap.get(name);
        if (cache == null) {
          cache = decorateCache(missingCache);
          this.cacheMap.put(name, cache);
          updateCacheNames(name);
        }
      }
    }
    return cache;
  }

  @Override
  public Collection<String> getCacheNames() {
    return this.cacheNames;
  }

  // Common cache initialization delegates for subclasses

  /**
   * Check for a registered cache of the given name.
   * In contrast to {@link #getCache(String)}, this method does not trigger
   * the lazy creation of missing caches via {@link #getMissingCache(String)}.
   *
   * @param name the cache identifier (must not be {@code null})
   * @return the associated Cache instance, or {@code null} if none found
   * @see #getCache(String)
   * @see #getMissingCache(String)
   * @since 4.0
   */
  @Nullable
  protected final Cache lookupCache(String name) {
    return this.cacheMap.get(name);
  }

  /**
   * Update the exposed {@link #cacheNames} set with the given name.
   * <p>This will always be called within a full {@link #cacheMap} lock
   * and effectively behaves like a {@code CopyOnWriteArraySet} with
   * preserved order but exposed as an unmodifiable reference.
   *
   * @param name the name of the cache to be added
   */
  private void updateCacheNames(String name) {
    Set<String> cacheNames = new LinkedHashSet<>(this.cacheNames);
    cacheNames.add(name);
    this.cacheNames = Collections.unmodifiableSet(cacheNames);
  }

  // Overridable template methods for cache initialization

  /**
   * Decorate the given Cache object if necessary.
   *
   * @param cache the Cache object to be added to this CacheManager
   * @return the decorated Cache object to be used instead,
   * or simply the passed-in Cache object by default
   */
  protected Cache decorateCache(Cache cache) {
    return cache;
  }

  /**
   * Return a missing cache with the specified {@code name}, or {@code null} if
   * such a cache does not exist or could not be created on demand.
   * <p>Caches may be lazily created at runtime if the native provider supports it.
   * If a lookup by name does not yield any result, an {@code AbstractCacheManager}
   * subclass gets a chance to register such a cache at runtime. The returned cache
   * will be automatically added to this cache manager.
   *
   * @param name the name of the cache to retrieve
   * @return the missing cache, or {@code null} if no such cache exists or could be
   * created on demand
   * @see #getCache(String)
   * @since 4.0
   */
  @Nullable
  protected Cache getMissingCache(String name) {
    return null;
  }

}
