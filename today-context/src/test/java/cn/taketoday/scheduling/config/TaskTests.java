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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/6 17:03
 */
class TaskTests {

  @Test
  void shouldRejectNullRunnable() {
    assertThatIllegalArgumentException().isThrownBy(() -> new Task(null));
  }

  @Test
  void initialStateShouldBeUnknown() {
    TestRunnable testRunnable = new TestRunnable();
    Task task = new Task(testRunnable);
    assertThat(testRunnable.hasRun).isFalse();
    TaskExecutionOutcome executionOutcome = task.getLastExecutionOutcome();
    assertThat(executionOutcome.executionTime()).isNull();
    assertThat(executionOutcome.status()).isEqualTo(TaskExecutionOutcome.Status.NONE);
    assertThat(executionOutcome.throwable()).isNull();
  }

  @Test
  void stateShouldUpdateAfterRun() {
    TestRunnable testRunnable = new TestRunnable();
    Task task = new Task(testRunnable);
    task.getRunnable().run();

    assertThat(testRunnable.hasRun).isTrue();
    TaskExecutionOutcome executionOutcome = task.getLastExecutionOutcome();
    assertThat(executionOutcome.executionTime()).isInThePast();
    assertThat(executionOutcome.status()).isEqualTo(TaskExecutionOutcome.Status.SUCCESS);
    assertThat(executionOutcome.throwable()).isNull();
  }

  @Test
  void stateShouldUpdateAfterFailingRun() {
    FailingTestRunnable testRunnable = new FailingTestRunnable();
    Task task = new Task(testRunnable);
    assertThatIllegalStateException().isThrownBy(() -> task.getRunnable().run());

    assertThat(testRunnable.hasRun).isTrue();
    TaskExecutionOutcome executionOutcome = task.getLastExecutionOutcome();
    assertThat(executionOutcome.executionTime()).isInThePast();
    assertThat(executionOutcome.status()).isEqualTo(TaskExecutionOutcome.Status.ERROR);
    assertThat(executionOutcome.throwable()).isInstanceOf(IllegalStateException.class);
  }

  static class TestRunnable implements Runnable {

    boolean hasRun;

    @Override
    public void run() {
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      this.hasRun = true;
    }
  }

  static class FailingTestRunnable implements Runnable {

    boolean hasRun;

    @Override
    public void run() {
      try {
        Thread.sleep(1);
      }
      catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
      this.hasRun = true;
      throw new IllegalStateException("test exception");
    }
  }

}