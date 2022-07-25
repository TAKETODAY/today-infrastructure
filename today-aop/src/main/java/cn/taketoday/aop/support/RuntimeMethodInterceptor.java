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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.lang.Assert;

/**
 * Runtime MethodInterceptor
 * <p>
 * use runtime {@link MethodMatcher} to match interceptor can be executed
 *
 * </p>
 *
 * @author TODAY 2021/2/1 19:31
 * @since 3.0
 */
public final class RuntimeMethodInterceptor implements MethodInterceptor {

  private final MethodMatcher methodMatcher;
  private final MethodInterceptor interceptor;

  public RuntimeMethodInterceptor(MethodInterceptor interceptor, MethodMatcher methodMatcher) {
    Assert.notNull(interceptor, "interceptor must not be null");
    Assert.notNull(methodMatcher, "methodMatcher must not be null");
    Assert.state(methodMatcher.isRuntime(), "methodMatcher must be a runtime Matcher");

    this.interceptor = interceptor;
    this.methodMatcher = methodMatcher;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    if (methodMatcher.matches(invocation)) {
      return interceptor.invoke(invocation);
    }
    else {
      // next in the chain.
      return invocation.proceed();
    }
  }

}
