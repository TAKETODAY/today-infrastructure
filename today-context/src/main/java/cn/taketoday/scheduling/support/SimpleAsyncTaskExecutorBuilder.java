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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.PropertyMapper;

/**
 * Builder that can be used to configure and create a {@link SimpleAsyncTaskExecutor}.
 * Provides convenience methods to set common {@link SimpleAsyncTaskExecutor} settings and
 * register {@link #taskDecorator(TaskDecorator)}). For advanced configuration, consider
 * using {@link SimpleAsyncTaskExecutorCustomizer}.
 * <p>
 * In a typical auto-configured Infra application this builder is available as a
 * bean and can be injected whenever a {@link SimpleAsyncTaskExecutor} is needed.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleAsyncTaskExecutorBuilder {

  @Nullable
  private final Boolean virtualThreads;

  @Nullable
  private final String threadNamePrefix;

  @Nullable
  private final Integer concurrencyLimit;

  @Nullable
  private final TaskDecorator taskDecorator;

  @Nullable
  private final Set<SimpleAsyncTaskExecutorCustomizer> customizers;

  @Nullable
  private final Duration taskTerminationTimeout;

  public SimpleAsyncTaskExecutorBuilder() {
    this(null, null, null, null, null, null);
  }

  private SimpleAsyncTaskExecutorBuilder(@Nullable Boolean virtualThreads, @Nullable String threadNamePrefix, @Nullable Integer concurrencyLimit,
          @Nullable TaskDecorator taskDecorator, @Nullable Set<SimpleAsyncTaskExecutorCustomizer> customizers, @Nullable Duration taskTerminationTimeout) {
    this.virtualThreads = virtualThreads;
    this.threadNamePrefix = threadNamePrefix;
    this.concurrencyLimit = concurrencyLimit;
    this.taskDecorator = taskDecorator;
    this.customizers = customizers;
    this.taskTerminationTimeout = taskTerminationTimeout;
  }

  /**
   * Set the prefix to use for the names of newly created threads.
   *
   * @param threadNamePrefix the thread name prefix to set
   * @return a new builder instance
   */
  public SimpleAsyncTaskExecutorBuilder threadNamePrefix(String threadNamePrefix) {
    return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, threadNamePrefix, this.concurrencyLimit,
            this.taskDecorator, this.customizers, this.taskTerminationTimeout);
  }

  /**
   * Set whether to use virtual threads.
   *
   * @param virtualThreads whether to use virtual threads
   * @return a new builder instance
   */
  public SimpleAsyncTaskExecutorBuilder virtualThreads(Boolean virtualThreads) {
    return new SimpleAsyncTaskExecutorBuilder(virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
            this.taskDecorator, this.customizers, this.taskTerminationTimeout);
  }

  /**
   * Set the concurrency limit.
   *
   * @param concurrencyLimit the concurrency limit
   * @return a new builder instance
   */
  public SimpleAsyncTaskExecutorBuilder concurrencyLimit(@Nullable Integer concurrencyLimit) {
    return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, concurrencyLimit,
            this.taskDecorator, this.customizers, this.taskTerminationTimeout);
  }

  /**
   * Set the {@link TaskDecorator} to use or {@code null} to not use any.
   *
   * @param taskDecorator the task decorator to use
   * @return a new builder instance
   */
  public SimpleAsyncTaskExecutorBuilder taskDecorator(@Nullable TaskDecorator taskDecorator) {
    return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
            taskDecorator, this.customizers, this.taskTerminationTimeout);
  }

  /**
   * Set the task termination timeout.
   *
   * @param taskTerminationTimeout the task termination timeout
   * @return a new builder instance
   */
  public SimpleAsyncTaskExecutorBuilder taskTerminationTimeout(@Nullable Duration taskTerminationTimeout) {
    return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
            this.taskDecorator, this.customizers, taskTerminationTimeout);
  }

  /**
   * Set the {@link SimpleAsyncTaskExecutorCustomizer customizers} that should be
   * applied to the {@link SimpleAsyncTaskExecutor}. Customizers are applied in the
   * order that they were added after builder configuration has been applied. Setting
   * this value will replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(SimpleAsyncTaskExecutorCustomizer...)
   */
  public SimpleAsyncTaskExecutorBuilder customizers(SimpleAsyncTaskExecutorCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return customizers(Arrays.asList(customizers));
  }

  /**
   * Set the {@link SimpleAsyncTaskExecutorCustomizer customizers} that should be
   * applied to the {@link SimpleAsyncTaskExecutor}. Customizers are applied in the
   * order that they were added after builder configuration has been applied. Setting
   * this value will replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(Iterable)
   */
  public SimpleAsyncTaskExecutorBuilder customizers(Iterable<? extends SimpleAsyncTaskExecutorCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
            this.taskDecorator, append(null, customizers), this.taskTerminationTimeout);
  }

  /**
   * Add {@link SimpleAsyncTaskExecutorCustomizer customizers} that should be applied to
   * the {@link SimpleAsyncTaskExecutor}. Customizers are applied in the order that they
   * were added after builder configuration has been applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(SimpleAsyncTaskExecutorCustomizer...)
   */
  public SimpleAsyncTaskExecutorBuilder additionalCustomizers(SimpleAsyncTaskExecutorCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return additionalCustomizers(Arrays.asList(customizers));
  }

  /**
   * Add {@link SimpleAsyncTaskExecutorCustomizer customizers} that should be applied to
   * the {@link SimpleAsyncTaskExecutor}. Customizers are applied in the order that they
   * were added after builder configuration has been applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(Iterable)
   */
  public SimpleAsyncTaskExecutorBuilder additionalCustomizers(Iterable<? extends SimpleAsyncTaskExecutorCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return new SimpleAsyncTaskExecutorBuilder(this.virtualThreads, this.threadNamePrefix, this.concurrencyLimit,
            this.taskDecorator, append(this.customizers, customizers), this.taskTerminationTimeout);
  }

  /**
   * Build a new {@link SimpleAsyncTaskExecutor} instance and configure it using this
   * builder.
   *
   * @return a configured {@link SimpleAsyncTaskExecutor} instance.
   * @see #build(Class)
   * @see #configure(SimpleAsyncTaskExecutor)
   */
  public SimpleAsyncTaskExecutor build() {
    return configure(new SimpleAsyncTaskExecutor());
  }

  /**
   * Build a new {@link SimpleAsyncTaskExecutor} instance of the specified type and
   * configure it using this builder.
   *
   * @param <T> the type of task executor
   * @param taskExecutorClass the template type to create
   * @return a configured {@link SimpleAsyncTaskExecutor} instance.
   * @see #build()
   * @see #configure(SimpleAsyncTaskExecutor)
   */
  public <T extends SimpleAsyncTaskExecutor> T build(Class<T> taskExecutorClass) {
    return configure(BeanUtils.newInstance(taskExecutorClass));
  }

  /**
   * Configure the provided {@link SimpleAsyncTaskExecutor} instance using this builder.
   *
   * @param <T> the type of task executor
   * @param taskExecutor the {@link SimpleAsyncTaskExecutor} to configure
   * @return the task executor instance
   * @see #build()
   * @see #build(Class)
   */
  public <T extends SimpleAsyncTaskExecutor> T configure(T taskExecutor) {
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(this.virtualThreads).to(taskExecutor::setVirtualThreads);
    map.from(this.threadNamePrefix).whenHasText().to(taskExecutor::setThreadNamePrefix);
    map.from(this.concurrencyLimit).to(taskExecutor::setConcurrencyLimit);
    map.from(this.taskDecorator).to(taskExecutor::setTaskDecorator);
    map.from(this.taskTerminationTimeout).as(Duration::toMillis).to(taskExecutor::setTaskTerminationTimeout);
    if (CollectionUtils.isNotEmpty(customizers)) {
      for (SimpleAsyncTaskExecutorCustomizer customizer : customizers) {
        customizer.customize(taskExecutor);
      }
    }
    return taskExecutor;
  }

  private <T> Set<T> append(@Nullable Set<T> set, Iterable<? extends T> additions) {
    LinkedHashSet<T> result = new LinkedHashSet<>((set != null) ? set : Collections.emptySet());
    for (T addition : additions) {
      result.add(addition);
    }
    return Collections.unmodifiableSet(result);
  }

}
