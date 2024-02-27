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

package cn.taketoday.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/27 14:02
 */
public class GlobalEventExecutor implements Executor {

  private static final Logger logger = LoggerFactory.getLogger(GlobalEventExecutor.class);

  public static final GlobalEventExecutor INSTANCE = new GlobalEventExecutor();
  final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();

  // because the GlobalEventExecutor is a singleton, tasks submitted to it can come from arbitrary threads and this
  // can trigger the creation of a thread from arbitrary thread groups; for this reason, the thread factory must not
  // be sticky about its thread group
  // visible for testing
  final ThreadFactory threadFactory;
  private final GlobalEventExecutor.TaskRunner taskRunner = new GlobalEventExecutor.TaskRunner();
  private final AtomicBoolean started = new AtomicBoolean();
  volatile Thread thread;

  private final Future<?> terminationFuture = new FailedFuture<Object>(this, new UnsupportedOperationException());

  private GlobalEventExecutor() {
    threadFactory = Executors.defaultThreadFactory();
  }

  /**
   * Take the next {@link Runnable} from the task queue and so will block if no task is currently present.
   *
   * @return {@code null} if the executor thread has been interrupted or waken up.
   */
  Runnable takeTask() {
    BlockingQueue<Runnable> taskQueue = this.taskQueue;
    for (; ; ) {
      Runnable task = null;
      try {
        task = taskQueue.take();
      }
      catch (InterruptedException e) {
        // Ignore
      }
      return task;
    }
  }

  /**
   * Return the number of tasks that are pending for processing.
   */
  public int pendingTasks() {
    return taskQueue.size();
  }

  /**
   * Add a task to the task queue, or throws a {@link RejectedExecutionException} if this instance was shutdown
   * before.
   */
  private void addTask(Runnable task) {
    taskQueue.add(task);
  }

  public boolean inEventLoop() {
    return inEventLoop(Thread.currentThread());
  }

  public boolean inEventLoop(Thread thread) {
    return thread == this.thread;
  }

  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
    return terminationFuture();
  }

  public Future<?> terminationFuture() {
    return terminationFuture;
  }

  /**
   * Waits until the worker thread of this executor has no tasks left in its task queue and terminates itself.
   * Because a new worker thread will be started again when a new task is submitted, this operation is only useful
   * when you want to ensure that the worker thread is terminated <strong>after</strong> your application is shut
   * down and there's no chance of submitting a new task afterwards.
   *
   * @return {@code true} if and only if the worker thread has been terminated
   */
  public boolean awaitInactivity(long timeout, TimeUnit unit) throws InterruptedException {
    Assert.notNull(unit, "unit is required");
    final Thread thread = this.thread;
    if (thread == null) {
      throw new IllegalStateException("thread was not started");
    }
    thread.join(unit.toMillis(timeout));
    return !thread.isAlive();
  }

  @Override
  public void execute(Runnable task) {
    execute0(task);
  }

  private void execute0(Runnable task) {
    addTask(task);
    if (!inEventLoop()) {
      startThread();
    }
  }

  private void startThread() {
    if (started.compareAndSet(false, true)) {
      final Thread t = threadFactory.newThread(taskRunner);
      t.setContextClassLoader(null);
      thread = t;
      t.start();
    }
  }

  protected static void runTask(Runnable task) {
    task.run();
  }

  final class TaskRunner implements Runnable {
    @Override
    public void run() {
      for (; ; ) {
        Runnable task = takeTask();
        if (task != null) {
          try {
            runTask(task);
          }
          catch (Throwable t) {
            logger.warn("Unexpected exception from the global event executor: ", t);
          }

        }

        // Terminate if there is no task in the queue (except the noop task).
        if (taskQueue.isEmpty()) {
          // Mark the current thread as stopped.
          // The following CAS must always success and must be uncontended,
          // because only one thread should be running at the same time.
          boolean stopped = started.compareAndSet(true, false);
          assert stopped;

          // Check if there are pending entries added by execute() or schedule*() while we do CAS above.
          // Do not check scheduledTaskQueue because it is not thread-safe and can only be mutated from a
          // TaskRunner actively running tasks.
          if (taskQueue.isEmpty()) {
            // A) No new task was added and thus there's nothing to handle
            //    -> safe to terminate because there's nothing left to do
            // B) A new thread started and handled all the new tasks.
            //    -> safe to terminate the new thread will take care the rest
            break;
          }

          // There are pending tasks added again.
          if (!started.compareAndSet(false, true)) {
            // startThread() started a new thread and set 'started' to true.
            // -> terminate this thread so that the new thread reads from taskQueue exclusively.
            break;
          }

          // New tasks were added, but this worker was faster to set 'started' to true.
          // i.e. a new worker thread was not started by startThread().
          // -> keep this thread alive to handle the newly added entries.
        }
      }
    }
  }

}
