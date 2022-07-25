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

/**
 * Intercepts the construction of a new object.
 *
 * <p>
 * The user should implement the {@link #construct(ConstructorInvocation)}
 * method to modify the original behavior. E.g. the following class implements a
 * singleton interceptor (allows only one unique instance for the intercepted
 * class):
 *
 * <pre class=code>
 * class DebuggingInterceptor implements ConstructorInterceptor {
 *     Object instance = null;
 *
 *     Object construct(ConstructorInvocation i) throws Throwable {
 *         if (instance == null) {
 *             return instance = i.proceed();
 *         }
 *         else {
 *             throw new Exception("singleton does not allow multiple instance");
 *         }
 *     }
 * }
 * </pre>
 */

public interface ConstructorInterceptor extends Interceptor {
  /**
   * Implement this method to perform extra treatments before and after the
   * consrution of a new object. Polite implementations would certainly like to
   * invoke {@link Joinpoint#proceed()}.
   *
   * @param invocation
   *         the construction joinpoint
   *
   * @return the newly created object, which is also the result of the call to
   * {@link Joinpoint#proceed()}, might be replaced by the interceptor.
   *
   * @throws Throwable
   *         if the interceptors or the target-object throws an exception.
   */
  Object construct(ConstructorInvocation invocation) throws Throwable;
}
