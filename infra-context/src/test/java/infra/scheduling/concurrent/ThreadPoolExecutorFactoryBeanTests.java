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

package infra.scheduling.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ThreadPoolExecutorFactoryBean}.
 *
 * @author Juergen Hoeller
 */
class ThreadPoolExecutorFactoryBeanTests {

  @Test
  void defaultExecutor() throws Exception {
    ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(ExecutorConfig.class);
    ExecutorService executor = context.getBean(ExecutorService.class);

    FutureTask<String> task = new FutureTask<>(() -> "foo");
    executor.execute(task);
    assertThat(task.get()).isEqualTo("foo");
    context.close();
  }

  @Test
  void executorWithDefaultSettingsDoesNotPrestartAllCoreThreads() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean("taskExecutor", ThreadPoolExecutorFactoryBean.class, TestThreadPoolExecutorFactoryBean::new);
    context.refresh();
    ThreadPoolExecutor threadPoolExecutor = context.getBean(ThreadPoolExecutor.class);
    verify(threadPoolExecutor, never()).prestartAllCoreThreads();
  }

  @Test
  void executorWithPrestartAllCoreThreads() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean("taskExecutor", ThreadPoolExecutorFactoryBean.class, () -> {
      TestThreadPoolExecutorFactoryBean factoryBean = new TestThreadPoolExecutorFactoryBean();
      factoryBean.setPrestartAllCoreThreads(true);
      return factoryBean;
    });
    context.refresh();
    ThreadPoolExecutor threadPoolExecutor = context.getBean(ThreadPoolExecutor.class);
    verify(threadPoolExecutor).prestartAllCoreThreads();
  }

  @Test
  void customKeepAliveTimeIsApplied() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.setKeepAliveSeconds(10);
    factory.afterPropertiesSet();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) factory.getObject();
    assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(10);
  }

  @Test
  void zeroQueueCapacityCreatesSynchronousQueue() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.setQueueCapacity(0);
    factory.afterPropertiesSet();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) factory.getObject();
    assertThat(executor.getQueue()).isInstanceOf(SynchronousQueue.class);
  }

  @Test
  void allowCoreThreadTimeoutIsRespected() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.setAllowCoreThreadTimeOut(true);
    factory.afterPropertiesSet();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) factory.getObject();
    assertThat(executor.allowsCoreThreadTimeOut()).isTrue();
  }

  @Test
  void strictEarlyShutdownRejectsNewTasks() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.setStrictEarlyShutdown(true);
    factory.afterPropertiesSet();

    ExecutorService executor = factory.getObject();
    factory.destroy();

    assertThatExceptionOfType(RejectedExecutionException.class)
            .isThrownBy(() -> executor.submit(() -> { }));
  }

  @Test
  void multipleCoresAreStartedWhenPrestarted() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.setCorePoolSize(3);
    factory.setPrestartAllCoreThreads(true);
    factory.afterPropertiesSet();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) factory.getObject();
    assertThat(executor.getPoolSize()).isEqualTo(3);
  }

  @Test
  void negativeQueueCapacityCreatesSynchronousQueue() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.setQueueCapacity(-1);
    factory.afterPropertiesSet();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) factory.getObject();
    assertThat(executor.getQueue()).isInstanceOf(SynchronousQueue.class);
  }

  @Test
  void minMaxPoolSizesAreRespected() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.setCorePoolSize(2);
    factory.setMaxPoolSize(4);
    factory.afterPropertiesSet();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) factory.getObject();
    assertThat(executor.getCorePoolSize()).isEqualTo(2);
    assertThat(executor.getMaximumPoolSize()).isEqualTo(4);
  }

  @Test
  void customRejectedExecutionHandlerIsUsed() throws Exception {
    RejectedExecutionHandler handler = mock(RejectedExecutionHandler.class);
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.setRejectedExecutionHandler(handler);
    factory.afterPropertiesSet();

    ThreadPoolExecutor executor = (ThreadPoolExecutor) factory.getObject();
    Runnable task = () -> { };
    executor.getRejectedExecutionHandler().rejectedExecution(task, executor);

    verify(handler).rejectedExecution(task, executor);
  }

  @Test
  void afterExecuteHookIsInvoked() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    AtomicBoolean afterExecuteCalled = new AtomicBoolean();

    factory = new ThreadPoolExecutorFactoryBean() {
      @Override
      protected void afterExecute(Runnable r, Throwable t) {
        afterExecuteCalled.set(true);
      }
    };

    factory.afterPropertiesSet();
    ThreadPoolExecutor executor = (ThreadPoolExecutor) factory.getObject();
    executor.execute(() -> { });

    Thread.sleep(100); // Wait for task completion
    assertThat(afterExecuteCalled.get()).isTrue();
  }

  @Test
  void isSingletonReturnsTrue() {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    assertThat(factory.isSingleton()).isTrue();
  }

  @Test
  void objectTypeReturnsExecutorServiceWhenNotInitialized() {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    assertThat(factory.getObjectType()).isEqualTo(ExecutorService.class);
  }

  @Test
  void objectTypeReturnsActualTypeWhenInitialized() throws Exception {
    ThreadPoolExecutorFactoryBean factory = new ThreadPoolExecutorFactoryBean();
    factory.afterPropertiesSet();
    assertThat(factory.getObjectType()).isEqualTo(factory.getObject().getClass());
  }

  @Configuration
  static class ExecutorConfig {

    @Bean
    ThreadPoolExecutorFactoryBean executor() {
      return new ThreadPoolExecutorFactoryBean();
    }

  }

  @SuppressWarnings("serial")
  private static class TestThreadPoolExecutorFactoryBean extends ThreadPoolExecutorFactoryBean {

    @Override
    protected ThreadPoolExecutor createExecutor(
            int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

      return mock(ThreadPoolExecutor.class);
    }
  }

}
