/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.cache;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.Collection;

import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.context.utils.Assert;

/**
 * {@link CacheManager} implementation that lazily builds {@link CaffeineCache}
 * instances for each {@link #getCache} request. Also supports a 'static' mode
 * where the set of cache names is pre-defined through {@link #setCacheNames} or
 * {@link #setCacheConfig(Collection)}, with no dynamic creation of further
 * cache regions at runtime that you do not call
 * {@link #setDynamicCreation(boolean)}.
 *
 * <p>
 * The configuration of the underlying cache can be fine-tuned through a
 * {@link Caffeine} builder or {@link CaffeineSpec}, passed into this
 * CacheManager through {@link #setCaffeine}/{@link #setCaffeineSpec}. A
 * {@link CaffeineSpec}-compliant expression value can also be applied via the
 * {@link #setCacheSpecification "cacheSpecification"} bean property.
 *
 * <p>
 * Requires Caffeine 2.1 or higher.
 *
 * @author TODAY <br>
 * 2020-08-15 20:05
 * @see CaffeineCache
 * @since 3.0
 */
public class CaffeineCacheManager extends AbstractCacheManager {

  private CacheLoader<Object, Object> cacheLoader;
  private Caffeine<Object, Object> caffeine = Caffeine.newBuilder();

  /**
   * Build a common {@link CaffeineCache} instance for the specified cache name,
   * using the common Caffeine configuration specified on this cache manager.
   * <p>
   * Delegates to {@link #adaptCaffeineCache} as the adaptation method to cache
   * abstraction (allowing for centralized decoration etc), passing in a freshly
   * built native Caffeine Cache instance.
   *
   * @param name
   *         the name of the cache
   *
   * @return the {@link CaffeineCache} adapter (or a decorator thereof)
   *
   * @see #adaptCaffeineCache
   * @see #createNativeCaffeineCache
   */
  @Override
  protected Cache doCreate(String name, CacheConfig cacheConfig) {
    if (isDefaultConfig(cacheConfig)) {
      return adaptCaffeineCache(name, createNativeCaffeineCache());
    }

    final Caffeine<Object, Object> caffeine =
            Caffeine.newBuilder()
                    .expireAfterWrite(cacheConfig.expire(), cacheConfig.timeUnit());

    final int maxSize = cacheConfig.maxSize();
    if (maxSize != 0) {
      caffeine.maximumSize(maxSize);
    }
    return adaptCaffeineCache(name, createNativeCaffeineCache(caffeine));
  }

  /**
   * Register the given native Caffeine Cache instance with this cache manager,
   * adapting it to cache API for exposure through {@link #getCache}. Any number
   * of such custom caches may be registered side by side.
   * <p>
   * This allows for custom settings per cache (as opposed to all caches sharing
   * the common settings in the cache manager's configuration) and is typically
   * used with the Caffeine builder API:
   * {@code registerCustomCache("myCache", Caffeine.newBuilder().maximumSize(10).build())}
   * <p>
   * Note that any other caches, whether statically specified through
   * {@link #setCacheNames} or dynamically built on demand, still operate with the
   * common settings in the cache manager's configuration.
   *
   * @param name
   *         the name of the cache
   * @param cache
   *         the custom Caffeine Cache instance to register
   *
   * @see #adaptCaffeineCache
   */
  public void registerCustomCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
    registerCustomCache(name, adaptCaffeineCache(name, cache));
  }

  /**
   * Set the Caffeine to use for building each individual {@link CaffeineCache}
   * instance.
   *
   * @see #createNativeCaffeineCache
   * @see com.github.benmanes.caffeine.cache.Caffeine#build()
   */
  public void setCaffeine(Caffeine<Object, Object> caffeine) {
    Assert.notNull(caffeine, "Caffeine must not be null");
    this.caffeine = caffeine;
  }

  /**
   * Set the {@link CaffeineSpec} to use for building each individual
   * {@link CaffeineCache} instance.
   *
   * @see #createNativeCaffeineCache
   * @see com.github.benmanes.caffeine.cache.Caffeine#from(CaffeineSpec)
   */
  public void setCaffeineSpec(CaffeineSpec caffeineSpec) {
    setCaffeine(Caffeine.from(caffeineSpec));
  }

  /**
   * Set the Caffeine cache specification String to use for building each
   * individual {@link CaffeineCache} instance. The given value needs to comply
   * with Caffeine's {@link CaffeineSpec} (see its javadoc).
   *
   * @see #createNativeCaffeineCache
   * @see com.github.benmanes.caffeine.cache.Caffeine#from(String)
   */
  public void setCacheSpecification(String cacheSpecification) {
    setCaffeine(Caffeine.from(cacheSpecification));
  }

  /**
   * Set the Caffeine CacheLoader to use for building each individual
   * {@link CaffeineCache} instance, turning it into a LoadingCache.
   *
   * @see #createNativeCaffeineCache
   * @see Caffeine#build(CacheLoader)
   * @see LoadingCache
   */
  public void setCacheLoader(CacheLoader<Object, Object> cacheLoader) {
    this.cacheLoader = cacheLoader;
    refreshCaches();
  }

  /**
   * Adapt the given new native Caffeine Cache instance to {@link Cache}
   * abstraction for the specified cache name.
   *
   * @param name
   *         the name of the cache
   * @param cache
   *         the native Caffeine Cache instance
   *
   * @return the CaffeineCache adapter (or a decorator thereof)
   *
   * @see CaffeineCache
   */
  protected Cache adaptCaffeineCache(String name, com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
    return new CaffeineCache(name, cache);
  }

  /**
   * Build a common Caffeine Cache instance for the specified cache name, using
   * the common Caffeine configuration specified on this cache manager.
   *
   * @return the native Caffeine Cache instance
   *
   * @see #adaptCaffeineCache
   */
  protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache() {
    return createNativeCaffeineCache(caffeine);
  }

  protected com.github.benmanes.caffeine.cache.Cache<Object, Object> createNativeCaffeineCache(
          Caffeine<Object, Object> caffeine) {
    return (this.cacheLoader != null ? caffeine.build(this.cacheLoader) : caffeine.build());
  }

}
