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

package cn.taketoday.scheduling.concurrent;

import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * JavaBean that describes a scheduled executor task, consisting of the
 * {@link Runnable} and a delay plus period. The period needs to be specified;
 * there is no point in a default for it.
 *
 * <p>The {@link java.util.concurrent.ScheduledExecutorService} does not offer
 * more sophisticated scheduling options such as cron expressions.
 * Consider using {@link ThreadPoolTaskScheduler} for such needs.
 *
 * <p>Note that the {@link java.util.concurrent.ScheduledExecutorService} mechanism
 * uses a {@link Runnable} instance that is shared between repeated executions,
 * in contrast to Quartz which creates a new Job instance for each execution.
 *
 * @author Juergen Hoeller
 * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)
 * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
 * @since 4.0
 */
public class ScheduledExecutorTask {

  @Nullable
  private Runnable runnable;

  private long delay = 0;

  private long period = -1;

  private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

  private boolean fixedRate = false;

  /**
   * Create a new ScheduledExecutorTask,
   * to be populated via bean properties.
   *
   * @see #setDelay
   * @see #setPeriod
   * @see #setFixedRate
   */
  public ScheduledExecutorTask() { }

  /**
   * Create a new ScheduledExecutorTask, with default
   * one-time execution without delay.
   *
   * @param executorTask the Runnable to schedule
   */
  public ScheduledExecutorTask(Runnable executorTask) {
    this.runnable = executorTask;
  }

  /**
   * Create a new ScheduledExecutorTask, with default
   * one-time execution with the given delay.
   *
   * @param executorTask the Runnable to schedule
   * @param delay the delay before starting the task for the first time (ms)
   */
  public ScheduledExecutorTask(Runnable executorTask, long delay) {
    this.runnable = executorTask;
    this.delay = delay;
  }

  /**
   * Create a new ScheduledExecutorTask.
   *
   * @param executorTask the Runnable to schedule
   * @param delay the delay before starting the task for the first time (ms)
   * @param period the period between repeated task executions (ms)
   * @param fixedRate whether to schedule as fixed-rate execution
   */
  public ScheduledExecutorTask(Runnable executorTask, long delay, long period, boolean fixedRate) {
    this.runnable = executorTask;
    this.delay = delay;
    this.period = period;
    this.fixedRate = fixedRate;
  }

  /**
   * Set the Runnable to schedule as executor task.
   */
  public void setRunnable(Runnable executorTask) {
    this.runnable = executorTask;
  }

  /**
   * Return the Runnable to schedule as executor task.
   */
  public Runnable getRunnable() {
    Assert.state(this.runnable != null, "No Runnable set");
    return this.runnable;
  }

  /**
   * Set the delay before starting the task for the first time,
   * in milliseconds. Default is 0, immediately starting the
   * task after successful scheduling.
   */
  public void setDelay(long delay) {
    this.delay = delay;
  }

  /**
   * Return the delay before starting the job for the first time.
   */
  public long getDelay() {
    return this.delay;
  }

  /**
   * Set the period between repeated task executions, in milliseconds.
   * <p>Default is -1, leading to one-time execution. In case of a positive value,
   * the task will be executed repeatedly, with the given interval in-between executions.
   * <p>Note that the semantics of the period value vary between fixed-rate and
   * fixed-delay execution.
   * <p><b>Note:</b> A period of 0 (for example as fixed delay) is <i>not</i> supported,
   * simply because {@code java.util.concurrent.ScheduledExecutorService} itself
   * does not support it. Hence a value of 0 will be treated as one-time execution;
   * however, that value should never be specified explicitly in the first place!
   *
   * @see #setFixedRate
   * @see #isOneTimeTask()
   * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)
   */
  public void setPeriod(long period) {
    this.period = period;
  }

  /**
   * Return the period between repeated task executions.
   */
  public long getPeriod() {
    return this.period;
  }

  /**
   * Is this task only ever going to execute once?
   *
   * @return {@code true} if this task is only ever going to execute once
   * @see #getPeriod()
   */
  public boolean isOneTimeTask() {
    return (this.period <= 0);
  }

  /**
   * Specify the time unit for the delay and period values.
   * Default is milliseconds ({@code TimeUnit.MILLISECONDS}).
   *
   * @see TimeUnit#MILLISECONDS
   * @see TimeUnit#SECONDS
   */
  public void setTimeUnit(@Nullable TimeUnit timeUnit) {
    this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
  }

  /**
   * Return the time unit for the delay and period values.
   */
  public TimeUnit getTimeUnit() {
    return this.timeUnit;
  }

  /**
   * Set whether to schedule as fixed-rate execution, rather than
   * fixed-delay execution. Default is "false", that is, fixed delay.
   * <p>See ScheduledExecutorService javadoc for details on those execution modes.
   *
   * @see java.util.concurrent.ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)
   * @see java.util.concurrent.ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
   */
  public void setFixedRate(boolean fixedRate) {
    this.fixedRate = fixedRate;
  }

  /**
   * Return whether to schedule as fixed-rate execution.
   */
  public boolean isFixedRate() {
    return this.fixedRate;
  }

}
