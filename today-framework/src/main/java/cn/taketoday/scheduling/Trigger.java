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

import java.util.Date;

import cn.taketoday.lang.Nullable;

/**
 * Common interface for trigger objects that determine the next execution time
 * of a task that they get associated with.
 *
 * @author Juergen Hoeller
 * @see TaskScheduler#schedule(Runnable, Trigger)
 * @see cn.taketoday.scheduling.support.CronTrigger
 * @since 4.0
 */
public interface Trigger {

  /**
   * Determine the next execution time according to the given trigger context.
   *
   * @param triggerContext context object encapsulating last execution times
   * and last completion time
   * @return the next execution time as defined by the trigger,
   * or {@code null} if the trigger won't fire anymore
   */
  @Nullable
  Date nextExecutionTime(TriggerContext triggerContext);

}
