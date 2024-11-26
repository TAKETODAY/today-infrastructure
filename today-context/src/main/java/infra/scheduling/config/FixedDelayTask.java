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

import java.time.Duration;

import infra.scheduling.annotation.Scheduled;

/**
 * Specialization of {@link IntervalTask} for fixed-delay semantics.
 *
 * @author Juergen Hoeller
 * @see Scheduled#fixedDelay()
 * @see ScheduledTaskRegistrar#addFixedDelayTask(IntervalTask)
 * @since 4.0
 */
public class FixedDelayTask extends IntervalTask {

  /**
   * Create a new {@code FixedDelayTask}.
   *
   * @param runnable the underlying task to execute
   * @param interval how often in milliseconds the task should be executed
   * @param initialDelay the initial delay before first execution of the task
   */
  public FixedDelayTask(Runnable runnable, long interval, long initialDelay) {
    super(runnable, interval, initialDelay);
  }

	/**
	 * Create a new {@code FixedDelayTask}.
	 * @param runnable the underlying task to execute
	 * @param interval how often the task should be executed
	 * @param initialDelay the initial delay before first execution of the task
	 */
	public FixedDelayTask(Runnable runnable, Duration interval, Duration initialDelay) {
		super(runnable, interval, initialDelay);
	}

	FixedDelayTask(IntervalTask task) {
		super(task);
	}

}
