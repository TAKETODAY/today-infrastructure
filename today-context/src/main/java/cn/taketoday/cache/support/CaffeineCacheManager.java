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
package cn.taketoday.cache.support;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link CacheManager} implementation that lazily builds {@link CaffeineCache}
 * instances for each {@link #getCache} request. Also supports a 'static' mode
 * where the set of cache names is pre-defined through {@link #setCacheNames},
 * with no dynamic creation of further cache regions at runtime.
 *
 * <p>The configuration of the underlying cache can be fine-tuned through a
 * {@link Caffeine} builder or {@link CaffeineSpec}, passed into this
 * CacheManager through {@link #setCaffeine}/{@link #setCaffeineSpec}.
 * A {@link CaffeineSpec}-compliant expression value can also be applied
 * via the {@link #setCacheSpecification "cacheSpecification"} bean property.
 *
 * <p>Requires Caffeine 2.1 or higher.
 *
 * @author Ben Manes
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author TODAY
 * @see CaffeineCache
 * @since 2020-08-15 20:05
 */
public class CaffeineCacheManager implements CacheManager {

  private Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();

  @Nullable
  private CacheLoader<Object, Object> cacheLoader;

  private boolean allowNullValues = true;

  private boolean dynamic = true;

  private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>(16);

  private final Collection<String> customCacheNames = new CopyOnWriteArrayList<>();

  /**
   * Construct a dynamic CaffeineCacheManager,
   * lazily creating cache instances as they are being requested.
   */
  public CaffeineCacheManager() {
  }

  /**
   * Construct a static CaffeineCacheManager,
   * managing caches for the specified cache names only.
   */
  public CaffeineCacheManager(String... cacheNames) {
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
        this.cacheMap.put(name, createCaffeineCache(name));
      }
      this.dynamic = false;
    }
    else {
      this.dynamic = true;
    }
  }

  /**
   * Set the Caffeine to use for building each individual
   * {@link CaffeineCache} instance.
   *
   * @see #createNativeCaffeineCache
   * @see com.github.benmanes.caffeine.cache.Caffeine#build()
   */
  public void setCaffeine(Caffeine<Object, Object> caffeine) {
    Assert.notNull(caffeine, "Caffeine must not be null");
    doSetCaffeine(caffeine);
  }

  /**
   * Set the {@link CaffeineSpec} to use for building each individual
   * {@link CaffeineCache} instance.
   *
   * @see #createNativeCaffeineCache
   * @see com.github.benmanes.caffeine.cache.Caffeine#from(CaffeineSpec)
   */
  public void setCaffeineSpec(CaffeineSpec caffeineSpec) {
    doSetCaffeine(Caffeine.from(caffeineSpec));
  }

  /**
   * Set the Caffeine cache specification String to use for building each
   * individual {@link CaffeineCache} instance. The given value needs to
   * comply with Caffeine's {@link CaffeineSpec} (see its javadoc).
   *
   * @see #createNativeCaffeineCache
   * @see com.github.benmanes.caffeine.cache.Caffeine#from(String)
   */
  public void setCacheSpecification(String cacheSpecification) {
    doSetCaffeine(Caffeine.from(cacheSpecification));
  }

  private void doSetCaffeine(Caffeine<Object, Object> cacheBuilder) {
    if (!ObjectUtils.nullSafeEquals(this.cacheBuilder, cacheBuilder)) {
      this.cacheBuilder = cacheBuilder;
      refreshCommonCaches();
    }
  }

  /**
   * Set the Caffeine CacheLoader to use for building each individual
   * {@link CaffeineCache} instance, turning it into a LoadingCache.
   *
   * @see #createNativeCaffeineCache
   * @see com.github.benmanes.caffeine.cache.Caffeine#build(CacheLoader)
   * @see com.github.benmanes.caffeine.cache.LoadingCache
   */
  public void setCacheLoader(CacheLoader<Object, Object> cacheLoader) {
    if (!ObjectUtils.nullSafeEquals(this.cacheLoader, cacheLoader)) {
      this.cacheLoader = cacheLoader;
      refreshCommonCaches();
    }
  }

  /**
   * Specify whether to accept and convert {@code null} values for all caches
   * in this cache manager.
   * <p>Default is "true", despite Caffeine itself not supporting {@code null} values.
   * An internal holder object will be used to store user-level {@code null}s.
   */
  public void setAllowNullValues(boolean allowNullValues) {
    if (this.allowNullValues != allowNullValues) {
      this.allowNullValues = allowNullValues;
      refreshCommonCaches();
    }
  }

  /**
   * Return whether this cache manager accepts and converts {@code null} values
   * for all of its caches.
   */
  public boolean isAllowNullValues() {
    return this.allowNullValues;
  }

  @Override
  public Collection<String> getCacheNames() {
    return Collections.unmodifiableSet(this.cacheMap.keySet());
  }

  @Override
  @Nullable
  public Cache getCache(String name) {
    return this.cacheMap.computeIfAbsent(name, cacheName ->
            this.dynamic ? createCaffeineCache(cacheName) : null);
  }

  /**
   * Register the given native Caffeine Cache instance with this cache manager,
   * adapting it to Framework's cache API for exposure through {@link #getCache}.
   * Any number of such custom caches may be registered side by side.
   * <p>This allows for custom settings per cache (as opposed to all caches
   * sharing the common settings in the cache manager's configuration) and
   * is typically used with the Caffeine builder API:
   * {@code registerCustomCache("myCache", Caffeine.newBuilder().maximumSize(10).build())}
   * <p>Note that any other caches, whether statically specified through
   * {@link #setCacheNames} or dynamically built on demand, still operate
   * with the common settings in the cache manager's configuration.
   *
   * @param name the name of the cache
   * @param cache the custom Caffeine Cache instance to register
   * @see #adaptCaffeineCache
   */
  public void registerCustomCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
    this.customCacheNames.add(name);
    this.cacheMap.put(name, adaptCaffeineCache(name, cache));
  }

  /**
   * Adapt the given new native Caffeine Cache instance to Framework's {@link Cache}
   * abstraction for the specified cache name.
   *
   * @param name the name of the cache
   * @param cache the native Caffeine Cache instance
   * @return the FrameworkCaffeineCache adapter (or a decorator thereof)
   * @see CaffeineCache
   * @see #isAllowNullValues()
   */
  protected Cache adaptCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
    return new CaffeineCache(name, cache, isAllowNullValues());
  }

  /**
   * Build a common {@link CaffeineCache} instance for the specified cache name,
   * using the common Caffeine configuration specified on this cache manager.
   * <p>Delegates to {@link #adaptCaffeineCache} as the adaptation method to
   * Framework's cache abstraction (allowing for centralized decoration etc),
   * passing in a freshly built native Caffeine Cache instance.
   *
   * @param name the name of the cache
   * @return the FrameworkCaffeineCache adapter (or a decorator thereof)
   * @see #adaptCaffeineCache
   * @see #createNativeCaffeineCache
   */
  protected Cache createCaffeineCache(String name) {
    return adaptCaffeineCache(name, createNativeCaffeineCache(name));
  }

  /**
   * Build a common Caffeine Cache instance for the specified cache name,
   * using the common Caffeine configuration specified on this cache manager.
   *
   * @param name the name of the cache
   * @return the native Caffeine Cache instance
   * @see #createCaffeineCache
   */
  protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(String name) {
    return (this.cacheLoader != null ? this.cacheBuilder.build(this.cacheLoader) : this.cacheBuilder.build());
  }

  /**
   * Recreate the common caches with the current state of this manager.
   */
  private void refreshCommonCaches() {
    for (Map.Entry<String, Cache> entry : this.cacheMap.entrySet()) {
      if (!this.customCacheNames.contains(entry.getKey())) {
        entry.setValue(createCaffeineCache(entry.getKey()));
      }
    }
  }

}
