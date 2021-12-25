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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.cache.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Constant;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for {@link CachePut}
 *
 * @author TODAY <br>
 * 2018-12-23 22:11
 */
public class CachePutInterceptor extends AbstractCacheInterceptor {

  public CachePutInterceptor() {
    setOrder(Ordered.HIGHEST_PRECEDENCE / 2);
  }

  public CachePutInterceptor(CacheManager cacheManager) {
    this();
    setCacheManager(cacheManager);
  }

  /**
   * Put cache operation
   *
   * @param invocation the method invocation join-point
   */
  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    // process
    Object result = invocation.proceed();
    // put cache
    Method method = invocation.getMethod();
    MethodKey methodKey = new MethodKey(method, CachePut.class);
    CacheConfiguration cachePut = expressionOperations.getConfig(methodKey);
    CacheEvaluationContext context = expressionOperations.prepareContext(methodKey, invocation);
    // use ${result.xxx}
    context.setVariable(Constant.KEY_RESULT, result);
    if (expressionOperations.passCondition(cachePut.condition(), context)) {
      Object key = expressionOperations.createKey(cachePut.key(), context, invocation);
      put(obtainCache(method, cachePut), key, result);
    }
    return result;
  }

}
