/**
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.lang.Assert;

/**
 * Abstract {@link CacheManager} implementation
 *
 * <p>
 * Abstract {@link CacheManager} implementation that lazily builds {@link Cache}
 * instances for each {@link #getCache(String, CacheConfig)} request. Also
 * supports a 'static' mode where the set of cache names is pre-defined through
 * {@link #setCacheNames(Collection)} or {@link #setCacheConfig(Collection)}, or
 * {@link #setCacheConfig(Map)}, with no dynamic creation of further cache
 * regions at runtime that you do not call {@link #setDynamicCreation(boolean)}.
 *
 * @author TODAY <br>
 * 2020-08-15 19:18
 */
public abstract class AbstractCacheManager implements CacheManager {

  private boolean dynamicCreation = true;
  private final HashMap<String, Cache> cacheMap = new HashMap<>(32);
  private final HashMap<String, CacheConfig> configMap = new HashMap<>();

  public AbstractCacheManager() { }

  public AbstractCacheManager(String... cacheNames) {
    Assert.notNull(cacheNames, "cacheNames s can't be null");
    setCacheNames(Arrays.asList(cacheNames));
  }

  /**
   * @since 3.0
   */
  public AbstractCacheManager(CacheConfig... config) {
    Assert.notNull(config, "cache config can't be null");
    setCacheConfig(Arrays.asList(config));
  }

  /**
   * Specify the set of cache names for this CacheManager's 'static' mode.
   * <p>
   * The number of caches and their names will be fixed after a call to this
   * method, with no creation of further cache regions at runtime that you do not
   * call {@link #setDynamicCreation(boolean)}.
   * <p>
   * Calling this with a {@code null} collection argument resets the mode to
   * 'dynamic', allowing for further creation of caches again.
   *
   * @since 3.0
   */
  public void setCacheNames(Collection<String> cacheNames) {
    Assert.notEmpty(cacheNames, "cacheNames must not be empty");
    setCacheConfig(cacheNames.stream()
                           .map(CacheConfiguration::new)
                           .collect(Collectors.toList()));
  }

  /**
   * Specify the set of cache config for this CacheManager's 'static' mode.
   * <p>
   * The number of caches and their names will be fixed after a call to this
   * method, with no creation of further cache regions at runtime that you do not
   * call {@link #setDynamicCreation(boolean)}.
   *
   * @see #setDynamicCreation(boolean)
   * @since 3.0
   */
  public void setCacheConfig(Collection<CacheConfig> cacheConfigs) {
    Assert.notEmpty(cacheConfigs, "cacheConfigs must not be empty");
    this.configMap.clear();
    for (final CacheConfig config : cacheConfigs) {
      this.configMap.put(config.cacheName(), config);
    }
    refreshCaches();
  }

  /**
   * Configure a new Map of {@link CacheConfig}
   * <p>
   * The number of caches and their names will be fixed after a call to this
   * method, with no creation of further cache regions at runtime that you do not
   * call {@link #setDynamicCreation(boolean)}.
   *
   * @param configMap
   *         Map of {@link CacheConfig}
   *
   * @see #setDynamicCreation(boolean)
   * @since 3.0
   */
  public void setCacheConfig(Map<String, CacheConfig> configMap) {
    Assert.notEmpty(configMap, "configMap must not be empty");
    this.configMap.clear();
    this.configMap.putAll(configMap);
    refreshCaches();
  }

  /**
   * Add {@link CacheConfig}
   *
   * @param config
   *         {@link CacheConfig}
   *
   * @since 3.0
   */
  public void addCacheConfig(CacheConfig config) {
    addCacheConfig(config.cacheName(), config);
  }

  /**
   * Add {@link CacheConfig} with given name
   *
   * @param name
   *         {@link Cache} name
   * @param config
   *         {@link CacheConfig}
   *
   * @since 3.0
   */
  public void addCacheConfig(String name, CacheConfig config) {
    Assert.notNull(name, "name must not be null");
    Assert.notNull(config, "config must not be null");
    this.configMap.put(name, config);
    refreshCache(name, config);
  }

  /**
   * Register cache
   *
   * @param cache
   *         {@link Cache}
   *
   * @since 3.0
   */
  public void registerCustomCache(Cache cache) {
    registerCustomCache(cache.getName(), cache);
  }

  /**
   * Register cache to configMap
   *
   * @param name
   *         the cache name
   * @param cache
   *         {@link Cache}
   *
   * @since 3.0
   */
  public void registerCustomCache(String name, Cache cache) {
    this.cacheMap.put(name, cache);
  }

  /**
   * Create a new Cache instance for the specified cache name.
   *
   * @param name
   *         the name of the cache
   *
   * @return the {@link Cache}
   *
   * @since 3.0
   */
  protected Cache createCache(String name) {
    return createCache(name, getCacheConfig(name));
  }

  /**
   * Create a new Cache instance for the specified cache name.
   *
   * @param name
   *         the name of the cache
   * @param cacheConfig
   *         {@link CacheConfig}
   *
   * @return the {@link Cache}
   */
  protected Cache createCache(String name, CacheConfig cacheConfig) {
    return decorateCache(doCreate(name, cacheConfig));
  }

  /**
   * Decorate the given Cache object if necessary.
   *
   * @param cache
   *         the Cache object to be added to this CacheManager
   *
   * @return the decorated Cache object to be used instead, or simply the
   * passed-in Cache object by default
   *
   * @since 3.0
   */
  protected Cache decorateCache(Cache cache) {
    return cache;
  }

  /**
   * Sub classes
   *
   * @param name
   *         the name of the cache
   * @param cacheConfig
   *         {@link CacheConfig}
   *
   * @return the {@link Cache}
   */
  protected abstract Cache doCreate(String name, CacheConfig cacheConfig);

  @Override
  public Cache getCache(final String name) {
    return getCache(name, getCacheConfig(name));
  }

  @Override
  public Cache getCache(String name, CacheConfig cacheConfig) {
    Cache cache = cacheMap.get(name);
    if (cache == null && isDynamicCreation()) {
      synchronized(cacheMap) {
        cache = cacheMap.get(name);
        if (cache == null) {
          cache = createCache(name, cacheConfig);
          configMap.put(name, cacheConfig); // sync config
          registerCustomCache(name, cache);
        }
      }
    }
    return cache;
  }

  /**
   * is dynamic creation feature enabled
   *
   * @return Is dynamic creation feature enabled
   *
   * @since 3.0
   */
  public boolean isDynamicCreation() {
    return dynamicCreation;
  }

  /**
   * Apply dynamic creation feature
   * <p>
   * When {@link #cacheMap} returns null, create new one
   *
   * @param dynamicCreation
   *         Enable dynamic creation
   *
   * @see #getCache(String)
   * @see #getCache(String, CacheConfig)
   * @since 3.0
   */
  public void setDynamicCreation(boolean dynamicCreation) {
    this.dynamicCreation = dynamicCreation;
  }

  /**
   * Refresh caches in a startup time or runtime
   * <p>
   * Recreate the common caches with the current state of this manager.
   *
   * @since 3.0
   */
  public void refreshCaches() {
    configMap.keySet().forEach(this::refreshCache);
  }

  /**
   * Refresh cache with given name in a startup time or runtime
   *
   * @param name
   *         cache name
   */
  public void refreshCache(String name, CacheConfig config) {
    registerCustomCache(name, createCache(name, config));
  }

  public void refreshCache(String name) {
    registerCustomCache(name, createCache(name));
  }

  /**
   * Get {@link CacheConfig} with given name
   *
   * @param name
   *         name of {@link Cache}
   *
   * @return {@link CacheConfig}
   *
   * @since 3.0
   */
  public final CacheConfig getCacheConfig(final String name) {
    final CacheConfig cacheConfig = configMap.get(name);
    return cacheConfig == null ? CacheConfig.EMPTY_CACHE_CONFIG : cacheConfig;
  }

  @Override
  public Collection<String> getCacheNames() {
    return configMap.keySet();
  }

  protected static boolean isDefaultConfig(CacheConfig cacheConfig) {
    if (cacheConfig == null || cacheConfig == CacheConfig.EMPTY_CACHE_CONFIG) {
      return true;
    }
    return cacheConfig.maxIdleTime() == 0 && cacheConfig.expire() == 0 && cacheConfig.maxSize() == 0;
  }

}
