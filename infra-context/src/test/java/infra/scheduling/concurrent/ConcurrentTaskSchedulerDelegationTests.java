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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import infra.core.task.TaskRejectedException;
import infra.scheduling.TaskScheduler;
import infra.scheduling.Trigger;
import jakarta.enterprise.concurrent.LastExecution;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

class ConcurrentTaskSchedulerDelegationTests {

  private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

  private static final Instant START = NOW.plusSeconds(2);

  private static final Duration INTERVAL = Duration.ofMillis(500);

  @Test
  void calculatesDelaysFromTheConfiguredClockAndDelegatesEachSchedulingMode() {
    var executor = mock(ScheduledExecutorService.class);
    var scheduler = new ConcurrentTaskScheduler(Runnable::run, executor);
    var clock = Clock.fixed(NOW, ZoneOffset.UTC);
    scheduler.setClock(clock);
    var task = mock(Runnable.class);
    var decorated = ArgumentCaptor.forClass(Runnable.class);

    assertThat(scheduler.getClock()).isSameAs(clock);
    scheduler.schedule(task, START);
    scheduler.scheduleAtFixedRate(task, START, INTERVAL);
    scheduler.scheduleAtFixedRate(task, INTERVAL);
    scheduler.scheduleWithFixedDelay(task, START, INTERVAL);
    scheduler.scheduleWithFixedDelay(task, INTERVAL);

    then(executor).should().schedule(decorated.capture(), eq(2_000_000_000L), eq(TimeUnit.NANOSECONDS));
    then(executor).should().scheduleAtFixedRate(any(Runnable.class), eq(2_000_000_000L), eq(500_000_000L), eq(TimeUnit.NANOSECONDS));
    then(executor).should().scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(500_000_000L), eq(TimeUnit.NANOSECONDS));
    then(executor).should().scheduleWithFixedDelay(any(Runnable.class), eq(2_000_000_000L), eq(500_000_000L), eq(TimeUnit.NANOSECONDS));
    then(executor).should().scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(500_000_000L), eq(TimeUnit.NANOSECONDS));
    then(task).shouldHaveNoInteractions();
    decorated.getValue().run();
    then(task).should().run();
  }

  @Test
  void usesAnErrorHandlerForLocalTriggerTasksAndDoesNotSubmitWhenTriggerHasNoNextExecution() {
    var executor = mock(ScheduledExecutorService.class);
    var scheduler = new ConcurrentTaskScheduler(executor);
    var failure = new IllegalStateException("task failed");
    var errors = new ArrayList<Throwable>();
    scheduler.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
    scheduler.setErrorHandler(errors::add);

    assertThatObject(scheduler.schedule(() -> { }, context -> null)).isNull();
    then(executor).shouldHaveNoInteractions();

    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    willReturn(future).given(executor).schedule(any(Runnable.class), eq(2_000_000_000L), eq(TimeUnit.NANOSECONDS));
    assertThatObject(scheduler.schedule(() -> { throw failure; }, context -> context.lastCompletion() == null ? START : null)).isNotNull();
    var callback = ArgumentCaptor.forClass(Runnable.class);
    then(executor).should().schedule(callback.capture(), eq(2_000_000_000L), eq(TimeUnit.NANOSECONDS));
    callback.getValue().run();
    assertThat(errors).containsExactly(failure);
  }

  @Test
  void adaptsManagedExecutorTriggerHistoryAndPreservesTaskIdentity() {
    var executor = mock(ManagedScheduledExecutorService.class);
    var scheduler = new ConcurrentTaskScheduler((ScheduledExecutorService) null);
    scheduler.setScheduledExecutor(executor);
    ScheduledFuture<?> future = mock(ScheduledFuture.class);
    willReturn(future).given(executor).schedule(any(Runnable.class), any(jakarta.enterprise.concurrent.Trigger.class));
    var task = mock(Runnable.class);
    var history = mock(LastExecution.class);
    given(history.getScheduledStart()).willReturn(Date.from(NOW));
    given(history.getRunStart()).willReturn(Date.from(NOW.plusMillis(50)));
    Trigger trigger = context -> {
      assertThat(context.lastCompletion()).isNull();
      if (context.lastScheduledExecution() == null) {
        assertThat(context.lastActualExecution()).isNull();
        return START;
      }
      assertThat(context.lastScheduledExecution()).isEqualTo(NOW);
      assertThat(context.lastActualExecution()).isEqualTo(NOW.plusMillis(50));
      return null;
    };

    assertThatObject(scheduler.schedule(task, trigger)).isSameAs(future);
    var adaptedTask = ArgumentCaptor.forClass(Runnable.class);
    var adaptedTrigger = ArgumentCaptor.forClass(jakarta.enterprise.concurrent.Trigger.class);
    then(executor).should().schedule(adaptedTask.capture(), adaptedTrigger.capture());
    assertThat(adaptedTask.getValue()).isInstanceOfSatisfying(ManagedTask.class,
            managed -> assertThat(managed.getExecutionProperties()).containsEntry(ManagedTask.IDENTITY_NAME, task.toString()));
    assertThat(adaptedTrigger.getValue().getNextRunTime(null, Date.from(NOW))).isEqualTo(Date.from(START));
    assertThat(adaptedTrigger.getValue().getNextRunTime(history, Date.from(NOW))).isNull();
    assertThat(adaptedTrigger.getValue().skipRun(history, Date.from(NOW))).isFalse();
  }

  @Test
  void rejectsSchedulingWhenNoExecutorIsConfigured() {
    var scheduler = new ConcurrentTaskScheduler((ScheduledExecutorService) null);
    assertThatIllegalStateException().isThrownBy(() -> scheduler.schedule(() -> { }, START))
            .withMessageContaining("No ScheduledExecutor");
  }

  @ParameterizedTest
  @MethodSource("schedulingModes")
  void translatesExecutorRejections(SchedulingMode mode) {
    var executor = new ScheduledThreadPoolExecutor(1);
    executor.shutdownNow();
    var scheduler = new ConcurrentTaskScheduler(executor);
    scheduler.setClock(Clock.fixed(NOW, ZoneOffset.UTC));

    assertThatThrownBy(() -> mode.schedule(scheduler, () -> { }))
            .isInstanceOf(TaskRejectedException.class).hasCauseInstanceOf(RejectedExecutionException.class);
  }

  private static Stream<SchedulingMode> schedulingModes() {
    return Stream.of(
            (scheduler, task) -> scheduler.schedule(task, START),
            (scheduler, task) -> scheduler.schedule(task, context -> START),
            (scheduler, task) -> scheduler.scheduleAtFixedRate(task, START, INTERVAL),
            (scheduler, task) -> scheduler.scheduleAtFixedRate(task, INTERVAL),
            (scheduler, task) -> scheduler.scheduleWithFixedDelay(task, START, INTERVAL),
            (scheduler, task) -> scheduler.scheduleWithFixedDelay(task, INTERVAL));
  }

  @FunctionalInterface
  private interface SchedulingMode {

    void schedule(TaskScheduler scheduler, Runnable task);
  }

}
