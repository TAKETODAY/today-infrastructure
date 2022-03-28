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

import javax.cache.annotation.CacheRemoveAll;

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;
import cn.taketoday.cache.interceptor.CacheOperationInvoker;

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

  @Override
  protected Object invoke(
          CacheOperationInvocationContext<CacheRemoveAllOperation> context, CacheOperationInvoker invoker) {

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
      if (!earlyRemove && operation.getExceptionTypeFilter().match(original.getClass())) {
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
