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
package cn.taketoday.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.core.Ordered;

/**
 * Interceptor that exposes the current {@link MethodInvocation}
 * as a thread-local object. We occasionally need to do this; for example, when a pointcut
 * (e.g. an AspectJ expression pointcut) needs to know the full invocation context.
 *
 * <p>Don't use this interceptor unless this is really necessary. Target objects should
 * not normally know about AOP, as this creates a dependency on API.
 * Target objects should be plain POJOs as far as possible.
 *
 * <p>If used, this interceptor will normally be the first in the interceptor chain.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/3 23:40
 * @since 3.0
 */
@SuppressWarnings("serial")
public final class ExposeInvocationInterceptor implements MethodInterceptor, Ordered, Serializable {

  /** Singleton instance of this class. */
  public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();

  /**
   * Singleton advisor for this class. Use in preference to INSTANCE when using
   * AOP, as it prevents the need to create a new Advisor to wrap the instance.
   */
  public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
    @Override
    public String toString() {
      return ExposeInvocationInterceptor.class.getName() + ".ADVISOR";
    }
  };

  private static final ThreadLocal<MethodInvocation> invocation =
          new NamedThreadLocal<>("Current AOP method invocation");

  /**
   * Return the AOP Alliance MethodInvocation object associated with the current invocation.
   *
   * @return the invocation object associated with the current invocation
   * @throws IllegalStateException if there is no AOP invocation in progress,
   * or if the ExposeInvocationInterceptor was not added to this interceptor chain
   */
  public static MethodInvocation currentInvocation() throws IllegalStateException {
    MethodInvocation mi = invocation.get();
    if (mi == null) {
      throw new IllegalStateException(
              "No MethodInvocation found: Check that an AOP invocation is in progress and that the " +
                      "ExposeInvocationInterceptor is upfront in the interceptor chain. Specifically, note that " +
                      "advices with order HIGHEST_PRECEDENCE will execute before ExposeInvocationInterceptor! " +
                      "In addition, ExposeInvocationInterceptor and ExposeInvocationInterceptor.currentInvocation() " +
                      "must be invoked from the same thread.");
    }
    return mi;
  }

  /**
   * Ensures that only the canonical instance can be created.
   */
  private ExposeInvocationInterceptor() { }

  @Override
  public Object invoke(MethodInvocation mi) throws Throwable {
    MethodInvocation oldInvocation = invocation.get();
    invocation.set(mi);
    try {
      return mi.proceed();
    }
    finally {
      invocation.set(oldInvocation);
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  /**
   * Required to support serialization. Replaces with canonical instance
   * on deserialization, protecting Singleton pattern.
   * <p>Alternative to overriding the {@code equals} method.
   */
  private Object readResolve() {
    return INSTANCE;
  }

}
