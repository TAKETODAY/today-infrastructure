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

package cn.taketoday.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.lang.Nullable;

/**
 * Simple customizable helper class for creating new {@link Thread} instances.
 * Provides various bean properties: thread name prefix, thread priority, etc.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/9/11 15:44
 * @since 4.0
 */
@SuppressWarnings("serial")
public class CustomizableThreadCreator implements Serializable {

  private String threadNamePrefix;

  private int threadPriority = Thread.NORM_PRIORITY;

  private boolean daemon = false;

  @Nullable
  private ThreadGroup threadGroup;

  private final AtomicInteger threadCount = new AtomicInteger();

  /**
   * Create a new CustomizableThreadCreator with default thread name prefix.
   */
  public CustomizableThreadCreator() {
    this.threadNamePrefix = getDefaultThreadNamePrefix();
  }

  /**
   * Create a new CustomizableThreadCreator with the given thread name prefix.
   *
   * @param threadNamePrefix the prefix to use for the names of newly created threads
   */
  public CustomizableThreadCreator(@Nullable String threadNamePrefix) {
    this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
  }

  /**
   * Specify the prefix to use for the names of newly created threads.
   * Default is "SimpleAsyncTaskExecutor-".
   */
  public void setThreadNamePrefix(@Nullable String threadNamePrefix) {
    this.threadNamePrefix = (threadNamePrefix != null ? threadNamePrefix : getDefaultThreadNamePrefix());
  }

  /**
   * Return the thread name prefix to use for the names of newly
   * created threads.
   */
  public String getThreadNamePrefix() {
    return this.threadNamePrefix;
  }

  /**
   * Set the priority of the threads that this factory creates.
   * Default is 5.
   *
   * @see java.lang.Thread#NORM_PRIORITY
   */
  public void setThreadPriority(int threadPriority) {
    this.threadPriority = threadPriority;
  }

  /**
   * Return the priority of the threads that this factory creates.
   */
  public int getThreadPriority() {
    return this.threadPriority;
  }

  /**
   * Set whether this factory is supposed to create daemon threads,
   * just executing as long as the application itself is running.
   * <p>Default is "false": Concrete factories usually support explicit cancelling.
   * Hence, if the application shuts down, Runnables will by default finish their
   * execution.
   * <p>Specify "true" for eager shutdown of threads which still actively execute
   * a {@link Runnable} at the time that the application itself shuts down.
   *
   * @see java.lang.Thread#setDaemon
   */
  public void setDaemon(boolean daemon) {
    this.daemon = daemon;
  }

  /**
   * Return whether this factory should create daemon threads.
   */
  public boolean isDaemon() {
    return this.daemon;
  }

  /**
   * Specify the name of the thread group that threads should be created in.
   *
   * @see #setThreadGroup
   */
  public void setThreadGroupName(String name) {
    this.threadGroup = new ThreadGroup(name);
  }

  /**
   * Specify the thread group that threads should be created in.
   *
   * @see #setThreadGroupName
   */
  public void setThreadGroup(@Nullable ThreadGroup threadGroup) {
    this.threadGroup = threadGroup;
  }

  /**
   * Return the thread group that threads should be created in
   * (or {@code null} for the default group).
   */
  @Nullable
  public ThreadGroup getThreadGroup() {
    return this.threadGroup;
  }

  /**
   * Template method for the creation of a new {@link Thread}.
   * <p>The default implementation creates a new Thread for the given
   * {@link Runnable}, applying an appropriate thread name.
   *
   * @param runnable the Runnable to execute
   * @see #nextThreadName()
   */
  public Thread createThread(Runnable runnable) {
    Thread thread = new Thread(getThreadGroup(), runnable, nextThreadName());
    thread.setPriority(getThreadPriority());
    thread.setDaemon(isDaemon());
    return thread;
  }

  /**
   * Return the thread name to use for a newly created {@link Thread}.
   * <p>The default implementation returns the specified thread name prefix
   * with an increasing thread count appended: e.g. "SimpleAsyncTaskExecutor-0".
   *
   * @see #getThreadNamePrefix()
   */
  protected String nextThreadName() {
    return getThreadNamePrefix() + this.threadCount.incrementAndGet();
  }

  /**
   * Build the default thread name prefix for this factory.
   *
   * @return the default thread name prefix (never {@code null})
   */
  protected String getDefaultThreadNamePrefix() {
    return ClassUtils.getShortName(getClass()) + "-";
  }

}
