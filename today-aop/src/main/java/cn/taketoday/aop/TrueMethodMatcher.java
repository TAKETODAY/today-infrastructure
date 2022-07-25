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

package cn.taketoday.aop;

import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Canonical MethodMatcher instance that matches all methods.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:24
 * @since 3.0
 */
final class TrueMethodMatcher implements MethodMatcher, Serializable {
  private static final long serialVersionUID = 1L;

  public static final TrueMethodMatcher INSTANCE = new TrueMethodMatcher();

  /**
   * Enforce Singleton pattern.
   */
  private TrueMethodMatcher() { }

  @Override
  public boolean isRuntime() {
    return false;
  }

  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    return true;
  }

  @Override
  public boolean matches(MethodInvocation invocation) {
    // Should never be invoked as isRuntime returns false.
    throw new UnsupportedOperationException();
  }

  @Override
  public String toString() {
    return "MethodMatcher.TRUE";
  }

  /**
   * Required to support serialization. Replaces with canonical
   * instance on deserialization, protecting Singleton pattern.
   * Alternative to overriding {@code equals()}.
   */
  private Object readResolve() {
    return INSTANCE;
  }

}
