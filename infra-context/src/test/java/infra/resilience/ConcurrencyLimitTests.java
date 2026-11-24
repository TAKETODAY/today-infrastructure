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

package infra.resilience;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import infra.aop.config.AopConfigUtils;
import infra.aop.framework.AopProxyUtils;
import infra.aop.framework.ProxyFactory;
import infra.aop.interceptor.ConcurrencyThrottleInterceptor;
import infra.aop.support.AopUtils;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.Ordered;
import infra.mock.env.MockPropertySource;
import infra.resilience.annotation.ConcurrencyLimit;
import infra.resilience.annotation.ConcurrencyLimitBeanPostProcessor;
import infra.resilience.annotation.EnableResilientMethods;
import infra.resilience.annotation.ResilientMethodsConfiguration;

import static infra.resilience.annotation.ConcurrencyLimit.ThrottlePolicy.REJECT;
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
  void withPostProcessorForMethodWithInterface() {
    AnnotatedInterface proxy = createProxy(AnnotatedMethodBeanWithInterface.class, AnnotatedInterface.class, false);
    AnnotatedMethodBeanWithInterface target = (AnnotatedMethodBeanWithInterface) AopProxyUtils.getSingletonTarget(proxy);

    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
  }

  @Test
  void withPostProcessorForMethodWithInterfaceAndDefaultTargetClass() {
    AnnotatedInterface proxy = createProxy(AnnotatedMethodBeanWithInterface.class, AnnotatedInterface.class, true);
    AnnotatedMethodBeanWithInterface target = (AnnotatedMethodBeanWithInterface) AopProxyUtils.getSingletonTarget(proxy);

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
  void withPostProcessorForMethodWithPlainInterface() {
    PlainInterface proxy = createProxy(AnnotatedMethodBeanWithPlainInterface.class, PlainInterface.class, false);
    AnnotatedMethodBeanWithPlainInterface target = (AnnotatedMethodBeanWithPlainInterface) AopProxyUtils.getSingletonTarget(proxy);

    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
  }

  @Test
  void withPostProcessorForMethodWithPlainInterfaceAndDefaultTargetClass() {
    PlainInterface proxy = createProxy(AnnotatedMethodBeanWithPlainInterface.class, PlainInterface.class, true);
    AnnotatedMethodBeanWithPlainInterface target = (AnnotatedMethodBeanWithPlainInterface) AopProxyUtils.getSingletonTarget(proxy);

    List<CompletableFuture<?>> futures = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) {
      futures.add(CompletableFuture.runAsync(proxy::concurrentOperation));
    }
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    assertThat(target.current).hasValue(0);
  }

  @Test
  void withPostProcessorForMethodWithRejection() throws Exception {
    AnnotatedMethodBean proxy = createProxy(AnnotatedMethodBean.class);
    try (RunningInvocation invocation = new RunningInvocation(proxy::rejectingOperation)) {
      invocation.awaitEntry();
      assertThatExceptionOfType(InvocationRejectedException.class)
              .isThrownBy(() -> proxy.rejectingOperation(() -> { }))
              .withMessageContaining("Concurrency limit reached: 1", AnnotatedMethodBean.class.getName() + ".rejectingOperation")
              .extracting(InvocationRejectedException::getTarget).isSameAs(proxy);
    }

    assertThat(proxy.rejectingOperation(() -> { })).isEqualTo("completed");
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
  void withPostProcessorForClassWithRejection() throws Exception {
    AnnotatedClassBeanWithRejection proxy = createProxy(AnnotatedClassBeanWithRejection.class);
    try (RunningInvocation invocation = new RunningInvocation(proxy::concurrentOperation)) {
      invocation.awaitEntry();
      assertThatExceptionOfType(InvocationRejectedException.class)
              .isThrownBy(proxy::otherOperation)
              .withMessageContaining("Concurrency limit reached: 1", AnnotatedClassBeanWithRejection.class.getName())
              .extracting(InvocationRejectedException::getTarget).isSameAs(proxy);
      assertThat(proxy.overrideOperation()).isEqualTo("override");
    }

    assertThat(proxy.otherOperation()).isEqualTo("completed");
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
  void blankResolvedLimitFallsBackToNumericLimit() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment().getPropertySources().addFirst(
              new MockPropertySource("test").withProperty("test.concurrency.limit", " "));
      context.register(PlaceholderTestConfig.class, PlaceholderBean.class);
      context.refresh();
      PlaceholderBean proxy = context.getBean(PlaceholderBean.class);

      assertThatExceptionOfType(InvocationRejectedException.class)
              .isThrownBy(proxy::disabledOperation)
              .withMessageContaining("NO_CONCURRENCY", PlaceholderBean.class.getName() + ".disabledOperation")
              .extracting(InvocationRejectedException::getTarget).isSameAs(proxy);
    }
  }

  @Test
  void enableResilientMethodsAppliesProxyTypeAndProcessorOrder() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            ClassProxyTestConfig.class, AnnotatedMethodBeanWithPlainInterface.class)) {
      ConcurrencyLimitBeanPostProcessor processor = context.getBean(ConcurrencyLimitBeanPostProcessor.class);
      assertThat(processor.getOrder()).isEqualTo(42);
      PlainInterface proxy = context.getBean(PlainInterface.class);
      assertThat(AopUtils.isCglibProxy(proxy)).isTrue();
    }
  }

  @Test
  void configurationWithoutImportMetadataRetainsDefaultProxySettings() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            ResilientMethodsConfiguration.class, AnnotatedMethodBeanWithPlainInterface.class)) {
      ConcurrencyLimitBeanPostProcessor processor = context.getBean(ConcurrencyLimitBeanPostProcessor.class);
      assertThat(processor.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
      PlainInterface proxy = context.getBean(PlainInterface.class);
      assertThat(AopUtils.isJdkDynamicProxy(proxy)).isTrue();
    }
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
    return createProxy(beanClass, beanClass, true);
  }

  private static <T> T createProxy(Class<?> beanClass, Class<T> exposedClass, boolean proxyTargetClass) {
    StandardBeanFactory bf = new StandardBeanFactory();
    if (exposedClass.isInterface() && proxyTargetClass) {
      AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(bf);
    }
    bf.registerBeanDefinition("bean", new RootBeanDefinition(beanClass));
    ConcurrencyLimitBeanPostProcessor bpp = new ConcurrencyLimitBeanPostProcessor();
    bpp.setBeanFactory(bf);
    bf.addBeanPostProcessor(bpp);
    T proxy = bf.getBean(exposedClass);
    assertThat(proxyTargetClass ? AopUtils.isCglibProxy(proxy) : AopUtils.isJdkDynamicProxy(proxy)).isTrue();
    return proxy;
  }

  private static class RunningInvocation implements AutoCloseable {

    private final CountDownLatch entered = new CountDownLatch(1);

    private final CountDownLatch release = new CountDownLatch(1);

    private final CompletableFuture<Void> invocation;

    RunningInvocation(Consumer<Runnable> operation) {
      this.invocation = CompletableFuture.runAsync(() -> operation.accept(() -> {
        this.entered.countDown();
        await(this.release);
      }));
    }

    void awaitEntry() {
      await(this.entered);
    }

    @Override
    public void close() throws Exception {
      this.release.countDown();
      this.invocation.get(5, TimeUnit.SECONDS);
    }

    private static void await(CountDownLatch latch) {
      try {
        assertThat(latch.await(5, TimeUnit.SECONDS)).as("invocation synchronization").isTrue();
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new AssertionError(ex);
      }
    }
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
        Thread.sleep(100);
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
        Thread.sleep(100);
      }
      catch (InterruptedException ex) {
        throw new IllegalStateException(ex);
      }
    }

    @ConcurrencyLimit(limit = 1, policy = REJECT)
    public String rejectingOperation(Runnable action) {
      action.run();
      return "completed";
    }
  }

  static class AnnotatedMethodBeanWithInterface implements AnnotatedInterface {

    final AtomicInteger current = new AtomicInteger();

    @Override
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

  interface AnnotatedInterface {

    @ConcurrencyLimit(2)
    void concurrentOperation();
  }

  static class AnnotatedMethodBeanWithPlainInterface implements PlainInterface {

    final AtomicInteger current = new AtomicInteger();

    @Override
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

  interface PlainInterface {

    void concurrentOperation();
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

    @ConcurrencyLimit(limit = 1)
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

  @ConcurrencyLimit(limit = 1, policy = REJECT)
  static class AnnotatedClassBeanWithRejection {

    public void concurrentOperation(Runnable action) {
      action.run();
    }

    public String otherOperation() {
      return "completed";
    }

    @ConcurrencyLimit(limit = 1, policy = REJECT)
    public String overrideOperation() {
      return "override";
    }
  }

  @EnableResilientMethods
  static class PlaceholderTestConfig {
  }

  @EnableResilientMethods(proxyTargetClass = true, order = 42)
  static class ClassProxyTestConfig {
  }

  static class PlaceholderBean {

    final AtomicInteger current = new AtomicInteger();

    @ConcurrencyLimit(limitString = "${test.concurrency.limit}")
    public void concurrentOperation() {
      if (current.incrementAndGet() > 3) {  // Assumes test.concurrency.limit=3
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

    @ConcurrencyLimit(limit = 0, limitString = "${test.concurrency.limit}")
    public void disabledOperation() {
      throw new AssertionError("Invocation should be rejected before entering the method");
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
