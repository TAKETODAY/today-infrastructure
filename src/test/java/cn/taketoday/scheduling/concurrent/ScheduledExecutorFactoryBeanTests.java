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

package cn.taketoday.scheduling.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import cn.taketoday.core.task.NoOpRunnable;
import cn.taketoday.core.testfixture.EnabledForTestGroups;

import static cn.taketoday.core.testfixture.TestGroup.LONG_RUNNING;
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
  void throwsExceptionIfPoolSizeIsLessThanZero() throws Exception {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    assertThatIllegalArgumentException().isThrownBy(() -> factory.setPoolSize(-1));
  }

  @Test
  @SuppressWarnings("serial")
  void shutdownNowIsPropagatedToTheExecutorOnDestroy() throws Exception {
    final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
      @Override
      protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        return executor;
      }
    };
    factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
    factory.afterPropertiesSet();
    factory.destroy();

    verify(executor).shutdownNow();
  }

  @Test
  @SuppressWarnings("serial")
  void shutdownIsPropagatedToTheExecutorOnDestroy() throws Exception {
    final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
      @Override
      protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
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
  @EnabledForTestGroups(LONG_RUNNING)
  void oneTimeExecutionIsSetUpAndFiresCorrectly() throws Exception {
    Runnable runnable = mock(Runnable.class);

    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    factory.setScheduledExecutorTasks(new ScheduledExecutorTask(runnable));
    factory.afterPropertiesSet();
    pauseToLetTaskStart(1);
    factory.destroy();

    verify(runnable).run();
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  void fixedRepeatedExecutionIsSetUpAndFiresCorrectly() throws Exception {
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
  @EnabledForTestGroups(LONG_RUNNING)
  void fixedRepeatedExecutionIsSetUpAndFiresCorrectlyAfterException() throws Exception {
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
  @EnabledForTestGroups(LONG_RUNNING)
  void withInitialDelayRepeatedExecutionIsSetUpAndFiresCorrectly() throws Exception {
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
  @EnabledForTestGroups(LONG_RUNNING)
  void withInitialDelayRepeatedExecutionIsSetUpAndFiresCorrectlyAfterException() throws Exception {
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
  @SuppressWarnings("serial")
  void settingThreadFactoryToNullForcesUseOfDefaultButIsOtherwiseCool() throws Exception {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
      @Override
      protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        assertThat("Bah; the setThreadFactory(..) method must use a default ThreadFactory if a null arg is passed in.").isNotNull();
        return super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
      }
    };
    factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
    factory.setThreadFactory(null); // the null must not propagate
    factory.afterPropertiesSet();
    factory.destroy();
  }

  @Test
  @SuppressWarnings("serial")
  void settingRejectedExecutionHandlerToNullForcesUseOfDefaultButIsOtherwiseCool() throws Exception {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean() {
      @Override
      protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {
        assertThat("Bah; the setRejectedExecutionHandler(..) method must use a default RejectedExecutionHandler if a null arg is passed in.").isNotNull();
        return super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
      }
    };
    factory.setScheduledExecutorTasks(new NoOpScheduledExecutorTask());
    factory.setRejectedExecutionHandler(null); // the null must not propagate
    factory.afterPropertiesSet();
    factory.destroy();
  }

  @Test
  void objectTypeReportsCorrectType() throws Exception {
    ScheduledExecutorFactoryBean factory = new ScheduledExecutorFactoryBean();
    assertThat(factory.getObjectType()).isEqualTo(ScheduledExecutorService.class);
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
