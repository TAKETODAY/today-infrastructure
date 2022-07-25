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

package org.aopalliance.intercept;

import cn.taketoday.lang.Nullable;

/**
 * Intercepts calls on an interface on its way to the target. These are nested
 * "on top" of the target.
 *
 * <p>
 * The user should implement the {@link #invoke(MethodInvocation)} method to
 * modify the original behavior. E.g. the following class implements a tracing
 * interceptor (traces all the calls on the intercepted method(s)):
 *
 * <pre class=code>
 * class TracingInterceptor implements MethodInterceptor {
 *     Object invoke(MethodInvocation i) throws Throwable {
 *         System.out.println("method " + i.getMethod() + " is called on " + i.getThis() + " with args " + i.getArguments());
 *         Object ret = i.proceed();
 *         System.out.println("method " + i.getMethod() + " returns " + ret);
 *         return ret;
 *     }
 * }
 * </pre>
 */
@FunctionalInterface
public interface MethodInterceptor extends Interceptor {

  /**
   * Implement this method to perform extra treatments before and after the
   * invocation. Polite implementations would certainly like to invoke
   * {@link Joinpoint#proceed()}.
   *
   * @param invocation the method invocation joinpoint
   * @return the result of the call to {@link Joinpoint#proceed()}, might be
   * intercepted by the interceptor.
   * @throws Throwable if the interceptors or the target-object throws an exception.
   */
  @Nullable
  Object invoke(MethodInvocation invocation) throws Throwable;
}
