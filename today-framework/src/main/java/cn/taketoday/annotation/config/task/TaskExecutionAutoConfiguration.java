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

package cn.taketoday.annotation.config.task;

import java.util.List;
import java.util.concurrent.Executor;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnThreading;
import cn.taketoday.context.condition.Threading;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.scheduling.support.SimpleAsyncTaskExecutorBuilder;
import cn.taketoday.scheduling.support.SimpleAsyncTaskExecutorCustomizer;
import cn.taketoday.scheduling.support.ThreadPoolTaskExecutorBuilder;
import cn.taketoday.scheduling.support.ThreadPoolTaskExecutorCustomizer;
import cn.taketoday.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskExecutor}.
 *
 * @author Stephane Nicoll
 * @author Camille Vienot
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDIAutoConfiguration
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
@EnableConfigurationProperties(TaskExecutionProperties.class)
public class TaskExecutionAutoConfiguration {

  /**
   * Bean name of the application {@link TaskExecutor}.
   */
  public static final String APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor";

  @Lazy
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(Executor.class)
  static class TaskExecutorConfiguration {

    @ConditionalOnThreading(Threading.VIRTUAL)
    @Component({ APPLICATION_TASK_EXECUTOR_BEAN_NAME, AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
    static SimpleAsyncTaskExecutor applicationTaskExecutorVirtualThreads(SimpleAsyncTaskExecutorBuilder builder) {
      return builder.build();
    }

    @Lazy
    @ConditionalOnThreading(Threading.PLATFORM)
    @Component({ APPLICATION_TASK_EXECUTOR_BEAN_NAME, AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
    static ThreadPoolTaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
      return builder.build();
    }

  }

  @Bean
  @ConditionalOnMissingBean(ThreadPoolTaskExecutorBuilder.class)
  static ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder(TaskExecutionProperties properties,
          List<ThreadPoolTaskExecutorCustomizer> customizers, ObjectProvider<TaskDecorator> taskDecorator) {
    TaskExecutionProperties.Pool pool = properties.getPool();
    ThreadPoolTaskExecutorBuilder builder = new ThreadPoolTaskExecutorBuilder();
    builder = builder.queueCapacity(pool.getQueueCapacity());
    builder = builder.corePoolSize(pool.getCoreSize());
    builder = builder.maxPoolSize(pool.getMaxSize());
    builder = builder.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
    builder = builder.keepAlive(pool.getKeepAlive());
    var shutdown = properties.getShutdown();
    builder = builder.awaitTermination(shutdown.isAwaitTermination());
    builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
    builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
    builder = builder.customizers(customizers);
    builder = builder.taskDecorator(taskDecorator.getIfUnique());
    return builder;
  }

  @Component
  @ConditionalOnMissingBean
  static SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilder(ObjectProvider<TaskDecorator> taskDecorator,
          Environment environment, TaskExecutionProperties properties, List<SimpleAsyncTaskExecutorCustomizer> customizers) {

    SimpleAsyncTaskExecutorBuilder builder = new SimpleAsyncTaskExecutorBuilder();
    builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
    builder = builder.customizers(customizers);
    builder = builder.taskDecorator(taskDecorator.getIfUnique());
    var simple = properties.getSimple();
    builder = builder.concurrencyLimit(simple.getConcurrencyLimit());

    var shutdown = properties.getShutdown();
    if (shutdown.isAwaitTermination()) {
      builder = builder.taskTerminationTimeout(shutdown.getAwaitTerminationPeriod());
    }

    if (Threading.VIRTUAL.isActive(environment)) {
      builder = builder.virtualThreads(true);
    }
    return builder;
  }

}
