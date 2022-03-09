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

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemoveAll;

import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.util.ExceptionTypeFilter;

/**
 * The {@link JCacheOperation} implementation for a {@link CacheRemoveAll} operation.
 *
 * @author Stephane Nicoll
 * @see CacheRemoveAll
 * @since 4.0
 */
class CacheRemoveAllOperation extends AbstractJCacheOperation<CacheRemoveAll> {

  private final ExceptionTypeFilter exceptionTypeFilter;

  public CacheRemoveAllOperation(CacheMethodDetails<CacheRemoveAll> methodDetails, CacheResolver cacheResolver) {
    super(methodDetails, cacheResolver);
    CacheRemoveAll ann = methodDetails.getCacheAnnotation();
    this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
  }

  @Override
  public ExceptionTypeFilter getExceptionTypeFilter() {
    return this.exceptionTypeFilter;
  }

  /**
   * Specify if the cache should be cleared before invoking the method. By default, the
   * cache is cleared after the method invocation.
   *
   * @see javax.cache.annotation.CacheRemoveAll#afterInvocation()
   */
  public boolean isEarlyRemove() {
    return !getCacheAnnotation().afterInvocation();
  }

}
