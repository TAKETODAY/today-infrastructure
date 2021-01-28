/**
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
package cn.taketoday.aop.advice;

import java.lang.reflect.Method;

/**
 * @author TODAY <br>
 * 2019-10-20 22:43
 */
public interface MethodMatcher {

  /**
   * Checking whether the given method matches.
   *
   * @param method
   *         the candidate method
   * @param targetClass
   *         the target class
   *
   * @return whether or not this method matches on application startup.
   */
  boolean matches(Method method, Class<?> targetClass);

  /**
   * Is this MethodMatcher dynamic, that is, must a final call be made on the
   * {@link #matches(java.lang.reflect.Method, Class, Object[])} method at runtime
   * even if the 2-arg matches method returns {@code true}?
   * <p>
   * Can be invoked when an AOP proxy is created, and need not be invoked again
   * before each method invocation,
   *
   * @return whether or not a runtime match via the 3-arg
   * {@link #matches(java.lang.reflect.Method, Class, Object[])} method is
   * required if static matching passed
   */
  boolean isRuntime();

  /**
   * Check whether there a runtime (dynamic) match for this method, which must
   * have matched statically.
   * <p>
   * This method is invoked only if the 2-arg matches method returns {@code true}
   * for the given method and target class, and if the {@link #isRuntime()} method
   * returns {@code true}. Invoked immediately before potential running of the
   * advice, after any advice earlier in the advice chain has run.
   *
   * @param method
   *         the candidate method
   * @param targetClass
   *         the target class
   * @param args
   *         arguments to the method
   *
   * @return whether there's a runtime match
   *
   * @see MethodMatcher#matches(Method, Class)
   */
  boolean matches(Method method, Class<?> targetClass, Object[] args);

}
