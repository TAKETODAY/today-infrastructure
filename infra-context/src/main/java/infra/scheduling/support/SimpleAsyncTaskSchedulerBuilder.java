/*
 * Copyright 2012-present the original author or authors.
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

package infra.scheduling.support;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.core.task.TaskDecorator;
import infra.lang.Assert;
import infra.scheduling.concurrent.SimpleAsyncTaskScheduler;
import infra.util.CollectionUtils;

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

  @Nullable
  private final Duration taskTerminationTimeout;

  @Nullable
  private final TaskDecorator taskDecorator;

  public SimpleAsyncTaskSchedulerBuilder() {
    this(null, null, null,
            null, null, null);
  }

  private SimpleAsyncTaskSchedulerBuilder(@Nullable String threadNamePrefix,
          @Nullable Integer concurrencyLimit, @Nullable Boolean virtualThreads,
          @Nullable Set<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers,
          @Nullable Duration taskTerminationTimeout, @Nullable TaskDecorator taskDecorator) {
    this.threadNamePrefix = threadNamePrefix;
    this.concurrencyLimit = concurrencyLimit;
    this.virtualThreads = virtualThreads;
    this.customizers = taskSchedulerCustomizers;
    this.taskTerminationTimeout = taskTerminationTimeout;
    this.taskDecorator = taskDecorator;
  }

  /**
   * Set the prefix to use for the names of newly created threads.
   *
   * @param threadNamePrefix the thread name prefix to set
   * @return a new builder instance
   */
  public SimpleAsyncTaskSchedulerBuilder threadNamePrefix(String threadNamePrefix) {
    return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, concurrencyLimit, this.virtualThreads,
            customizers, taskTerminationTimeout, taskDecorator);
  }

  /**
   * Set the concurrency limit.
   *
   * @param concurrencyLimit the concurrency limit
   * @return a new builder instance
   */
  public SimpleAsyncTaskSchedulerBuilder concurrencyLimit(@Nullable Integer concurrencyLimit) {
    return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, concurrencyLimit, virtualThreads,
            customizers, taskTerminationTimeout, taskDecorator);
  }

  /**
   * Set whether to use virtual threads.
   *
   * @param virtualThreads whether to use virtual threads
   * @return a new builder instance
   */
  public SimpleAsyncTaskSchedulerBuilder virtualThreads(Boolean virtualThreads) {
    return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, concurrencyLimit, virtualThreads,
            customizers, taskTerminationTimeout, taskDecorator);
  }

  /**
   * Set the task termination timeout.
   *
   * @param taskTerminationTimeout the task termination timeout
   * @return a new builder instance
   */
  public SimpleAsyncTaskSchedulerBuilder taskTerminationTimeout(@Nullable Duration taskTerminationTimeout) {
    return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, concurrencyLimit, virtualThreads,
            customizers, taskTerminationTimeout, taskDecorator);
  }

  /**
   * Set the task decorator to be used by the {@link SimpleAsyncTaskScheduler}.
   *
   * @param taskDecorator the task decorator to set
   * @return a new builder instance
   * @since 5.0
   */
  public SimpleAsyncTaskSchedulerBuilder taskDecorator(@Nullable TaskDecorator taskDecorator) {
    return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, concurrencyLimit, virtualThreads,
            customizers, taskTerminationTimeout, taskDecorator);
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
    Assert.notNull(customizers, "Customizers is required");
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
  public SimpleAsyncTaskSchedulerBuilder customizers(Iterable<? extends SimpleAsyncTaskSchedulerCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, concurrencyLimit, virtualThreads,
            append(null, customizers), taskTerminationTimeout, taskDecorator);
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
    Assert.notNull(customizers, "Customizers is required");
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
  public SimpleAsyncTaskSchedulerBuilder additionalCustomizers(Iterable<? extends SimpleAsyncTaskSchedulerCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return new SimpleAsyncTaskSchedulerBuilder(threadNamePrefix, concurrencyLimit, virtualThreads,
            append(this.customizers, customizers), taskTerminationTimeout, taskDecorator);
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
    if (threadNamePrefix != null) {
      taskScheduler.setThreadNamePrefix(threadNamePrefix);
    }
    if (concurrencyLimit != null) {
      taskScheduler.setConcurrencyLimit(concurrencyLimit);
    }

    if (virtualThreads != null) {
      taskScheduler.setVirtualThreads(virtualThreads);
    }
    if (taskTerminationTimeout != null) {
      taskScheduler.setTaskTerminationTimeout(taskTerminationTimeout.toMillis());
    }

    if (CollectionUtils.isNotEmpty(this.customizers)) {
      for (SimpleAsyncTaskSchedulerCustomizer customizer : customizers) {
        customizer.customize(taskScheduler);
      }
    }

    if (taskDecorator != null) {
      taskScheduler.setTaskDecorator(taskDecorator);
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
