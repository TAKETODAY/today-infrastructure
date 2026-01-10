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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
class ScheduledExecutorFactoryBeanTests {

  @Test
  void throwsExceptionIfPoolSizeIsLessThanZero() {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setPoolSize(-1));
  }

  @Test
  @SuppressWarnings("serial")
  void shutdownNowIsPropagatedToTheExecutorOnDestroy() {
    final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
      @Override
      protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
        return executor;
      }
    };
    factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
    factory.afterPropertiesSet();
    factory.destroy();

    verify(executor).shutdownNow();
  }

  @Test
  void shutdownIsPropagatedToTheExecutorOnDestroy() {
    final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
      @Override
      protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
        return executor;
      }
    };
    factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
    factory.setWaitForTasksToCompleteOnShutdown(true);
    factory.afterPropertiesSet();
    factory.destroy();

    verify(executor).shutdown();
  }

  @Test
    // @EnabledForTestGroups(LONG_RUNNING)
  void oneTimeExecutionIsSetUpAndFiresCorrectly() {
    Runnable runnable = mock(Runnable.class);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(new ScheduledExecutorTask(runnable));
    factory.afterPropertiesSet();
    pauseToLetTaskStart(1);
    factory.destroy();

    verify(runnable).run();
  }

  @Test
    // @EnabledForTestGroups(LONG_RUNNING)
  void fixedRepeatedExecutionIsSetUpAndFiresCorrectly() {
    Runnable runnable = mock(Runnable.class);

    ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
    task.setPeriod(500);
    task.setFixedRate(true);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(task);
    factory.afterPropertiesSet();
    pauseToLetTaskStart(2);
    factory.destroy();

    verify(runnable, atLeast(2)).run();
  }

  @Test
    // @EnabledForTestGroups(LONG_RUNNING)
  void fixedRepeatedExecutionIsSetUpAndFiresCorrectlyAfterException() {
    Runnable runnable = mock(Runnable.class);
    willThrow(new IllegalStateException()).given(runnable).run();

    ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
    task.setPeriod(500);
    task.setFixedRate(true);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(task);
    factory.setContinueScheduledExecutionAfterException(true);
    factory.afterPropertiesSet();
    pauseToLetTaskStart(2);
    factory.destroy();

    verify(runnable, atLeast(2)).run();
  }

  @Test
    // @EnabledForTestGroups(LONG_RUNNING)
  void withInitialDelayRepeatedExecutionIsSetUpAndFiresCorrectly() {
    Runnable runnable = mock(Runnable.class);

    ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
    task.setPeriod(500);
    task.setDelay(3000); // nice long wait...

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(task);
    factory.afterPropertiesSet();
    pauseToLetTaskStart(1);
    // invoke destroy before tasks have even been scheduled...
    factory.destroy();

    // Mock must never have been called
    verify(runnable, never()).run();
  }

  @Test
    // @EnabledForTestGroups(LONG_RUNNING)
  void withInitialDelayRepeatedExecutionIsSetUpAndFiresCorrectlyAfterException() {
    Runnable runnable = mock(Runnable.class);
    willThrow(new IllegalStateException()).given(runnable).run();

    ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);
    task.setPeriod(500);
    task.setDelay(3000); // nice long wait...

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(task);
    factory.setContinueScheduledExecutionAfterException(true);
    factory.afterPropertiesSet();
    pauseToLetTaskStart(1);
    // invoke destroy before tasks have even been scheduled...
    factory.destroy();

    // Mock must never have been called
    verify(runnable, never()).run();
  }

  @Test
  void settingThreadFactoryToNullForcesUseOfDefaultButIsOtherwiseCool() {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
      @Override
      protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
        assertThat("Bah; the setThreadFactory(..) method must use a default ThreadFactory if a null arg is passed in.").isNotNull();
        return super.createExecutor(poolSize, threadFactory, rejectedHandler);
      }
    };
    factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
    factory.setThreadFactory(null); // the null must not propagate
    factory.afterPropertiesSet();
    factory.destroy();
  }

  @Test
  void settingRejectedExecutionHandlerToNullForcesUseOfDefaultButIsOtherwiseCool() {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
      @Override
      protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
        assertThat("Bah; the setRejectedExecutionHandler(..) method must use a default RejectedExecutionHandler if a null arg is passed in.").isNotNull();
        return super.createExecutor(poolSize, threadFactory, rejectedHandler);
      }
    };
    factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
    factory.setRejectedExecutionHandler(null); // the null must not propagate
    factory.afterPropertiesSet();
    factory.destroy();
  }

  @Test
  void objectTypeReportsCorrectType() {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    assertThat(factory.getObjectType()).isEqualTo(ScheduledExecutorService.class);
  }

  @Test
  void disallowsNegativePoolSize() {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    assertThatIllegalArgumentException()
            .isThrownBy(() -> factory.setPoolSize(-5));
  }

  @Test
  void allowsZeroScheduledTasks() throws Exception {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(new ScheduledExecutorTask[0]);
    factory.afterPropertiesSet();
    assertThat(factory.getObject()).isNotNull();
  }

  @Test
  void executorContinuesAfterExceptionWhenConfigured() throws Exception {
    AtomicInteger counter = new AtomicInteger();
    Runnable failingTask = () -> {
      counter.incrementAndGet();
      throw new RuntimeException("Expected");
    };

    ScheduledExecutorTask task = new ScheduledExecutorTask(failingTask);
    task.setPeriod(100);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(task);
    factory.setContinueScheduledExecutionAfterException(true);
    factory.afterPropertiesSet();

    Thread.sleep(250);
    assertThat(counter.get()).isGreaterThanOrEqualTo(2);
  }

  @Test
  void appliesRemoveOnCancelPolicyWhenConfigured() throws Exception {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setRemoveOnCancelPolicy(true);
    factory.afterPropertiesSet();

    ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) factory.getObject();
    assertThat(executor.getRemoveOnCancelPolicy()).isTrue();
  }

  @Test
  void schedulesFixedDelayTasksCorrectly() throws Exception {
    CountDownLatch latch = new CountDownLatch(2);
    Runnable countingTask = latch::countDown;

    ScheduledExecutorTask task = new ScheduledExecutorTask(countingTask);
    task.setPeriod(100);
    task.setFixedRate(false);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(task);
    factory.afterPropertiesSet();

    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void schedulesFixedRateTasksCorrectly() throws Exception {
    CountDownLatch latch = new CountDownLatch(3);
    Runnable countingTask = latch::countDown;

    ScheduledExecutorTask task = new ScheduledExecutorTask(countingTask);
    task.setPeriod(100);
    task.setFixedRate(true);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(task);
    factory.afterPropertiesSet();

    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void delayedTaskExecutesAfterInitialDelay() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Runnable delayedTask = latch::countDown;

    ScheduledExecutorTask task = new ScheduledExecutorTask(delayedTask);
    task.setDelay(500);
    task.setFixedRate(false);
    task.setPeriod(100);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(task);
    factory.afterPropertiesSet();

    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
  }

  @Test
  void respectsPoolSizeSettings() throws Exception {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setPoolSize(3);
    factory.afterPropertiesSet();

    ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) factory.getObject();
    assertThat(executor.getCorePoolSize()).isEqualTo(3);
  }

  @Test
  void customThreadFactoryIsUsed() throws Exception {
    ThreadFactory threadFactory = new CustomThreadFactory();

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setThreadFactory(threadFactory);
    factory.afterPropertiesSet();

    CountDownLatch latch = new CountDownLatch(1);
    factory.getObject().execute(latch::countDown);

    assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
  }

  static class CustomThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
      Thread thread = new Thread(r);
      thread.setDaemon(true);
      return thread;
    }
  }

  private static void pauseToLetTaskStart(int seconds) {
    try {
      Thread.sleep(seconds * 1000);
    }
    catch (InterruptedException ignored) {
    }
  }

  private static class NoOpScheduledExecutorTask extends ScheduledExecutorTask {

    NoOpScheduledExecutorTask() {
      super(new NoOpRunnable());
    }
  }

}
