/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

package cn.taketoday.framework.hotswap;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;

import cn.taketoday.context.utils.Assert;

/**
 * A runnable that spawns a monitoring thread triggering any
 * registered {@link FileAlterationObserver} at a specified interval.
 *
 * @author TODAY 2021/2/18 11:43
 * @see FileAlterationObserver
 */
public final class FileAlterationMonitor implements Runnable {

  private final long interval;
  private Thread thread = null;
  private ThreadFactory threadFactory;
  private volatile boolean running = false;
  private final List<FileAlterationObserver> observers = new CopyOnWriteArrayList<>();

  /**
   * Constructs a monitor with a default interval of 10 seconds.
   */
  public FileAlterationMonitor() {
    this(5000);
  }

  /**
   * Constructs a monitor with the specified interval.
   *
   * @param interval
   *         The amount of time in milliseconds to wait between
   *         checks of the file system
   */
  public FileAlterationMonitor(final long interval) {
    this.interval = interval;
  }

  /**
   * Constructs a monitor with the specified interval and set of observers.
   *
   * @param interval
   *         The amount of time in milliseconds to wait between
   *         checks of the file system
   * @param observers
   *         The set of observers to add to the monitor.
   */
  public FileAlterationMonitor(final long interval, final FileAlterationObserver... observers) {
    this(interval);
    if (observers != null) {
      for (final FileAlterationObserver observer : observers) {
        addObserver(observer);
      }
    }
  }

  /**
   * Returns the interval.
   *
   * @return the interval
   */
  public long getInterval() {
    return interval;
  }

  /**
   * Sets the thread factory.
   *
   * @param threadFactory
   *         the thread factory
   */
  public synchronized void setThreadFactory(final ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }

  /**
   * Adds a file system observer to this monitor.
   *
   * @param observer
   *         The file system observer to add
   */
  public void addObserver(final FileAlterationObserver observer) {
    if (observer != null) {
      observers.add(observer);
    }
  }

  /**
   * Removes a file system observer from this monitor.
   *
   * @param observer
   *         The file system observer to remove
   */
  public void removeObserver(final FileAlterationObserver observer) {
    if (observer != null) {
      while (observers.remove(observer)) {
        // empty
      }
    }
  }

  /**
   * Returns the set of {@link FileAlterationObserver} registered with
   * this monitor.
   *
   * @return The set of {@link FileAlterationObserver}
   */
  public Iterable<FileAlterationObserver> getObservers() {
    return observers;
  }

  /**
   * Starts monitoring.
   *
   * @throws Exception
   *         if an error occurs initializing the observer
   */
  public synchronized void start() throws Exception {
    if (running) {
      throw new IllegalStateException("Monitor is already running");
    }
    for (final FileAlterationObserver observer : observers) {
      observer.initialize();
    }
    running = true;
    if (threadFactory != null) {
      thread = threadFactory.newThread(this);
    }
    else {
      thread = new Thread(this);
    }
    thread.start();
  }

  /**
   * Stops monitoring.
   *
   * @throws Exception
   *         if an error occurs initializing the observer
   */
  public synchronized void stop() throws Exception {
    stop(interval);
  }

  /**
   * Stops monitoring.
   *
   * @param stopInterval
   *         the amount of time in milliseconds to wait for the thread to finish.
   *         A value of zero will wait until the thread is finished (see {@link Thread#join(long)}).
   *
   * @throws Exception
   *         if an error occurs initializing the observer
   * @since 2.1
   */
  public synchronized void stop(final long stopInterval) throws Exception {
    Assert.state(running, "Monitor is not running");
    running = false;
    try {
      thread.interrupt();
      thread.join(stopInterval);
    }
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    for (final FileAlterationObserver observer : observers) {
      observer.destroy();
    }
  }

  /**
   * Runs this monitor.
   */
  @Override
  public void run() {
    while (running) {
      for (final FileAlterationObserver observer : observers) {
        observer.checkAndNotify();
      }
      if (!running) {
        break;
      }
      try {
        Thread.sleep(interval);
      }
      catch (final InterruptedException ignored) { }
    }
  }
}
