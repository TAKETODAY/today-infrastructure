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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;

import infra.cache.Cache;
import infra.cache.interceptor.AbstractCacheInvoker;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheOperationInvocationContext;
import infra.cache.interceptor.CacheOperationInvoker;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;

/**
 * A base interceptor for JSR-107 cache annotations.
 *
 * @param <O> the operation type
 * @param <A> the annotation type
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings("serial")
abstract class AbstractCacheInterceptor<O extends AbstractJCacheOperation<A>, A extends Annotation>
        extends AbstractCacheInvoker implements Serializable {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected AbstractCacheInterceptor(CacheErrorHandler errorHandler) {
    super(errorHandler);
  }

  @Nullable
  protected abstract Object invoke(CacheOperationInvocationContext<O> context, CacheOperationInvoker invoker)
          throws Throwable;

  /**
   * Resolve the cache to use.
   *
   * @param context the invocation context
   * @return the cache to use (never {@code null})
   */
  protected Cache resolveCache(CacheOperationInvocationContext<O> context) {
    Collection<? extends Cache> caches = context.getOperation().getCacheResolver().resolveCaches(context);
    Cache cache = extractFrom(caches);
    if (cache == null) {
      throw new IllegalStateException("Cache could not have been resolved for " + context.getOperation());
    }
    return cache;
  }

  /**
   * Convert the collection of caches in a single expected element.
   * <p>Throw an {@link IllegalStateException} if the collection holds more than one element
   *
   * @return the single element, or {@code null} if the collection is empty
   */
  @Nullable
  static Cache extractFrom(Collection<? extends Cache> caches) {
    if (CollectionUtils.isEmpty(caches)) {
      return null;
    }
    else if (caches.size() == 1) {
      return caches.iterator().next();
    }
    else {
      throw new IllegalStateException("Unsupported cache resolution result " + caches +
              ": JSR-107 only supports a single cache.");
    }
  }

}
