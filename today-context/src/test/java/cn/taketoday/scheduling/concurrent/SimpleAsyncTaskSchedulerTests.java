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

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.TriggerContext;
import cn.taketoday.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class SimpleAsyncTaskSchedulerTests extends AbstractSchedulingTaskExecutorTests {

  private final SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();

  private final AtomicBoolean taskRun = new AtomicBoolean();

  @Override
  protected AsyncTaskExecutor buildExecutor() {
    scheduler.setTaskDecorator(runnable -> () -> {
      taskRun.set(true);
      runnable.run();
    });
    scheduler.setThreadNamePrefix(this.threadNamePrefix);
    return scheduler;
  }

  @Test
  @Override
  void submitRunnableWithGetAfterShutdown() {
    // decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
  }

  @Test
  @Override
  void submitListenableRunnableWithGetAfterShutdown() {
    // decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
  }

  @Test
  @Override
  void submitCompletableRunnableWithGetAfterShutdown() {
    // decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
  }

  @Test
  @Override
  void submitCallableWithGetAfterShutdown() {
    // decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
  }

  @Test
  @Override
  void submitListenableCallableWithGetAfterShutdown() {
    // decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
  }

  @Test
  @Override
  void submitCompletableCallableWithGetAfterShutdown() {
    // decorated Future cannot be cancelled on shutdown with SimpleAsyncTaskScheduler
  }

  @Test
  void executeFailingRunnableWithErrorHandler() {
    TestTask task = new TestTask(this.testName, 0);
    TestErrorHandler errorHandler = new TestErrorHandler(1);
    scheduler.setErrorHandler(errorHandler);
    scheduler.execute(task);
    await(errorHandler);
    assertThat(errorHandler.lastError).isNotNull();
    assertThat(taskRun.get()).isTrue();
  }

  @Test
  void submitFailingRunnableWithErrorHandler() throws Exception {
    TestTask task = new TestTask(this.testName, 0);
    TestErrorHandler errorHandler = new TestErrorHandler(1);
    scheduler.setErrorHandler(errorHandler);
    Future<?> future = scheduler.submit(task);
    Object result = future.get(1000, TimeUnit.MILLISECONDS);
    assertThat(future.isDone()).isTrue();
    assertThat(result).isNull();
    assertThat(errorHandler.lastError).isNotNull();
    assertThat(taskRun.get()).isTrue();
  }

  @Test
  void submitFailingCallableWithErrorHandler() throws Exception {
    TestCallable task = new TestCallable(this.testName, 0);
    TestErrorHandler errorHandler = new TestErrorHandler(1);
    scheduler.setErrorHandler(errorHandler);
    Future<String> future = scheduler.submit(task);
    Object result = future.get(1000, TimeUnit.MILLISECONDS);
    assertThat(future.isDone()).isTrue();
    assertThat(result).isNull();
    assertThat(errorHandler.lastError).isNotNull();
    assertThat(taskRun.get()).isTrue();
  }

  @Test
  void scheduleOneTimeTask() throws Exception {
    TestTask task = new TestTask(this.testName, 1);
    Future<?> future = scheduler.schedule(task, new Date());
    Object result = future.get(1000, TimeUnit.MILLISECONDS);
    assertThat(result).isNull();
    await(task);
    assertThat(taskRun.get()).isTrue();
    assertThreadNamePrefix(task);
  }

  @Test
  void scheduleOneTimeFailingTaskWithErrorHandler() throws Exception {
    TestTask task = new TestTask(this.testName, 0);
    TestErrorHandler errorHandler = new TestErrorHandler(1);
    scheduler.setErrorHandler(errorHandler);
    Future<?> future = scheduler.schedule(task, new Date());
    Object result = future.get(1000, TimeUnit.MILLISECONDS);
    await(errorHandler);
    assertThat(result).isNull();
    assertThat(errorHandler.lastError).isNotNull();
    assertThat(taskRun.get()).isTrue();
  }

  @RepeatedTest(20)
  void scheduleMultipleTriggerTasks() throws Exception {
    TestTask task = new TestTask(this.testName, 3);
    Future<?> future = scheduler.schedule(task, new TestTrigger(3));
    Object result = future.get(1000, TimeUnit.MILLISECONDS);
    assertThat(result).isNull();
    await(task);
    assertThat(taskRun.get()).isTrue();
    assertThreadNamePrefix(task);
  }

  private void await(TestTask task) {
    await(task.latch);
  }

  private void await(TestErrorHandler errorHandler) {
    await(errorHandler.latch);
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await(1000, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException ex) {
      throw new IllegalStateException(ex);
    }
    assertThat(latch.getCount()).as("latch did not count down").isEqualTo(0);
  }

  private static class TestErrorHandler implements ErrorHandler {

    private final CountDownLatch latch;

    private volatile Throwable lastError;

    TestErrorHandler(int expectedErrorCount) {
      this.latch = new CountDownLatch(expectedErrorCount);
    }

    @Override
    public void handleError(Throwable t) {
      this.lastError = t;
      this.latch.countDown();
    }
  }

  private static class TestTrigger implements Trigger {

    private final int maxRunCount;

    private final AtomicInteger actualRunCount = new AtomicInteger();

    TestTrigger(int maxRunCount) {
      this.maxRunCount = maxRunCount;
    }

    @Override
    public Instant nextExecution(TriggerContext triggerContext) {
      if (this.actualRunCount.incrementAndGet() > this.maxRunCount) {
        return null;
      }
      return Instant.now();
    }
  }

}
