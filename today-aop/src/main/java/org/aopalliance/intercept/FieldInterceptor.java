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
 * Intercepts field access on a target object.
 *
 * <p>
 * The user should implement the {@link #set(FieldAccess)} and
 * {@link #get(FieldAccess)} methods to modify the original behavior. E.g. the
 * following class implements a tracing interceptor (traces the accesses to the
 * intercepted field(s)):
 *
 * <pre class=code>
 * class TracingInterceptor implements FieldInterceptor {
 *
 *     Object set(FieldAccess fa) throws Throwable {
 *         System.out.println("field " + fa.getField() + " is set with value " + fa.getValueToSet());
 *         Object ret = fa.proceed();
 *         System.out.println("field " + fa.getField() + " was set to value " + ret);
 *         return ret;
 *     }
 *
 *     Object get(FieldAccess fa) throws Throwable {
 *         System.out.println("field " + fa.getField() + " is about to be read");
 *         Object ret = fa.proceed();
 *         System.out.println("field " + fa.getField() + " was read; value is " + ret);
 *         return ret;
 *     }
 * }
 * </pre>
 */

public interface FieldInterceptor extends Interceptor {

  /**
   * Do the stuff you want to do before and after the field is getted.
   *
   * <p>
   * Polite implementations would certainly like to call
   * {@link Joinpoint#proceed()}.
   *
   * @param fieldRead
   *         the joinpoint that corresponds to the field read
   *
   * @return the result of the field read {@link Joinpoint#proceed()}, might be
   * intercepted by the interceptor.
   *
   * @throws Throwable
   *         if the interceptors or the target-object throws an exception.
   */
  Object get(FieldAccess fieldRead) throws Throwable;

  /**
   * Do the stuff you want to do before and after the field is setted.
   *
   * <p>
   * Polite implementations would certainly like to implement
   * {@link Joinpoint#proceed()}.
   *
   * @param fieldWrite
   *         the joinpoint that corresponds to the field write
   *
   * @return the result of the field set {@link Joinpoint#proceed()}, might be
   * intercepted by the interceptor.
   *
   * @throws Throwable
   *         if the interceptors or the target-object throws an exception.
   */
  Object set(FieldAccess fieldWrite) throws Throwable;

}
