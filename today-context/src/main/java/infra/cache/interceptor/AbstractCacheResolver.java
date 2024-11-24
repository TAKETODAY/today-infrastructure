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

package infra.cache.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import infra.beans.factory.InitializingBean;
import infra.cache.Cache;
import infra.cache.CacheManager;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A base {@link CacheResolver} implementation that requires the concrete
 * implementation to provide the collection of cache name(s) based on the
 * invocation context.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractCacheResolver implements CacheResolver, InitializingBean {

  @Nullable
  private CacheManager cacheManager;

  /**
   * Construct a new {@code AbstractCacheResolver}.
   *
   * @see #setCacheManager
   */
  protected AbstractCacheResolver() {
  }

  /**
   * Construct a new {@code AbstractCacheResolver} for the given {@link CacheManager}.
   *
   * @param cacheManager the CacheManager to use
   */
  protected AbstractCacheResolver(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Set the {@link CacheManager} that this instance should use.
   */
  public void setCacheManager(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Return the {@link CacheManager} that this instance uses.
   */
  public CacheManager getCacheManager() {
    Assert.state(this.cacheManager != null, "No CacheManager set");
    return this.cacheManager;
  }

  @Override
  public void afterPropertiesSet() {
    Assert.notNull(this.cacheManager, "CacheManager is required");
  }

  @Override
  public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
    Collection<String> cacheNames = getCacheNames(context);
    if (cacheNames == null) {
      return Collections.emptyList();
    }
    ArrayList<Cache> result = new ArrayList<>(cacheNames.size());
    CacheManager cacheManager = getCacheManager();
    for (String cacheName : cacheNames) {
      Cache cache = cacheManager.getCache(cacheName);
      if (cache == null) {
        throw new IllegalArgumentException("Cannot find cache named '%s' for %s".formatted(cacheName, context.getOperation()));
      }
      result.add(cache);
    }
    return result;
  }

  /**
   * Provide the name of the cache(s) to resolve against the current cache manager.
   * <p>It is acceptable to return {@code null} to indicate that no cache could
   * be resolved for this invocation.
   *
   * @param context the context of the particular invocation
   * @return the cache name(s) to resolve, or {@code null} if no cache should be resolved
   */
  @Nullable
  protected abstract Collection<String> getCacheNames(CacheOperationInvocationContext<?> context);

}
