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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.scheduling.concurrent;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import cn.taketoday.scheduling.Trigger;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/12/11 23:07
 */
class DefaultManagedTaskSchedulerTests {

  private final Runnable NO_OP = () -> { };

  @Test
  void scheduleWithTriggerAndNoScheduledExecutorProvidesDedicatedException() {
    DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    assertNoExecutorException(() -> scheduler.schedule(NO_OP, mock(Trigger.class)));
  }

  @Test
  void scheduleWithInstantAndNoScheduledExecutorProvidesDedicatedException() {
    DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    assertNoExecutorException(() -> scheduler.schedule(NO_OP, Instant.now()));
  }

  @Test
  void scheduleAtFixedRateWithStartTimeAndDurationAndNoScheduledExecutorProvidesDedicatedException() {
    DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    assertNoExecutorException(() -> scheduler.scheduleAtFixedRate(
            NO_OP, Instant.now(), Duration.of(1, ChronoUnit.MINUTES)));
  }

  @Test
  void scheduleAtFixedRateWithDurationAndNoScheduledExecutorProvidesDedicatedException() {
    DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    assertNoExecutorException(() -> scheduler.scheduleAtFixedRate(
            NO_OP, Duration.of(1, ChronoUnit.MINUTES)));
  }

  @Test
  void scheduleWithFixedDelayWithStartTimeAndDurationAndNoScheduledExecutorProvidesDedicatedException() {
    DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    assertNoExecutorException(() -> scheduler.scheduleWithFixedDelay(
            NO_OP, Instant.now(), Duration.of(1, ChronoUnit.MINUTES)));
  }

  @Test
  void scheduleWithFixedDelayWithDurationAndNoScheduledExecutorProvidesDedicatedException() {
    DefaultManagedTaskScheduler scheduler = new DefaultManagedTaskScheduler();
    assertNoExecutorException(() -> scheduler.scheduleWithFixedDelay(
            NO_OP, Duration.of(1, ChronoUnit.MINUTES)));
  }

  private void assertNoExecutorException(ThrowableAssert.ThrowingCallable callable) {
    assertThatThrownBy(callable)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("No ScheduledExecutor is configured");
  }

}