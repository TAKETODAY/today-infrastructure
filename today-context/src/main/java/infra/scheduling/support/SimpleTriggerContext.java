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

package infra.scheduling.support;

import org.jspecify.annotations.Nullable;

import java.time.Clock;
import java.time.Instant;

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
