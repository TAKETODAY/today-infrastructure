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

package cn.taketoday.aop.proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.SerializationTestUtils;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.Pointcuts;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.aop.support.interceptor.ExposeInvocationInterceptor;
import cn.taketoday.aop.target.EmptyTargetSource;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/2/3 23:37
 */

public class AopUtilsTests {

  @Test
  public void testPointcutCanNeverApply() {
    class TestPointcut extends StaticMethodMatcherPointcut {
      @Override
      public boolean matches(Method method, Class<?> clazzy) {
        return false;
      }
    }

    Pointcut no = new TestPointcut();
    assertThat(AopUtils.canApply(no, Object.class)).isFalse();
  }

  @Test
  public void testPointcutAlwaysApplies() {
    assertThat(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), Object.class)).isTrue();
    assertThat(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), TestBean.class)).isTrue();
  }

  @Test
  public void testPointcutAppliesToOneMethodOnObject() {
    class TestPointcut extends StaticMethodMatcherPointcut {
      @Override
      public boolean matches(Method method, Class<?> clazz) {
        return method.getName().equals("hashCode");
      }
    }

    Pointcut pc = new TestPointcut();

    // will return true if we're not proxying interfaces
    assertThat(AopUtils.canApply(pc, Object.class)).isTrue();
  }

  /**
   * Test that when we serialize and deserialize various canonical instances
   * of AOP classes, they return the same instance, not a new instance
   * that's subverted the singleton construction limitation.
   */
  @Test
  public void testCanonicalFrameworkClassesStillCanonicalOnDeserialization() throws Exception {
    assertThat(SerializationTestUtils.serializeAndDeserialize(MethodMatcher.TRUE)).isSameAs(MethodMatcher.TRUE);
    assertThat(SerializationTestUtils.serializeAndDeserialize(ClassFilter.TRUE)).isSameAs(ClassFilter.TRUE);
    assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcut.TRUE)).isSameAs(Pointcut.TRUE);
    assertThat(SerializationTestUtils.serializeAndDeserialize(EmptyTargetSource.INSTANCE)).isSameAs(EmptyTargetSource.INSTANCE);
    assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcuts.SETTERS)).isSameAs(Pointcuts.SETTERS);
    assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcuts.GETTERS)).isSameAs(Pointcuts.GETTERS);
    assertThat(SerializationTestUtils.serializeAndDeserialize(ExposeInvocationInterceptor.INSTANCE))
            .isSameAs(ExposeInvocationInterceptor.INSTANCE);
  }

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

}

