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

package infra.scheduling.concurrent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import infra.core.task.AsyncTaskExecutor;
import infra.core.task.TaskDecorator;
import infra.lang.Assert;
import infra.scheduling.SchedulingAwareRunnable;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Test
  void schedulingAwareRunnableIsDetectedForManagedTask() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    SchedulingAwareRunnable task = mock(SchedulingAwareRunnable.class);
    when(task.isLongLived()).thenReturn(true);

    executor.execute(task);

    verify(task).isLongLived();
  }

  @Test
  void decoratorIsAppliedToSubmittedTasks() throws Exception {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    TaskDecorator decorator = task -> () -> { };
    executor.setTaskDecorator(decorator);

    Runnable originalTask = () -> { };
    Callable<String> originalCallable = () -> "test";

    executor.submit(originalTask);
    executor.submit(originalCallable);

    Runnable decoratedTask = executor.decorateTaskIfNecessary(originalTask);
    assertThat(decoratedTask).isNotSameAs(originalTask);
  }

  @Test
  void nullExecutorThrowsIllegalStateException() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor(null);
    assertThatCode(() -> executor.execute(() -> { }))
            .hasMessage("Executor not configured");
  }

  @Test
  void startTimeoutParameterIsRespected() {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();
    CountDownLatch taskStarted = new CountDownLatch(1);

    executor.execute(() -> {
      try {
        Thread.sleep(200);
        taskStarted.countDown();
      }
      catch (InterruptedException ex) {
        // ignore
      }
    }, 100);

    assertThat(taskStarted.getCount()).isEqualTo(1);
  }

  @Test
  void submittedTasksReturnFutureResults() throws Exception {
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor();

    Future<String> future = executor.submit(() -> "test");
    assertThat(future.get()).isEqualTo("test");

    Future<Void> voidFuture = executor.submit(() -> { });
    assertThat(voidFuture.get()).isNull();
  }

  @Test
  void longRunningTasksAreMarkedInManagedProperties() {
    ManagedExecutorService managedExecutor = mock(ManagedExecutorService.class);
    ConcurrentTaskExecutor executor = new ConcurrentTaskExecutor(managedExecutor);

    SchedulingAwareRunnable task = mock(SchedulingAwareRunnable.class);
    when(task.isLongLived()).thenReturn(true);

    executor.execute(task);

    verify(managedExecutor).execute(argThat(t ->
            ((ManagedTask) t).getExecutionProperties()
                    .get(ManagedTask.LONGRUNNING_HINT).equals("true")));
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
