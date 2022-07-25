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

package cn.taketoday.core.task;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadFactory;

import cn.taketoday.util.ConcurrencyThrottleSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class SimpleAsyncTaskExecutorTests {

  @Test
  void cannotExecuteWhenConcurrencyIsSwitchedOff() throws Exception {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
    executor.setConcurrencyLimit(ConcurrencyThrottleSupport.NO_CONCURRENCY);
    assertThat(executor.isThrottleActive()).isTrue();
    assertThatIllegalStateException()
            .isThrownBy(() -> executor.execute(new NoOpRunnable()));
  }

  @Test
  void throttleIsNotActiveByDefault() throws Exception {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
    assertThat(executor.isThrottleActive()).as("Concurrency throttle must not default to being active (on)").isFalse();
  }

  @Test
  void threadNameGetsSetCorrectly() throws Exception {
    final String customPrefix = "chankPop#";
    final Object monitor = new Object();
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(customPrefix);
    ThreadNameHarvester task = new ThreadNameHarvester(monitor);
    executeAndWait(executor, task, monitor);
    assertThat(task.getThreadName()).startsWith(customPrefix);
  }

  @Test
  void threadFactoryOverridesDefaults() throws Exception {
    final Object monitor = new Object();
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(r, "test");
      }
    });
    ThreadNameHarvester task = new ThreadNameHarvester(monitor);
    executeAndWait(executor, task, monitor);
    assertThat(task.getThreadName()).isEqualTo("test");
  }

  @Test
  void throwsExceptionWhenSuppliedWithNullRunnable() throws Exception {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new SimpleAsyncTaskExecutor().execute(null));
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

  private static abstract class AbstractNotifyingRunnable implements Runnable {

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
