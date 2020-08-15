/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.allowPutCache;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.createKey;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.isConditionPassing;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.prepareAnnotation;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.prepareELContext;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.aop.annotation.Advice;
import cn.taketoday.aop.annotation.Aspect;
import cn.taketoday.cache.Cache;
import cn.taketoday.cache.CacheExpressionContext;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.CacheValueRetrievalException;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.cache.annotation.Cacheable;
import cn.taketoday.context.Ordered;

/**
 * @author TODAY <br>
 *         2019-02-27 19:50
 */
@Aspect
@Advice(Cacheable.class)
public class CacheableInterceptor extends AbstractCacheInterceptor {

    public CacheableInterceptor() {
        setOrder(Ordered.HIGHEST_PRECEDENCE * 2);
    }

    public CacheableInterceptor(CacheManager cacheManager) {
        this();
        setCacheManager(cacheManager);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        final Method method = invocation.getMethod();
        final MethodKey methodKey = new MethodKey(method, Cacheable.class);

        final CacheConfiguration cacheable = prepareAnnotation(methodKey);

        final CacheExpressionContext context = prepareELContext(methodKey, invocation);

        if (isConditionPassing(cacheable.condition(), context)) {// pass the condition
            final Cache cache = obtainCache(method, cacheable);
            final Object key = createKey(cacheable.key(), context, invocation);

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
                    if (allowPutCache(cacheable.unless(), value, context)) {
                        put(cache, key, value);
                    }
                }
                return value;
            }
        }
        return invocation.proceed();
    }

}
