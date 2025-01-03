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

package infra.cache.jcache.interceptor;

import java.util.Collection;
import java.util.Collections;

import javax.cache.annotation.CacheInvocationContext;

import infra.cache.Cache;
import infra.cache.interceptor.CacheOperationInvocationContext;
import infra.cache.interceptor.CacheResolver;
import infra.cache.jcache.JCacheCache;
import infra.lang.Assert;

/**
 * Framework's {@link CacheResolver} implementation that delegates to a standard
 * JSR-107 {@link javax.cache.annotation.CacheResolver}.
 * <p>Used internally to invoke user-based JSR-107 cache resolvers.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
class CacheResolverAdapter implements CacheResolver {

  private final javax.cache.annotation.CacheResolver target;

  /**
   * Create a new instance with the JSR-107 cache resolver to invoke.
   */
  public CacheResolverAdapter(javax.cache.annotation.CacheResolver target) {
    Assert.notNull(target, "JSR-107 CacheResolver is required");
    this.target = target;
  }

  /**
   * Return the underlying {@link javax.cache.annotation.CacheResolver}
   * that this instance is using.
   */
  protected javax.cache.annotation.CacheResolver getTarget() {
    return this.target;
  }

  @Override
  public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
    if (!(context instanceof CacheInvocationContext<?> cacheInvocationContext)) {
      throw new IllegalStateException("Unexpected context " + context);
    }
    javax.cache.Cache<Object, Object> cache = target.resolveCache(cacheInvocationContext);
    if (cache == null) {
      throw new IllegalStateException("Could not resolve cache for " + context + " using " + this.target);
    }
    return Collections.singleton(new JCacheCache(cache));
  }

}
