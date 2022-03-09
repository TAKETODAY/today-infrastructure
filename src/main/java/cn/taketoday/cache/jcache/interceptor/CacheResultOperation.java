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
import javax.cache.annotation.CacheResult;

import cn.taketoday.cache.interceptor.CacheResolver;
import cn.taketoday.cache.interceptor.KeyGenerator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionTypeFilter;
import cn.taketoday.util.StringUtils;

/**
 * The {@link JCacheOperation} implementation for a {@link CacheResult} operation.
 *
 * @author Stephane Nicoll
 * @see CacheResult
 * @since 4.0
 */
class CacheResultOperation extends AbstractJCacheKeyOperation<CacheResult> {

  private final ExceptionTypeFilter exceptionTypeFilter;

  @Nullable
  private final CacheResolver exceptionCacheResolver;

  @Nullable
  private final String exceptionCacheName;

  public CacheResultOperation(CacheMethodDetails<CacheResult> methodDetails, CacheResolver cacheResolver,
          KeyGenerator keyGenerator, @Nullable CacheResolver exceptionCacheResolver) {

    super(methodDetails, cacheResolver, keyGenerator);

    CacheResult ann = methodDetails.getCacheAnnotation();
    this.exceptionTypeFilter = createExceptionTypeFilter(ann.cachedExceptions(), ann.nonCachedExceptions());
    this.exceptionCacheResolver = exceptionCacheResolver;
    this.exceptionCacheName = (StringUtils.hasText(ann.exceptionCacheName()) ? ann.exceptionCacheName() : null);
  }

  @Override
  public ExceptionTypeFilter getExceptionTypeFilter() {
    return this.exceptionTypeFilter;
  }

  /**
   * Specify if the method should always be invoked regardless of a cache hit.
   * By default, the method is only invoked in case of a cache miss.
   *
   * @see javax.cache.annotation.CacheResult#skipGet()
   */
  public boolean isAlwaysInvoked() {
    return getCacheAnnotation().skipGet();
  }

  /**
   * Return the {@link CacheResolver} instance to use to resolve the cache to
   * use for matching exceptions thrown by this operation.
   */
  @Nullable
  public CacheResolver getExceptionCacheResolver() {
    return this.exceptionCacheResolver;
  }

  /**
   * Return the name of the cache to cache exceptions, or {@code null} if
   * caching exceptions should be disabled.
   *
   * @see javax.cache.annotation.CacheResult#exceptionCacheName()
   */
  @Nullable
  public String getExceptionCacheName() {
    return this.exceptionCacheName;
  }

}
