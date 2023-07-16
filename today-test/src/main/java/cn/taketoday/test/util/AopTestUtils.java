/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.util;

import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.AopProxyUtils;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.lang.Assert;

/**
 * {@code AopTestUtils} is a collection of AOP-related utility methods for
 * use in unit and integration testing scenarios.
 *
 * <p>For Framework's core AOP utilities, see
 * {@link cn.taketoday.aop.support.AopUtils AopUtils} and
 * {@link cn.taketoday.aop.framework.AopProxyUtils AopProxyUtils}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see cn.taketoday.aop.support.AopUtils
 * @see cn.taketoday.aop.framework.AopProxyUtils
 * @see ReflectionTestUtils
 * @since 4.0
 */
public abstract class AopTestUtils {

  /**
   * Get the <em>target</em> object of the supplied {@code candidate} object.
   * <p>If the supplied {@code candidate} is a Infra
   * {@linkplain AopUtils#isAopProxy proxy}, the target of the proxy will
   * be returned; otherwise, the {@code candidate} will be returned
   * <em>as is</em>.
   *
   * @param <T> the type of the target object
   * @param candidate the instance to check (potentially a Infra AOP proxy;
   * never {@code null})
   * @return the target object or the {@code candidate} (never {@code null})
   * @throws IllegalStateException if an error occurs while unwrapping a proxy
   * @see Advised#getTargetSource()
   * @see #getUltimateTargetObject
   */
  @SuppressWarnings("unchecked")
  public static <T> T getTargetObject(Object candidate) {
    Assert.notNull(candidate, "Candidate must not be null");
    try {
      if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised advised) {
        Object target = advised.getTargetSource().getTarget();
        if (target != null) {
          return (T) target;
        }
      }
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Failed to unwrap proxied object", ex);
    }
    return (T) candidate;
  }

  /**
   * Get the ultimate <em>target</em> object of the supplied {@code candidate}
   * object, unwrapping not only a top-level proxy but also any number of
   * nested proxies.
   * <p>If the supplied {@code candidate} is a Infra
   * {@linkplain AopUtils#isAopProxy proxy}, the ultimate target of all
   * nested proxies will be returned; otherwise, the {@code candidate}
   * will be returned <em>as is</em>.
   * <p>NOTE: If the top-level proxy or a nested proxy is not backed by a
   * {@linkplain TargetSource#isStatic() static}
   * {@link TargetSource TargetSource}, invocation of
   * this utility method may result in undesired behavior such as infinite
   * recursion leading to a {@link StackOverflowError}.
   *
   * @param <T> the type of the target object
   * @param candidate the instance to check (potentially a Infra AOP proxy;
   * never {@code null})
   * @return the target object or the {@code candidate} (never {@code null})
   * @throws IllegalStateException if an error occurs while unwrapping a proxy
   * @see Advised#getTargetSource()
   * @see TargetSource#isStatic()
   * @see AopProxyUtils#ultimateTargetClass
   */
  @SuppressWarnings("unchecked")
  public static <T> T getUltimateTargetObject(Object candidate) {
    Assert.notNull(candidate, "Candidate must not be null");
    try {
      if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised advised) {
        Object target = advised.getTargetSource().getTarget();
        if (target != null) {
          return getUltimateTargetObject(target);
        }
      }
    }
    catch (Throwable ex) {
      throw new IllegalStateException("Failed to unwrap proxied object", ex);
    }
    return (T) candidate;
  }

}
