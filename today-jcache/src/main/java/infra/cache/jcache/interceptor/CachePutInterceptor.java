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

import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.CachePut;

import infra.cache.Cache;
import infra.cache.interceptor.CacheErrorHandler;
import infra.cache.interceptor.CacheOperationInvocationContext;
import infra.cache.interceptor.CacheOperationInvoker;

/**
 * Intercept methods annotated with {@link CachePut}.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings("serial")
class CachePutInterceptor extends AbstractKeyCacheInterceptor<CachePutOperation, CachePut> {

  public CachePutInterceptor(CacheErrorHandler errorHandler) {
    super(errorHandler);
  }

  @Nullable
  @Override
  protected Object invoke(CacheOperationInvocationContext<CachePutOperation> context, CacheOperationInvoker invoker) {
    CachePutOperation operation = context.getOperation();
    CacheKeyInvocationContext<CachePut> invocationContext = createCacheKeyInvocationContext(context);

    boolean earlyPut = operation.isEarlyPut();
    Object value = invocationContext.getValueParameter().getValue();
    if (earlyPut) {
      cacheValue(context, value);
    }

    try {
      Object result = invoker.invoke();
      if (!earlyPut) {
        cacheValue(context, value);
      }
      return result;
    }
    catch (CacheOperationInvoker.ThrowableWrapper ex) {
      Throwable original = ex.getOriginal();
      if (!earlyPut && operation.getExceptionTypeFilter().match(original)) {
        cacheValue(context, value);
      }
      throw ex;
    }
  }

  protected void cacheValue(CacheOperationInvocationContext<CachePutOperation> context, Object value) {
    Object key = generateKey(context);
    Cache cache = resolveCache(context);
    doPut(cache, key, value);
  }

}
