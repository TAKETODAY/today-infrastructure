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
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnVirtualThreads;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.scheduling.support.TaskExecutorBuilder;
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

  @ConditionalOnVirtualThreads
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(Executor.class)
  static class VirtualThreadTaskExecutorConfiguration {

    @Component(name = { TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
    SimpleAsyncTaskExecutor applicationTaskExecutor(TaskExecutionProperties properties,
            ObjectProvider<TaskDecorator> taskDecorator) {
      SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor(properties.getThreadNamePrefix());
      executor.setVirtualThreads(true);
      executor.setTaskDecorator(taskDecorator.getIfUnique());
      return executor;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean(Executor.class)
  static class ThreadPoolTaskExecutorConfiguration {

    @Lazy
    @Component(name = { TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME })
    ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder builder) {
      return builder.build();
    }

  }

}
