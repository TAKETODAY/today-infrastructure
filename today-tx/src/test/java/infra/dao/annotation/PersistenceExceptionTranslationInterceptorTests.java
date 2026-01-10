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

package infra.dao.annotation;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.aop.framework.ProxyFactory;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.Ordered;
import infra.core.annotation.AnnotationAwareOrderComparator;
import infra.core.annotation.AnnotationUtils;
import infra.dao.DataAccessException;
import infra.dao.support.ChainedPersistenceExceptionTranslator;
import infra.dao.support.MapPersistenceExceptionTranslator;
import infra.dao.support.PersistenceExceptionTranslationInterceptor;
import infra.dao.support.PersistenceExceptionTranslator;
import infra.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for standalone usage of a PersistenceExceptionTranslationInterceptor,
 * as explicit advice bean in a BeanFactory rather than applied as part of a
 * PersistenceExceptionTranslationAdvisor.
 *
 * @author Juergen Hoeller
 * @author Tadaya Tsuyukubo
 */
public class PersistenceExceptionTranslationInterceptorTests extends PersistenceExceptionTranslationAdvisorTests {

  @Override
  protected void addPersistenceExceptionTranslation(ProxyFactory pf, PersistenceExceptionTranslator pet) {
    if (AnnotationUtils.findAnnotation(pf.getTargetClass(), Repository.class) != null) {
      StandardBeanFactory bf = new StandardBeanFactory();
      bf.registerBeanDefinition("peti", new RootBeanDefinition(PersistenceExceptionTranslationInterceptor.class));
      bf.registerSingleton("pet", pet);
      pf.addAdvice((PersistenceExceptionTranslationInterceptor) bf.getBean("peti"));
    }
  }

  @Test
  void detectPersistenceExceptionTranslators() throws Throwable {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
    bf.registerBeanDefinition("peti", new RootBeanDefinition(PersistenceExceptionTranslationInterceptor.class));

    List<Integer> callOrder = new ArrayList<>();
    bf.registerSingleton("pet20", new CallOrderAwareExceptionTranslator(20, callOrder));
    bf.registerSingleton("pet10", new CallOrderAwareExceptionTranslator(10, callOrder));
    bf.registerSingleton("pet30", new CallOrderAwareExceptionTranslator(30, callOrder));

    PersistenceExceptionTranslationInterceptor interceptor =
            bf.getBean("peti", PersistenceExceptionTranslationInterceptor.class);
    interceptor.setAlwaysTranslate(true);

    RuntimeException exception = new RuntimeException();
    MethodInvocation invocation = mock();
    given(invocation.proceed()).willThrow(exception);

    assertThatThrownBy(() -> interceptor.invoke(invocation)).isSameAs(exception);
    assertThat(callOrder).containsExactly(10, 20, 30);
  }

  @Test
  void detectPersistenceExceptionTranslatorsOnShutdown() throws Throwable {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
    bf.registerBeanDefinition("peti", new RootBeanDefinition(PersistenceExceptionTranslationInterceptor.class));
    bf.registerBeanDefinition("pet", new RootBeanDefinition(ChainedPersistenceExceptionTranslator.class));

    PersistenceExceptionTranslationInterceptor interceptor =
            bf.getBean("peti", PersistenceExceptionTranslationInterceptor.class);
    interceptor.setAlwaysTranslate(true);

    RuntimeException exception = new RuntimeException();
    MethodInvocation invocation = mock();
    given(invocation.proceed()).willThrow(exception);

    AtomicBoolean correctException = new AtomicBoolean(false);
    bf.registerDisposableBean("disposable", () -> {
      try {
        interceptor.invoke(invocation);
      }
      catch (Throwable ex) {
        correctException.set(ex == exception);
      }
    });
    bf.destroySingletons();
    assertThat(correctException).isTrue();
  }

  @Test
  void constructorWithPersistenceExceptionTranslator() {
    PersistenceExceptionTranslator pet = new MapPersistenceExceptionTranslator();
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor(pet);

    assertThat(interceptor).isNotNull();
  }

  @Test
  void constructorWithBeanFactory() {
    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor(bf);

    assertThat(interceptor).isNotNull();
  }

  @Test
  void setPersistenceExceptionTranslator() {
    PersistenceExceptionTranslator pet = new MapPersistenceExceptionTranslator();
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor();
    interceptor.setPersistenceExceptionTranslator(pet);

    assertThat(interceptor).isNotNull();
  }

  @Test
  void setAlwaysTranslate() {
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor();
    interceptor.setAlwaysTranslate(true);

    assertThat(interceptor).isNotNull();
  }

  @Test
  void afterPropertiesSetWithoutTranslatorOrBeanFactory() {
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor();

    assertThatThrownBy(interceptor::afterPropertiesSet)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Property 'persistenceExceptionTranslator' is required");
  }

  @Test
  void afterPropertiesSetWithTranslator() {
    PersistenceExceptionTranslator pet = new MapPersistenceExceptionTranslator();
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor(pet);

    assertThatNoException().isThrownBy(interceptor::afterPropertiesSet);
  }

  @Test
  void afterPropertiesSetWithBeanFactory() {
    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor(bf);

    assertThatNoException().isThrownBy(interceptor::afterPropertiesSet);
  }

  @Test
  void invokeWithNoException() throws Throwable {
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor();
    MethodInvocation invocation = mock(MethodInvocation.class);
    given(invocation.proceed()).willReturn("result");

    Object result = interceptor.invoke(invocation);

    assertThat(result).isEqualTo("result");
  }

  @Test
  void invokeWithUndeclaredExceptionAndTranslator() throws Throwable {
    MapPersistenceExceptionTranslator pet = new MapPersistenceExceptionTranslator();
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor(pet);
    interceptor.setAlwaysTranslate(true);

    RuntimeException ex = new RuntimeException();
    MethodInvocation invocation = mock(MethodInvocation.class);
    given(invocation.proceed()).willThrow(ex);
    given(invocation.getMethod()).willReturn(Object.class.getMethod("toString"));

    assertThatThrownBy(() -> interceptor.invoke(invocation)).isSameAs(ex);
  }

  @Test
  void invokeWithBeanFactoryAndNoTranslator() throws Throwable {
    StandardBeanFactory bf = new StandardBeanFactory();
    PersistenceExceptionTranslationInterceptor interceptor = new PersistenceExceptionTranslationInterceptor(bf);

    RuntimeException ex = new RuntimeException();
    MethodInvocation invocation = mock(MethodInvocation.class);
    given(invocation.proceed()).willThrow(ex);
    given(invocation.getMethod()).willReturn(Object.class.getMethod("toString"));

    assertThatThrownBy(() -> interceptor.invoke(invocation)).isSameAs(ex);
  }

  private static class CallOrderAwareExceptionTranslator implements PersistenceExceptionTranslator, Ordered {

    private final int order;

    private final List<Integer> callOrder;

    public CallOrderAwareExceptionTranslator(int order, List<Integer> callOrder) {
      this.order = order;
      this.callOrder = callOrder;
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
      callOrder.add(this.order);
      return null;
    }

    @Override
    public int getOrder() {
      return this.order;
    }
  }

}
