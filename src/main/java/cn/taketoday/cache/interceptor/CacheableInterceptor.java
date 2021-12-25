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

import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.CacheValueRetrievalException;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.core.Ordered;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for {@link Cacheable}
 *
 * @author TODAY <br>
 * 2019-02-27 19:50
 */
public class CacheableInterceptor extends AbstractCacheInterceptor {

  public CacheableInterceptor() {
    setOrder(Ordered.HIGHEST_PRECEDENCE / 2);
  }

  public CacheableInterceptor(CacheManager cacheManager) {
    this();
    setCacheManager(cacheManager);
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {

    Method method = invocation.getMethod();
    MethodKey methodKey = new MethodKey(method, Cacheable.class);
    CacheConfiguration cacheable = expressionOperations.getConfig(methodKey);

    CacheEvaluationContext context = expressionOperations.prepareContext(methodKey, invocation);
    if (expressionOperations.passCondition(cacheable.condition(), context)) {
      // pass the condition
      Cache cache = obtainCache(method, cacheable);
      Object key = expressionOperations.createKey(cacheable.key(), context, invocation);
      if (cacheable.sync()) { // for sync
        try {
          return cache.get(key, invocation::proceed);
        }
        catch (CacheValueRetrievalException e) {
          throw e.getCause();
        }
      }
      else {
        Object value = get(cache, key);
        if (value == null) {
          value = invocation.proceed();
          if (expressionOperations.allowPutCache(cacheable.unless(), value, context)) {
            put(cache, key, value);
          }
        }
        return value;
      }
    }
    return invocation.proceed();
  }

}
