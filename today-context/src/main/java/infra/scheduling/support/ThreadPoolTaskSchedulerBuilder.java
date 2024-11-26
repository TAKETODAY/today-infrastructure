/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.scheduling.concurrent.ThreadPoolTaskScheduler;
import infra.util.CollectionUtils;

/**
 * Builder that can be used to configure and create a {@link ThreadPoolTaskScheduler}.
 * Provides convenience methods to set common {@link ThreadPoolTaskScheduler} settings.
 * For advanced configuration, consider using {@link ThreadPoolTaskSchedulerCustomizer}.
 * <p>
 * In a typical auto-configured Infra application this builder is available as a
 * bean and can be injected whenever a {@link ThreadPoolTaskScheduler} is needed.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ThreadPoolTaskSchedulerBuilder {

  @Nullable
  private final Integer poolSize;

  @Nullable
  private final Boolean awaitTermination;

  @Nullable
  private final Duration awaitTerminationPeriod;

  @Nullable
  private final String threadNamePrefix;

  @Nullable
  private final Set<ThreadPoolTaskSchedulerCustomizer> customizers;

  public ThreadPoolTaskSchedulerBuilder() {
    this.poolSize = null;
    this.awaitTermination = null;
    this.awaitTerminationPeriod = null;
    this.threadNamePrefix = null;
    this.customizers = null;
  }

  public ThreadPoolTaskSchedulerBuilder(@Nullable Integer poolSize, @Nullable Boolean awaitTermination,
          @Nullable Duration awaitTerminationPeriod, @Nullable String threadNamePrefix,
          @Nullable Set<ThreadPoolTaskSchedulerCustomizer> taskSchedulerCustomizers) {
    this.poolSize = poolSize;
    this.awaitTermination = awaitTermination;
    this.awaitTerminationPeriod = awaitTerminationPeriod;
    this.threadNamePrefix = threadNamePrefix;
    this.customizers = taskSchedulerCustomizers;
  }

  /**
   * Set the maximum allowed number of threads.
   *
   * @param poolSize the pool size to set
   * @return a new builder instance
   */
  public ThreadPoolTaskSchedulerBuilder poolSize(int poolSize) {
    return new ThreadPoolTaskSchedulerBuilder(poolSize, this.awaitTermination, this.awaitTerminationPeriod,
            this.threadNamePrefix, this.customizers);
  }

  /**
   * Set whether the executor should wait for scheduled tasks to complete on shutdown,
   * not interrupting running tasks and executing all tasks in the queue.
   *
   * @param awaitTermination whether the executor needs to wait for the tasks to
   * complete on shutdown
   * @return a new builder instance
   * @see #awaitTerminationPeriod(Duration)
   */
  public ThreadPoolTaskSchedulerBuilder awaitTermination(boolean awaitTermination) {
    return new ThreadPoolTaskSchedulerBuilder(this.poolSize, awaitTermination, this.awaitTerminationPeriod,
            this.threadNamePrefix, this.customizers);
  }

  /**
   * Set the maximum time the executor is supposed to block on shutdown. When set, the
   * executor blocks on shutdown in order to wait for remaining tasks to complete their
   * execution before the rest of the container continues to shut down. This is
   * particularly useful if your remaining tasks are likely to need access to other
   * resources that are also managed by the container.
   *
   * @param awaitTerminationPeriod the await termination period to set
   * @return a new builder instance
   */
  public ThreadPoolTaskSchedulerBuilder awaitTerminationPeriod(@Nullable Duration awaitTerminationPeriod) {
    return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, awaitTerminationPeriod,
            this.threadNamePrefix, this.customizers);
  }

  /**
   * Set the prefix to use for the names of newly created threads.
   *
   * @param threadNamePrefix the thread name prefix to set
   * @return a new builder instance
   */
  public ThreadPoolTaskSchedulerBuilder threadNamePrefix(@Nullable String threadNamePrefix) {
    return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, this.awaitTerminationPeriod,
            threadNamePrefix, this.customizers);
  }

  /**
   * Set the {@link ThreadPoolTaskSchedulerCustomizer
   * threadPoolTaskSchedulerCustomizers} that should be applied to the
   * {@link ThreadPoolTaskScheduler}. Customizers are applied in the order that they
   * were added after builder configuration has been applied. Setting this value will
   * replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(ThreadPoolTaskSchedulerCustomizer...)
   */
  public ThreadPoolTaskSchedulerBuilder customizers(ThreadPoolTaskSchedulerCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return customizers(Arrays.asList(customizers));
  }

  /**
   * Set the {@link ThreadPoolTaskSchedulerCustomizer
   * threadPoolTaskSchedulerCustomizers} that should be applied to the
   * {@link ThreadPoolTaskScheduler}. Customizers are applied in the order that they
   * were added after builder configuration has been applied. Setting this value will
   * replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(ThreadPoolTaskSchedulerCustomizer...)
   */
  public ThreadPoolTaskSchedulerBuilder customizers(Iterable<? extends ThreadPoolTaskSchedulerCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, this.awaitTerminationPeriod,
            this.threadNamePrefix, append(null, customizers));
  }

  /**
   * Add {@link ThreadPoolTaskSchedulerCustomizer threadPoolTaskSchedulerCustomizers}
   * that should be applied to the {@link ThreadPoolTaskScheduler}. Customizers are
   * applied in the order that they were added after builder configuration has been
   * applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(ThreadPoolTaskSchedulerCustomizer...)
   */
  public ThreadPoolTaskSchedulerBuilder additionalCustomizers(ThreadPoolTaskSchedulerCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return additionalCustomizers(Arrays.asList(customizers));
  }

  /**
   * Add {@link ThreadPoolTaskSchedulerCustomizer threadPoolTaskSchedulerCustomizers}
   * that should be applied to the {@link ThreadPoolTaskScheduler}. Customizers are
   * applied in the order that they were added after builder configuration has been
   * applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(ThreadPoolTaskSchedulerCustomizer...)
   */
  public ThreadPoolTaskSchedulerBuilder additionalCustomizers(Iterable<? extends ThreadPoolTaskSchedulerCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return new ThreadPoolTaskSchedulerBuilder(this.poolSize, this.awaitTermination, this.awaitTerminationPeriod,
            this.threadNamePrefix, append(this.customizers, customizers));
  }

  /**
   * Build a new {@link ThreadPoolTaskScheduler} instance and configure it using this
   * builder.
   *
   * @return a configured {@link ThreadPoolTaskScheduler} instance.
   * @see #configure(ThreadPoolTaskScheduler)
   */
  public ThreadPoolTaskScheduler build() {
    return configure(new ThreadPoolTaskScheduler());
  }

  /**
   * Configure the provided {@link ThreadPoolTaskScheduler} instance using this builder.
   *
   * @param <T> the type of task scheduler
   * @param taskScheduler the {@link ThreadPoolTaskScheduler} to configure
   * @return the task scheduler instance
   * @see #build()
   */
  public <T extends ThreadPoolTaskScheduler> T configure(T taskScheduler) {
    if (poolSize != null) {
      taskScheduler.setPoolSize(poolSize);
    }

    if (awaitTermination != null) {
      taskScheduler.setWaitForTasksToCompleteOnShutdown(awaitTermination);
    }

    if (awaitTerminationPeriod != null) {
      taskScheduler.setAwaitTerminationSeconds(Math.toIntExact(awaitTerminationPeriod.getSeconds()));
    }

    if (threadNamePrefix != null) {
      taskScheduler.setThreadNamePrefix(threadNamePrefix);
    }

    if (CollectionUtils.isNotEmpty(customizers)) {
      for (ThreadPoolTaskSchedulerCustomizer customizer : customizers) {
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
