/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.scheduling.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import infra.core.task.TaskRejectedException;
import infra.scheduling.TaskScheduler;
import infra.scheduling.Trigger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
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

  @Test
  void schedulesConfiguredTasksAndCancelsTheirFuturesOnDestruction() {
    var scheduler = mock(TaskScheduler.class);
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var triggerTask = mock(Runnable.class);
    var cronTask = mock(Runnable.class);
    var disabledTask = mock(Runnable.class);
    var rateTask = mock(Runnable.class);
    var delayTask = mock(Runnable.class);
    var oneTimeTask = mock(Runnable.class);
    Trigger trigger = context -> now;
    var interval = Duration.ofSeconds(2);
    var initialDelay = Duration.ofSeconds(3);
    ScheduledFuture<?> oneTimeFuture = mock(ScheduledFuture.class);

    assertThat(taskRegistrar.hasTasks()).isFalse();
    taskRegistrar.setTriggerTasks(Map.of(triggerTask, trigger));
    taskRegistrar.setCronTasks(Map.of(cronTask, "0 * * * * *", disabledTask, ScheduledTaskRegistrar.CRON_DISABLED));
    taskRegistrar.setFixedRateTasks(Map.of(rateTask, interval.toMillis()));
    taskRegistrar.setFixedDelayTasks(Map.of(delayTask, interval.toMillis()));
    taskRegistrar.addOneTimeTask(new DelayedTask(oneTimeTask, initialDelay));
    taskRegistrar.setScheduler(scheduler);
    given(scheduler.getClock()).willReturn(Clock.fixed(now, ZoneOffset.UTC));
    willReturn(oneTimeFuture).given(scheduler).schedule(any(Runnable.class), eq(now.plus(initialDelay)));

    taskRegistrar.afterPropertiesSet();

    assertThat(taskRegistrar.hasTasks()).isTrue();
    assertThat(taskRegistrar.getScheduledTasks()).hasSize(5);
    assertThat(taskRegistrar.getCronTaskList()).extracting(CronTask::getExpression).containsExactly("0 * * * * *");
    var callback = ArgumentCaptor.forClass(Runnable.class);
    then(scheduler).should().schedule(callback.capture(), eq(trigger));
    then(scheduler).should().schedule(callback.capture(), eq(taskRegistrar.getCronTaskList().get(0).getTrigger()));
    then(scheduler).should().scheduleAtFixedRate(callback.capture(), eq(interval));
    then(scheduler).should().scheduleWithFixedDelay(callback.capture(), eq(now), eq(interval));
    then(scheduler).should().schedule(callback.capture(), eq(now.plus(initialDelay)));
    callback.getAllValues().forEach(Runnable::run);
    then(triggerTask).should().run();
    then(cronTask).should().run();
    then(disabledTask).shouldHaveNoInteractions();
    then(rateTask).should().run();
    then(delayTask).should().run();
    then(oneTimeTask).should().run();

    taskRegistrar.destroy();
    then(oneTimeFuture).should().cancel(false);
  }

  @Test
  void bindsDeferredHandlesToTheFuturesCreatedAtInitialization() {
    var scheduler = mock(TaskScheduler.class);
    var now = Instant.parse("2026-01-01T00:00:00Z");
    var initialDelay = Duration.ofSeconds(3);
    var interval = Duration.ofSeconds(2);
    var triggerTask = new TriggerTask(mock(Runnable.class), context -> now);
    var cronTask = new CronTask(mock(Runnable.class), "0 * * * * *");
    var rateTask = new FixedRateTask(mock(Runnable.class), interval, initialDelay);
    var delayTask = new FixedDelayTask(mock(Runnable.class), interval, Duration.ofMillis(-1));
    var oneTimeTask = new OneTimeTask(mock(Runnable.class), initialDelay);
    var triggerHandle = taskRegistrar.scheduleTriggerTask(triggerTask);
    var cronHandle = taskRegistrar.scheduleCronTask(cronTask);
    var rateHandle = taskRegistrar.scheduleFixedRateTask(rateTask);
    var delayHandle = taskRegistrar.scheduleFixedDelayTask(delayTask);
    var oneTimeHandle = taskRegistrar.scheduleOneTimeTask(oneTimeTask);
    assertThat(taskRegistrar.getScheduledTasks()).isEmpty();

    ScheduledFuture<?> triggerFuture = mock(ScheduledFuture.class);
    ScheduledFuture<?> cronFuture = mock(ScheduledFuture.class);
    ScheduledFuture<?> rateFuture = mock(ScheduledFuture.class);
    ScheduledFuture<?> delayFuture = mock(ScheduledFuture.class);
    ScheduledFuture<?> oneTimeFuture = mock(ScheduledFuture.class);
    taskRegistrar.setTaskScheduler(scheduler);
    given(scheduler.getClock()).willReturn(Clock.fixed(now, ZoneOffset.UTC));
    willReturn(triggerFuture).given(scheduler).schedule(triggerTask.getRunnable(), triggerTask.getTrigger());
    willReturn(cronFuture).given(scheduler).schedule(cronTask.getRunnable(), cronTask.getTrigger());
    willReturn(rateFuture).given(scheduler).scheduleAtFixedRate(rateTask.getRunnable(), now.plus(initialDelay), interval);
    willReturn(delayFuture).given(scheduler).scheduleWithFixedDelay(delayTask.getRunnable(), interval);
    willReturn(oneTimeFuture).given(scheduler).schedule(oneTimeTask.getRunnable(), now.plus(initialDelay));

    taskRegistrar.afterPropertiesSet();
    triggerHandle.cancel();
    cronHandle.cancel();
    rateHandle.cancel();
    delayHandle.cancel();
    oneTimeHandle.cancel();

    then(triggerFuture).should().cancel(true);
    then(cronFuture).should().cancel(true);
    then(rateFuture).should().cancel(true);
    then(delayFuture).should().cancel(true);
    then(oneTimeFuture).should().cancel(true);
  }

  @Test
  void ownsOnlyItsFallbackExecutor() {
    var external = mock(ScheduledExecutorService.class);
    taskRegistrar.setScheduler(external);
    assertThat(taskRegistrar.getScheduler()).isNotNull();
    taskRegistrar.destroy();
    then(external).shouldHaveNoInteractions();

    assertThatIllegalArgumentException().isThrownBy(() -> taskRegistrar.setScheduler(new Object()))
            .withMessageContaining("Unsupported scheduler type");
    taskRegistrar.setScheduler(null);
    assertThat(taskRegistrar.getScheduler()).isNull();

    taskRegistrar.afterPropertiesSet();
    var scheduler = taskRegistrar.getScheduler();
    taskRegistrar.destroy();
    assertThatThrownBy(() -> scheduler.schedule(no_op, Instant.now())).isInstanceOf(TaskRejectedException.class);
  }

}
