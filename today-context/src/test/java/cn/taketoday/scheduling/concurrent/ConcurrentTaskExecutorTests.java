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

package cn.taketoday.scheduling.concurrent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.lang.Assert;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
class ConcurrentTaskExecutorTests extends AbstractSchedulingTaskExecutorTests {

  private final ThreadPoolExecutor concurrentExecutor =
          new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

  @Override
  protected AsyncTaskExecutor buildExecutor() {
    concurrentExecutor.setThreadFactory(new CustomizableThreadFactory(this.threadNamePrefix));
    return new ConcurrentTaskExecutor(concurrentExecutor);
  }

  @Override
  @AfterEach
  void shutdownExecutor() {
    for (Runnable task : concurrentExecutor.shutdownNow()) {
      if (task instanceof Future) {
        ((Future<?>) task).cancel(true);
      }
    }
  }

  @Test
  void zeroArgCtorResultsInDefaultTaskExecutorBeingUsed() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
  }

  @Test
  void passingNullExecutorToCtorResultsInDefaultTaskExecutorBeingUsed() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor(null);
    assertThatCode(() -> executor.execute(new NoOpRunnable())).hasMessage("Executor not configured");
  }

  @Test
  void earlySetConcurrentExecutorCallRespectsConfiguredTaskDecorator() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    executor.setConcurrentExecutor(new DecoratedExecutor());
    executor.setTaskDecorator(new RunnableDecorator());
    assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
  }

  @Test
  void lateSetConcurrentExecutorCallRespectsConfiguredTaskDecorator() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    executor.setTaskDecorator(new RunnableDecorator());
    executor.setConcurrentExecutor(new DecoratedExecutor());
    assertThatCode(() -> executor.execute(new NoOpRunnable())).doesNotThrowAnyException();
  }

  private static class DecoratedRunnable implements Runnable {

    @Override
    public void run() { }
  }

  private static class RunnableDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
      return new DecoratedRunnable();
    }
  }

  private static class DecoratedExecutor implements Executor {

    @Override
    public void execute(Runnable command) {
      Assert.state(command instanceof DecoratedRunnable, "TaskDecorator not applied");
    }
  }

}
