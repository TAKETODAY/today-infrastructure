/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.cache.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.core.serializer.support.SerializationDelegate;
import cn.taketoday.lang.Nullable;

/**
 * {@link CacheManager} implementation that lazily builds {@link ConcurrentMapCache}
 * instances for each {@link #getCache} request. Also supports a 'static' mode where
 * the set of cache names is pre-defined through {@link #setCacheNames}, with no
 * dynamic creation of further cache regions at runtime.
 *
 * <p>Supports the asynchronous {@link Cache#retrieve(Object)} and
 * {@link Cache#retrieve(Object, Supplier)} operations through basic
 * {@code CompletableFuture} adaptation, with early-determined cache misses.
 *
 * <p>Note: This is by no means a sophisticated CacheManager; it comes with no
 * cache configuration options. However, it may be useful for testing or simple
 * caching scenarios. For advanced local caching needs, consider
 * {@link cn.taketoday.cache.support.CaffeineCacheManager} or
 * {@link cn.taketoday.cache.jcache.JCacheCacheManager}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ConcurrentMapCache
 * @since 4.0
 */
public class ConcurrentMapCacheManager implements CacheManager, BeanClassLoaderAware {

  private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

  private boolean dynamic = true;

  private boolean allowNullValues = true;

  private boolean storeByValue = false;

  @Nullable
  private SerializationDelegate serialization;

  /**
   * Construct a dynamic ConcurrentMapCacheManager,
   * lazily creating cache instances as they are being requested.
   */
  public ConcurrentMapCacheManager() { }

  /**
   * Construct a static ConcurrentMapCacheManager,
   * managing caches for the specified cache names only.
   */
  public ConcurrentMapCacheManager(String... cacheNames) {
    setCacheNames(Arrays.asList(cacheNames));
  }

  /**
   * Specify the set of cache names for this CacheManager's 'static' mode.
   * <p>The number of caches and their names will be fixed after a call to this method,
   * with no creation of further cache regions at runtime.
   * <p>Calling this with a {@code null} collection argument resets the
   * mode to 'dynamic', allowing for further creation of caches again.
   */
  public void setCacheNames(@Nullable Collection<String> cacheNames) {
    if (cacheNames != null) {
      for (String name : cacheNames) {
        this.cacheMap.put(name, createConcurrentMapCache(name));
      }
      this.dynamic = false;
    }
    else {
      this.dynamic = true;
    }
  }

  /**
   * Specify whether to accept and convert {@code null} values for all caches
   * in this cache manager.
   * <p>Default is "true", despite ConcurrentHashMap itself not supporting {@code null}
   * values. An internal holder object will be used to store user-level {@code null}s.
   * <p>Note: A change of the null-value setting will reset all existing caches,
   * if any, to reconfigure them with the new null-value requirement.
   */
  public void setAllowNullValues(boolean allowNullValues) {
    if (allowNullValues != this.allowNullValues) {
      this.allowNullValues = allowNullValues;
      // Need to recreate all Cache instances with the new null-value configuration...
      recreateCaches();
    }
  }

  /**
   * Return whether this cache manager accepts and converts {@code null} values
   * for all of its caches.
   */
  public boolean isAllowNullValues() {
    return this.allowNullValues;
  }

  /**
   * Specify whether this cache manager stores a copy of each entry ({@code true}
   * or the reference ({@code false} for all of its caches.
   * <p>Default is "false" so that the value itself is stored and no serializable
   * contract is required on cached values.
   * <p>Note: A change of the store-by-value setting will reset all existing caches,
   * if any, to reconfigure them with the new store-by-value requirement.
   *
   * @since 4.0
   */
  public void setStoreByValue(boolean storeByValue) {
    if (storeByValue != this.storeByValue) {
      this.storeByValue = storeByValue;
      // Need to recreate all Cache instances with the new store-by-value configuration...
      recreateCaches();
    }
  }

  /**
   * Return whether this cache manager stores a copy of each entry or
   * a reference for all its caches. If store by value is enabled, any
   * cache entry must be serializable.
   *
   * @since 4.0
   */
  public boolean isStoreByValue() {
    return this.storeByValue;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.serialization = new SerializationDelegate(classLoader);
    // Need to recreate all Cache instances with new ClassLoader in store-by-value mode...
    if (isStoreByValue()) {
      recreateCaches();
    }
  }

  @Override
  public Collection<String> getCacheNames() {
    return Collections.unmodifiableSet(this.cacheMap.keySet());
  }

  @Override
  @Nullable
  public Cache getCache(String name) {
    Cache cache = this.cacheMap.get(name);
    if (cache == null && this.dynamic) {
      synchronized(this.cacheMap) {
        cache = this.cacheMap.get(name);
        if (cache == null) {
          cache = createConcurrentMapCache(name);
          this.cacheMap.put(name, cache);
        }
      }
    }
    return cache;
  }

  private void recreateCaches() {
    for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
      entry.setValue(createConcurrentMapCache(entry.getKey()));
    }
  }

  /**
   * Create a new ConcurrentMapCache instance for the specified cache name.
   *
   * @param name the name of the cache
   * @return the ConcurrentMapCache (or a decorator thereof)
   */
  protected Cache createConcurrentMapCache(String name) {
    SerializationDelegate actualSerialization = (isStoreByValue() ? this.serialization : null);
    return new ConcurrentMapCache(name, new ConcurrentHashMap<>(256), isAllowNullValues(), actualSerialization);
  }

}
