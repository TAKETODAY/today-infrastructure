/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.scheduling.support.TaskExecutorBuilder;
import cn.taketoday.scheduling.support.TaskExecutorCustomizer;
import cn.taketoday.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskExecutor}.
 *
 * @author Stephane Nicoll
 * @author Camille Vienot
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@AutoConfiguration
@ConditionalOnClass(ThreadPoolTaskExecutor.class)
@EnableConfigurationProperties(TaskExecutionProperties.class)
public class TaskExecutionAutoConfiguration {

  /**
   * Bean name of the application {@link TaskExecutor}.
   */
  public static final String APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor";

  @Component
  @ConditionalOnMissingBean
  public TaskExecutorBuilder taskExecutorBuilder(TaskExecutionProperties properties,
          ObjectProvider<TaskDecorator> taskDecorator,
          ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers) {

    var pool = properties.getPool();
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
    builder = builder.customizers(taskExecutorCustomizers.orderedStream()::iterator);
    builder = builder.taskDecorator(taskDecorator.getIfUnique());
    return builder;
  }

  @Lazy
  @ConditionalOnMissingBean(Executor.class)
  @Component(name = {
          APPLICATION_TASK_EXECUTOR_BEAN_NAME,
          AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME
  })
  public ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder builder) {
    return builder.build();
  }

}
