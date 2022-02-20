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

package cn.taketoday.scheduling.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import cn.taketoday.aop.proxy.ProxyFactory;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class AsyncAnnotationBeanPostProcessorTests {

  @Test
  public void proxyCreated() {
    ConfigurableApplicationContext context = initContext(
            new BeanDefinition(AsyncAnnotationBeanPostProcessor.class));
    Object target = context.getBean("target");
    assertThat(AopUtils.isAopProxy(target)).isTrue();
    context.close();
  }

  @Test
  public void invokedAsynchronously() {
    ConfigurableApplicationContext context = initContext(
            new BeanDefinition(AsyncAnnotationBeanPostProcessor.class));

    ITestBean testBean = context.getBean("target", ITestBean.class);
    testBean.test();
    Thread mainThread = Thread.currentThread();
    testBean.await(3000);
    Thread asyncThread = testBean.getThread();
    assertThat(asyncThread).isNotSameAs(mainThread);
    context.close();
  }

  @Test
  public void invokedAsynchronouslyOnProxyTarget() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBeanDefinition("postProcessor", new BeanDefinition(AsyncAnnotationBeanPostProcessor.class));
    TestBean tb = new TestBean();
    ProxyFactory pf = new ProxyFactory(ITestBean.class,
            (MethodInterceptor) invocation -> invocation.getMethod().invoke(tb, invocation.getArguments()));
    context.registerBean("target", ITestBean.class, () -> (ITestBean) pf.getProxy());
    context.refresh();

    ITestBean testBean = context.getBean("target", ITestBean.class);
    testBean.test();
    Thread mainThread = Thread.currentThread();
    testBean.await(3000);
    Thread asyncThread = testBean.getThread();
    assertThat(asyncThread).isNotSameAs(mainThread);
    context.close();
  }

  @Test
  public void threadNamePrefix() {
    BeanDefinition processorDefinition = new BeanDefinition(AsyncAnnotationBeanPostProcessor.class);
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("testExecutor");
    executor.afterPropertiesSet();
    processorDefinition.propertyValues().add("executor", executor);
    ConfigurableApplicationContext context = initContext(processorDefinition);

    ITestBean testBean = context.getBean("target", ITestBean.class);
    testBean.test();
    testBean.await(3000);
    Thread asyncThread = testBean.getThread();
    assertThat(asyncThread.getName().startsWith("testExecutor")).isTrue();
    context.close();
  }

  @Test
  public void taskExecutorByBeanType() {
    GenericApplicationContext context = new GenericApplicationContext();

    BeanDefinition processorDefinition = new BeanDefinition(AsyncAnnotationBeanPostProcessor.class);
    context.registerBeanDefinition("postProcessor", processorDefinition);

    BeanDefinition executorDefinition = new BeanDefinition(ThreadPoolTaskExecutor.class);
    executorDefinition.propertyValues().add("threadNamePrefix", "testExecutor");
    context.registerBeanDefinition("myExecutor", executorDefinition);

    BeanDefinition targetDefinition =
            new BeanDefinition(TestBean.class);
    context.registerBeanDefinition("target", targetDefinition);

    context.refresh();

    ITestBean testBean = context.getBean("target", ITestBean.class);
    testBean.test();
    testBean.await(3000);
    Thread asyncThread = testBean.getThread();
    assertThat(asyncThread.getName().startsWith("testExecutor")).isTrue();
    context.close();
  }

  @Test
  public void taskExecutorByBeanName() {
    GenericApplicationContext context = new GenericApplicationContext();

    BeanDefinition processorDefinition = new BeanDefinition(AsyncAnnotationBeanPostProcessor.class);
    context.registerBeanDefinition("postProcessor", processorDefinition);

    BeanDefinition executorDefinition = new BeanDefinition(ThreadPoolTaskExecutor.class);
    executorDefinition.propertyValues().add("threadNamePrefix", "testExecutor");
    context.registerBeanDefinition("myExecutor", executorDefinition);

    BeanDefinition executorDefinition2 = new BeanDefinition(ThreadPoolTaskExecutor.class);
    executorDefinition2.propertyValues().add("threadNamePrefix", "testExecutor2");
    context.registerBeanDefinition("taskExecutor", executorDefinition2);

    BeanDefinition targetDefinition =
            new BeanDefinition(TestBean.class);
    context.registerBeanDefinition("target", targetDefinition);

    context.refresh();

    ITestBean testBean = context.getBean("target", ITestBean.class);
    testBean.test();
    testBean.await(3000);
    Thread asyncThread = testBean.getThread();
    assertThat(asyncThread.getName().startsWith("testExecutor2")).isTrue();
    context.close();
  }

//
//  @Test
//  public void configuredThroughNamespace() {
//    GenericXmlApplicationContext context = new GenericXmlApplicationContext();
//    context.load(new ClassPathResource("taskNamespaceTests.xml", getClass()));
//    context.refresh();
//    ITestBean testBean = context.getBean("target", ITestBean.class);
//    testBean.test();
//    testBean.await(3000);
//    Thread asyncThread = testBean.getThread();
//    assertThat(asyncThread.getName().startsWith("testExecutor")).isTrue();
//
//    TestableAsyncUncaughtExceptionHandler exceptionHandler =
//            context.getBean("exceptionHandler", TestableAsyncUncaughtExceptionHandler.class);
//    assertThat(exceptionHandler.isCalled()).as("handler should not have been called yet").isFalse();
//
//    testBean.failWithVoid();
//    exceptionHandler.await(3000);
//    Method m = ReflectionUtils.findMethod(TestBean.class, "failWithVoid");
//    exceptionHandler.assertCalledWith(m, UnsupportedOperationException.class);
//    context.close();
//  }

  @Test
  @SuppressWarnings("resource")
  public void handleExceptionWithFuture() {
    ConfigurableApplicationContext context =
            new StandardApplicationContext(ConfigWithExceptionHandler.class);
    ITestBean testBean = context.getBean("target", ITestBean.class);

    TestableAsyncUncaughtExceptionHandler exceptionHandler =
            context.getBean("exceptionHandler", TestableAsyncUncaughtExceptionHandler.class);
    assertThat(exceptionHandler.isCalled()).as("handler should not have been called yet").isFalse();
    Future<Object> result = testBean.failWithFuture();
    assertFutureWithException(result, exceptionHandler);
  }

  @Test
  @SuppressWarnings("resource")
  public void handleExceptionWithListenableFuture() {
    ConfigurableApplicationContext context =
            new StandardApplicationContext(ConfigWithExceptionHandler.class);
    ITestBean testBean = context.getBean("target", ITestBean.class);

    TestableAsyncUncaughtExceptionHandler exceptionHandler =
            context.getBean("exceptionHandler", TestableAsyncUncaughtExceptionHandler.class);
    assertThat(exceptionHandler.isCalled()).as("handler should not have been called yet").isFalse();
    Future<Object> result = testBean.failWithListenableFuture();
    assertFutureWithException(result, exceptionHandler);
  }

  private void assertFutureWithException(Future<Object> result,
                                         TestableAsyncUncaughtExceptionHandler exceptionHandler) {
    assertThatExceptionOfType(ExecutionException.class).isThrownBy(
                    result::get)
            .withCauseExactlyInstanceOf(UnsupportedOperationException.class);
    assertThat(exceptionHandler.isCalled()).as("handler should never be called with Future return type").isFalse();
  }

  @Test
  public void handleExceptionWithCustomExceptionHandler() {
    Method m = ReflectionUtils.findMethod(TestBean.class, "failWithVoid");
    TestableAsyncUncaughtExceptionHandler exceptionHandler =
            new TestableAsyncUncaughtExceptionHandler();
    BeanDefinition processorDefinition = new BeanDefinition(AsyncAnnotationBeanPostProcessor.class);
    processorDefinition.propertyValues().add("exceptionHandler", exceptionHandler);

    ConfigurableApplicationContext context = initContext(processorDefinition);
    ITestBean testBean = context.getBean("target", ITestBean.class);

    assertThat(exceptionHandler.isCalled()).as("Handler should not have been called").isFalse();
    testBean.failWithVoid();
    exceptionHandler.await(3000);
    exceptionHandler.assertCalledWith(m, UnsupportedOperationException.class);
  }

  @Test
  public void exceptionHandlerThrowsUnexpectedException() {
    Method m = ReflectionUtils.findMethod(TestBean.class, "failWithVoid");
    TestableAsyncUncaughtExceptionHandler exceptionHandler =
            new TestableAsyncUncaughtExceptionHandler(true);
    BeanDefinition processorDefinition = new BeanDefinition(AsyncAnnotationBeanPostProcessor.class);
    processorDefinition.propertyValues().add("exceptionHandler", exceptionHandler);
    processorDefinition.propertyValues().add("executor", new DirectExecutor());

    ConfigurableApplicationContext context = initContext(processorDefinition);
    ITestBean testBean = context.getBean("target", ITestBean.class);

    assertThat(exceptionHandler.isCalled()).as("Handler should not have been called").isFalse();
    testBean.failWithVoid();
    exceptionHandler.assertCalledWith(m, UnsupportedOperationException.class);
  }

  private ConfigurableApplicationContext initContext(BeanDefinition asyncAnnotationBeanPostProcessorDefinition) {
    GenericApplicationContext context = new GenericApplicationContext();
    BeanDefinition targetDefinition = new BeanDefinition(TestBean.class);
    context.registerBeanDefinition("postProcessor", asyncAnnotationBeanPostProcessorDefinition);
    context.registerBeanDefinition("target", targetDefinition);
    context.refresh();
    return context;
  }

  private interface ITestBean {

    Thread getThread();

    @Async
    void test();

    Future<Object> failWithFuture();

    ListenableFuture<Object> failWithListenableFuture();

    void failWithVoid();

    void await(long timeout);
  }

  static class TestBean implements ITestBean {

    private Thread thread;

    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public Thread getThread() {
      return this.thread;
    }

    @Override
    @Async
    public void test() {
      this.thread = Thread.currentThread();
      this.latch.countDown();
    }

    @Async
    @Override
    public Future<Object> failWithFuture() {
      throw new UnsupportedOperationException("failWithFuture");
    }

    @Async
    @Override
    public ListenableFuture<Object> failWithListenableFuture() {
      throw new UnsupportedOperationException("failWithListenableFuture");
    }

    @Async
    @Override
    public void failWithVoid() {
      throw new UnsupportedOperationException("failWithVoid");
    }

    @Override
    public void await(long timeout) {
      try {
        this.latch.await(timeout, TimeUnit.MILLISECONDS);
      }
      catch (Exception e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static class DirectExecutor implements Executor {

    @Override
    public void execute(Runnable r) {
      r.run();
    }
  }

  @Configuration
  @EnableAsync
  static class ConfigWithExceptionHandler implements AsyncConfigurer {

    @Bean
    public ITestBean target() {
      return new TestBean();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
      return exceptionHandler();
    }

    @Bean
    public TestableAsyncUncaughtExceptionHandler exceptionHandler() {
      return new TestableAsyncUncaughtExceptionHandler();
    }
  }

}
