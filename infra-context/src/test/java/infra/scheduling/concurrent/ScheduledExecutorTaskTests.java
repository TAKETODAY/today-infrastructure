/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.scheduling.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 20:18
 */
class ScheduledExecutorTaskTests {


  @Test
  void defaultConstructorCreatesTaskWithDefaultValues() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    assertThat(task.getDelay()).isEqualTo(0);
    assertThat(task.getPeriod()).isEqualTo(-1);
    assertThat(task.getTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    assertThat(task.isFixedRate()).isFalse();
    assertThatThrownBy(task::getRunnable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No Runnable set");
  }

  @Test
  void runnableOnlyConstructorSetsDefaultValues() {
    Runnable runnable = () -> {};
    ScheduledExecutorTask task = new ScheduledExecutorTask(runnable);

    assertThat(task.getRunnable()).isSameAs(runnable);
    assertThat(task.getDelay()).isZero();
    assertThat(task.getPeriod()).isEqualTo(-1);
    assertThat(task.isFixedRate()).isFalse();
  }

  @Test
  void constructorWithDelayInitializesCorrectly() {
    Runnable runnable = () -> {};
    ScheduledExecutorTask task = new ScheduledExecutorTask(runnable, 1000);

    assertThat(task.getRunnable()).isSameAs(runnable);
    assertThat(task.getDelay()).isEqualTo(1000);
    assertThat(task.getPeriod()).isEqualTo(-1);
  }

  @Test
  void fullConstructorInitializesAllProperties() {
    Runnable runnable = () -> {};
    ScheduledExecutorTask task = new ScheduledExecutorTask(runnable, 1000, 5000, true);

    assertThat(task.getRunnable()).isSameAs(runnable);
    assertThat(task.getDelay()).isEqualTo(1000);
    assertThat(task.getPeriod()).isEqualTo(5000);
    assertThat(task.isFixedRate()).isTrue();
  }

  @Test
  void oneTimeTaskWhenPeriodIsZero() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setPeriod(0);
    assertThat(task.isOneTimeTask()).isTrue();
  }

  @Test
  void oneTimeTaskWhenPeriodIsNegative() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setPeriod(-100);
    assertThat(task.isOneTimeTask()).isTrue();
  }

  @Test
  void notOneTimeTaskWhenPeriodIsPositive() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setPeriod(100);
    assertThat(task.isOneTimeTask()).isFalse();
  }

  @Test
  void defaultTimeUnitWhenSettingNull() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setTimeUnit(null);
    assertThat(task.getTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);
  }

  @Test
  void customTimeUnitIsRespected() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setTimeUnit(TimeUnit.SECONDS);
    assertThat(task.getTimeUnit()).isEqualTo(TimeUnit.SECONDS);
  }


  @Test
  void setRunnableUpdatesTask() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    Runnable runnable = () -> {};
    task.setRunnable(runnable);
    assertThat(task.getRunnable()).isSameAs(runnable);
  }

  @Test
  void negativeDelayIsAllowed() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setDelay(-1000);
    assertThat(task.getDelay()).isEqualTo(-1000);
  }

  @Test
  void setFixedRateTogglesBetweenTrueAndFalse() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setFixedRate(true);
    assertThat(task.isFixedRate()).isTrue();
    task.setFixedRate(false);
    assertThat(task.isFixedRate()).isFalse();
  }

  @Test
  void timeUnitCanBeChangedMultipleTimes() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setTimeUnit(TimeUnit.SECONDS);
    assertThat(task.getTimeUnit()).isEqualTo(TimeUnit.SECONDS);
    task.setTimeUnit(TimeUnit.MINUTES);
    assertThat(task.getTimeUnit()).isEqualTo(TimeUnit.MINUTES);
    task.setTimeUnit(TimeUnit.HOURS);
    assertThat(task.getTimeUnit()).isEqualTo(TimeUnit.HOURS);
  }

  @Test
  void equalPeriodsAreHandledCorrectly() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    task.setPeriod(1000);
    assertThat(task.getPeriod()).isEqualTo(1000);
    task.setPeriod(1000);
    assertThat(task.getPeriod()).isEqualTo(1000);
  }

  @Test
  void nullRunnableThrowsExceptionOnGet() {
    ScheduledExecutorTask task = new ScheduledExecutorTask();
    assertThatThrownBy(task::getRunnable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No Runnable set");
  }



}