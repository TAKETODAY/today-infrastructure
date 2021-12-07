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

import java.time.Clock;
import java.util.Date;

import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.TriggerContext;

/**
 * Simple data holder implementation of the {@link TriggerContext} interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SimpleTriggerContext implements TriggerContext {

  private final Clock clock;

  @Nullable
  private volatile Date lastScheduledExecutionTime;

  @Nullable
  private volatile Date lastActualExecutionTime;

  @Nullable
  private volatile Date lastCompletionTime;

  /**
   * Create a SimpleTriggerContext with all time values set to {@code null},
   * exposing the system clock for the default time zone.
   */
  public SimpleTriggerContext() {
    this.clock = Clock.systemDefaultZone();
  }

  /**
   * Create a SimpleTriggerContext with the given time values,
   * exposing the system clock for the default time zone.
   *
   * @param lastScheduledExecutionTime last <i>scheduled</i> execution time
   * @param lastActualExecutionTime last <i>actual</i> execution time
   * @param lastCompletionTime last completion time
   */
  public SimpleTriggerContext(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
    this();
    this.lastScheduledExecutionTime = lastScheduledExecutionTime;
    this.lastActualExecutionTime = lastActualExecutionTime;
    this.lastCompletionTime = lastCompletionTime;
  }

  /**
   * Create a SimpleTriggerContext with all time values set to {@code null},
   * exposing the given clock.
   *
   * @param clock the clock to use for trigger calculation
   * @see #update(Date, Date, Date)
   * @since 4.0
   */
  public SimpleTriggerContext(Clock clock) {
    this.clock = clock;
  }

  /**
   * Update this holder's state with the latest time values.
   *
   * @param lastScheduledExecutionTime last <i>scheduled</i> execution time
   * @param lastActualExecutionTime last <i>actual</i> execution time
   * @param lastCompletionTime last completion time
   */
  public void update(Date lastScheduledExecutionTime, Date lastActualExecutionTime, Date lastCompletionTime) {
    this.lastScheduledExecutionTime = lastScheduledExecutionTime;
    this.lastActualExecutionTime = lastActualExecutionTime;
    this.lastCompletionTime = lastCompletionTime;
  }

  @Override
  public Clock getClock() {
    return this.clock;
  }

  @Override
  @Nullable
  public Date lastScheduledExecutionTime() {
    return this.lastScheduledExecutionTime;
  }

  @Override
  @Nullable
  public Date lastActualExecutionTime() {
    return this.lastActualExecutionTime;
  }

  @Override
  @Nullable
  public Date lastCompletionTime() {
    return this.lastCompletionTime;
  }

}
