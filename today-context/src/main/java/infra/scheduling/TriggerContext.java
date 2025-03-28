/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.scheduling;

import java.time.Clock;
import java.time.Instant;

import infra.lang.Nullable;

/**
 * Context object encapsulating last execution times and last completion time
 * of a given task.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
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
  Instant lastScheduledExecution();

  /**
   * Return the last <i>actual</i> execution time of the task,
   * or {@code null} if not scheduled before.
   */
  @Nullable
  Instant lastActualExecution();

  /**
   * Return the last completion time of the task,
   * or {@code null} if not scheduled before.
   */
  @Nullable
  Instant lastCompletion();

}
