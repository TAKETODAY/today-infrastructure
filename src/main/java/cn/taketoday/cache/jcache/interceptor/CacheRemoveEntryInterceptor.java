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

import javax.cache.annotation.CacheRemove;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;

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
      if (!earlyRemove && operation.getExceptionTypeFilter().match(ex.getClass())) {
        removeValue(context);
      }
      throw wrapperException;
    }
  }

  private void removeValue(CacheOperationInvocationContext<CacheRemoveOperation> context) {
    Object key = generateKey(context);
    Cache cache = resolveCache(context);
    if (logger.isTraceEnabled()) {
      logger.trace("Invalidating key [" + key + "] on cache '" + cache.getName() +
              "' for operation " + context.getOperation());
    }
    doEvict(cache, key, context.getOperation().isEarlyRemove());
  }

}
