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

import java.util.concurrent.Executor;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnThreading;
import cn.taketoday.context.condition.Threading;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.scheduling.support.SimpleAsyncTaskExecutorBuilder;
import cn.taketoday.scheduling.support.SimpleAsyncTaskExecutorCustomizer;
import cn.taketoday.scheduling.support.TaskExecutorBuilder;
import cn.taketoday.scheduling.support.TaskExecutorCustomizer;
import cn.taketoday.scheduling.support.ThreadPoolTaskExecutorBuilder;
import cn.taketoday.scheduling.support.ThreadPoolTaskExecutorCustomizer;
import cn.taketoday.stereotype.Component;

/**
 * {@link TaskExecutor} configurations to be imported by
 * {@link TaskExecutionAutoConfiguration} in a specific order.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TaskExecutorConfigurations {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(Executor.class)
  @SuppressWarnings("removal")
  static class TaskExecutorConfiguration {

    @Bean(name = { TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
    @ConditionalOnThreading(Threading.VIRTUAL)
    SimpleAsyncTaskExecutor applicationTaskExecutorVirtualThreads(SimpleAsyncTaskExecutorBuilder builder) {
      return builder.build();
    }

    @Lazy
    @Bean(name = { TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
    @ConditionalOnThreading(Threading.PLATFORM)
    ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder taskExecutorBuilder,
            ObjectProvider<ThreadPoolTaskExecutorBuilder> threadPoolTaskExecutorBuilderProvider) {
      ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder = threadPoolTaskExecutorBuilderProvider
              .getIfUnique();
      if (threadPoolTaskExecutorBuilder != null) {
        return threadPoolTaskExecutorBuilder.build();
      }
      return taskExecutorBuilder.build();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @SuppressWarnings("removal")
  static class TaskExecutorBuilderConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @Deprecated
    TaskExecutorBuilder taskExecutorBuilder(TaskExecutionProperties properties,
            ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers,
            ObjectProvider<TaskDecorator> taskDecorator) {
      TaskExecutionProperties.Pool pool = properties.getPool();
      TaskExecutorBuilder builder = new TaskExecutorBuilder();
      builder = builder.queueCapacity(pool.getQueueCapacity());
      builder = builder.corePoolSize(pool.getCoreSize());
      builder = builder.maxPoolSize(pool.getMaxSize());
      builder = builder.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
      builder = builder.keepAlive(pool.getKeepAlive());
      var shutdown = properties.getShutdown();
      builder = builder.awaitTermination(shutdown.isAwaitTermination());
      builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
      builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
      builder = builder.customizers(taskExecutorCustomizers);
      builder = builder.taskDecorator(taskDecorator.getIfUnique());
      return builder;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @SuppressWarnings("removal")
  static class ThreadPoolTaskExecutorBuilderConfiguration {

    @Bean
    @ConditionalOnMissingBean({ TaskExecutorBuilder.class, ThreadPoolTaskExecutorBuilder.class })
    ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder(TaskExecutionProperties properties,
            ObjectProvider<ThreadPoolTaskExecutorCustomizer> threadPoolTaskExecutorCustomizers,
            ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers,
            ObjectProvider<TaskDecorator> taskDecorator) {
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
      builder = builder.customizers(threadPoolTaskExecutorCustomizers);
      builder = builder.taskDecorator(taskDecorator.getIfUnique());
      // Apply the deprecated TaskExecutorCustomizers, too
      builder = builder.additionalCustomizers(taskExecutorCustomizers.orderedStream().map(this::adapt).toList());
      return builder;
    }

    private ThreadPoolTaskExecutorCustomizer adapt(TaskExecutorCustomizer customizer) {
      return customizer::customize;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SimpleAsyncTaskExecutorBuilderConfiguration {

    private final TaskExecutionProperties properties;

    private final ObjectProvider<SimpleAsyncTaskExecutorCustomizer> taskExecutorCustomizers;

    private final ObjectProvider<TaskDecorator> taskDecorator;

    SimpleAsyncTaskExecutorBuilderConfiguration(TaskExecutionProperties properties,
            ObjectProvider<SimpleAsyncTaskExecutorCustomizer> taskExecutorCustomizers,
            ObjectProvider<TaskDecorator> taskDecorator) {
      this.properties = properties;
      this.taskExecutorCustomizers = taskExecutorCustomizers;
      this.taskDecorator = taskDecorator;
    }

    @Component
    @ConditionalOnMissingBean
    @ConditionalOnThreading(Threading.PLATFORM)
    SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilder() {
      return builder();
    }

    @Component(name = "simpleAsyncTaskExecutorBuilder")
    @ConditionalOnMissingBean
    @ConditionalOnThreading(Threading.VIRTUAL)
    SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilderVirtualThreads() {
      SimpleAsyncTaskExecutorBuilder builder = builder();
      builder = builder.virtualThreads(true);
      return builder;
    }

    private SimpleAsyncTaskExecutorBuilder builder() {
      SimpleAsyncTaskExecutorBuilder builder = new SimpleAsyncTaskExecutorBuilder();
      builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
      builder = builder.customizers(taskExecutorCustomizers);
      builder = builder.taskDecorator(taskDecorator.getIfUnique());
      var simple = properties.getSimple();
      builder = builder.concurrencyLimit(simple.getConcurrencyLimit());
      return builder;
    }

  }

}
