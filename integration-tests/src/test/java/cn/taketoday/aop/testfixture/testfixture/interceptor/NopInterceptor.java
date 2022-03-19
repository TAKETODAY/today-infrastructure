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

package cn.taketoday.aop.testfixture.testfixture.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Trivial interceptor that can be introduced in a chain to display it.
 *
 * @author Rod Johnson
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
