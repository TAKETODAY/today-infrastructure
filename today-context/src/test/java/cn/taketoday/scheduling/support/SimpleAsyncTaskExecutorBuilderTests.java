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

import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.test.assertj.SimpleAsyncTaskExecutorAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/13 13:23
 */
class SimpleAsyncTaskExecutorBuilderTests {

  private final SimpleAsyncTaskExecutorBuilder builder = new SimpleAsyncTaskExecutorBuilder();

  @Test
  void threadNamePrefixShouldApply() {
    SimpleAsyncTaskExecutor executor = this.builder.threadNamePrefix("test-").build();
    assertThat(executor.getThreadNamePrefix()).isEqualTo("test-");
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void virtualThreadsShouldApply() {
    SimpleAsyncTaskExecutor executor = this.builder.virtualThreads(true).build();
    SimpleAsyncTaskExecutorAssert.assertThat(executor).usesVirtualThreads();
  }

  @Test
  void concurrencyLimitShouldApply() {
    SimpleAsyncTaskExecutor executor = this.builder.concurrencyLimit(1).build();
    assertThat(executor.getConcurrencyLimit()).isEqualTo(1);
  }

  @Test
  void taskDecoratorShouldApply() {
    TaskDecorator taskDecorator = mock(TaskDecorator.class);
    SimpleAsyncTaskExecutor executor = this.builder.taskDecorator(taskDecorator).build();
    assertThat(executor).extracting("taskDecorator").isSameAs(taskDecorator);
  }

  @Test
  void customizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((SimpleAsyncTaskExecutorCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((Set<SimpleAsyncTaskExecutorCustomizer>) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersShouldApply() {
    SimpleAsyncTaskExecutorCustomizer customizer = mock(SimpleAsyncTaskExecutorCustomizer.class);
    SimpleAsyncTaskExecutor executor = this.builder.customizers(customizer).build();
    then(customizer).should().customize(executor);
  }

  @Test
  void customizersShouldBeAppliedLast() {
    TaskDecorator taskDecorator = mock(TaskDecorator.class);
    SimpleAsyncTaskExecutor executor = spy(new SimpleAsyncTaskExecutor());
    this.builder.threadNamePrefix("test-")
            .virtualThreads(true)
            .concurrencyLimit(1)
            .taskDecorator(taskDecorator)
            .additionalCustomizers((taskExecutor) -> {
              then(taskExecutor).should().setConcurrencyLimit(1);
              then(taskExecutor).should().setVirtualThreads(true);
              then(taskExecutor).should().setThreadNamePrefix("test-");
              then(taskExecutor).should().setTaskDecorator(taskDecorator);
            });
    this.builder.configure(executor);
  }

  @Test
  void customizersShouldReplaceExisting() {
    SimpleAsyncTaskExecutorCustomizer customizer1 = mock(SimpleAsyncTaskExecutorCustomizer.class);
    SimpleAsyncTaskExecutorCustomizer customizer2 = mock(SimpleAsyncTaskExecutorCustomizer.class);
    SimpleAsyncTaskExecutor executor = this.builder.customizers(customizer1)
            .customizers(Collections.singleton(customizer2))
            .build();
    then(customizer1).shouldHaveNoInteractions();
    then(customizer2).should().customize(executor);
  }

  @Test
  void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((SimpleAsyncTaskExecutorCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((Set<SimpleAsyncTaskExecutorCustomizer>) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersShouldAddToExisting() {
    SimpleAsyncTaskExecutorCustomizer customizer1 = mock(SimpleAsyncTaskExecutorCustomizer.class);
    SimpleAsyncTaskExecutorCustomizer customizer2 = mock(SimpleAsyncTaskExecutorCustomizer.class);
    SimpleAsyncTaskExecutor executor = this.builder.customizers(customizer1)
            .additionalCustomizers(customizer2)
            .build();
    then(customizer1).should().customize(executor);
    then(customizer2).should().customize(executor);
  }

  @Test
  void taskTerminationTimeoutShouldApply() {
    SimpleAsyncTaskExecutor executor = this.builder.taskTerminationTimeout(Duration.ofSeconds(1)).build();
    assertThat(executor).extracting("taskTerminationTimeout").isEqualTo(1000L);
  }

}