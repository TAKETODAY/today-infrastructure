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

package cn.taketoday.cache.jcache;

import java.util.Collection;
import java.util.LinkedHashSet;

import javax.cache.CacheManager;
import javax.cache.Caching;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.transaction.AbstractTransactionSupportingCacheManager;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.cache.CacheManager} implementation
 * backed by a JCache {@link CacheManager javax.cache.CacheManager}.
 *
 * <p>Note: This class has been updated for JCache 1.0.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see JCacheCache
 * @since 4.0
 */
public class JCacheCacheManager extends AbstractTransactionSupportingCacheManager {

  @Nullable
  private CacheManager cacheManager;

  private boolean allowNullValues = true;

  /**
   * Create a new {@code JCacheCacheManager} without a backing JCache
   * {@link CacheManager javax.cache.CacheManager}.
   * <p>The backing JCache {@code javax.cache.CacheManager} can be set via the
   * {@link #setCacheManager} bean property.
   */
  public JCacheCacheManager() { }

  /**
   * Create a new {@code JCacheCacheManager} for the given backing JCache
   * {@link CacheManager javax.cache.CacheManager}.
   *
   * @param cacheManager the backing JCache {@code javax.cache.CacheManager}
   */
  public JCacheCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Set the backing JCache {@link CacheManager javax.cache.CacheManager}.
   */
  public void setCacheManager(@Nullable CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Return the backing JCache {@link CacheManager javax.cache.CacheManager}.
   */
  @Nullable
  public CacheManager getCacheManager() {
    return this.cacheManager;
  }

  /**
   * Specify whether to accept and convert {@code null} values for all caches
   * in this cache manager.
   * <p>Default is "true", despite JSR-107 itself not supporting {@code null} values.
   * An internal holder object will be used to store user-level {@code null}s.
   */
  public void setAllowNullValues(boolean allowNullValues) {
    this.allowNullValues = allowNullValues;
  }

  /**
   * Return whether this cache manager accepts and converts {@code null} values
   * for all of its caches.
   */
  public boolean isAllowNullValues() {
    return this.allowNullValues;
  }

  @Override
  public void afterPropertiesSet() {
    if (getCacheManager() == null) {
      setCacheManager(Caching.getCachingProvider().getCacheManager());
    }
    super.afterPropertiesSet();
  }

  @Override
  protected Collection<Cache> loadCaches() {
    CacheManager cacheManager = getCacheManager();
    Assert.state(cacheManager != null, "No CacheManager set");

    Collection<Cache> caches = new LinkedHashSet<>();
    for (String cacheName : cacheManager.getCacheNames()) {
      javax.cache.Cache<Object, Object> jcache = cacheManager.getCache(cacheName);
      caches.add(new JCacheCache(jcache, isAllowNullValues()));
    }
    return caches;
  }

  @Override
  protected Cache getMissingCache(String name) {
    CacheManager cacheManager = getCacheManager();
    Assert.state(cacheManager != null, "No CacheManager set");

    // Check the JCache cache again (in case the cache was added at runtime)
    javax.cache.Cache<Object, Object> jcache = cacheManager.getCache(name);
    if (jcache != null) {
      return new JCacheCache(jcache, isAllowNullValues());
    }
    return null;
  }

}
