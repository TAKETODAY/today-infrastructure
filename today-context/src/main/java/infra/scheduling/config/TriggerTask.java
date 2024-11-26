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

package infra.scheduling.config;

import infra.lang.Assert;
import infra.scheduling.TaskScheduler;
import infra.scheduling.Trigger;

/**
 * {@link Task} implementation defining a {@code Runnable} to be executed
 * according to a given {@link Trigger}.
 *
 * @author Chris Beams
 * @see ScheduledTaskRegistrar#addTriggerTask(TriggerTask)
 * @see TaskScheduler#schedule(Runnable, Trigger)
 * @since 4.0
 */
public class TriggerTask extends Task {

  private final Trigger trigger;

  /**
   * Create a new {@link TriggerTask}.
   *
   * @param runnable the underlying task to execute
   * @param trigger specifies when the task should be executed
   */
  public TriggerTask(Runnable runnable, Trigger trigger) {
    super(runnable);
    Assert.notNull(trigger, "Trigger is required");
    this.trigger = trigger;
  }

  /**
   * Return the associated trigger.
   */
  public Trigger getTrigger() {
    return this.trigger;
  }

}
