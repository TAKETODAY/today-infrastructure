/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.scheduling.config;

import java.time.Duration;

import infra.lang.Assert;

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
