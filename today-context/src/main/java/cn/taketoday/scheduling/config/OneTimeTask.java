/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.time.Duration;

/**
 * {@link Task} implementation defining a {@code Runnable} with an initial delay.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScheduledTaskRegistrar#addOneTimeTask(DelayedTask)
 * @since 4.0
 */
public class OneTimeTask extends DelayedTask {

  /**
   * Create a new {@code DelayedTask}.
   *
   * @param runnable the underlying task to execute
   * @param initialDelay the initial delay before execution of the task
   */
  public OneTimeTask(Runnable runnable, Duration initialDelay) {
    super(runnable, initialDelay);
  }

  OneTimeTask(DelayedTask task) {
    super(task);
  }

}
