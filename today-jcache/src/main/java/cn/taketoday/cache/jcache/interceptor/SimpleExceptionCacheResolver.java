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

import java.util.Collection;
import java.util.Collections;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.interceptor.AbstractCacheResolver;
import cn.taketoday.cache.interceptor.BasicOperation;
import cn.taketoday.cache.interceptor.CacheOperationInvocationContext;
import cn.taketoday.cache.interceptor.CacheResolver;

/**
 * A simple {@link CacheResolver} that resolves the exception cache
 * based on a configurable {@link CacheManager} and the name of the
 * cache: {@link CacheResultOperation#getExceptionCacheName()}.
 *
 * @author Stephane Nicoll
 * @see CacheResultOperation#getExceptionCacheName()
 * @since 4.0
 */
public class SimpleExceptionCacheResolver extends AbstractCacheResolver {

  public SimpleExceptionCacheResolver(CacheManager cacheManager) {
    super(cacheManager);
  }

  @Override
  protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
    BasicOperation operation = context.getOperation();
    if (!(operation instanceof CacheResultOperation cacheResultOperation)) {
      throw new IllegalStateException("Could not extract exception cache name from " + operation);
    }
    String exceptionCacheName = cacheResultOperation.getExceptionCacheName();
    if (exceptionCacheName != null) {
      return Collections.singleton(exceptionCacheName);
    }
    return null;
  }

}
