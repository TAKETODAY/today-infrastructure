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

package infra.annotation.config.task;

import java.util.List;
import java.util.concurrent.Executor;

import infra.beans.factory.BeanFactory;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.AnyNestedCondition;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.condition.ConditionalOnThreading;
import infra.context.condition.Threading;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.env.Environment;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.core.task.TaskDecorator;
import infra.core.task.TaskExecutor;
import org.jspecify.annotations.Nullable;
import infra.scheduling.annotation.AsyncConfigurer;
import infra.scheduling.concurrent.ThreadPoolTaskExecutor;
import infra.scheduling.support.SimpleAsyncTaskExecutorBuilder;
import infra.scheduling.support.SimpleAsyncTaskExecutorCustomizer;
import infra.scheduling.support.ThreadPoolTaskExecutorBuilder;
import infra.scheduling.support.ThreadPoolTaskExecutorCustomizer;
import infra.stereotype.Component;

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

  private TaskExecutionAutoConfiguration() {
  }

  /**
   * Bean name of the application {@link TaskExecutor}.
   */
  public static final String APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor";

  @Component
  @ConditionalOnMissingBean(ThreadPoolTaskExecutorBuilder.class)
  public static ThreadPoolTaskExecutorBuilder threadPoolTaskExecutorBuilder(TaskExecutionProperties properties,
          List<ThreadPoolTaskExecutorCustomizer> customizers, @Nullable TaskDecorator taskDecorator) {
    TaskExecutionProperties.Pool pool = properties.getPool();
    ThreadPoolTaskExecutorBuilder builder = new ThreadPoolTaskExecutorBuilder();
    builder = builder.queueCapacity(pool.getQueueCapacity());
    builder = builder.corePoolSize(pool.getCoreSize());
    builder = builder.maxPoolSize(pool.getMaxSize());
    builder = builder.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
    builder = builder.keepAlive(pool.getKeepAlive());
    builder = builder.acceptTasksAfterContextClose(pool.getShutdown().isAcceptTasksAfterContextClose());
    var shutdown = properties.getShutdown();
    builder = builder.awaitTermination(shutdown.isAwaitTermination());
    builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
    builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
    builder = builder.customizers(customizers);
    builder = builder.taskDecorator(taskDecorator);
    return builder;
  }

  @Component
  @ConditionalOnMissingBean
  public static SimpleAsyncTaskExecutorBuilder simpleAsyncTaskExecutorBuilder(ObjectProvider<TaskDecorator> taskDecorator,
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

  @Component
  public static BeanFactoryPostProcessor bootstrapExecutorAliasPostProcessor() {
    return beanFactory -> {
      boolean hasBootstrapExecutor = beanFactory.containsBean(ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
      boolean hasApplicationTaskExecutor = beanFactory.containsBean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
      if (!hasBootstrapExecutor && hasApplicationTaskExecutor) {
        beanFactory.registerAlias(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME,
                ConfigurableApplicationContext.BOOTSTRAP_EXECUTOR_BEAN_NAME);
      }
    };
  }

  @Lazy
  @Configuration(proxyBeanMethods = false)
  @Conditional(OnExecutorCondition.class)
  public static class TaskExecutorConfiguration {

    @ConditionalOnThreading(Threading.VIRTUAL)
    @Component(APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public static SimpleAsyncTaskExecutor applicationTaskExecutorVirtualThreads(SimpleAsyncTaskExecutorBuilder builder) {
      return builder.build();
    }

    @Lazy
    @ConditionalOnThreading(Threading.PLATFORM)
    @Component(APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public static ThreadPoolTaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
      return builder.build();
    }

    @Component
    @ConditionalOnMissingBean(AsyncConfigurer.class)
    public static AsyncConfigurer applicationTaskExecutorAsyncConfigurer(BeanFactory beanFactory) {
      return new AsyncConfigurer() {
        @Override
        public Executor getAsyncExecutor() {
          return beanFactory.getBean(APPLICATION_TASK_EXECUTOR_BEAN_NAME, Executor.class);
        }
      };
    }

  }

  static class OnExecutorCondition extends AnyNestedCondition {

    OnExecutorCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnMissingBean(Executor.class)
    private static final class ExecutorBeanCondition {

    }

    @ConditionalOnProperty(value = "infra.task.execution.mode", havingValue = "force")
    private static final class ModeCondition {

    }

  }

}
