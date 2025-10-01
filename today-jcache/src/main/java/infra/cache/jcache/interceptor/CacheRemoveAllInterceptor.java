/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import javax.cache.annotation.CacheRemoveAll;

import infra.cache.Cache;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheOperationInvocationContext;
import infra.cache.interceptor.CacheOperationInvoker;

/**
 * Intercept methods annotated with {@link CacheRemoveAll}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings("serial")
class CacheRemoveAllInterceptor extends AbstractCacheInterceptor<CacheRemoveAllOperation, CacheRemoveAll> {

  protected CacheRemoveAllInterceptor(CacheErrorHandler errorHandler) {
    super(errorHandler);
  }

  @Nullable
  @Override
  protected Object invoke(CacheOperationInvocationContext<CacheRemoveAllOperation> context, CacheOperationInvoker invoker) {
    CacheRemoveAllOperation operation = context.getOperation();
    boolean earlyRemove = operation.isEarlyRemove();
    if (earlyRemove) {
      removeAll(context);
    }

    try {
      Object result = invoker.invoke();
      if (!earlyRemove) {
        removeAll(context);
      }
      return result;
    }
    catch (CacheOperationInvoker.ThrowableWrapper ex) {
      Throwable original = ex.getOriginal();
      if (!earlyRemove && operation.getExceptionTypeFilter().match(original)) {
        removeAll(context);
      }
      throw ex;
    }
  }

  protected void removeAll(CacheOperationInvocationContext<CacheRemoveAllOperation> context) {
    Cache cache = resolveCache(context);
    if (logger.isTraceEnabled()) {
      logger.trace("Invalidating entire cache '{}' for operation {}", cache.getName(), context.getOperation());
    }
    doClear(cache, context.getOperation().isEarlyRemove());
  }

}
