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

package infra.core.task;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import infra.util.ConcurrencyThrottleSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class SimpleAsyncTaskExecutorTests {

  @Test
  void isActiveUntilClose() {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
    assertThat(executor.isActive()).isTrue();
    assertThat(executor.isThrottleActive()).isFalse();
    executor.close();
    assertThat(executor.isActive()).isFalse();
    assertThat(executor.isThrottleActive()).isFalse();
  }

  @Test
  void throwsExceptionWhenSuppliedWithNullRunnable() {
    try (SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor()) {
      assertThatIllegalArgumentException().isThrownBy(() -> executor.execute(null));
    }
  }

  @Test
  void cannotExecuteWhenConcurrencyIsSwitchedOff() {
    try (SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor()) {
      executor.setConcurrencyLimit(ConcurrencyThrottleSupport.NO_CONCURRENCY);
      assertThat(executor.isThrottleActive()).isTrue();
      assertThatIllegalStateException().isThrownBy(() -> executor.execute(new NoOpRunnable()));
    }
  }

  @Test
  void taskRejectedWhenConcurrencyLimitReached() {
    try (SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor()) {
      executor.setConcurrencyLimit(1);
      executor.setRejectTasksWhenLimitReached(true);
      assertThat(executor.isThrottleActive()).isTrue();
      executor.execute(new NoOpRunnable());
      assertThatExceptionOfType(TaskRejectedException.class).isThrownBy(() -> executor.execute(new NoOpRunnable()));
    }
  }

  /**
   * Verify that when thread creation fails in doExecute() while concurrency
   * limiting is active, the concurrency permit is properly released to
   * prevent permanent deadlock.
   *
   * <p>This test reproduces a critical bug where OutOfMemoryError from
   * Thread.start() causes the executor to permanently deadlock:
   * <ol>
   *   <li>beforeAccess() increments concurrencyCount
   *   <li>doExecute() throws Error before thread starts
   *   <li>TaskTrackingRunnable.run() never executes
   *   <li>afterAccess() in finally block never called
   *   <li>Subsequent tasks block forever in onLimitReached()
   * </ol>
   *
   * <p>Test approach: The first execute() should fail with some exception
   * (type doesn't matter - could be Error or TaskRejectedException).
   * The second execute() is the real test: it should complete without
   * deadlock if the permit was properly released.
   */
  @Test
  void executeFailsToStartThreadReleasesConcurrencyPermit() throws InterruptedException {
    // Arrange
    SimpleAsyncTaskExecutor executor = spy(new SimpleAsyncTaskExecutor());
    executor.setConcurrencyLimit(1);  // Enable concurrency limiting

    Runnable task = () -> { };
    Error failure = new OutOfMemoryError("TEST: Cannot start thread");

    // Simulate thread creation failure
    doThrow(failure).when(executor).doExecute(any(Runnable.class));

    // Act - First execution fails
    // Both "before fix" (throws Error) and "after fix" (throws TaskRejectedException)
    // should throw some exception here - that's expected and correct
    assertThatThrownBy(() -> executor.execute(task))
            .isInstanceOf(Throwable.class);

    // Arrange - Reset mock to allow second execution to succeed
    willCallRealMethod().given(executor).doExecute(any(Runnable.class));

    // Assert - Second execution should NOT deadlock
    // This is the real test: if permit was leaked, this will timeout
    CountDownLatch latch = new CountDownLatch(1);
    executor.execute(() -> latch.countDown());

    boolean completed = latch.await(1, TimeUnit.SECONDS);

    assertThat(completed)
            .withFailMessage("Executor should not deadlock if concurrency permit was properly released after first failure")
            .isTrue();
  }

  @Test
  void threadNameGetsSetCorrectly() {
    String customPrefix = "chankPop#";
    Object monitor = new Object();
    try (SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(customPrefix)) {
      ThreadNameHarvester task = new ThreadNameHarvester(monitor);
      executeAndWait(executor, task, monitor);
      assertThat(task.getThreadName()).startsWith(customPrefix);
    }
  }

  @Test
  void threadFactoryOverridesDefaults() {
    Object monitor = new Object();
    try (SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(runnable -> new Thread(runnable, "test"))) {
      ThreadNameHarvester task = new ThreadNameHarvester(monitor);
      executeAndWait(executor, task, monitor);
      assertThat(task.getThreadName()).isEqualTo("test");
    }
  }

  private void executeAndWait(SimpleAsyncTaskExecutor executor, Runnable task, Object monitor) {
    synchronized(monitor) {
      executor.execute(task);
      try {
        monitor.wait();
      }
      catch (InterruptedException ignored) {
      }
    }
  }

  private static final class NoOpRunnable implements Runnable {

    @Override
    public void run() {
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private abstract static class AbstractNotifyingRunnable implements Runnable {

    private final Object monitor;

    protected AbstractNotifyingRunnable(Object monitor) {
      this.monitor = monitor;
    }

    @Override
    public final void run() {
      synchronized(this.monitor) {
        try {
          doRun();
        }
        finally {
          this.monitor.notifyAll();
        }
      }
    }

    protected abstract void doRun();
  }

  private static final class ThreadNameHarvester extends AbstractNotifyingRunnable {

    private String threadName;

    protected ThreadNameHarvester(Object monitor) {
      super(monitor);
    }

    public String getThreadName() {
      return this.threadName;
    }

    @Override
    protected void doRun() {
      this.threadName = Thread.currentThread().getName();
    }
  }

}
