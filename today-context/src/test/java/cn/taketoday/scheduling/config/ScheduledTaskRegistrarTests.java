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

package cn.taketoday.scheduling.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ScheduledTaskRegistrar}.
 *
 * @author Tobias Montagna-Hay
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
class ScheduledTaskRegistrarTests {

  private static final Runnable no_op = () -> { };

  private final ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();

  @BeforeEach
  void preconditions() {
    assertThat(this.taskRegistrar.getTriggerTaskList()).isEmpty();
    assertThat(this.taskRegistrar.getCronTaskList()).isEmpty();
    assertThat(this.taskRegistrar.getFixedRateTaskList()).isEmpty();
    assertThat(this.taskRegistrar.getFixedDelayTaskList()).isEmpty();
  }

  @Test
  void getTriggerTasks() {
    TriggerTask mockTriggerTask = mock(TriggerTask.class);
    this.taskRegistrar.setTriggerTasksList(Collections.singletonList(mockTriggerTask));
    assertThat(this.taskRegistrar.getTriggerTaskList()).containsExactly(mockTriggerTask);
  }

  @Test
  void getCronTasks() {
    CronTask mockCronTask = mock(CronTask.class);
    this.taskRegistrar.setCronTasksList(Collections.singletonList(mockCronTask));
    assertThat(this.taskRegistrar.getCronTaskList()).containsExactly(mockCronTask);
  }

  @Test
  void getFixedRateTasks() {
    IntervalTask mockFixedRateTask = mock(IntervalTask.class);
    this.taskRegistrar.setFixedRateTasksList(Collections.singletonList(mockFixedRateTask));
    assertThat(this.taskRegistrar.getFixedRateTaskList()).containsExactly(mockFixedRateTask);
  }

  @Test
  void getFixedDelayTasks() {
    IntervalTask mockFixedDelayTask = mock(IntervalTask.class);
    this.taskRegistrar.setFixedDelayTasksList(Collections.singletonList(mockFixedDelayTask));
    assertThat(this.taskRegistrar.getFixedDelayTaskList()).containsExactly(mockFixedDelayTask);
  }

  @Test
  void addCronTaskWithValidExpression() {
    this.taskRegistrar.addCronTask(no_op, "* * * * * ?");
    assertThat(this.taskRegistrar.getCronTaskList()).hasSize(1);
  }

  @Test
  void addCronTaskWithInvalidExpression() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.taskRegistrar.addCronTask(no_op, "* * *"))
            .withMessage("Cron expression must consist of 6 fields (found 3 in \"* * *\")");
  }

  @Test
  void addCronTaskWithDisabledExpression() {
    this.taskRegistrar.addCronTask(no_op, ScheduledTaskRegistrar.CRON_DISABLED);
    assertThat(this.taskRegistrar.getCronTaskList()).isEmpty();
  }

}
