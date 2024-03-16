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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.core.task.AsyncListenableTaskExecutor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
abstract class AbstractSchedulingTaskExecutorTests {

  private AsyncListenableTaskExecutor executor;

  protected String testName;

  protected String threadNamePrefix;

  private volatile Object outcome;

  @BeforeEach
  void setup(TestInfo testInfo) {
    this.testName = testInfo.getTestMethod().get().getName();
    this.threadNamePrefix = this.testName + "-";
    this.executor = buildExecutor();
  }

  protected abstract AsyncListenableTaskExecutor buildExecutor();

  @AfterEach
  void shutdownExecutor() throws Exception {
    if (executor instanceof DisposableBean) {
      ((DisposableBean) executor).destroy();
    }
  }

  @Test
  void executeRunnable() {
    TestTask task = new TestTask(this.testName, 1);
    executor.execute(task);
    await(task);
    assertThreadNamePrefix(task);
  }

  @Test
  void executeFailingRunnable() {
    TestTask task = new TestTask(this.testName, 0);
    executor.execute(task);
    Awaitility.await()
            .dontCatchUncaughtExceptions()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> task.exception.get() != null && task.exception.get().getMessage().equals(
                    "TestTask failure for test 'executeFailingRunnable': expectedRunCount:<0>, actualRunCount:<1>"));
  }

  @Test
  void submitRunnable() throws Exception {
    TestTask task = new TestTask(this.testName, 1);
    Future<?> future = executor.submit(task);
    Object result = future.get(1000, TimeUnit.MILLISECONDS);
    assertThat(result).isNull();
    assertThreadNamePrefix(task);
  }

  @Test
  void submitFailingRunnable() {
    TestTask task = new TestTask(this.testName, 0);
    Future<?> future = executor.submit(task);
    assertThatExceptionOfType(ExecutionException.class).isThrownBy(() ->
            future.get(1000, TimeUnit.MILLISECONDS));
    assertThat(future.isDone()).isTrue();
  }

  @Test
  void submitRunnableWithGetAfterShutdown() throws Exception {
    Future<?> future1 = executor.submit(new TestTask(this.testName, -1));
    Future<?> future2 = executor.submit(new TestTask(this.testName, -1));
    shutdownExecutor();
    assertThatExceptionOfType(CancellationException.class).isThrownBy(() -> {
      future1.get(1000, TimeUnit.MILLISECONDS);
      future2.get(1000, TimeUnit.MILLISECONDS);
    });
  }

  @Test
  void submitListenableRunnable() {
    TestTask task = new TestTask(this.testName, 1);
    // Act
    ListenableFuture<?> future = executor.submitListenable(task);
    future.addListener(result -> outcome = result, ex -> outcome = ex);
    // Assert
    Awaitility.await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(future::isDone);
    assertThat(outcome).isNull();
    assertThreadNamePrefix(task);
  }

  @Test
  void submitCompletableRunnable() {
    TestTask task = new TestTask(this.testName, 1);
    // Act
    CompletableFuture<Void> future = executor.submitCompletable(task);
    future.whenComplete(this::storeOutcome);
    // Assert
    Awaitility.await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(future::isDone);
    assertThat(outcome).isNull();
    assertThreadNamePrefix(task);
  }

  @Test
  void submitFailingListenableRunnable() {
    TestTask task = new TestTask(this.testName, 0);
    ListenableFuture<?> future = executor.submitListenable(task);
    future.addListener(result -> outcome = result, ex -> outcome = ex);

    Awaitility.await()
            .dontCatchUncaughtExceptions()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> future.isDone() && outcome != null);
    assertThat(outcome.getClass()).isSameAs(RuntimeException.class);
  }

  @Test
  void submitFailingCompletableRunnable() {
    TestTask task = new TestTask(this.testName, 0);
    CompletableFuture<?> future = executor.submitCompletable(task);
    future.whenComplete(this::storeOutcome);

    Awaitility.await()
            .dontCatchUncaughtExceptions()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> future.isDone() && outcome != null);
    assertThat(outcome.getClass()).isSameAs(CompletionException.class);
  }

  @Test
  void submitListenableRunnableWithGetAfterShutdown() throws Exception {
    ListenableFuture<?> future1 = executor.submitListenable(new TestTask(this.testName, -1));
    ListenableFuture<?> future2 = executor.submitListenable(new TestTask(this.testName, -1));
    shutdownExecutor();

    try {
      future1.get(1000, TimeUnit.MILLISECONDS);
    }
    catch (Exception ex) {
      // ignore
    }
    Awaitility.await()
            .atMost(4, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> assertThatExceptionOfType(CancellationException.class)
                    .isThrownBy(() -> future2.get(1000, TimeUnit.MILLISECONDS)));
  }

  @Test
  void submitCompletableRunnableWithGetAfterShutdown() throws Exception {
    CompletableFuture<?> future1 = executor.submitCompletable(new TestTask(this.testName, -1));
    CompletableFuture<?> future2 = executor.submitCompletable(new TestTask(this.testName, -1));
    shutdownExecutor();

    try {
      future1.get(1000, TimeUnit.MILLISECONDS);
    }
    catch (Exception ex) {
      // ignore
    }
    Awaitility.await()
            .atMost(4, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> assertThatExceptionOfType(TimeoutException.class)
                    .isThrownBy(() -> future2.get(1000, TimeUnit.MILLISECONDS)));
  }

  @Test
  void submitCallable() throws Exception {
    TestCallable task = new TestCallable(this.testName, 1);
    Future<String> future = executor.submit(task);
    String result = future.get(1000, TimeUnit.MILLISECONDS);
    assertThat(result.substring(0, this.threadNamePrefix.length())).isEqualTo(this.threadNamePrefix);
  }

  @Test
  void submitFailingCallable() {
    TestCallable task = new TestCallable(this.testName, 0);
    Future<String> future = executor.submit(task);
    assertThatExceptionOfType(ExecutionException.class)
            .isThrownBy(() -> future.get(1000, TimeUnit.MILLISECONDS));
    assertThat(future.isDone()).isTrue();
  }

  @Test
  void submitCallableWithGetAfterShutdown() throws Exception {
    Future<?> future1 = executor.submit(new TestCallable(this.testName, -1));
    Future<?> future2 = executor.submit(new TestCallable(this.testName, -1));
    shutdownExecutor();

    try {
      future1.get(1000, TimeUnit.MILLISECONDS);
    }
    catch (Exception ex) {
      // ignore
    }
    Awaitility.await()
            .atMost(4, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .untilAsserted(() -> assertThatExceptionOfType(CancellationException.class)
                    .isThrownBy(() -> future2.get(1000, TimeUnit.MILLISECONDS)));
  }

  @Test
  void submitListenableCallable() {
    TestCallable task = new TestCallable(this.testName, 1);
    // Act
    ListenableFuture<String> future = executor.submitListenable(task);
    future.addListener(result -> outcome = result, ex -> outcome = ex);
    // Assert
    Awaitility.await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> future.isDone() && outcome != null);
    assertThat(outcome.toString().substring(0, this.threadNamePrefix.length())).isEqualTo(this.threadNamePrefix);
  }

  @Test
  void submitFailingListenableCallable() {
    TestCallable task = new TestCallable(this.testName, 0);
    // Act
    ListenableFuture<String> future = executor.submitListenable(task);
    future.addListener(result -> outcome = result, ex -> outcome = ex);
    // Assert
    Awaitility.await()
            .dontCatchUncaughtExceptions()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> future.isDone() && outcome != null);
    assertThat(outcome.getClass()).isSameAs(RuntimeException.class);
  }

  @Test
  void submitListenableCallableWithGetAfterShutdown() throws Exception {
    ListenableFuture<?> future1 = executor.submitListenable(new TestCallable(this.testName, -1));
    ListenableFuture<?> future2 = executor.submitListenable(new TestCallable(this.testName, -1));
    shutdownExecutor();
    assertThatExceptionOfType(CancellationException.class).isThrownBy(() -> {
      future1.get(1000, TimeUnit.MILLISECONDS);
      future2.get(1000, TimeUnit.MILLISECONDS);
    });
  }

  @Test
  void submitCompletableCallable() {
    TestCallable task = new TestCallable(this.testName, 1);
    // Act
    CompletableFuture<String> future = this.executor.submitCompletable(task);
    future.whenComplete(this::storeOutcome);
    // Assert
    Awaitility.await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> future.isDone() && outcome != null);
    assertThat(outcome.toString().substring(0, this.threadNamePrefix.length())).isEqualTo(this.threadNamePrefix);
  }

  @Test
  void submitFailingCompletableCallable() {
    TestCallable task = new TestCallable(this.testName, 0);
    // Act
    CompletableFuture<String> future = this.executor.submitCompletable(task);
    future.whenComplete(this::storeOutcome);
    // Assert
    Awaitility.await()
            .dontCatchUncaughtExceptions()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(10, TimeUnit.MILLISECONDS)
            .until(() -> future.isDone() && outcome != null);
    assertThat(outcome.getClass()).isSameAs(CompletionException.class);
  }

  @Test
  void submitCompletableCallableWithGetAfterShutdown() throws Exception {
    CompletableFuture<?> future1 = executor.submitCompletable(new TestCallable(this.testName, -1));
    CompletableFuture<?> future2 = executor.submitCompletable(new TestCallable(this.testName, -1));
    shutdownExecutor();
    assertThatExceptionOfType(TimeoutException.class).isThrownBy(() -> {
      future1.get(1000, TimeUnit.MILLISECONDS);
      future2.get(1000, TimeUnit.MILLISECONDS);
    });
  }

  private void storeOutcome(@Nullable Object o, @Nullable Throwable t) {
    if (o != null) {
      this.outcome = o;
    }
    else if (t != null) {
      this.outcome = t;
    }
  }

  protected void assertThreadNamePrefix(TestTask task) {
    assertThat(task.lastThread.getName().substring(0, this.threadNamePrefix.length())).isEqualTo(this.threadNamePrefix);
  }

  private void await(TestTask task) {
    await(task.latch);
  }

  private void await(CountDownLatch latch) {
    try {
      latch.await(1000, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException ex) {
      throw new IllegalStateException(ex);
    }
    assertThat(latch.getCount()).as("latch did not count down,").isEqualTo(0);
  }

  static class TestTask implements Runnable {

    private final int expectedRunCount;

    private final String testName;

    private final AtomicInteger actualRunCount = new AtomicInteger();

    private final AtomicReference<Exception> exception = new AtomicReference<>();

    final CountDownLatch latch;

    Thread lastThread;

    TestTask(String testName, int expectedRunCount) {
      this.testName = testName;
      this.expectedRunCount = expectedRunCount;
      this.latch = (expectedRunCount > 0 ? new CountDownLatch(expectedRunCount) : null);
    }

    @Override
    public void run() {
      lastThread = Thread.currentThread();
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
      }
      if (expectedRunCount >= 0) {
        if (actualRunCount.incrementAndGet() > expectedRunCount) {
          RuntimeException exception = new RuntimeException(String.format(
                  "%s failure for test '%s': expectedRunCount:<%d>, actualRunCount:<%d>",
                  getClass().getSimpleName(), this.testName, expectedRunCount, actualRunCount.get()));
          this.exception.set(exception);
          throw exception;
        }
        latch.countDown();
      }
    }
  }

  static class TestCallable implements Callable<String> {

    private final String testName;

    private final int expectedRunCount;

    private final AtomicInteger actualRunCount = new AtomicInteger();

    TestCallable(String testName, int expectedRunCount) {
      this.testName = testName;
      this.expectedRunCount = expectedRunCount;
    }

    @Override
    public String call() {
      try {
        Thread.sleep(10);
      }
      catch (InterruptedException ex) {
      }
      if (expectedRunCount >= 0) {
        if (actualRunCount.incrementAndGet() > expectedRunCount) {
          throw new RuntimeException(String.format(
                  "%s failure for test '%s': expectedRunCount:<%d>, actualRunCount:<%d>",
                  getClass().getSimpleName(), this.testName, expectedRunCount, actualRunCount.get()));
        }
      }
      return Thread.currentThread().getName();
    }
  }

}
