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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import cn.taketoday.scheduling.concurrent.SimpleAsyncTaskScheduler;

import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/7/6 17:01
 */
class ScheduledTaskTests {

  private CountingRunnable countingRunnable = new CountingRunnable();

  private SimpleAsyncTaskScheduler taskScheduler = new SimpleAsyncTaskScheduler();

  private ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();

  @BeforeEach
  void setup() {
    this.taskRegistrar.setTaskScheduler(this.taskScheduler);
    taskScheduler.start();
  }

  @AfterEach
  void tearDown() {
    taskScheduler.stop();
  }

  @Test
  void shouldReturnConfiguredTask() {
    Task task = new Task(countingRunnable);
    ScheduledTask scheduledTask = new ScheduledTask(task);
    assertThat(scheduledTask.getTask()).isEqualTo(task);
  }

  @Test
  void shouldUseTaskToString() {
    Task task = new Task(countingRunnable);
    ScheduledTask scheduledTask = new ScheduledTask(task);
    assertThat(scheduledTask.toString()).isEqualTo(task.toString());
  }

  @Test
  void unscheduledTaskShouldNotHaveNextExecution() {
    ScheduledTask scheduledTask = new ScheduledTask(new Task(countingRunnable));
    assertThat(scheduledTask.nextExecution()).isNull();
    assertThat(countingRunnable.executionCount).isZero();
  }

  @Test
  void scheduledTaskShouldHaveNextExecution() {
    ScheduledTask scheduledTask = taskRegistrar.scheduleFixedDelayTask(new FixedDelayTask(countingRunnable,
            Duration.ofSeconds(10), Duration.ofSeconds(10)));
    assertThat(scheduledTask.nextExecution()).isBefore(Instant.now().plusSeconds(11));
  }

  @Test
  void cancelledTaskShouldNotHaveNextExecution() {
    ScheduledTask scheduledTask = taskRegistrar.scheduleFixedDelayTask(new FixedDelayTask(countingRunnable,
            Duration.ofSeconds(10), Duration.ofSeconds(10)));
    scheduledTask.cancel(true);
    assertThat(scheduledTask.nextExecution()).isNull();
  }

  @Test
  void singleExecutionShouldNotHaveNextExecution() {
    ScheduledTask scheduledTask = taskRegistrar.scheduleOneTimeTask(new OneTimeTask(countingRunnable, Duration.ofSeconds(0)));
    Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> countingRunnable.executionCount > 0);
    assertThat(scheduledTask.nextExecution()).isNull();
  }

  class CountingRunnable implements Runnable {

    int executionCount;

    @Override
    public void run() {
      executionCount++;
    }
  }

}