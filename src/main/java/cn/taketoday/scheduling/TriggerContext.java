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

package cn.taketoday.scheduling;

import java.time.Clock;
import java.util.Date;

import cn.taketoday.lang.Nullable;

/**
 * Context object encapsulating last execution times and last completion time
 * of a given task.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface TriggerContext {

  /**
   * Return the clock to use for trigger calculation.
   *
   * @see TaskScheduler#getClock()
   * @see Clock#systemDefaultZone()
   */
  default Clock getClock() {
    return Clock.systemDefaultZone();
  }

  /**
   * Return the last <i>scheduled</i> execution time of the task,
   * or {@code null} if not scheduled before.
   */
  @Nullable
  Date lastScheduledExecutionTime();

  /**
   * Return the last <i>actual</i> execution time of the task,
   * or {@code null} if not scheduled before.
   */
  @Nullable
  Date lastActualExecutionTime();

  /**
   * Return the last completion time of the task,
   * or {@code null} if not scheduled before.
   */
  @Nullable
  Date lastCompletionTime();

}
