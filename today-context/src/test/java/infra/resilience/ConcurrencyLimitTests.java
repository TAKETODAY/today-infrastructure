/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.resilience;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import infra.aop.framework.AopProxyUtils;
import infra.aop.framework.ProxyFactory;
import infra.aop.interceptor.ConcurrencyThrottleInterceptor;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.mock.env.MockPropertySource;
import infra.resilience.annotation.ConcurrencyLimit;
import infra.resilience.annotation.ConcurrencyLimitBeanPostProcessor;
import infra.resilience.annotation.EnableResilientMethods;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Juergen Hoeller
 * @since 5.0
 */
class ConcurrencyLimitTests {

  @Test
  void withSimpleInterceptor() {
    NonAnnotatedBean target = new NonAnnotatedBean();
    ProxyFactory pf = new ProxyFactory();
    pf.setTarget(target);
    pf.addAdvice(new ConcurrencyThrottleInterceptor(2));
    NonAnnotatedBean proxy = (NonAnnotatedBean) pf.getProxy();

    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
    assertThat(target.counter).hasValue(10);
  }

  @Test
  void withPostProcessorForMethod() {
    AnnotatedMethodBean proxy = createProxy(AnnotatedMethodBean.class);
    AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
  }

  @Test
  void withPostProcessorForMethodWithUnboundedConcurrency() {
    AnnotatedMethodBean proxy = createProxy(AnnotatedMethodBean.class);
    AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::unboundedConcurrency));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(10);
  }

  @Test
  void withPostProcessorForClass() {
    AnnotatedClassBean proxy = createProxy(AnnotatedClassBean.class);
    AnnotatedClassBean target = (AnnotatedClassBean) AopProxyUtils.getSingletonTarget(proxy);

    List<CompletableFuture<?>> futures = new ArrayList<>(30);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
    }
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::otherOperation));
    }
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::overrideOperation));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
  }

  @Test
  void withPlaceholderResolution() {
    MockPropertySource mockPropertySource = new MockPropertySource("test").withProperty("test.concurrency.limit", "3");
    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    ctx.getEnvironment().getPropertySources().addFirst(mockPropertySource);
    ctx.register(PlaceholderTestConfig.class, PlaceholderBean.class);
    ctx.refresh();

    PlaceholderBean proxy = ctx.getBean(PlaceholderBean.class);
    PlaceholderBean target = (PlaceholderBean) AopProxyUtils.getSingletonTarget(proxy);

    // Test with limit=3 from MockPropertySource
    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
    ctx.close();
  }

  @Test
  void configurationErrors() {
    ConfigurationErrorsBean proxy = createProxy(ConfigurationErrorsBean.class);

    assertThatIllegalStateException()
            .isThrownBy(proxy::emptyDeclaration)
            .withMessageMatching("@.+?ConcurrencyLimit(.+?) must be configured with a valid limit")
            .withMessageContaining(String.valueOf(Integer.MIN_VALUE));

    assertThatIllegalStateException()
            .isThrownBy(proxy::negative42Int)
            .withMessageMatching("@.+?ConcurrencyLimit(.+?) must be configured with a valid limit")
            .withMessageContaining("-42");

    assertThatIllegalStateException()
            .isThrownBy(proxy::negative42String)
            .withMessageMatching("@.+?ConcurrencyLimit(.+?) must be configured with a valid limit")
            .withMessageContaining("-42");

    assertThatExceptionOfType(NumberFormatException.class)
            .isThrownBy(proxy::alphanumericString)
            .withMessageContaining("B2");
  }

  private static <T> T createProxy(Class<T> beanClass) {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("bean", new RootBeanDefinition(beanClass));
    ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    return bf.getBean(beanClass);
  }

  static class NonAnnotatedBean {

    final AtomicInteger current = new AtomicInteger();

    final AtomicInteger counter = new AtomicInteger();

    public void concurrentOperation() {
      if (current.incrementAndGet() > 2) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      current.decrementAndGet();
      counter.incrementAndGet();
    }
  }

  static class AnnotatedMethodBean {

    final AtomicInteger current = new AtomicInteger();

    @ConcurrencyLimit(2)
    public void concurrentOperation() {
      if (current.incrementAndGet() > 2) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      current.decrementAndGet();
    }

    @ConcurrencyLimit(limit = -1)
    public void unboundedConcurrency() {
      current.incrementAndGet();
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  @ConcurrencyLimit(2)
  static class AnnotatedClassBean {

    final AtomicInteger current = new AtomicInteger();

    final AtomicInteger currentOverride = new AtomicInteger();

    public void concurrentOperation() {
      if (current.incrementAndGet() > 2) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      current.decrementAndGet();
    }

    public void otherOperation() {
      if (current.incrementAndGet() > 2) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      current.decrementAndGet();
    }

    @ConcurrencyLimit(limit = 1)
    public void overrideOperation() {
      if (currentOverride.incrementAndGet() > 1) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      currentOverride.decrementAndGet();
    }
  }

  @EnableResilientMethods
  static class PlaceholderTestConfig {
  }

  static class PlaceholderBean {

    final AtomicInteger current = new AtomicInteger();

    @ConcurrencyLimit(limitString = "${test.concurrency.limit}")
    public void concurrentOperation() {
      if (current.incrementAndGet() > 3) {  // Assumes test.concurrency.limit=3
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      current.decrementAndGet();
    }
  }

  static class ConfigurationErrorsBean {

    @ConcurrencyLimit
    public void emptyDeclaration() {
    }

    @ConcurrencyLimit(-42)
    public void negative42Int() {
    }

    @ConcurrencyLimit(limitString = "-42")
    public void negative42String() {
    }

    @ConcurrencyLimit(limitString = "B2")
    public void alphanumericString() {
    }
  }

}
