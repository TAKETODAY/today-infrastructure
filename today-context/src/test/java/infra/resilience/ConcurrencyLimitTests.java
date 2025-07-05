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
import infra.resilience.annotation.ConcurrencyLimit;
import infra.resilience.annotation.ConcurrencyLimitBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(target.counter).hasValue(0);
  }

  @Test
  void withPostProcessorForMethod() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedMethodBean.class));
    ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    AnnotatedMethodBean proxy = bf.getBean(AnnotatedMethodBean.class);
    AnnotatedMethodBean target = (AnnotatedMethodBean) AopProxyUtils.getSingletonTarget(proxy);

    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
  }

  @Test
  void withPostProcessorForClass() {
    StandardBeanFactory bf = new StandardBeanFactory();
    bf.registerBeanDefinition("bean", new RootBeanDefinition(AnnotatedClassBean.class));
    ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    AnnotatedClassBean proxy = bf.getBean(AnnotatedClassBean.class);
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

  static class NonAnnotatedBean {

    AtomicInteger counter = new AtomicInteger();

    public void concurrentOperation() {
      if (counter.incrementAndGet() > 2) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      counter.decrementAndGet();
    }
  }

  static class AnnotatedMethodBean {

    AtomicInteger current = new AtomicInteger();

    @ConcurrencyLimit(2)
    public void concurrentOperation() {
      if (current.incrementAndGet() > 2) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      current.decrementAndGet();
    }
  }

  @ConcurrencyLimit(2)
  static class AnnotatedClassBean {

    AtomicInteger current = new AtomicInteger();

    AtomicInteger currentOverride = new AtomicInteger();

    public void concurrentOperation() {
      if (current.incrementAndGet() > 2) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(100);
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
        Thread.sleep(100);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      current.decrementAndGet();
    }

    @ConcurrencyLimit(1)
    public void overrideOperation() {
      if (currentOverride.incrementAndGet() > 1) {
        throw new IllegalStateException();
      }
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
      currentOverride.decrementAndGet();
    }
  }

}
