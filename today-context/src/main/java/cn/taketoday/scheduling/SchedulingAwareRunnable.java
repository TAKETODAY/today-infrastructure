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

/**
 * Extension of the {@link Runnable} interface, adding special callbacks
 * for long-running operations.
 *
 * <p>Scheduling-capable TaskExecutors are encouraged to check a submitted
 * Runnable, detecting whether this interface is implemented and reacting
 * as appropriately as they are able to.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.core.task.TaskExecutor
 * @see SchedulingTaskExecutor
 * @since 4.0
 */
public interface SchedulingAwareRunnable extends Runnable {

  /**
   * Return whether the Runnable's operation is long-lived
   * ({@code true}) versus short-lived ({@code false}).
   * <p>In the former case, the task will not allocate a thread from the thread
   * pool (if any) but rather be considered as long-running background thread.
   * <p>This should be considered a hint. Of course TaskExecutor implementations
   * are free to ignore this flag and the SchedulingAwareRunnable interface overall.
   */
  boolean isLongLived();

}
