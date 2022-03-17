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

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

import cn.taketoday.aop.MethodMatcher;

/**
 * Convenient abstract superclass for dynamic method matchers,
 * which do care about arguments at runtime.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/4 11:48
 * @since 3.0
 */
public abstract class DynamicMethodMatcher implements MethodMatcher {

  @Override
  public final boolean isRuntime() {
    return true;
  }

  /**
   * Can override to add preconditions for dynamic matching. This implementation
   * always returns true.
   */
  @Override
  public boolean matches(Method method, Class<?> targetClass) {
    return true;
  }

  protected Class<?> getTargetClass(MethodInvocation invocation) {
    return AopUtils.getTargetClass(invocation);
  }

}
