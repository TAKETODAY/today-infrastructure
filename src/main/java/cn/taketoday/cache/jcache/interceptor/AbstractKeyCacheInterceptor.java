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

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheKeyInvocationContext;

import cn.taketoday.cache.interceptor.CacheErrorHandler;
import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;
import cn.taketoday.cache.interceptor.KeyGenerator;

/**
 * A base interceptor for JSR-107 key-based cache annotations.
 *
 * @param <O> the operation type
 * @param <A> the annotation type
 * @author Stephane Nicoll
 * @since 4.0
 */
@SuppressWarnings("serial")
abstract class AbstractKeyCacheInterceptor<O extends AbstractJCacheKeyOperation<A>, A extends Annotation>
        extends AbstractCacheInterceptor<O, A> {

  protected AbstractKeyCacheInterceptor(CacheErrorHandler errorHandler) {
    super(errorHandler);
  }

  /**
   * Generate a key for the specified invocation.
   *
   * @param context the context of the invocation
   * @return the key to use
   */
  protected Object generateKey(CacheOperationInvocationContext<O> context) {
    KeyGenerator keyGenerator = context.getOperation().getKeyGenerator();
    Object key = keyGenerator.generate(context.getTarget(), context.getMethod(), context.getArgs());
    if (logger.isTraceEnabled()) {
      logger.trace("Computed cache key " + key + " for operation " + context.getOperation());
    }
    return key;
  }

  /**
   * Create a {@link CacheKeyInvocationContext} based on the specified invocation.
   *
   * @param context the context of the invocation.
   * @return the related {@code CacheKeyInvocationContext}
   */
  protected CacheKeyInvocationContext<A> createCacheKeyInvocationContext(CacheOperationInvocationContext<O> context) {
    return new DefaultCacheKeyInvocationContext<>(context.getOperation(), context.getTarget(), context.getArgs());
  }

}
