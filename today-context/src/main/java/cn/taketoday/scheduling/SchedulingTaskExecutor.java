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

import cn.taketoday.core.task.AsyncTaskExecutor;

/**
 * A {@link cn.taketoday.core.task.TaskExecutor} extension exposing
 * scheduling characteristics that are relevant to potential task submitters.
 *
 * <p>Scheduling clients are encouraged to submit
 * {@link Runnable Runnables} that match the exposed preferences
 * of the {@code TaskExecutor} implementation in use.
 *
 * <p>Note: {@link SchedulingTaskExecutor} implementations are encouraged to also
 * implement the {@link cn.taketoday.core.task.AsyncTaskExecutor}
 * interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SchedulingAwareRunnable
 * @see cn.taketoday.core.task.TaskExecutor
 * @since 4.0
 */
public interface SchedulingTaskExecutor extends AsyncTaskExecutor {

  /**
   * Does this {@code TaskExecutor} prefer short-lived tasks over long-lived tasks?
   * <p>A {@code SchedulingTaskExecutor} implementation can indicate whether it
   * prefers submitted tasks to perform as little work as they can within a single
   * task execution. For example, submitted tasks might break a repeated loop into
   * individual subtasks which submit a follow-up task afterwards (if feasible).
   * <p>This should be considered a hint. Of course {@code TaskExecutor} clients
   * are free to ignore this flag and hence the {@code SchedulingTaskExecutor}
   * interface overall. However, thread pools will usually indicated a preference
   * for short-lived tasks, allowing for more fine-grained scheduling.
   *
   * @return {@code true} if this executor prefers short-lived tasks (the default),
   * {@code false} otherwise (for treatment like a regular {@code TaskExecutor})
   */
  default boolean prefersShortLivedTasks() {
    return true;
  }

}
