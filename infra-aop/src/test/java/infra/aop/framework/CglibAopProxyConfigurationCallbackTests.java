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

package infra.aop.framework;

import org.junit.jupiter.api.Test;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.util.Set;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/** Regression tests for final methods declared on container callback interfaces. */
class CglibAopProxyConfigurationCallbackTests {

  @Test
  void finalAfterPropertiesSetIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalAfterPropertiesSet.class, "afterPropertiesSet", true);
  }

  @Test
  void finalDestroyIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalDestroy.class, "destroy", true);
  }

  @Test
  void finalAwareCallbackIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalBeanFactoryAware.class, "setBeanFactory", true, BeanFactory.class);
  }

  @Test
  void finalCloseableCloseIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalCloseableClose.class, "close", true);
  }

  @Test
  void finalAutoCloseableCloseIsRecognisedAsCallback() throws NoSuchMethodException {
    assertCallback(WithFinalAutoCloseableClose.class, "close", true);
  }

  @Test
  void finalUserInterfaceMethodIsNotSuppressed() throws NoSuchMethodException {
    assertCallback(WithFinalUserApi.class, "execute", false);
  }

  @Test
  void methodSharedBetweenCallbackAndUserInterfaceIsNotSuppressed() throws NoSuchMethodException {
    assertCallback(WithSharedSignature.class, "afterPropertiesSet", false);
  }

  @Test
  void finalMethodWithoutInterfaceMatchIsNotSuppressed() throws NoSuchMethodException {
    assertCallback(WithStandaloneFinal.class, "doSomething", false);
  }

  private static void assertCallback(Class<?> type, String name, boolean expected, Class<?>... parameterTypes)
          throws NoSuchMethodException {
    Method method = type.getDeclaredMethod(name, parameterTypes);
    Set<Class<?>> interfaces = ClassUtils.getAllInterfacesForClassAsSet(type);
    assertThat(CglibAopProxy.implementsOnlyConfigurationCallbackInterfaces(method, interfaces)).isEqualTo(expected);
  }

  static class WithFinalAfterPropertiesSet implements InitializingBean {
    @Override
    public final void afterPropertiesSet() { }
  }

  static class WithFinalDestroy implements DisposableBean {
    @Override
    public final void destroy() { }
  }

  static class WithFinalBeanFactoryAware implements BeanFactoryAware {
    @Override
    public final void setBeanFactory(BeanFactory beanFactory) { }
  }

  static class WithFinalCloseableClose implements Closeable {
    @Override
    public final void close() { }
  }

  static class WithFinalAutoCloseableClose implements AutoCloseable {
    @Override
    public final void close() { }
  }

  interface UserApi {
    void execute();
  }

  static class WithFinalUserApi implements UserApi {
    @Override
    public final void execute() { }
  }

  interface CustomLifecycle {
    void afterPropertiesSet();
  }

  static class WithSharedSignature implements InitializingBean, CustomLifecycle {
    @Override
    public final void afterPropertiesSet() { }
  }

  static class WithStandaloneFinal {
    public final void doSomething() { }
  }
}
