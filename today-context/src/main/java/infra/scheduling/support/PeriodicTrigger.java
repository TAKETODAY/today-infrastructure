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

package infra.scheduling.support;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.scheduling.Trigger;
import infra.scheduling.TriggerContext;

/**
 * A trigger for periodic task execution. The period may be applied as either
 * fixed-rate or fixed-delay, and an initial delay value may also be configured.
 * The default initial delay is 0, and the default behavior is fixed-delay
 * (i.e. the interval between successive executions is measured from each
 * <i>completion</i> time). To measure the interval between the
 * scheduled <i>start</i> time of each execution instead, set the
 * 'fixedRate' property to {@code true}.
 *
 * <p>Note that the TaskScheduler interface already defines methods for scheduling
 * tasks at fixed-rate or with fixed-delay. Both also support an optional value
 * for the initial delay. Those methods should be used directly whenever
 * possible. The value of this Trigger implementation is that it can be used
 * within components that rely on the Trigger abstraction. For example, it may
 * be convenient to allow periodic triggers, cron-based triggers, and even
 * custom Trigger implementations to be used interchangeably.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Mark Fisher
 * @since 4.0
 */
public class PeriodicTrigger implements Trigger {

  private final Duration period;

  @Nullable
  private final ChronoUnit chronoUnit;

  @Nullable
  private volatile Duration initialDelay;

  private volatile boolean fixedRate;

  /**
   * Create a trigger with the given period in milliseconds.
   */
  public PeriodicTrigger(long period) {
    this(period, null);
  }

  /**
   * Create a trigger with the given period and time unit. The time unit will
   * apply not only to the period but also to any 'initialDelay' value, if
   * configured on this Trigger later via {@link #setInitialDelay(long)}.
   */
  public PeriodicTrigger(long period, @Nullable TimeUnit timeUnit) {
    this(toDuration(period, timeUnit), timeUnit);
  }

  /**
   * Create a trigger with the given period as a duration.
   */
  public PeriodicTrigger(Duration period) {
    this(period, null);
  }

  private PeriodicTrigger(Duration period, @Nullable TimeUnit timeUnit) {
    Assert.notNull(period, "Period is required");
    Assert.isTrue(!period.isNegative(), "Period must not be negative");
    this.period = period;
    if (timeUnit != null) {
      this.chronoUnit = timeUnit.toChronoUnit();
    }
    else {
      this.chronoUnit = null;
    }
  }

  /**
   * Return this trigger's period.
   */
  public long getPeriod() {
    if (this.chronoUnit != null) {
      return this.period.get(this.chronoUnit);
    }
    else {
      return this.period.toMillis();
    }
  }

  /**
   * Return this trigger's period.
   */
  public Duration getPeriodDuration() {
    return this.period;
  }

  /**
   * Return this trigger's time unit (milliseconds by default).
   */
  public TimeUnit getTimeUnit() {
    if (this.chronoUnit != null) {
      return TimeUnit.of(this.chronoUnit);
    }
    else {
      return TimeUnit.MILLISECONDS;
    }
  }

  /**
   * Specify the delay for the initial execution. It will be evaluated in
   * terms of this trigger's {@link TimeUnit}. If no time unit was explicitly
   * provided upon instantiation, the default is milliseconds.
   */
  public void setInitialDelay(long initialDelay) {
    if (this.chronoUnit != null) {
      this.initialDelay = Duration.of(initialDelay, this.chronoUnit);
    }
    else {
      this.initialDelay = Duration.ofMillis(initialDelay);
    }
  }

  /**
   * Specify the delay for the initial execution.
   */
  public void setInitialDelay(Duration initialDelay) {
    this.initialDelay = initialDelay;
  }

  /**
   * Return the initial delay, or 0 if none.
   */
  public long getInitialDelay() {
    Duration initialDelay = this.initialDelay;
    if (initialDelay != null) {
      if (this.chronoUnit != null) {
        return initialDelay.get(this.chronoUnit);
      }
      else {
        return initialDelay.toMillis();
      }
    }
    else {
      return 0;
    }
  }

  /**
   * Return the initial delay, or {@code null} if none.
   */
  @Nullable
  public Duration getInitialDelayDuration() {
    return this.initialDelay;
  }

  /**
   * Specify whether the periodic interval should be measured between the
   * scheduled start times rather than between actual completion times.
   * The latter, "fixed delay" behavior, is the default.
   */
  public void setFixedRate(boolean fixedRate) {
    this.fixedRate = fixedRate;
  }

  /**
   * Return whether this trigger uses fixed rate ({@code true}) or
   * fixed delay ({@code false}) behavior.
   */
  public boolean isFixedRate() {
    return this.fixedRate;
  }

  /**
   * Returns the time after which a task should run again.
   */
  @Override
  public Instant nextExecution(TriggerContext triggerContext) {
    Instant lastExecution = triggerContext.lastScheduledExecution();
    Instant lastCompletion = triggerContext.lastCompletion();
    if (lastExecution == null || lastCompletion == null) {
      Instant instant = triggerContext.getClock().instant();
      Duration initialDelay = this.initialDelay;
      if (initialDelay == null) {
        return instant;
      }
      else {
        return instant.plus(initialDelay);
      }
    }
    if (this.fixedRate) {
      return lastExecution.plus(this.period);
    }
    return lastCompletion.plus(this.period);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PeriodicTrigger otherTrigger)) {
      return false;
    }
    return this.fixedRate == otherTrigger.fixedRate
            && this.period.equals(otherTrigger.period)
            && Objects.equals(this.initialDelay, otherTrigger.initialDelay);
  }

  @Override
  public int hashCode() {
    return this.period.hashCode();
  }

  private static Duration toDuration(long amount, @Nullable TimeUnit timeUnit) {
    if (timeUnit != null) {
      return Duration.of(amount, timeUnit.toChronoUnit());
    }
    else {
      return Duration.ofMillis(amount);
    }
  }
}
