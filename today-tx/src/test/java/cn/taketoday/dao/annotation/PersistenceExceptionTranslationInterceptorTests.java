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

package cn.taketoday.dao.annotation;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.support.ChainedPersistenceExceptionTranslator;
import cn.taketoday.dao.support.PersistenceExceptionTranslationInterceptor;
import cn.taketoday.dao.support.PersistenceExceptionTranslator;
import cn.taketoday.stereotype.Repository;

import static org.assertj.core.api.Assertions.assertThat;
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
