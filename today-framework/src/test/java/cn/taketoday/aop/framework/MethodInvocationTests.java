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

package cn.taketoday.aop.framework;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 * @since 14.03.2003
 */
class MethodInvocationTests {

  @Test
  void testValidInvocation() throws Throwable {
    Method method = Object.class.getMethod("hashCode");
    Object proxy = new Object();
    Object returnValue = new Object();
    List<Object> interceptors = Collections.singletonList((MethodInterceptor) invocation -> returnValue);
    DefaultMethodInvocation invocation = new DefaultMethodInvocation(proxy, null, method, null, null, interceptors.toArray(new MethodInterceptor[0]));
    Object rv = invocation.proceed();
    assertThat(rv).as("correct response").isSameAs(returnValue);
  }

  /**
   * toString on target can cause failure.
   */
  @Test
  void testToStringDoesntHitTarget() throws Throwable {
    Object target = new TestBean() {
      @Override
      public String toString() {
        throw new UnsupportedOperationException("toString");
      }
    };
    List<Object> interceptors = Collections.emptyList();

    Method m = Object.class.getMethod("hashCode");
    Object proxy = new Object();
    DefaultMethodInvocation invocation = new DefaultMethodInvocation(proxy, target, m, null, null, interceptors.toArray(new MethodInterceptor[0]));

    // If it hits target, the test will fail with the UnsupportedOpException
    // in the inner class above.
    invocation.toString();
  }

}
