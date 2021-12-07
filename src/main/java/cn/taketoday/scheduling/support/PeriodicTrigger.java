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

package cn.taketoday.scheduling.support;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.TriggerContext;

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
 * @author Mark Fisher
 * @since 4.0
 */
public class PeriodicTrigger implements Trigger {

  private final long period;

  private final TimeUnit timeUnit;

  private volatile long initialDelay;

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
    Assert.isTrue(period >= 0, "period must not be negative");
    this.timeUnit = (timeUnit != null ? timeUnit : TimeUnit.MILLISECONDS);
    this.period = this.timeUnit.toMillis(period);
  }

  /**
   * Return this trigger's period.
   */
  public long getPeriod() {
    return this.period;
  }

  /**
   * Return this trigger's time unit (milliseconds by default).
   */
  public TimeUnit getTimeUnit() {
    return this.timeUnit;
  }

  /**
   * Specify the delay for the initial execution. It will be evaluated in
   * terms of this trigger's {@link TimeUnit}. If no time unit was explicitly
   * provided upon instantiation, the default is milliseconds.
   */
  public void setInitialDelay(long initialDelay) {
    this.initialDelay = this.timeUnit.toMillis(initialDelay);
  }

  /**
   * Return the initial delay, or 0 if none.
   */
  public long getInitialDelay() {
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
  public Date nextExecutionTime(TriggerContext triggerContext) {
    Date lastExecution = triggerContext.lastScheduledExecutionTime();
    Date lastCompletion = triggerContext.lastCompletionTime();
    if (lastExecution == null || lastCompletion == null) {
      return new Date(triggerContext.getClock().millis() + this.initialDelay);
    }
    if (this.fixedRate) {
      return new Date(lastExecution.getTime() + this.period);
    }
    return new Date(lastCompletion.getTime() + this.period);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof PeriodicTrigger otherTrigger)) {
      return false;
    }
    return (this.fixedRate == otherTrigger.fixedRate && this.initialDelay == otherTrigger.initialDelay &&
            this.period == otherTrigger.period);
  }

  @Override
  public int hashCode() {
    return (this.fixedRate ? 17 : 29) + (int) (37 * this.period) + (int) (41 * this.initialDelay);
  }

}
