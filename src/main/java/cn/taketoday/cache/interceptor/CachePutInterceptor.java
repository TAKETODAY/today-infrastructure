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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.cache.interceptor;

import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.createKey;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.isConditionPassing;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.prepareAnnotation;
import static cn.taketoday.cache.interceptor.AbstractCacheInterceptor.Operations.prepareELContext;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.aop.annotation.Advice;
import cn.taketoday.aop.annotation.Aspect;
import cn.taketoday.cache.CacheExpressionContext;
import cn.taketoday.cache.CacheManager;
import cn.taketoday.cache.annotation.CacheConfiguration;
import cn.taketoday.cache.annotation.CachePut;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;

/**
 * @author TODAY <br>
 *         2018-12-23 22:11
 */
@Aspect
@Advice(CachePut.class)
@Order(Ordered.HIGHEST_PRECEDENCE * 2)
public class CachePutInterceptor extends AbstractCacheInterceptor {

    public CachePutInterceptor() {

    }

    public CachePutInterceptor(CacheManager cacheManager) {
        setCacheManager(cacheManager);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {

        final Object result = invocation.proceed();

        final Method method = invocation.getMethod();
        final MethodKey methodKey = new MethodKey(method, CachePut.class);
        final CacheConfiguration cachePut = prepareAnnotation(methodKey);
        final CacheExpressionContext context = prepareELContext(methodKey, invocation);

        context.putBean(Operations.KEY_RESULT, result);

        if (isConditionPassing(cachePut.condition(), context)) {
            final Object key = createKey(cachePut.key(), context, invocation);
            put(obtainCache(method, cachePut), key, result);
        }
        return result;
    }

}
