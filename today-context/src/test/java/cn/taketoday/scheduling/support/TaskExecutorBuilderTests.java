/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.scheduling.support;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 21:52
 */
class TaskExecutorBuilderTests {

  private TaskExecutorBuilder builder = new TaskExecutorBuilder();

  @Test
  void poolSettingsShouldApply() {
    ThreadPoolTaskExecutor executor = this.builder.queueCapacity(10).corePoolSize(4).maxPoolSize(8)
            .allowCoreThreadTimeOut(true).keepAlive(Duration.ofMinutes(1)).build();
    assertThat(executor).hasFieldOrPropertyWithValue("queueCapacity", 10);
    assertThat(executor.getCorePoolSize()).isEqualTo(4);
    assertThat(executor.getMaxPoolSize()).isEqualTo(8);
    assertThat(executor).hasFieldOrPropertyWithValue("allowCoreThreadTimeOut", true);
    assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
  }

  @Test
  void awaitTerminationShouldApply() {
    ThreadPoolTaskExecutor executor = this.builder.awaitTermination(true).build();
    assertThat(executor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
  }

  @Test
  void awaitTerminationPeriodShouldApplyWithMillisecondPrecision() {
    Duration period = Duration.ofMillis(50);
    ThreadPoolTaskExecutor executor = this.builder.awaitTerminationPeriod(period).build();
    assertThat(executor).hasFieldOrPropertyWithValue("awaitTerminationMillis", period.toMillis());
  }

  @Test
  void threadNamePrefixShouldApply() {
    ThreadPoolTaskExecutor executor = this.builder.threadNamePrefix("test-").build();
    assertThat(executor.getThreadNamePrefix()).isEqualTo("test-");
  }

  @Test
  void taskDecoratorShouldApply() {
    TaskDecorator taskDecorator = mock(TaskDecorator.class);
    ThreadPoolTaskExecutor executor = this.builder.taskDecorator(taskDecorator).build();
    assertThat(executor).extracting("taskDecorator").isSameAs(taskDecorator);
  }

  @Test
  void customizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.builder.customizers((TaskExecutorCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.customizers((Set<TaskExecutorCustomizer>) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void customizersShouldApply() {
    TaskExecutorCustomizer customizer = mock(TaskExecutorCustomizer.class);
    ThreadPoolTaskExecutor executor = this.builder.customizers(customizer).build();
    then(customizer).should().customize(executor);
  }

  @Test
  void customizersShouldBeAppliedLast() {
    TaskDecorator taskDecorator = mock(TaskDecorator.class);
    ThreadPoolTaskExecutor executor = spy(new ThreadPoolTaskExecutor());
    this.builder.queueCapacity(10).corePoolSize(4).maxPoolSize(8).allowCoreThreadTimeOut(true)
            .keepAlive(Duration.ofMinutes(1)).awaitTermination(true).awaitTerminationPeriod(Duration.ofSeconds(30))
            .threadNamePrefix("test-").taskDecorator(taskDecorator).additionalCustomizers((taskExecutor) -> {
              then(taskExecutor).should().setQueueCapacity(10);
              then(taskExecutor).should().setCorePoolSize(4);
              then(taskExecutor).should().setMaxPoolSize(8);
              then(taskExecutor).should().setAllowCoreThreadTimeOut(true);
              then(taskExecutor).should().setKeepAliveSeconds(60);
              then(taskExecutor).should().setWaitForTasksToCompleteOnShutdown(true);
              then(taskExecutor).should().setAwaitTerminationSeconds(30);
              then(taskExecutor).should().setThreadNamePrefix("test-");
              then(taskExecutor).should().setTaskDecorator(taskDecorator);
            });
    this.builder.configure(executor);
  }

  @Test
  void customizersShouldReplaceExisting() {
    TaskExecutorCustomizer customizer1 = mock(TaskExecutorCustomizer.class);
    TaskExecutorCustomizer customizer2 = mock(TaskExecutorCustomizer.class);
    ThreadPoolTaskExecutor executor = this.builder.customizers(customizer1)
            .customizers(Collections.singleton(customizer2)).build();
    then(customizer1).shouldHaveNoInteractions();
    then(customizer2).should().customize(executor);
  }

  @Test
  void additionalCustomizersWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((TaskExecutorCustomizer[]) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersCollectionWhenCustomizersAreNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> this.builder.additionalCustomizers((Set<TaskExecutorCustomizer>) null))
            .withMessageContaining("Customizers is required");
  }

  @Test
  void additionalCustomizersShouldAddToExisting() {
    TaskExecutorCustomizer customizer1 = mock(TaskExecutorCustomizer.class);
    TaskExecutorCustomizer customizer2 = mock(TaskExecutorCustomizer.class);
    ThreadPoolTaskExecutor executor = this.builder.customizers(customizer1).additionalCustomizers(customizer2)
            .build();
    then(customizer1).should().customize(executor);
    then(customizer2).should().customize(executor);
  }

}