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

package infra.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;

import infra.beans.testfixture.beans.TestBean;

abstract class ExposedInvocationTestBean extends TestBean {

  @Override
  public String getName() {
    MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
    assertions(invocation);
    return super.getName();
  }

  @Override
  public void absquatulate() {
    MethodInvocation invocation = ExposeInvocationInterceptor.currentInvocation();
    assertions(invocation);
    super.absquatulate();
  }

  protected abstract void assertions(MethodInvocation invocation);
}
