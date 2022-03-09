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

package cn.taketoday.cache.jcache.interceptor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collection;

import cn.taketoday.cache.interceptor.AbstractCacheInvoker;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;
import cn.taketoday.cache.Cache;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

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
