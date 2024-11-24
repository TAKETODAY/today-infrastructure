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

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemove;

import infra.cache.interceptor.CacheResolver;
import infra.cache.interceptor.KeyGenerator;
import infra.util.ExceptionTypeFilter;

/**
 * The {@link JCacheOperation} implementation for a {@link CacheRemove} operation.
 *
 * @author Stephane Nicoll
 * @see CacheRemove
 * @since 4.0
 */
class CacheRemoveOperation extends AbstractJCacheKeyOperation<CacheRemove> {

  private final ExceptionTypeFilter exceptionTypeFilter;

  public CacheRemoveOperation(
          CacheMethodDetails<CacheRemove> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator) {

    super(methodDetails, cacheResolver, keyGenerator);
    CacheRemove ann = methodDetails.getCacheAnnotation();
    this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
  }

  @Override
  public ExceptionTypeFilter getExceptionTypeFilter() {
    return this.exceptionTypeFilter;
  }

  /**
   * Specify if the cache entry should be removed before invoking the method.
   * <p>By default, the cache entry is removed after the method invocation.
   *
   * @see javax.cache.annotation.CacheRemove#afterInvocation()
   */
  public boolean isEarlyRemove() {
    return !getCacheAnnotation().afterInvocation();
  }

}
