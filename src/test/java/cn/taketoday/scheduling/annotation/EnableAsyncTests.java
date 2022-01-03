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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.proxy.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.interceptor.AsyncUncaughtExceptionHandler;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Singleton;
import cn.taketoday.scheduling.concurrent.CustomizableThreadFactory;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests use of @EnableAsync on @Configuration classes.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 4.0
 */
public class EnableAsyncTests {

  @Test
  public void proxyingOccurs() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AsyncConfig.class);
    ctx.refresh();

    AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
    assertThat(AopUtils.isAopProxy(asyncBean)).isTrue();
    asyncBean.work();
    ctx.close();
  }

  @Test
  public void proxyingOccursWithMockitoStub() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AsyncConfigWithMockito.class, AsyncBeanUser.class);
    ctx.refresh();

    AsyncBeanUser asyncBeanUser = ctx.getBean(AsyncBeanUser.class);
    AsyncBean asyncBean = asyncBeanUser.getAsyncBean();
    assertThat(AopUtils.isAopProxy(asyncBean)).isTrue();
    asyncBean.work();
    ctx.close();
  }

/*
 @Test
  public void properExceptionForExistingProxyDependencyMismatch() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AsyncConfig.class, AsyncBeanWithInterface.class, AsyncBeanUser.class);
    assertThatExceptionOfType(UnsatisfiedDependencyException.class)
            .isThrownBy(ctx::refresh)
            .withCauseInstanceOf(BeanNotOfRequiredTypeException.class);
    ctx.close();
  }
*/

/*
  @Test
  public void properExceptionForResolvedProxyDependencyMismatch() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AsyncConfig.class, AsyncBeanUser.class, AsyncBeanWithInterface.class);
    assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
                    ctx::refresh)
            .withCauseInstanceOf(BeanNotOfRequiredTypeException.class);
    ctx.close();
  }
*/

  @Test
  public void withAsyncBeanWithExecutorQualifiedByName() throws ExecutionException, InterruptedException {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AsyncWithExecutorQualifiedByNameConfig.class);
    ctx.refresh();

    AsyncBeanWithExecutorQualifiedByName asyncBean = ctx.getBean(AsyncBeanWithExecutorQualifiedByName.class);
    Future<Thread> workerThread0 = asyncBean.work0();
    assertThat(workerThread0.get().getName()).doesNotStartWith("e1-").doesNotStartWith("otherExecutor-");
    Future<Thread> workerThread = asyncBean.work();
    assertThat(workerThread.get().getName()).startsWith("e1-");
    Future<Thread> workerThread2 = asyncBean.work2();
    assertThat(workerThread2.get().getName()).startsWith("otherExecutor-");
    Future<Thread> workerThread3 = asyncBean.work3();
    assertThat(workerThread3.get().getName()).startsWith("otherExecutor-");

    ctx.close();
  }

  @Test
  public void asyncProcessorIsOrderedLowestPrecedenceByDefault() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AsyncConfig.class);
    ctx.refresh();

    AsyncAnnotationBeanPostProcessor bpp = ctx.getBean(AsyncAnnotationBeanPostProcessor.class);
    assertThat(bpp.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);

    ctx.close();
  }

  @Test
  public void orderAttributeIsPropagated() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(OrderedAsyncConfig.class);
    ctx.refresh();

    AsyncAnnotationBeanPostProcessor bpp = ctx.getBean(AsyncAnnotationBeanPostProcessor.class);
    assertThat(bpp.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);

    ctx.close();
  }

  @Test
  public void customAsyncAnnotationIsPropagated() {
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(CustomAsyncAnnotationConfig.class, CustomAsyncBean.class);
    ctx.refresh();

    Object bean = ctx.getBean(CustomAsyncBean.class);
    assertThat(AopUtils.isAopProxy(bean)).isTrue();
    boolean isAsyncAdvised = false;
    for (Advisor advisor : ((Advised) bean).getAdvisors()) {
      if (advisor instanceof AsyncAnnotationAdvisor) {
        isAsyncAdvised = true;
        break;
      }
    }
    assertThat(isAsyncAdvised).as("bean was not async advised as expected").isTrue();

    ctx.close();
  }

  /**
   * Fails with classpath errors on trying to classload AnnotationAsyncExecutionAspect.
   */
  @Test
  public void aspectModeAspectJAttemptsToRegisterAsyncAspect() {
    @SuppressWarnings("resource")
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(AspectJAsyncAnnotationConfig.class);
    assertThatExceptionOfType(ApplicationContextException.class).isThrownBy(
            ctx::refresh);
  }

  @Test
  public void customExecutorBean() {
    // Arrange
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(CustomExecutorBean.class);
    ctx.refresh();
    AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
    // Act
    asyncBean.work();
    // Assert
    Awaitility.await()
            .atMost(500, TimeUnit.MILLISECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> asyncBean.getThreadOfExecution() != null);
    assertThat(asyncBean.getThreadOfExecution().getName()).startsWith("Custom-");
    ctx.close();
  }

  @Test
  public void customExecutorConfig() {
    // Arrange
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(CustomExecutorConfig.class);
    ctx.refresh();
    AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
    // Act
    asyncBean.work();
    // Assert
    Awaitility.await()
            .atMost(500, TimeUnit.MILLISECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> asyncBean.getThreadOfExecution() != null);
    assertThat(asyncBean.getThreadOfExecution().getName()).startsWith("Custom-");
    ctx.close();
  }

  @Test
  public void customExecutorConfigWithThrowsException() {
    // Arrange
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(CustomExecutorConfig.class);
    ctx.refresh();
    AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
    Method method = ReflectionUtils.findMethod(AsyncBean.class, "fail");
    TestableAsyncUncaughtExceptionHandler exceptionHandler =
            (TestableAsyncUncaughtExceptionHandler) ctx.getBean("exceptionHandler");
    assertThat(exceptionHandler.isCalled()).as("handler should not have been called yet").isFalse();
    // Act
    asyncBean.fail();
    // Assert
    Awaitility.await()
            .atMost(500, TimeUnit.MILLISECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> exceptionHandler.assertCalledWith(method, UnsupportedOperationException.class));
    ctx.close();
  }

  @Test
  public void customExecutorBeanConfig() {
    // Arrange
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(CustomExecutorBeanConfig.class, ExecutorPostProcessor.class);
    ctx.refresh();
    AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
    // Act
    asyncBean.work();
    // Assert
    Awaitility.await()
            .atMost(500, TimeUnit.MILLISECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> asyncBean.getThreadOfExecution() != null);
    assertThat(asyncBean.getThreadOfExecution().getName()).startsWith("Post-");
    ctx.close();
  }

  @Test
  public void customExecutorBeanConfigWithThrowsException() {
    // Arrange
    StandardApplicationContext ctx = new StandardApplicationContext();
    ctx.register(CustomExecutorBeanConfig.class, ExecutorPostProcessor.class);
    ctx.refresh();
    AsyncBean asyncBean = ctx.getBean(AsyncBean.class);
    TestableAsyncUncaughtExceptionHandler exceptionHandler =
            (TestableAsyncUncaughtExceptionHandler) ctx.getBean("exceptionHandler");
    assertThat(exceptionHandler.isCalled()).as("handler should not have been called yet").isFalse();
    Method method = ReflectionUtils.findMethod(AsyncBean.class, "fail");
    // Act
    asyncBean.fail();
    // Assert
    Awaitility.await()
            .atMost(500, TimeUnit.MILLISECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> exceptionHandler.assertCalledWith(method, UnsupportedOperationException.class));
    ctx.close();
  }

  @Test  // SPR-14949
  public void findOnInterfaceWithInterfaceProxy() {
    // Arrange
    StandardApplicationContext ctx = new StandardApplicationContext(Spr14949ConfigA.class);
    AsyncInterface asyncBean = ctx.getBean(AsyncInterface.class);
    // Act
    asyncBean.work();
    // Assert
    Awaitility.await()
            .atMost(500, TimeUnit.MILLISECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> asyncBean.getThreadOfExecution() != null);
    assertThat(asyncBean.getThreadOfExecution().getName()).startsWith("Custom-");
    ctx.close();
  }

  @Test  // SPR-14949
  public void findOnInterfaceWithCglibProxy() {
    // Arrange
    StandardApplicationContext ctx = new StandardApplicationContext(Spr14949ConfigB.class);
    AsyncInterface asyncBean = ctx.getBean(AsyncInterface.class);
    // Act
    asyncBean.work();
    // Assert
    Awaitility.await()
            .atMost(500, TimeUnit.MILLISECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> asyncBean.getThreadOfExecution() != null);
    assertThat(asyncBean.getThreadOfExecution().getName()).startsWith("Custom-");
    ctx.close();
  }

  @Test
  @SuppressWarnings("resource")
  public void exceptionThrownWithBeanNotOfRequiredTypeRootCause() {
    assertThatExceptionOfType(Throwable.class)
            .isThrownBy(() -> new StandardApplicationContext(JdkProxyConfiguration.class))
            .withCauseInstanceOf(UnsatisfiedDependencyException.class);
  }

  static class AsyncBeanWithExecutorQualifiedByName {

    @Async
    public Future<Thread> work0() {
      return new AsyncResult<>(Thread.currentThread());
    }

    @Async("e1")
    public Future<Thread> work() {
      return new AsyncResult<>(Thread.currentThread());
    }

    @Async("otherExecutor")
    public Future<Thread> work2() {
      return new AsyncResult<>(Thread.currentThread());
    }

    @Async("e2")
    public Future<Thread> work3() {
      return new AsyncResult<>(Thread.currentThread());
    }
  }

  static class AsyncBean {

    private Thread threadOfExecution;

    @Async
    public void work() {
      this.threadOfExecution = Thread.currentThread();
    }

    @Async
    public void fail() {
      throw new UnsupportedOperationException();
    }

    public Thread getThreadOfExecution() {
      return threadOfExecution;
    }
  }

  @Component("asyncBean")
  static class AsyncBeanWithInterface extends AsyncBean implements Runnable {

    @Override
    public void run() {
    }
  }

  static class AsyncBeanUser {

    private final AsyncBean asyncBean;

    public AsyncBeanUser(AsyncBean asyncBean) {
      this.asyncBean = asyncBean;
    }

    public AsyncBean getAsyncBean() {
      return asyncBean;
    }
  }

  @EnableAsync(annotation = CustomAsync.class)
  static class CustomAsyncAnnotationConfig {
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface CustomAsync {
  }

  static class CustomAsyncBean {

    @CustomAsync
    public void work() {
    }
  }

  @Configuration
  @EnableAsync(order = Ordered.HIGHEST_PRECEDENCE)
  static class OrderedAsyncConfig {

    @Singleton
    public AsyncBean asyncBean() {
      return new AsyncBean();
    }
  }

  @Configuration
  @EnableAsync(mode = AdviceMode.ASPECTJ)
  static class AspectJAsyncAnnotationConfig {

    @Singleton
    public AsyncBean asyncBean() {
      return new AsyncBean();
    }
  }

  @Configuration
  @EnableAsync
  static class AsyncConfig {

    @Singleton
    public AsyncBean asyncBean() {
      return new AsyncBean();
    }
  }

  @Configuration
  @EnableAsync
  static class AsyncConfigWithMockito {

    @Singleton
    @Lazy
    public AsyncBean asyncBean() {
      return Mockito.mock(AsyncBean.class);
    }
  }

  @Configuration
  @EnableAsync
  static class AsyncWithExecutorQualifiedByNameConfig {

    @Singleton
    public AsyncBeanWithExecutorQualifiedByName asyncBean() {
      return new AsyncBeanWithExecutorQualifiedByName();
    }

    @Singleton
    public Executor e1() {
      return new ThreadPoolTaskExecutor();
    }

    @Singleton
    @Qualifier("e2")
    public Executor otherExecutor() {
      return new ThreadPoolTaskExecutor();
    }
  }

  @Configuration
  @EnableAsync
  static class CustomExecutorBean {

    @Singleton
    public AsyncBean asyncBean() {
      return new AsyncBean();
    }

    @Singleton
    public Executor taskExecutor() {
      return Executors.newSingleThreadExecutor(new CustomizableThreadFactory("Custom-"));
    }
  }

  @Configuration
  @EnableAsync
  static class CustomExecutorConfig implements AsyncConfigurer {

    @Singleton
    public AsyncBean asyncBean() {
      return new AsyncBean();
    }

    @Override
    public Executor getAsyncExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setThreadNamePrefix("Custom-");
      executor.initialize();
      return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
      return exceptionHandler();
    }

    @Singleton
    public AsyncUncaughtExceptionHandler exceptionHandler() {
      return new TestableAsyncUncaughtExceptionHandler();
    }
  }

  @Configuration
  @EnableAsync
  static class CustomExecutorBeanConfig implements AsyncConfigurer {

    @Singleton
    public AsyncBean asyncBean() {
      return new AsyncBean();
    }

    @Override
    public Executor getAsyncExecutor() {
      return executor();
    }

    @Singleton
    public ThreadPoolTaskExecutor executor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setThreadNamePrefix("Custom-");
      executor.initialize();
      return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
      return exceptionHandler();
    }

    @Singleton
    public AsyncUncaughtExceptionHandler exceptionHandler() {
      return new TestableAsyncUncaughtExceptionHandler();
    }
  }

  public static class ExecutorPostProcessor implements InitializationBeanPostProcessor {

    @Nullable
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
      if (bean instanceof ThreadPoolTaskExecutor) {
        ((ThreadPoolTaskExecutor) bean).setThreadNamePrefix("Post-");
      }
      return bean;
    }
  }

  public interface AsyncInterface {

    @Async
    void work();

    Thread getThreadOfExecution();
  }

  public static class AsyncService implements AsyncInterface {

    private Thread threadOfExecution;

    @Override
    public void work() {
      this.threadOfExecution = Thread.currentThread();
    }

    @Override
    public Thread getThreadOfExecution() {
      return threadOfExecution;
    }
  }

  @Configuration
  @EnableAsync
  static class Spr14949ConfigA implements AsyncConfigurer {

    @Singleton
    public AsyncInterface asyncBean() {
      return new AsyncService();
    }

    @Override
    public Executor getAsyncExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setThreadNamePrefix("Custom-");
      executor.initialize();
      return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
      return null;
    }
  }

  @Configuration
  @EnableAsync(proxyTargetClass = true)
  static class Spr14949ConfigB implements AsyncConfigurer {

    @Singleton
    public AsyncInterface asyncBean() {
      return new AsyncService();
    }

    @Override
    public Executor getAsyncExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setThreadNamePrefix("Custom-");
      executor.initialize();
      return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
      return null;
    }
  }

  @Configuration
  @EnableAsync
  @Import(UserConfiguration.class)
  static class JdkProxyConfiguration {

    @Singleton
    public AsyncBeanWithInterface asyncBean() {
      return new AsyncBeanWithInterface();
    }
  }

  @Configuration
  static class UserConfiguration {

    @Singleton
    public AsyncBeanUser user(AsyncBeanWithInterface bean) {
      return new AsyncBeanUser(bean);
    }
  }

}
