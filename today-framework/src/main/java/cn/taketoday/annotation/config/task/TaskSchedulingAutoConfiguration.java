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

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnBean;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnThreading;
import cn.taketoday.context.condition.Threading;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.LazyInitializationExcludeFilter;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.concurrent.SimpleAsyncTaskScheduler;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskScheduler;
import cn.taketoday.scheduling.config.TaskManagementConfigUtils;
import cn.taketoday.scheduling.support.SimpleAsyncTaskSchedulerBuilder;
import cn.taketoday.scheduling.support.SimpleAsyncTaskSchedulerCustomizer;
import cn.taketoday.scheduling.support.ThreadPoolTaskSchedulerBuilder;
import cn.taketoday.scheduling.support.ThreadPoolTaskSchedulerCustomizer;
import cn.taketoday.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskScheduler}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableAllDependencyInjection
@ConditionalOnClass(ThreadPoolTaskScheduler.class)
@AutoConfiguration(after = TaskExecutionAutoConfiguration.class)
@EnableConfigurationProperties(TaskSchedulingProperties.class)
public class TaskSchedulingAutoConfiguration {

  @Component
  @ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
  public static LazyInitializationExcludeFilter scheduledBeanLazyInitializationExcludeFilter() {
    return new ScheduledBeanLazyInitializationExcludeFilter();
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
  @ConditionalOnMissingBean({ TaskScheduler.class, ScheduledExecutorService.class })
  static class TaskSchedulerConfiguration {

    @Component(name = "taskScheduler")
    @ConditionalOnThreading(Threading.VIRTUAL)
    static SimpleAsyncTaskScheduler taskSchedulerVirtualThreads(SimpleAsyncTaskSchedulerBuilder builder) {
      return builder.build();
    }

    @Component
    @ConditionalOnThreading(Threading.PLATFORM)
    static ThreadPoolTaskScheduler taskScheduler(ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder) {
      return threadPoolTaskSchedulerBuilder.build();
    }

    @Component
    @ConditionalOnMissingBean
    static ThreadPoolTaskSchedulerBuilder threadPoolTaskSchedulerBuilder(TaskSchedulingProperties properties,
            List<ThreadPoolTaskSchedulerCustomizer> threadPoolTaskSchedulerCustomizers) {
      TaskSchedulingProperties.Shutdown shutdown = properties.getShutdown();
      ThreadPoolTaskSchedulerBuilder builder = new ThreadPoolTaskSchedulerBuilder();
      builder = builder.poolSize(properties.getPool().getSize());
      builder = builder.awaitTermination(shutdown.isAwaitTermination());
      builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
      builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
      builder = builder.customizers(threadPoolTaskSchedulerCustomizers);
      return builder;
    }

    @Component
    @ConditionalOnMissingBean
    static SimpleAsyncTaskSchedulerBuilder simpleAsyncTaskSchedulerBuilder(Environment environment,
            TaskSchedulingProperties properties, List<SimpleAsyncTaskSchedulerCustomizer> taskSchedulerCustomizers) {
      SimpleAsyncTaskSchedulerBuilder builder = new SimpleAsyncTaskSchedulerBuilder();
      builder = builder.customizers(taskSchedulerCustomizers);
      builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
      builder = builder.concurrencyLimit(properties.getSimple().getConcurrencyLimit());
      if (Threading.VIRTUAL.isActive(environment)) {
        builder = builder.virtualThreads(true);
      }
      return builder;
    }
  }
}
