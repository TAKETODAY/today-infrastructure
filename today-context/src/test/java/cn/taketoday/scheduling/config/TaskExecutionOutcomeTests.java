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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/6 17:06
 */
class TaskExecutionOutcomeTests {

  @Test
  void shouldCreateWithNoneStatus() {
    TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
    assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.NONE);
    assertThat(outcome.executionTime()).isNull();
    assertThat(outcome.throwable()).isNull();
  }

  @Test
  void startedTaskShouldBeOngoing() {
    TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
    Instant now = Instant.now();
    outcome = outcome.start(now);
    assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.STARTED);
    assertThat(outcome.executionTime()).isEqualTo(now);
    assertThat(outcome.throwable()).isNull();
  }

  @Test
  void shouldRejectSuccessWhenNotStarted() {
    TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
    assertThatIllegalStateException().isThrownBy(outcome::success);
  }

  @Test
  void shouldRejectErrorWhenNotStarted() {
    TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
    assertThatIllegalStateException().isThrownBy(() -> outcome.failure(new IllegalArgumentException("test error")));
  }

  @Test
  void finishedTaskShouldBeSuccessful() {
    TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
    Instant now = Instant.now();
    outcome = outcome.start(now);
    outcome = outcome.success();
    assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.SUCCESS);
    assertThat(outcome.executionTime()).isEqualTo(now);
    assertThat(outcome.throwable()).isNull();
  }

  @Test
  void errorTaskShouldBeFailure() {
    TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
    Instant now = Instant.now();
    outcome = outcome.start(now);
    outcome = outcome.failure(new IllegalArgumentException(("test error")));
    assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.ERROR);
    assertThat(outcome.executionTime()).isEqualTo(now);
    assertThat(outcome.throwable()).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void newTaskExecutionShouldNotFail() {
    TaskExecutionOutcome outcome = TaskExecutionOutcome.create();
    Instant now = Instant.now();
    outcome = outcome.start(now);
    outcome = outcome.failure(new IllegalArgumentException(("test error")));

    outcome = outcome.start(now.plusSeconds(2));
    assertThat(outcome.status()).isEqualTo(TaskExecutionOutcome.Status.STARTED);
    assertThat(outcome.executionTime()).isAfter(now);
    assertThat(outcome.throwable()).isNull();
  }

}