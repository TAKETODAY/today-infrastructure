/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.cache.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.cache.annotation.CacheEvict;
import cn.taketoday.core.Ordered;

import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.createKey;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.prepareAnnotation;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.prepareELContext;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for {@link CacheEvict}
 *
 * @author TODAY <br>
 * 2019-02-27 20:54
 */
public class CacheEvictInterceptor extends AbstractCacheInterceptor {

  public CacheEvictInterceptor() {
    setOrder(Ordered.HIGHEST_PRECEDENCE * 2);
  }

  public CacheEvictInterceptor(CacheManager cacheManager) {
    this();
    setCacheManager(cacheManager);
  }

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    final Method method = invocation.getMethod();
    final MethodKey methodKey = new MethodKey(method, CacheEvict.class);

    final CacheConfiguration cacheEvict = prepareAnnotation(methodKey);

    // before
    if (cacheEvict.beforeInvocation()) {
      if (cacheEvict.allEntries()) {
        clear(obtainCache(method, cacheEvict));
      }
      else {
        final Object key = createKey(cacheEvict.key(),
                                     prepareELContext(methodKey, invocation),
                                     invocation);
        evict(obtainCache(method, cacheEvict), key);
      }
      return invocation.proceed();
    }

    // after
    // if any exception occurred in this operation will not do evict or clear
    final Object proceed = invocation.proceed();

    if (cacheEvict.allEntries()) {
      clear(obtainCache(method, cacheEvict));
    }
    else {
      final Object key = createKey(cacheEvict.key(),
                                   prepareELContext(methodKey, invocation),
                                   invocation);
      evict(obtainCache(method, cacheEvict), key);
    }
    return proceed;
  }

}
