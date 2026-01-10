/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import infra.aop.ClassFilter;
import infra.aop.MethodMatcher;
import infra.aop.Pointcut;
import infra.aop.framework.ProxyFactory;
import infra.aop.interceptor.ExposeInvocationInterceptor;
import infra.aop.target.EmptyTargetSource;
import infra.aop.testfixture.interceptor.NopInterceptor;
import infra.beans.testfixture.beans.TestBean;
import infra.core.ResolvableType;
import infra.core.testfixture.io.SerializationTestUtils;
import infra.util.ReflectionUtils;

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
  void mostSpecificMethodBetweenJdkProxyAndTarget() throws Exception {
    Class<?> proxyClass = new ProxyFactory(new WithInterface()).getProxyClass(getClass().getClassLoader());
    Method specificMethod = AopUtils.getMostSpecificMethod(proxyClass.getMethod("handle", List.class), WithInterface.class);
    assertThat(ResolvableType.forMethodParameter(specificMethod, 0).getGeneric().toClass()).isEqualTo(String.class);
  }

  @Test
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
