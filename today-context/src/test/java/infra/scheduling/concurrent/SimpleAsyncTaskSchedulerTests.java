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

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import infra.core.task.AsyncTaskExecutor;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.scheduling.Trigger;
import infra.scheduling.TriggerContext;
import infra.util.ErrorHandler;

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
    Future<?> future = scheduler.schedule(task, Instant.now());
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
    Future<?> future = scheduler.schedule(task, Instant.now());
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

  @Test
  void fixedRateTaskExecutesMultipleTimes() throws Exception {
    TestTask task = new TestTask(testName, 3);
    Future<?> future = scheduler.scheduleAtFixedRate(task, Duration.ofMillis(10));
    await(task);
    future.cancel(true);
    assertThat(taskRun.get()).isTrue();
    assertThreadNamePrefix(task);
  }

  @Test
  void taskTerminationTimeoutIsRespected() throws Exception {
    scheduler.setTaskTerminationTimeout(100);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(1);

    Runnable longRunningTask = () -> {
      startLatch.countDown();
      try {
        Thread.sleep(500);
      }
      catch (InterruptedException ex) {
        // Expected
      }
      endLatch.countDown();
    };

    scheduler.execute(longRunningTask);
    assertThat(startLatch.await(100, TimeUnit.MILLISECONDS)).isTrue();

    scheduler.stop();
    assertThat(endLatch.getCount()).isEqualTo(1);
  }

  @Test
  void customErrorHandlerReceivesSchedulingExceptions() {
    TestErrorHandler errorHandler = new TestErrorHandler(1);
    scheduler.setErrorHandler(errorHandler);

    scheduler.schedule(() -> { throw new RuntimeException("Test Exception"); },
            new TestTrigger(1));

    await(errorHandler);
    assertThat(errorHandler.lastError)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Test Exception");
  }

  @Test
  void targetExecutorIsUsedForTaskExecution() throws Exception {
    CountDownLatch taskLatch = new CountDownLatch(1);
    AtomicReference<String> threadName = new AtomicReference<>();

    SimpleAsyncTaskExecutor targetExecutor = new SimpleAsyncTaskExecutor();
    targetExecutor.setThreadNamePrefix("target-");
    scheduler.setTargetTaskExecutor(targetExecutor);

    scheduler.execute(() -> {
      threadName.set(Thread.currentThread().getName());
      taskLatch.countDown();
    });

    assertThat(taskLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
    assertThat(threadName.get()).startsWith("target-");
  }

  @Test
  void lifecycleStartAndStopAreIdempotent() {
    scheduler.start();
    scheduler.start(); // Second call should be no-op
    assertThat(scheduler.isRunning()).isTrue();

    scheduler.stop();
    scheduler.stop(); // Second call should be no-op
    assertThat(scheduler.isRunning()).isFalse();
  }

  @Test
  void shutdownCancelsScheduledTasks() throws Exception {
    CountDownLatch taskLatch = new CountDownLatch(1);
    AtomicInteger execCount = new AtomicInteger();

    Future<?> future = scheduler.scheduleAtFixedRate(() -> {
      execCount.incrementAndGet();
      taskLatch.countDown();
    }, Duration.ofMillis(10));

    assertThat(taskLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
    future.cancel(true);

    Thread.sleep(50);
    int finalCount = execCount.get();
    Thread.sleep(50);
    assertThat(execCount.get()).isEqualTo(finalCount);
  }

  @Test
  void scheduledTasksAreExecutedWithCustomClock() {
    Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    scheduler.setClock(fixedClock);

    TestTask task = new TestTask(testName, 1);
    Future<?> future = scheduler.schedule(task, fixedClock.instant().plusMillis(100));

    await(task);
    future.cancel(true);
    assertThat(taskRun.get()).isTrue();
    assertThreadNamePrefix(task);
  }

  @Test
  void gracefulShutdownWaitsForScheduledTasks() throws Exception {
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(1);

    scheduler.schedule(() -> {
      startLatch.countDown();
      try {
        Thread.sleep(200);
      }
      catch (InterruptedException ex) {
        // Expected
      }
      endLatch.countDown();
    }, Instant.now());

    assertThat(startLatch.await(100, TimeUnit.MILLISECONDS)).isTrue();
    scheduler.stop();
    assertThat(endLatch.await(1000, TimeUnit.MILLISECONDS)).isTrue();
  }

  @Test
  void scheduledTaskErrorsAreHandledByErrorHandler() {
    TestErrorHandler errorHandler = new TestErrorHandler(2);
    scheduler.setErrorHandler(errorHandler);

    RuntimeException expected = new RuntimeException("Expected test exception");
    scheduler.scheduleAtFixedRate(() -> {
      throw expected;
    }, Duration.ofMillis(10));

    await(errorHandler);
    assertThat(errorHandler.lastError).isSameAs(expected);
  }

  @Test
  void multipleSchedulersCanRunConcurrently() throws Exception {
    SimpleAsyncTaskScheduler scheduler2 = new SimpleAsyncTaskScheduler();
    scheduler2.setThreadNamePrefix("scheduler2-");

    CountDownLatch latch1 = new CountDownLatch(1);
    CountDownLatch latch2 = new CountDownLatch(1);

    scheduler.scheduleAtFixedRate(latch1::countDown, Duration.ofMillis(10));
    scheduler2.scheduleAtFixedRate(latch2::countDown, Duration.ofMillis(10));

    assertThat(latch1.await(1000, TimeUnit.MILLISECONDS)).isTrue();
    assertThat(latch2.await(1000, TimeUnit.MILLISECONDS)).isTrue();
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
