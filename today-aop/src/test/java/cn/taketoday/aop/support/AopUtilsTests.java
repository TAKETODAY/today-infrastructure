/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.interceptor.ExposeInvocationInterceptor;
import cn.taketoday.aop.target.EmptyTargetSource;
import cn.taketoday.aop.testfixture.interceptor.NopInterceptor;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AopUtilsTests {

  @Test
  public void testPointcutCanNeverApply() {
    class TestPointcut extends StaticMethodMatcherPointcut {
      @Override
      public boolean matches(Method method, @Nullable Class<?> clazzy) {
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
      public boolean matches(Method method, @Nullable Class<?> clazz) {
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
    assertThat(SerializationTestUtils.serializeAndDeserialize(ExposeInvocationInterceptor.INSTANCE)).isSameAs(ExposeInvocationInterceptor.INSTANCE);
  }

}
