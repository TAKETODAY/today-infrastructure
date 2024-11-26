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

package infra.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Trivial interceptor that can be introduced in a chain to display it.
 *
 * @author Rod Johnson
 * @author TODAY 2021/2/4 11:56
 */
public class NopInterceptor implements MethodInterceptor {

  private int count;

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    increment();
    return invocation.proceed();
  }

  protected void increment() {
    this.count++;
  }

  public int getCount() {
    return this.count;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof NopInterceptor)) {
      return false;
    }
    if (this == other) {
      return true;
    }
    return this.count == ((NopInterceptor) other).count;
  }

  @Override
  public int hashCode() {
    return NopInterceptor.class.hashCode();
  }

}
