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

package cn.taketoday.scheduling.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.scheduling.concurrent.SimpleAsyncTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/12/3 11:54
 */
class SimpleAsyncTaskSchedulerBuilderTests {

  private final SimpleAsyncTaskSchedulerBuilder builder = new SimpleAsyncTaskSchedulerBuilder();

  @Test
  void threadNamePrefixShouldApply() {
    SimpleAsyncTaskScheduler scheduler = this.builder.threadNamePrefix("test-").build();
    assertThat(scheduler.getThreadNamePrefix()).isEqualTo("test-");
  }

  @Test
  void concurrencyLimitShouldApply() {
    SimpleAsyncTaskScheduler scheduler = this.builder.concurrencyLimit(1).build();
    assertThat(scheduler.getConcurrencyLimit()).isEqualTo(1);
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void virtualThreadsShouldApply() {
    SimpleAsyncTaskScheduler scheduler = this.builder.virtualThreads(true).build();
    assertThat(scheduler).extracting("virtualThreadDelegate").isNotNull();
  }

  @Test
  void customizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((SimpleAsyncTaskSchedulerCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((Set<SimpleAsyncTaskSchedulerCustomizer>) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersShouldApply() {
    SimpleAsyncTaskSchedulerCustomizer customizer = mock(SimpleAsyncTaskSchedulerCustomizer.class);
    SimpleAsyncTaskScheduler scheduler = this.builder.customizers(customizer).build();
    then(customizer).should().customize(scheduler);
  }

  @Test
  void customizersShouldBeAppliedLast() {
    SimpleAsyncTaskScheduler scheduler = spy(new SimpleAsyncTaskScheduler());
    this.builder.concurrencyLimit(1).threadNamePrefix("test-").additionalCustomizers((taskScheduler) -> {
      then(taskScheduler).should().setConcurrencyLimit(1);
      then(taskScheduler).should().setThreadNamePrefix("test-");
    });
    this.builder.configure(scheduler);
  }

  @Test
  void customizersShouldReplaceExisting() {
    SimpleAsyncTaskSchedulerCustomizer customizer1 = mock(SimpleAsyncTaskSchedulerCustomizer.class);
    SimpleAsyncTaskSchedulerCustomizer customizer2 = mock(SimpleAsyncTaskSchedulerCustomizer.class);
    SimpleAsyncTaskScheduler scheduler = this.builder.customizers(customizer1)
            .customizers(Collections.singleton(customizer2))
            .build();
    then(customizer1).shouldHaveNoInteractions();
    then(customizer2).should().customize(scheduler);
  }

  @Test
  void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((SimpleAsyncTaskSchedulerCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((Set<SimpleAsyncTaskSchedulerCustomizer>) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersShouldAddToExisting() {
    SimpleAsyncTaskSchedulerCustomizer customizer1 = mock(SimpleAsyncTaskSchedulerCustomizer.class);
    SimpleAsyncTaskSchedulerCustomizer customizer2 = mock(SimpleAsyncTaskSchedulerCustomizer.class);
    SimpleAsyncTaskScheduler scheduler = this.builder.customizers(customizer1)
            .additionalCustomizers(customizer2)
            .build();
    then(customizer1).should().customize(scheduler);
    then(customizer2).should().customize(scheduler);
  }

  @Test
  void taskTerminationTimeoutShouldApply() {
    SimpleAsyncTaskScheduler scheduler = this.builder.taskTerminationTimeout(Duration.ofSeconds(1)).build();
    assertThat(scheduler).extracting("taskTerminationTimeout").isEqualTo(1000L);
  }

}