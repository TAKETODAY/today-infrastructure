/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

import cn.taketoday.aop.MethodMatcher;

/**
 * Runtime MethodInterceptor
 *
 * @author TODAY 2021/2/1 19:31
 */
public class RuntimeMethodInterceptor implements MethodInterceptor, MethodMatcher {

  private final MethodMatcher methodMatcher;
  private final MethodInterceptor interceptor;

  public RuntimeMethodInterceptor(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
    this.interceptor = interceptor;
    this.methodMatcher = methodMatcher;
  }

//  @Override
//  public final Object invoke(MethodInvocation invocation) throws Throwable {
//    if (methodMatcher.matches(invocation.getMethod(), target.getClass(), invocation.getArguments())) {
//      return interceptor.invoke(invocation);
//    }
//    // next in the chain.
//    return invocation.proceed();
//  }
//

  @Override
  public final Object invoke(MethodInvocation invocation) throws Throwable {
    return interceptor.invoke(invocation);
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    return methodMatcher.matches(method, targetClass);
  }

  @Override
  public boolean isRuntime() {
    return true;
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass, Object[] args) {
    return methodMatcher.matches(method, targetClass, args);
  }
}
