/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.support;

import org.aopalliance.intercept.MethodInvocation;

import infra.aop.MethodMatcher;
import infra.core.OrderedSupport;

/**
 * Convenient abstract superclass for static method matchers, which don't care
 * about arguments at runtime.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/1 18:33
 * @since 3.0
 */
public abstract class StaticMethodMatcher
        extends OrderedSupport implements MethodMatcher {

  @Override
  public final boolean isRuntime() {
    return false;
  }

  @Override
  public boolean matches(MethodInvocation invocation) {
    // should never be invoked because isRuntime() returns false
    throw new UnsupportedOperationException("Illegal MethodMatcher usage");
  }
}
