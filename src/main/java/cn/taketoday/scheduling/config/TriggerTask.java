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

package cn.taketoday.scheduling.config;

import cn.taketoday.lang.Assert;
import cn.taketoday.scheduling.Trigger;

/**
 * {@link Task} implementation defining a {@code Runnable} to be executed
 * according to a given {@link Trigger}.
 *
 * @author Chris Beams
 * @see ScheduledTaskRegistrar#addTriggerTask(TriggerTask)
 * @see cn.taketoday.scheduling.TaskScheduler#schedule(Runnable, Trigger)
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
    Assert.notNull(trigger, "Trigger must not be null");
    this.trigger = trigger;
  }

  /**
   * Return the associated trigger.
   */
  public Trigger getTrigger() {
    return this.trigger;
  }

}
