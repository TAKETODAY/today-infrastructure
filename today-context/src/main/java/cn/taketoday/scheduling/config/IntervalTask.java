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

package cn.taketoday.scheduling.config;

import java.time.Duration;

import cn.taketoday.lang.Assert;

/**
 * {@link Task} implementation defining a {@code Runnable} to be executed at a given
 * millisecond interval which may be treated as fixed-rate or fixed-delay depending on
 * context.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScheduledTaskRegistrar#addFixedRateTask(IntervalTask)
 * @see ScheduledTaskRegistrar#addFixedDelayTask(IntervalTask)
 * @since 4.0
 */
public class IntervalTask extends DelayedTask {

  private final Duration interval;

  /**
   * Create a new {@code IntervalTask}.
   *
   * @param runnable the underlying task to execute
   * @param interval how often in milliseconds the task should be executed
   * @param initialDelay the initial delay before first execution of the task
   */
  public IntervalTask(Runnable runnable, long interval, long initialDelay) {
    this(runnable, Duration.ofMillis(interval), Duration.ofMillis(initialDelay));
  }

  /**
   * Create a new {@code IntervalTask} with no initial delay.
   *
   * @param runnable the underlying task to execute
   * @param interval how often in milliseconds the task should be executed
   */
  public IntervalTask(Runnable runnable, long interval) {
    this(runnable, Duration.ofMillis(interval), Duration.ZERO);
  }

  /**
   * Create a new {@code IntervalTask} with no initial delay.
   *
   * @param runnable the underlying task to execute
   * @param interval how often the task should be executed
   */
  public IntervalTask(Runnable runnable, Duration interval) {
    this(runnable, interval, Duration.ZERO);
  }

  /**
   * Create a new {@code IntervalTask}.
   *
   * @param runnable the underlying task to execute
   * @param interval how often the task should be executed
   * @param initialDelay the initial delay before first execution of the task
   */
  public IntervalTask(Runnable runnable, Duration interval, Duration initialDelay) {
    super(runnable, initialDelay);
    Assert.notNull(interval, "Interval is required");
    Assert.notNull(initialDelay, "InitialDelay is required");

    this.interval = interval;
  }

  /**
   * Copy constructor.
   */
  IntervalTask(IntervalTask task) {
    super(task);
    Assert.notNull(task, "IntervalTask is required");

    this.interval = task.getIntervalDuration();
  }

  /**
   * Return how often in milliseconds the task should be executed.
   */
  public long getInterval() {
    return this.interval.toMillis();
  }

  /**
   * Return how often the task should be executed.
   */
  public Duration getIntervalDuration() {
    return this.interval;
  }

  /**
   * Return the initial delay before first execution of the task.
   */
  public long getInitialDelay() {
    return getInitialDelayDuration().toMillis();
  }

}
