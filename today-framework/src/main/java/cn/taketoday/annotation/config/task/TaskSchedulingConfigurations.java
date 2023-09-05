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

package cn.taketoday.annotation.config.task;

import java.util.concurrent.ScheduledExecutorService;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnThreading;
import cn.taketoday.context.condition.Threading;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.concurrent.SimpleAsyncTaskScheduler;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskScheduler;
import cn.taketoday.scheduling.config.TaskManagementConfigUtils;
import cn.taketoday.scheduling.support.SimpleAsyncTaskSchedulerBuilder;
import cn.taketoday.scheduling.support.SimpleAsyncTaskSchedulerCustomizer;
import cn.taketoday.scheduling.support.TaskSchedulerBuilder;
import cn.taketoday.scheduling.support.TaskSchedulerCustomizer;
import cn.taketoday.scheduling.support.ThreadPoolTaskSchedulerBuilder;
import cn.taketoday.scheduling.support.ThreadPoolTaskSchedulerCustomizer;
import cn.taketoday.stereotype.Component;

/**
 * {@link TaskScheduler} configurations to be imported by
 * {@link TaskSchedulingAutoConfiguration} in a specific order.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TaskSchedulingConfigurations {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
  @ConditionalOnMissingBean({ TaskScheduler.class, ScheduledExecutorService.class })
  @SuppressWarnings("removal")
  static class TaskSchedulerConfiguration {

    @Component(name = "taskScheduler")
    @ConditionalOnThreading(Threading.VIRTUAL)
    static SimpleAsyncTaskScheduler taskSchedulerVirtualThreads(SimpleAsyncTaskSchedulerBuilder builder) {
      return builder.build();
    }

    @Component
    @ConditionalOnThreading(Threading.PLATFORM)
    static ThreadPoolTaskScheduler taskScheduler(TaskSchedulerBuilder taskSchedulerBuilder,
            ObjectProvider<ThreadPoolTaskSchedulerBuilder> threadPoolTaskSchedulerBuilderProvider) {
      ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder = threadPoolTaskSchedulerBuilderProvider
              .getIfUnique();
      if (threadPoolTaskSchedulerBuilder != null) {
        return threadPoolTaskSchedulerBuilder.build();
      }
      return taskSchedulerBuilder.build();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @SuppressWarnings("removal")
  static class TaskSchedulerBuilderConfiguration {

    @Component
    @ConditionalOnMissingBean
    static TaskSchedulerBuilder taskSchedulerBuilder(TaskSchedulingProperties properties,
            ObjectProvider<TaskSchedulerCustomizer> taskSchedulerCustomizers) {
      TaskSchedulerBuilder builder = new TaskSchedulerBuilder();
      builder = builder.poolSize(properties.getPool().getSize());
      TaskSchedulingProperties.Shutdown shutdown = properties.getShutdown();
      builder = builder.awaitTermination(shutdown.isAwaitTermination());
      builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
      builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
      builder = builder.customizers(taskSchedulerCustomizers);
      return builder;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @SuppressWarnings("removal")
  static class ThreadPoolTaskSchedulerBuilderConfiguration {

    @Component
    @ConditionalOnMissingBean({ TaskSchedulerBuilder.class, ThreadPoolTaskSchedulerBuilder.class })
    static ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder(TaskSchedulingProperties properties,
            ObjectProvider<ThreadPoolTaskSchedulerCustomizer> threadPoolTaskSchedulerCustomizers,
            ObjectProvider<TaskSchedulerCustomizer> taskSchedulerCustomizers) {
      TaskSchedulingProperties.Shutdown shutdown = properties.getShutdown();
      ThreadPoolTaskSchedulerBuilder builder = new ThreadPoolTaskSchedulerBuilder();
      builder = builder.poolSize(properties.getPool().getSize());
      builder = builder.awaitTermination(shutdown.isAwaitTermination());
      builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
      builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
      builder = builder.customizers(threadPoolTaskSchedulerCustomizers);
      // Apply the deprecated TaskSchedulerCustomizers, too
      builder = builder.additionalCustomizers(taskSchedulerCustomizers.orderedStream().map(this::adapt).toList());
      return builder;
    }

    private ThreadPoolTaskSchedulerCustomizer adapt(TaskSchedulerCustomizer customizer) {
      return customizer::customize;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SimpleAsyncTaskSchedulerBuilderConfiguration {

    private final TaskSchedulingProperties properties;

    private final ObjectProvider<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers;

    SimpleAsyncTaskSchedulerBuilderConfiguration(TaskSchedulingProperties properties,
            ObjectProvider<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers) {
      this.properties = properties;
      this.taskSchedulerCustomizers = taskSchedulerCustomizers;
    }

    @Component
    @ConditionalOnMissingBean
    @ConditionalOnThreading(Threading.PLATFORM)
    SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilder() {
      return builder();
    }

    @Component(name = "simpleAsyncTaskSchedulerBuilder")
    @ConditionalOnMissingBean
    @ConditionalOnThreading(Threading.VIRTUAL)
    SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilderVirtualThreads() {
      SimpleAsyncTaskSchedulerBuilder builder = builder();
      builder = builder.virtualThreads(true);
      return builder;
    }

    private SimpleAsyncTaskSchedulerBuilder builder() {
      SimpleAsyncTaskSchedulerBuilder builder = new SimpleAsyncTaskSchedulerBuilder();
      builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
      builder = builder.customizers(taskSchedulerCustomizers);
      var simple = properties.getSimple();
      builder = builder.concurrencyLimit(simple.getConcurrencyLimit());
      return builder;
    }

  }

}
