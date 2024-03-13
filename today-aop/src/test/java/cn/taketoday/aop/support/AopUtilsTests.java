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

package cn.taketoday.aop.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.interceptor.ExposeInvocationInterceptor;
import cn.taketoday.aop.target.EmptyTargetSource;
import cn.taketoday.aop.testfixture.interceptor.NopInterceptor;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AopUtilsTests {

  @Test
  void testPointcutCanNeverApply() {
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
  void testPointcutAlwaysApplies() {
    assertThat(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), Object.class)).isTrue();
    assertThat(AopUtils.canApply(new DefaultPointcutAdvisor(new NopInterceptor()), TestBean.class)).isTrue();
  }

  @Test
  void testPointcutAppliesToOneMethodOnObject() {
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
  void testCanonicalFrameworkClassesStillCanonicalOnDeserialization() throws Exception {
    assertThat(SerializationTestUtils.serializeAndDeserialize(MethodMatcher.TRUE)).isSameAs(MethodMatcher.TRUE);
    assertThat(SerializationTestUtils.serializeAndDeserialize(ClassFilter.TRUE)).isSameAs(ClassFilter.TRUE);
    assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcut.TRUE)).isSameAs(Pointcut.TRUE);
    assertThat(SerializationTestUtils.serializeAndDeserialize(EmptyTargetSource.INSTANCE)).isSameAs(EmptyTargetSource.INSTANCE);
    assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcut.SETTERS)).isSameAs(Pointcut.SETTERS);
    assertThat(SerializationTestUtils.serializeAndDeserialize(Pointcut.GETTERS)).isSameAs(Pointcut.GETTERS);
    assertThat(SerializationTestUtils.serializeAndDeserialize(ExposeInvocationInterceptor.INSTANCE)).isSameAs(ExposeInvocationInterceptor.INSTANCE);
  }

  @Test
  void testInvokeJoinpointUsingReflection() throws Throwable {
    String name = "foo";
    TestBean testBean = new TestBean(name);
    Method method = ReflectionUtils.findMethod(TestBean.class, "getName");
    Object result = AopUtils.invokeJoinpointUsingReflection(testBean, method, new Object[0]);
    assertThat(result).isEqualTo(name);
  }

  @Test
    // gh-32365
  void mostSpecificMethodBetweenJdkProxyAndTarget() throws Exception {
    Class<?> proxyClass = new ProxyFactory(new WithInterface()).getProxyClass(getClass().getClassLoader());
    Method specificMethod = AopUtils.getMostSpecificMethod(proxyClass.getMethod("handle", List.class), WithInterface.class);
    assertThat(ResolvableType.forMethodParameter(specificMethod, 0).getGeneric().toClass()).isEqualTo(String.class);
  }

  @Test
    // gh-32365
  void mostSpecificMethodBetweenCglibProxyAndTarget() throws Exception {
    Class<?> proxyClass = new ProxyFactory(new WithoutInterface()).getProxyClass(getClass().getClassLoader());
    Method specificMethod = AopUtils.getMostSpecificMethod(proxyClass.getMethod("handle", List.class), WithoutInterface.class);
    assertThat(ResolvableType.forMethodParameter(specificMethod, 0).getGeneric().toClass()).isEqualTo(String.class);
  }

  interface ProxyInterface {

    void handle(List<String> list);
  }

  static class WithInterface implements ProxyInterface {

    public void handle(List<String> list) {
    }
  }

  static class WithoutInterface {

    public void handle(List<String> list) {
    }
  }

}
