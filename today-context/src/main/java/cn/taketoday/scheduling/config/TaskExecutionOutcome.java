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

package cn.taketoday.scheduling.config;

import java.time.Instant;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Outcome of a {@link Task} execution.
 *
 * @param executionTime the instant when the task execution started, {@code null} if the task has not started.
 * @param status the {@link Status} of the execution outcome.
 * @param throwable the exception thrown from the task execution, if any.
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public record TaskExecutionOutcome(@Nullable Instant executionTime, Status status, @Nullable Throwable throwable) {

  TaskExecutionOutcome start(Instant executionTime) {
    return new TaskExecutionOutcome(executionTime, Status.STARTED, null);
  }

  TaskExecutionOutcome success() {
    Assert.state(this.executionTime != null, "Task has not been started yet");
    return new TaskExecutionOutcome(this.executionTime, Status.SUCCESS, null);
  }

  TaskExecutionOutcome failure(Throwable throwable) {
    Assert.state(this.executionTime != null, "Task has not been started yet");
    return new TaskExecutionOutcome(this.executionTime, Status.ERROR, throwable);
  }

  static TaskExecutionOutcome create() {
    return new TaskExecutionOutcome(null, Status.NONE, null);
  }

  /**
   * Status of the task execution outcome.
   */
  public enum Status {
    /**
     * The task has not been executed so far.
     */
    NONE,
    /**
     * The task execution has been started and is ongoing.
     */
    STARTED,
    /**
     * The task execution finished successfully.
     */
    SUCCESS,
    /**
     * The task execution finished with an error.
     */
    ERROR
  }
}
