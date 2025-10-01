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

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.beans.BeanUtils;
import infra.core.task.TaskDecorator;
import infra.lang.Assert;
import infra.scheduling.concurrent.ThreadPoolTaskExecutor;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * Builder that can be used to configure and create a {@link ThreadPoolTaskExecutor}.
 * Provides convenience methods to set common {@link ThreadPoolTaskExecutor} settings and
 * register {@link #taskDecorator(TaskDecorator)}). For advanced configuration, consider
 * using {@link ThreadPoolTaskExecutorCustomizer}.
 * <p>
 * In a typical auto-configured Infra application this builder is available as a
 * bean and can be injected whenever a {@link ThreadPoolTaskExecutor} is needed.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ThreadPoolTaskExecutorBuilder {

  @Nullable
  private final Integer queueCapacity;

  @Nullable
  private final Integer corePoolSize;

  @Nullable
  private final Integer maxPoolSize;

  @Nullable
  private final Boolean allowCoreThreadTimeOut;

  @Nullable
  private final Duration keepAlive;

  @Nullable
  private final Boolean acceptTasksAfterContextClose;

  @Nullable
  private final Boolean awaitTermination;

  @Nullable
  private final Duration awaitTerminationPeriod;

  @Nullable
  private final String threadNamePrefix;

  @Nullable
  private final TaskDecorator taskDecorator;

  @Nullable
  private final Set<ThreadPoolTaskExecutorCustomizer> customizers;

  public ThreadPoolTaskExecutorBuilder() {
    this.queueCapacity = null;
    this.corePoolSize = null;
    this.maxPoolSize = null;
    this.allowCoreThreadTimeOut = null;
    this.keepAlive = null;
    this.awaitTermination = null;
    this.awaitTerminationPeriod = null;
    this.threadNamePrefix = null;
    this.taskDecorator = null;
    this.customizers = null;
    this.acceptTasksAfterContextClose = null;
  }

  private ThreadPoolTaskExecutorBuilder(@Nullable Integer queueCapacity, @Nullable Integer corePoolSize, @Nullable Integer maxPoolSize,
          @Nullable Boolean allowCoreThreadTimeOut, @Nullable Duration keepAlive, @Nullable Boolean acceptTasksAfterContextClose,
          @Nullable Boolean awaitTermination, @Nullable Duration awaitTerminationPeriod, @Nullable String threadNamePrefix,
          @Nullable TaskDecorator taskDecorator, @Nullable Set<ThreadPoolTaskExecutorCustomizer> customizers) {
    this.queueCapacity = queueCapacity;
    this.corePoolSize = corePoolSize;
    this.maxPoolSize = maxPoolSize;
    this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    this.keepAlive = keepAlive;
    this.acceptTasksAfterContextClose = acceptTasksAfterContextClose;
    this.awaitTermination = awaitTermination;
    this.awaitTerminationPeriod = awaitTerminationPeriod;
    this.threadNamePrefix = threadNamePrefix;
    this.taskDecorator = taskDecorator;
    this.customizers = customizers;
  }

  /**
   * Set the capacity of the queue. An unbounded capacity does not increase the pool and
   * therefore ignores {@link #maxPoolSize(int) maxPoolSize}.
   *
   * @param queueCapacity the queue capacity to set
   * @return a new builder instance
   */
  public ThreadPoolTaskExecutorBuilder queueCapacity(int queueCapacity) {
    return new ThreadPoolTaskExecutorBuilder(queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination,
            this.awaitTerminationPeriod, this.threadNamePrefix, this.taskDecorator, this.customizers);
  }

  /**
   * Set the core number of threads. Effectively that maximum number of threads as long
   * as the queue is not full.
   * <p>
   * Core threads can grow and shrink if {@link #allowCoreThreadTimeOut(boolean)} is
   * enabled.
   *
   * @param corePoolSize the core pool size to set
   * @return a new builder instance
   */
  public ThreadPoolTaskExecutorBuilder corePoolSize(int corePoolSize) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination,
            this.awaitTerminationPeriod, this.threadNamePrefix, this.taskDecorator, this.customizers);
  }

  /**
   * Set the maximum allowed number of threads. When the {@link #queueCapacity(int)
   * queue} is full, the pool can expand up to that size to accommodate the load.
   * <p>
   * If the {@link #queueCapacity(int) queue capacity} is unbounded, this setting is
   * ignored.
   *
   * @param maxPoolSize the max pool size to set
   * @return a new builder instance
   */
  public ThreadPoolTaskExecutorBuilder maxPoolSize(int maxPoolSize) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination, this.awaitTerminationPeriod,
            this.threadNamePrefix, this.taskDecorator, this.customizers);
  }

  /**
   * Set whether core threads are allowed to time out. When enabled, this enables
   * dynamic growing and shrinking of the pool.
   *
   * @param allowCoreThreadTimeOut if core threads are allowed to time out
   * @return a new builder instance
   */
  public ThreadPoolTaskExecutorBuilder allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination,
            this.awaitTerminationPeriod, this.threadNamePrefix, this.taskDecorator, this.customizers);
  }

  /**
   * Set the time limit for which threads may remain idle before being terminated.
   *
   * @param keepAlive the keep alive to set
   * @return a new builder instance
   */
  public ThreadPoolTaskExecutorBuilder keepAlive(@Nullable Duration keepAlive) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, keepAlive, acceptTasksAfterContextClose, this.awaitTermination,
            this.awaitTerminationPeriod, this.threadNamePrefix, this.taskDecorator, this.customizers);
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
  public ThreadPoolTaskExecutorBuilder awaitTermination(boolean awaitTermination) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, awaitTermination,
            this.awaitTerminationPeriod, this.threadNamePrefix, this.taskDecorator, this.customizers);
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
  public ThreadPoolTaskExecutorBuilder awaitTerminationPeriod(@Nullable Duration awaitTerminationPeriod) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination,
            awaitTerminationPeriod, this.threadNamePrefix, this.taskDecorator, this.customizers);
  }

  /**
   * Set whether to accept further tasks after the application context close phase has
   * begun.
   *
   * @param acceptTasksAfterContextClose whether to accept further tasks after the
   * application context close phase has begun
   * @return a new builder instance
   */
  public ThreadPoolTaskExecutorBuilder acceptTasksAfterContextClose(boolean acceptTasksAfterContextClose) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination,
            this.awaitTerminationPeriod, this.threadNamePrefix, this.taskDecorator, this.customizers);
  }

  /**
   * Set the prefix to use for the names of newly created threads.
   *
   * @param threadNamePrefix the thread name prefix to set
   * @return a new builder instance
   */
  public ThreadPoolTaskExecutorBuilder threadNamePrefix(@Nullable String threadNamePrefix) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination,
            this.awaitTerminationPeriod, threadNamePrefix, this.taskDecorator, this.customizers);
  }

  /**
   * Set the {@link TaskDecorator} to use or {@code null} to not use any.
   *
   * @param taskDecorator the task decorator to use
   * @return a new builder instance
   */
  public ThreadPoolTaskExecutorBuilder taskDecorator(@Nullable TaskDecorator taskDecorator) {
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination,
            this.awaitTerminationPeriod, this.threadNamePrefix, taskDecorator, this.customizers);
  }

  /**
   * Set the {@link ThreadPoolTaskExecutorCustomizer ThreadPoolTaskExecutorCustomizers}
   * that should be applied to the {@link ThreadPoolTaskExecutor}. Customizers are
   * applied in the order that they were added after builder configuration has been
   * applied. Setting this value will replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(ThreadPoolTaskExecutorCustomizer...)
   */
  public ThreadPoolTaskExecutorBuilder customizers(ThreadPoolTaskExecutorCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return customizers(Arrays.asList(customizers));
  }

  /**
   * Set the {@link ThreadPoolTaskExecutorCustomizer ThreadPoolTaskExecutorCustomizers}
   * that should be applied to the {@link ThreadPoolTaskExecutor}. Customizers are
   * applied in the order that they were added after builder configuration has been
   * applied. Setting this value will replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(ThreadPoolTaskExecutorCustomizer...)
   */
  public ThreadPoolTaskExecutorBuilder customizers(Iterable<? extends ThreadPoolTaskExecutorCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination, this.awaitTerminationPeriod,
            this.threadNamePrefix, this.taskDecorator, append(null, customizers));
  }

  /**
   * Add {@link ThreadPoolTaskExecutorCustomizer ThreadPoolTaskExecutorCustomizers} that
   * should be applied to the {@link ThreadPoolTaskExecutor}. Customizers are applied in
   * the order that they were added after builder configuration has been applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(ThreadPoolTaskExecutorCustomizer...)
   */
  public ThreadPoolTaskExecutorBuilder additionalCustomizers(ThreadPoolTaskExecutorCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return additionalCustomizers(Arrays.asList(customizers));
  }

  /**
   * Add {@link ThreadPoolTaskExecutorCustomizer ThreadPoolTaskExecutorCustomizers} that
   * should be applied to the {@link ThreadPoolTaskExecutor}. Customizers are applied in
   * the order that they were added after builder configuration has been applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(ThreadPoolTaskExecutorCustomizer...)
   */
  public ThreadPoolTaskExecutorBuilder additionalCustomizers(Iterable<? extends ThreadPoolTaskExecutorCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers is required");
    return new ThreadPoolTaskExecutorBuilder(this.queueCapacity, this.corePoolSize, this.maxPoolSize,
            this.allowCoreThreadTimeOut, this.keepAlive, acceptTasksAfterContextClose, this.awaitTermination, this.awaitTerminationPeriod,
            this.threadNamePrefix, this.taskDecorator, append(this.customizers, customizers));
  }

  /**
   * Build a new {@link ThreadPoolTaskExecutor} instance and configure it using this
   * builder.
   *
   * @return a configured {@link ThreadPoolTaskExecutor} instance.
   * @see #build(Class)
   * @see #configure(ThreadPoolTaskExecutor)
   */
  public ThreadPoolTaskExecutor build() {
    return configure(new ThreadPoolTaskExecutor());
  }

  /**
   * Build a new {@link ThreadPoolTaskExecutor} instance of the specified type and
   * configure it using this builder.
   *
   * @param <T> the type of task executor
   * @param taskExecutorClass the template type to create
   * @return a configured {@link ThreadPoolTaskExecutor} instance.
   * @see #build()
   * @see #configure(ThreadPoolTaskExecutor)
   */
  public <T extends ThreadPoolTaskExecutor> T build(Class<T> taskExecutorClass) {
    return configure(BeanUtils.newInstance(taskExecutorClass));
  }

  /**
   * Configure the provided {@link ThreadPoolTaskExecutor} instance using this builder.
   *
   * @param <T> the type of task executor
   * @param taskExecutor the {@link ThreadPoolTaskExecutor} to configure
   * @return the task executor instance
   * @see #build()
   * @see #build(Class)
   */
  public <T extends ThreadPoolTaskExecutor> T configure(T taskExecutor) {
    if (queueCapacity != null) {
      taskExecutor.setQueueCapacity(queueCapacity);
    }

    if (corePoolSize != null) {
      taskExecutor.setCorePoolSize(corePoolSize);
    }

    if (maxPoolSize != null) {
      taskExecutor.setMaxPoolSize(maxPoolSize);
    }
    if (keepAlive != null) {
      taskExecutor.setKeepAliveSeconds((int) keepAlive.getSeconds());
    }
    if (allowCoreThreadTimeOut != null) {
      taskExecutor.setAllowCoreThreadTimeOut(allowCoreThreadTimeOut);
    }

    if (awaitTermination != null) {
      taskExecutor.setWaitForTasksToCompleteOnShutdown(awaitTermination);
    }
    if (awaitTerminationPeriod != null) {
      taskExecutor.setAwaitTerminationMillis(awaitTerminationPeriod.toMillis());
    }
    if (StringUtils.hasText(threadNamePrefix)) {
      taskExecutor.setThreadNamePrefix(threadNamePrefix);
    }
    if (taskDecorator != null) {
      taskExecutor.setTaskDecorator(taskDecorator);
    }
    if (acceptTasksAfterContextClose != null) {
      taskExecutor.setAcceptTasksAfterContextClose(acceptTasksAfterContextClose);
    }

    if (CollectionUtils.isNotEmpty(customizers)) {
      for (ThreadPoolTaskExecutorCustomizer customizer : customizers) {
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
