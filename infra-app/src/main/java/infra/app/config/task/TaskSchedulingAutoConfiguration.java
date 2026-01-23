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

package infra.app.config.task;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import infra.app.LazyInitializationExcludeFilter;
import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnThreading;
import infra.context.condition.Threading;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.env.Environment;
import infra.core.task.TaskDecorator;
import infra.scheduling.TaskScheduler;
import infra.scheduling.concurrent.SimpleAsyncTaskScheduler;
import infra.scheduling.concurrent.ThreadPoolTaskScheduler;
import infra.scheduling.config.TaskManagementConfigUtils;
import infra.scheduling.support.SimpleAsyncTaskSchedulerBuilder;
import infra.scheduling.support.SimpleAsyncTaskSchedulerCustomizer;
import infra.scheduling.support.ThreadPoolTaskSchedulerBuilder;
import infra.scheduling.support.ThreadPoolTaskSchedulerCustomizer;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskScheduler}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@ConditionalOnClass(ThreadPoolTaskScheduler.class)
@ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
@DisableDIAutoConfiguration(after = infra.app.config.task.TaskExecutionAutoConfiguration.class)
@EnableConfigurationProperties(TaskSchedulingProperties.class)
public final class TaskSchedulingAutoConfiguration {

  @Component
  public static LazyInitializationExcludeFilter scheduledBeanLazyInitializationExcludeFilter() {
    return new ScheduledBeanLazyInitializationExcludeFilter();
  }

  @Component
  @ConditionalOnMissingBean
  public static ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder(TaskSchedulingProperties properties,
          List<ThreadPoolTaskSchedulerCustomizer> threadPoolTaskSchedulerCustomizers, ObjectProvider<TaskDecorator> taskDecorator) {
    TaskSchedulingProperties.Shutdown shutdown = properties.getShutdown();
    ThreadPoolTaskSchedulerBuilder builder = new ThreadPoolTaskSchedulerBuilder();
    builder = builder.poolSize(properties.getPool().getSize());
    builder = builder.awaitTermination(shutdown.isAwaitTermination());
    builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
    builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
    builder = builder.customizers(threadPoolTaskSchedulerCustomizers);
    builder = builder.taskDecorator(taskDecorator.getIfUnique());
    return builder;
  }

  @Component
  @ConditionalOnMissingBean
  public static SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilder(Environment environment,
          ObjectProvider<TaskDecorator> taskDecorator, TaskSchedulingProperties properties,
          List<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers) {
    SimpleAsyncTaskSchedulerBuilder builder = new SimpleAsyncTaskSchedulerBuilder();
    builder = builder.customizers(taskSchedulerCustomizers);
    builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
    builder = builder.concurrencyLimit(properties.getSimple().getConcurrencyLimit());
    builder = builder.taskDecorator(taskDecorator.getIfUnique());

    var shutdown = properties.getShutdown();
    if (shutdown.isAwaitTermination()) {
      builder = builder.taskTerminationTimeout(shutdown.getAwaitTerminationPeriod());
    }
    if (Threading.VIRTUAL.isActive(environment)) {
      builder = builder.virtualThreads(true);
    }
    return builder;
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean({ TaskScheduler.class, ScheduledExecutorService.class })
  public static class TaskSchedulerConfiguration {

    @Component(name = "taskScheduler")
    @ConditionalOnThreading(Threading.VIRTUAL)
    public static SimpleAsyncTaskScheduler taskSchedulerVirtualThreads(SimpleAsyncTaskSchedulerBuilder builder) {
      return builder.build();
    }

    @Component
    @ConditionalOnThreading(Threading.PLATFORM)
    public static ThreadPoolTaskScheduler taskScheduler(ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder) {
      return threadPoolTaskSchedulerBuilder.build();
    }

  }

}
