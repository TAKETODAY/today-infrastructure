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

import javax.cache.annotation.CacheRemove;

import infra.cache.Cache;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheOperationInvocationContext;
import infra.cache.interceptor.CacheOperationInvoker;

/**
 * Intercept methods annotated with {@link CacheRemove}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings("serial")
class CacheRemoveEntryInterceptor extends AbstractKeyCacheInterceptor<CacheRemoveOperation, CacheRemove> {

  protected CacheRemoveEntryInterceptor(CacheErrorHandler errorHandler) {
    super(errorHandler);
  }

  @Override
  protected Object invoke(
          CacheOperationInvocationContext<CacheRemoveOperation> context, CacheOperationInvoker invoker) {

    CacheRemoveOperation operation = context.getOperation();
    boolean earlyRemove = operation.isEarlyRemove();
    if (earlyRemove) {
      removeValue(context);
    }

    try {
      Object result = invoker.invoke();
      if (!earlyRemove) {
        removeValue(context);
      }
      return result;
    }
    catch (CacheOperationInvoker.ThrowableWrapper wrapperException) {
      Throwable ex = wrapperException.getOriginal();
      if (!earlyRemove && operation.getExceptionTypeFilter().match(ex)) {
        removeValue(context);
      }
      throw wrapperException;
    }
  }

  private void removeValue(CacheOperationInvocationContext<CacheRemoveOperation> context) {
    Object key = generateKey(context);
    Cache cache = resolveCache(context);
    if (logger.isTraceEnabled()) {
      logger.trace("Invalidating key [{}] on cache '{}' for operation {}", key, cache.getName(), context.getOperation());
    }
    doEvict(cache, key, context.getOperation().isEarlyRemove());
  }

}
