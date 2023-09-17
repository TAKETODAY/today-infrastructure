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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.scheduling.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.concurrent.SimpleAsyncTaskScheduler;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.PropertyMapper;

/**
 * Builder that can be used to configure and create a {@link SimpleAsyncTaskScheduler}.
 * Provides convenience methods to set common {@link SimpleAsyncTaskScheduler} settings.
 * For advanced configuration, consider using {@link SimpleAsyncTaskSchedulerCustomizer}.
 * <p>
 * In a typical auto-configured Infra application this builder is available as a
 * bean and can be injected whenever a {@link SimpleAsyncTaskScheduler} is needed.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SimpleAsyncTaskSchedulerBuilder {

  @Nullable
  private final String threadNamePrefix;

  @Nullable
  private final Integer concurrencyLimit;

  @Nullable
  private final Boolean virtualThreads;

  @Nullable
  private final Set<SimpleAsyncTaskSchedulerCustomizer> customizers;

  public SimpleAsyncTaskSchedulerBuilder() {
    this(null, null, null, null);
  }

  private SimpleAsyncTaskSchedulerBuilder(@Nullable String threadNamePrefix, @Nullable Integer concurrencyLimit,
          @Nullable Boolean virtualThreads, @Nullable Set<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers) {
    this.threadNamePrefix = threadNamePrefix;
    this.concurrencyLimit = concurrencyLimit;
    this.virtualThreads = virtualThreads;
    this.customizers = taskSchedulerCustomizers;
  }

  /**
   * Set the prefix to use for the names of newly created threads.
   *
   * @param threadNamePrefix the thread name prefix to set
   * @return a new builder instance
   */
  public SimpleAsyncTaskSchedulerBuilder threadNamePrefix(String threadNamePrefix) {
    return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, this.concurrencyLimit, this.virtualThreads,
            this.customizers);
  }

  /**
   * Set the concurrency limit.
   *
   * @param concurrencyLimit the concurrency limit
   * @return a new builder instance
   */
  public SimpleAsyncTaskSchedulerBuilder concurrencyLimit(Integer concurrencyLimit) {
    return new SimpleAsyncTaskSchedulerBuilder(this.threadNamePrefix, concurrencyLimit, this.virtualThreads,
            this.customizers);
  }

  /**
   * Set whether to use virtual threads.
   *
   * @param virtualThreads whether to use virtual threads
   * @return a new builder instance
   */
  public SimpleAsyncTaskSchedulerBuilder virtualThreads(Boolean virtualThreads) {
    return new SimpleAsyncTaskSchedulerBuilder(this.threadNamePrefix, this.concurrencyLimit, virtualThreads,
            this.customizers);
  }

  /**
   * Set the {@link SimpleAsyncTaskSchedulerCustomizer customizers} that should be
   * applied to the {@link SimpleAsyncTaskScheduler}. Customizers are applied in the
   * order that they were added after builder configuration has been applied. Setting
   * this value will replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(SimpleAsyncTaskSchedulerCustomizer...)
   */
  public SimpleAsyncTaskSchedulerBuilder customizers(SimpleAsyncTaskSchedulerCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    return customizers(Arrays.asList(customizers));
  }

  /**
   * Set the {@link SimpleAsyncTaskSchedulerCustomizer customizers} that should be
   * applied to the {@link SimpleAsyncTaskScheduler}. Customizers are applied in the
   * order that they were added after builder configuration has been applied. Setting
   * this value will replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(Iterable)
   */
  public SimpleAsyncTaskSchedulerBuilder customizers(
          Iterable<? extends SimpleAsyncTaskSchedulerCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    return new SimpleAsyncTaskSchedulerBuilder(this.threadNamePrefix, this.concurrencyLimit, this.virtualThreads,
            append(null, customizers));
  }

  /**
   * Add {@link SimpleAsyncTaskSchedulerCustomizer customizers} that should be applied
   * to the {@link SimpleAsyncTaskScheduler}. Customizers are applied in the order that
   * they were added after builder configuration has been applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(SimpleAsyncTaskSchedulerCustomizer...)
   */
  public SimpleAsyncTaskSchedulerBuilder additionalCustomizers(SimpleAsyncTaskSchedulerCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    return additionalCustomizers(Arrays.asList(customizers));
  }

  /**
   * Add {@link SimpleAsyncTaskSchedulerCustomizer customizers} that should be applied
   * to the {@link SimpleAsyncTaskScheduler}. Customizers are applied in the order that
   * they were added after builder configuration has been applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(Iterable)
   */
  public SimpleAsyncTaskSchedulerBuilder additionalCustomizers(
          Iterable<? extends SimpleAsyncTaskSchedulerCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    return new SimpleAsyncTaskSchedulerBuilder(this.threadNamePrefix, this.concurrencyLimit, this.virtualThreads,
            append(this.customizers, customizers));
  }

  /**
   * Build a new {@link SimpleAsyncTaskScheduler} instance and configure it using this
   * builder.
   *
   * @return a configured {@link SimpleAsyncTaskScheduler} instance.
   * @see #configure(SimpleAsyncTaskScheduler)
   */
  public SimpleAsyncTaskScheduler build() {
    return configure(new SimpleAsyncTaskScheduler());
  }

  /**
   * Configure the provided {@link SimpleAsyncTaskScheduler} instance using this
   * builder.
   *
   * @param <T> the type of task scheduler
   * @param taskScheduler the {@link SimpleAsyncTaskScheduler} to configure
   * @return the task scheduler instance
   * @see #build()
   */
  public <T extends SimpleAsyncTaskScheduler> T configure(T taskScheduler) {
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(this.threadNamePrefix).to(taskScheduler::setThreadNamePrefix);
    map.from(this.concurrencyLimit).to(taskScheduler::setConcurrencyLimit);
    map.from(this.virtualThreads).to(taskScheduler::setVirtualThreads);
    if (CollectionUtils.isNotEmpty(this.customizers)) {
      for (SimpleAsyncTaskSchedulerCustomizer customizer : customizers) {
        customizer.customize(taskScheduler);
      }
    }
    return taskScheduler;
  }

  private <T> Set<T> append(@Nullable Set<T> set, Iterable<? extends T> additions) {
    LinkedHashSet<T> result = new LinkedHashSet<>((set != null) ? set : Collections.emptySet());
    for (T addition : additions) {
      result.add(addition);
    }
    return Collections.unmodifiableSet(result);
  }

}
