/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.cache.jcache.interceptor;

import org.jspecify.annotations.Nullable;

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

  @Nullable
  @Override
  protected Object invoke(CacheOperationInvocationContext<CacheRemoveOperation> context, CacheOperationInvoker invoker) {
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
