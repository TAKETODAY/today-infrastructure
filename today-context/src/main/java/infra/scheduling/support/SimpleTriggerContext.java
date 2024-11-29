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

import java.time.Clock;
import java.time.Instant;

import infra.lang.Nullable;
import infra.scheduling.TriggerContext;

/**
 * Simple data holder implementation of the {@link TriggerContext} interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleTriggerContext implements TriggerContext {

  private final Clock clock;

  @Nullable
  private volatile Instant lastScheduledExecution;

  @Nullable
  private volatile Instant lastActualExecution;

  @Nullable
  private volatile Instant lastCompletion;

  /**
   * Create a SimpleTriggerContext with all time values set to {@code null},
   * exposing the system clock for the default time zone.
   */
  public SimpleTriggerContext() {
    this.clock = Clock.systemDefaultZone();
  }

  /**
   * Create a SimpleTriggerContext with all time values set to {@code null},
   * exposing the given clock.
   *
   * @param clock the clock to use for trigger calculation
   * @see #update(Instant, Instant, Instant)
   */
  public SimpleTriggerContext(Clock clock) {
    this.clock = clock;
  }

  /**
   * Create a SimpleTriggerContext with the given time values,
   * exposing the system clock for the default time zone.
   *
   * @param lastScheduledExecution last <i>scheduled</i> execution time
   * @param lastActualExecution last <i>actual</i> execution time
   * @param lastCompletion last completion time
   */
  public SimpleTriggerContext(@Nullable Instant lastScheduledExecution,
          @Nullable Instant lastActualExecution, @Nullable Instant lastCompletion) {

    this();
    this.lastScheduledExecution = lastScheduledExecution;
    this.lastActualExecution = lastActualExecution;
    this.lastCompletion = lastCompletion;
  }

  /**
   * Update this holder's state with the latest time values.
   *
   * @param lastScheduledExecution last <i>scheduled</i> execution time
   * @param lastActualExecution last <i>actual</i> execution time
   * @param lastCompletion last completion time
   */
  public void update(@Nullable Instant lastScheduledExecution,
          @Nullable Instant lastActualExecution, @Nullable Instant lastCompletion) {

    this.lastScheduledExecution = lastScheduledExecution;
    this.lastActualExecution = lastActualExecution;
    this.lastCompletion = lastCompletion;
  }

  @Override
  public Clock getClock() {
    return this.clock;
  }

  @Override
  @Nullable
  public Instant lastScheduledExecution() {
    return this.lastScheduledExecution;
  }

  @Override
  @Nullable
  public Instant lastActualExecution() {
    return this.lastActualExecution;
  }

  @Override
  @Nullable
  public Instant lastCompletion() {
    return this.lastCompletion;
  }

}
