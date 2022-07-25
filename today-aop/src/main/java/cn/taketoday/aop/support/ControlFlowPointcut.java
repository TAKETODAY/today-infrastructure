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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

/**
 * Pointcut and method matcher for use in simple <b>cflow</b>-style pointcut.
 * Note that evaluating such pointcuts is 10-15 times slower than evaluating
 * normal pointcuts, but they are useful in some cases.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 23:01
 */
@SuppressWarnings("serial")
public class ControlFlowPointcut implements Pointcut, ClassFilter, MethodMatcher, Serializable {

  private final Class<?> clazz;

  @Nullable
  private final String methodName;

  private final AtomicInteger evaluations = new AtomicInteger();

  /**
   * Construct a new pointcut that matches all control flows below that class.
   *
   * @param clazz the clazz
   */
  public ControlFlowPointcut(Class<?> clazz) {
    this(clazz, null);
  }

  /**
   * Construct a new pointcut that matches all calls below the given method
   * in the given class. If no method name is given, matches all control flows
   * below the given class.
   *
   * @param clazz the clazz
   * @param methodName the name of the method (may be {@code null})
   */
  public ControlFlowPointcut(Class<?> clazz, @Nullable String methodName) {
    Assert.notNull(clazz, "Class must not be null");
    this.clazz = clazz;
    this.methodName = methodName;
  }

  /**
   * Subclasses can override this for greater filtering (and performance).
   */
  @Override
  public boolean matches(Class<?> clazz) {
    return true;
  }

  /**
   * Subclasses can override this if it's possible to filter out some candidate classes.
   */
  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    return true;
  }

  @Override
  public boolean isRuntime() {
    return true;
  }

  @Override
  public boolean matches(MethodInvocation invocation) {
    this.evaluations.incrementAndGet();

    for (StackTraceElement element : new Throwable().getStackTrace()) {
      if (element.getClassName().equals(this.clazz.getName()) &&
              (this.methodName == null || element.getMethodName().equals(this.methodName))) {
        return true;
      }
    }
    return false;
  }

  /**
   * It's useful to know how many times we've fired, for optimization.
   */
  public int getEvaluations() {
    return this.evaluations.get();
  }

  @Override
  public ClassFilter getClassFilter() {
    return this;
  }

  @Override
  public MethodMatcher getMethodMatcher() {
    return this;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ControlFlowPointcut that)) {
      return false;
    }
    return (this.clazz.equals(that.clazz)) && ObjectUtils.nullSafeEquals(this.methodName, that.methodName);
  }

  @Override
  public int hashCode() {
    int code = this.clazz.hashCode();
    if (this.methodName != null) {
      code = 37 * code + this.methodName.hashCode();
    }
    return code;
  }

  @Override
  public String toString() {
    return getClass().getName() + ": class = " + this.clazz.getName() + "; methodName = " + this.methodName;
  }

}
