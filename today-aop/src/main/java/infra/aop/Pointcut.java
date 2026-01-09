/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop;

import org.aopalliance.intercept.MethodInvocation;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;

import infra.aop.framework.DefaultMethodInvocation;
import infra.aop.support.AopUtils;
import infra.aop.support.ComposablePointcut;
import infra.aop.support.StaticMethodMatcherPointcut;
import infra.lang.Assert;

/**
 * Core  pointcut abstraction.
 *
 * <p>A pointcut is composed of a {@link ClassFilter} and a {@link MethodMatcher}.
 * Both these basic terms and a Pointcut itself can be combined to build up combinations
 * (e.g. through {@link ComposablePointcut}).
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:12
 * @see ClassFilter
 * @see MethodMatcher
 * @since 3.0
 */
public interface Pointcut {

  /**
   * Canonical Pointcut instance that always matches.
   */
  Pointcut TRUE = TruePointcut.INSTANCE;

  /** Pointcut matching all bean property setters, in any class. */
  Pointcut SETTERS = SetterPointcut.INSTANCE;

  /** Pointcut matching all bean property getters, in any class. */
  Pointcut GETTERS = GetterPointcut.INSTANCE;

  /**
   * Return the ClassFilter for this pointcut.
   *
   * @return the ClassFilter (never {@code null})
   */
  ClassFilter getClassFilter();

  /**
   * Return the MethodMatcher for this pointcut.
   *
   * @return the MethodMatcher (never {@code null})
   */
  MethodMatcher getMethodMatcher();

  /**
   * Match all methods that <b>either</b> (or both) of the given pointcuts matches.
   *
   * @param pc1 the first Pointcut
   * @param pc2 the second Pointcut
   * @return a distinct Pointcut that matches all methods that either
   * of the given Pointcuts matches
   */
  static Pointcut union(Pointcut pc1, Pointcut pc2) {
    return new ComposablePointcut(pc1).union(pc2);
  }

  /**
   * Match all methods that <b>both</b> the given pointcuts match.
   *
   * @param pc1 the first Pointcut
   * @param pc2 the second Pointcut
   * @return a distinct Pointcut that matches all methods that both
   * of the given Pointcuts match
   */
  static Pointcut intersection(Pointcut pc1, Pointcut pc2) {
    return new ComposablePointcut(pc1).intersection(pc2);
  }

  /**
   * Perform the least expensive check for a pointcut match.
   *
   * @param pointcut the pointcut to match
   * @param invocation runtime invocation contains the candidate method
   * and target class, arguments to the method
   * @return whether there's a runtime match
   */
  static boolean matches(Pointcut pointcut, MethodInvocation invocation) {
    Assert.notNull(pointcut, "Pointcut is required");
    if (pointcut == Pointcut.TRUE) {
      return true;
    }
    final Class<?> targetClass = AopUtils.getTargetClass(invocation);
    if (pointcut.getClassFilter().matches(targetClass)) {
      // Only check if it gets past first hurdle.
      MethodMatcher mm = pointcut.getMethodMatcher();
      if (mm.matches(invocation.getMethod(), targetClass)) {
        // We may need additional runtime (argument) check.
        return (!mm.isRuntime() || mm.matches(invocation));
      }
    }
    return false;
  }

  /**
   * Perform the least expensive check for a pointcut match.
   *
   * @param pointcut the pointcut to match
   * @param method the candidate method
   * @param targetClass the target class
   * @param args arguments to the method
   * @return whether there's a runtime match
   */
  @SuppressWarnings("NullAway")
  static boolean matches(Pointcut pointcut, Method method, Class<?> targetClass, Object... args) {
    Assert.notNull(pointcut, "Pointcut is required");
    if (pointcut == Pointcut.TRUE) {
      return true;
    }
    if (pointcut.getClassFilter().matches(targetClass)) {
      // Only check if it gets past first hurdle.
      MethodMatcher mm = pointcut.getMethodMatcher();
      if (mm.matches(method, targetClass)) {
        // We may need additional runtime (argument) check.
        return !mm.isRuntime() || mm.matches(
                new DefaultMethodInvocation(null, method, targetClass, args));
      }
    }
    return false;
  }

  /**
   * Pointcut implementation that matches bean property setters.
   */
  class SetterPointcut extends StaticMethodMatcherPointcut implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final SetterPointcut INSTANCE = new SetterPointcut();

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      return method.getName().startsWith("set")
              && method.getParameterCount() == 1
              && method.getReturnType() == void.class;
    }

    @Serial
    private Object readResolve() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "Pointcuts.SETTERS";
    }
  }

  /**
   * Pointcut implementation that matches bean property getters.
   */
  class GetterPointcut extends StaticMethodMatcherPointcut implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final GetterPointcut INSTANCE = new GetterPointcut();

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
      return method.getName().startsWith("get")
              && method.getParameterCount() == 0
              && method.getReturnType() != void.class;
    }

    @Serial
    private Object readResolve() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "Pointcuts.GETTERS";
    }
  }

}
