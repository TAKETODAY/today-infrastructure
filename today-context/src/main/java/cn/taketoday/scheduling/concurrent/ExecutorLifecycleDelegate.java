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

package cn.taketoday.scheduling.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.lang.Nullable;

/**
 * An internal delegate for common {@link ExecutorService} lifecycle management
 * with pause/resume support.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ExecutorConfigurationSupport
 * @see SimpleAsyncTaskScheduler
 * @since 4.0
 */
final class ExecutorLifecycleDelegate implements SmartLifecycle {

  private final ExecutorService executor;

  private final ReentrantLock pauseLock = new ReentrantLock();

  private final Condition unpaused = this.pauseLock.newCondition();

  private volatile boolean paused;

  private int executingTaskCount = 0;

  @Nullable
  private Runnable stopCallback;

  public ExecutorLifecycleDelegate(ExecutorService executor) {
    this.executor = executor;
  }

  @Override
  public void start() {
    this.pauseLock.lock();
    try {
      this.paused = false;
      this.unpaused.signalAll();
    }
    finally {
      this.pauseLock.unlock();
    }
  }

  @Override
  public void stop() {
    this.pauseLock.lock();
    try {
      this.paused = true;
      this.stopCallback = null;
    }
    finally {
      this.pauseLock.unlock();
    }
  }

  @Override
  public void stop(Runnable callback) {
    this.pauseLock.lock();
    try {
      this.paused = true;
      if (this.executingTaskCount == 0) {
        this.stopCallback = null;
        callback.run();
      }
      else {
        this.stopCallback = callback;
      }
    }
    finally {
      this.pauseLock.unlock();
    }
  }

  @Override
  public boolean isRunning() {
    return (!this.paused && !this.executor.isTerminated());
  }

  void beforeExecute(Thread thread) {
    this.pauseLock.lock();
    try {
      while (this.paused && !this.executor.isShutdown()) {
        this.unpaused.await();
      }
    }
    catch (InterruptedException ex) {
      thread.interrupt();
    }
    finally {
      this.executingTaskCount++;
      this.pauseLock.unlock();
    }
  }

  void afterExecute() {
    this.pauseLock.lock();
    try {
      this.executingTaskCount--;
      if (this.executingTaskCount == 0) {
        Runnable callback = this.stopCallback;
        if (callback != null) {
          callback.run();
          this.stopCallback = null;
        }
      }
    }
    finally {
      this.pauseLock.unlock();
    }
  }

}
