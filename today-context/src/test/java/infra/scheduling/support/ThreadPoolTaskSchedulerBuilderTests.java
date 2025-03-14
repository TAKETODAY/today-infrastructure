/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.scheduling.support;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import infra.core.task.TaskDecorator;
import infra.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/13 13:40
 */
class ThreadPoolTaskSchedulerBuilderTests {

  private final ThreadPoolTaskSchedulerBuilder builder = new ThreadPoolTaskSchedulerBuilder();

  @Test
  void poolSettingsShouldApply() {
    ThreadPoolTaskScheduler scheduler = this.builder.poolSize(4).build();
    assertThat(scheduler.getPoolSize()).isEqualTo(4);
  }

  @Test
  void awaitTerminationShouldApply() {
    ThreadPoolTaskScheduler executor = this.builder.awaitTermination(true).build();
    assertThat(executor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
  }

  @Test
  void awaitTerminationPeriodShouldApply() {
    Duration period = Duration.ofMinutes(1);
    ThreadPoolTaskScheduler executor = this.builder.awaitTerminationPeriod(period).build();
    assertThat(executor).hasFieldOrPropertyWithValue("awaitTerminationMillis", period.toMillis());
  }

  @Test
  void threadNamePrefixShouldApply() {
    ThreadPoolTaskScheduler scheduler = this.builder.threadNamePrefix("test-").build();
    assertThat(scheduler.getThreadNamePrefix()).isEqualTo("test-");
  }

  @Test
  void customizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((ThreadPoolTaskSchedulerCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((Set<ThreadPoolTaskSchedulerCustomizer>) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersShouldApply() {
    ThreadPoolTaskSchedulerCustomizer customizer = mock(ThreadPoolTaskSchedulerCustomizer.class);
    ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer).build();
    then(customizer).should().customize(scheduler);
  }

  @Test
  void customizersShouldBeAppliedLast() {
    ThreadPoolTaskScheduler scheduler = spy(new ThreadPoolTaskScheduler());
    this.builder.poolSize(4).threadNamePrefix("test-").additionalCustomizers((taskScheduler) -> {
      then(taskScheduler).should().setPoolSize(4);
      then(taskScheduler).should().setThreadNamePrefix("test-");
    });
    this.builder.configure(scheduler);
  }

  @Test
  void customizersShouldReplaceExisting() {
    ThreadPoolTaskSchedulerCustomizer customizer1 = mock(ThreadPoolTaskSchedulerCustomizer.class);
    ThreadPoolTaskSchedulerCustomizer customizer2 = mock(ThreadPoolTaskSchedulerCustomizer.class);
    ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer1)
            .customizers(Collections.singleton(customizer2))
            .build();
    then(customizer1).shouldHaveNoInteractions();
    then(customizer2).should().customize(scheduler);
  }

  @Test
  void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((ThreadPoolTaskSchedulerCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((Set<ThreadPoolTaskSchedulerCustomizer>) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersShouldAddToExisting() {
    ThreadPoolTaskSchedulerCustomizer customizer1 = mock(ThreadPoolTaskSchedulerCustomizer.class);
    ThreadPoolTaskSchedulerCustomizer customizer2 = mock(ThreadPoolTaskSchedulerCustomizer.class);
    ThreadPoolTaskScheduler scheduler = this.builder.customizers(customizer1)
            .additionalCustomizers(customizer2)
            .build();
    then(customizer1).should().customize(scheduler);
    then(customizer2).should().customize(scheduler);
  }

  @Test
  void taskDecoratorShouldApply() {
    TaskDecorator taskDecorator = mock(TaskDecorator.class);
    ThreadPoolTaskScheduler scheduler = this.builder.taskDecorator(taskDecorator).build();
    assertThat(scheduler).extracting("taskDecorator").isSameAs(taskDecorator);
  }

}