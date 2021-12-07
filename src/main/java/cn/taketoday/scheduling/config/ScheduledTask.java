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

import java.util.concurrent.ScheduledFuture;

import cn.taketoday.lang.Nullable;

/**
 * A representation of a scheduled task at runtime,
 * used as a return value for scheduling methods.
 *
 * @author Juergen Hoeller
 * @see ScheduledTaskRegistrar#scheduleCronTask(CronTask)
 * @see ScheduledTaskRegistrar#scheduleFixedRateTask(FixedRateTask)
 * @see ScheduledTaskRegistrar#scheduleFixedDelayTask(FixedDelayTask)
 * @since 4.0
 */
public final class ScheduledTask {

  private final Task task;

  @Nullable
  volatile ScheduledFuture<?> future;

  ScheduledTask(Task task) {
    this.task = task;
  }

  /**
   * Return the underlying task (typically a {@link CronTask},
   * {@link FixedRateTask} or {@link FixedDelayTask}).
   */
  public Task getTask() {
    return this.task;
  }

  /**
   * Trigger cancellation of this scheduled task.
   */
  public void cancel() {
    ScheduledFuture<?> future = this.future;
    if (future != null) {
      future.cancel(true);
    }
  }

  @Override
  public String toString() {
    return this.task.toString();
  }

}
