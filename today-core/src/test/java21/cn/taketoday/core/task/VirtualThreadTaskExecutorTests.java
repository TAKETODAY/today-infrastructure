/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/21 13:55
 */
class VirtualThreadTaskExecutorTests {

  @Test
  void virtualThreadsWithoutName() {
    final Object monitor = new Object();
    VirtualThreadTaskExecutor executor = new VirtualThreadTaskExecutor();
    ThreadNameHarvester task = new ThreadNameHarvester(monitor);
    executeAndWait(executor, task, monitor);
    assertThat(task.getThreadName()).isEmpty();
    assertThat(task.isVirtual()).isTrue();
    assertThat(task.runCount()).isOne();
  }

  @Test
  void virtualThreadsWithNamePrefix() {
    final Object monitor = new Object();
    VirtualThreadTaskExecutor executor = new VirtualThreadTaskExecutor("test-");
    ThreadNameHarvester task = new ThreadNameHarvester(monitor);
    executeAndWait(executor, task, monitor);
    assertThat(task.getThreadName()).isEqualTo("test-0");
    assertThat(task.isVirtual()).isTrue();
    assertThat(task.runCount()).isOne();
  }

  @Test
  void simpleWithVirtualThreadFactory() {
    final Object monitor = new Object();
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(Thread.ofVirtual().name("test").factory());
    ThreadNameHarvester task = new ThreadNameHarvester(monitor);
    executeAndWait(executor, task, monitor);
    assertThat(task.getThreadName()).isEqualTo("test");
    assertThat(task.isVirtual()).isTrue();
    assertThat(task.runCount()).isOne();
  }

  @Test
  void simpleWithVirtualThreadFlag() {
    final String customPrefix = "chankPop#";
    final Object monitor = new Object();
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(customPrefix);
    executor.setVirtualThreads(true);
    ThreadNameHarvester task = new ThreadNameHarvester(monitor);
    executeAndWait(executor, task, monitor);
    assertThat(task.getThreadName()).startsWith(customPrefix);
    assertThat(task.isVirtual()).isTrue();
    assertThat(task.runCount()).isOne();
  }

  private void executeAndWait(TaskExecutor executor, Runnable task, Object monitor) {
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
      // no-op
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

    private final AtomicInteger runCount = new AtomicInteger();

    private String threadName;

    private boolean virtual;

    protected ThreadNameHarvester(Object monitor) {
      super(monitor);
    }

    public String getThreadName() {
      return this.threadName;
    }

    public boolean isVirtual() {
      return this.virtual;
    }

    public int runCount() {
      return this.runCount.get();
    }

    @Override
    protected void doRun() {
      Thread thread = Thread.currentThread();
      this.threadName = thread.getName();
      this.virtual = thread.isVirtual();
      runCount.incrementAndGet();
    }
  }

}