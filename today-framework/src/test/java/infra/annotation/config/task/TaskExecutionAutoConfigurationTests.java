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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ContextConsumer;
import infra.app.test.system.OutputCaptureExtension;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanFactoryPostProcessor;
import infra.beans.factory.support.BeanDefinitionOverrideException;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.task.SimpleAsyncTaskExecutor;
import infra.core.task.SyncTaskExecutor;
import infra.core.task.TaskDecorator;
import infra.core.task.TaskExecutor;
import infra.scheduling.annotation.Async;
import infra.scheduling.annotation.AsyncConfigurer;
import infra.scheduling.annotation.EnableAsync;
import infra.scheduling.annotation.EnableScheduling;
import infra.scheduling.concurrent.ThreadPoolTaskExecutor;
import infra.scheduling.support.SimpleAsyncTaskExecutorBuilder;
import infra.scheduling.support.ThreadPoolTaskExecutorBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/23 21:56
 */
@ExtendWith(OutputCaptureExtension.class)
class TaskExecutionAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class));

  @Test
  void shouldSupplyBeans() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
      assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
      assertThat(context).hasSingleBean(SimpleAsyncTaskExecutorBuilder.class);
    });
  }

  @Test
  void simpleAsyncTaskExecutorBuilderShouldReadProperties() {
    this.contextRunner
            .withPropertyValues("infra.task.execution.thread-name-prefix=mytest-",
                    "infra.task.execution.simple.concurrency-limit=1",
                    "infra.task.execution.shutdown.await-termination=true",
                    "infra.task.execution.shutdown.await-termination-period=30s")
            .run(assertSimpleAsyncTaskExecutor((taskExecutor) -> {
              assertThat(taskExecutor.getConcurrencyLimit()).isEqualTo(1);
              assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
              assertThat(taskExecutor).hasFieldOrPropertyWithValue("taskTerminationTimeout", 30000L);
            }));
  }

  @Test
  void threadPoolTaskExecutorBuilderShouldApplyCustomSettings() {
    this.contextRunner.withPropertyValues("infra.task.execution.pool.queue-capacity=10",
                    "infra.task.execution.pool.core-size=2", "infra.task.execution.pool.max-size=4",
                    "infra.task.execution.pool.allow-core-thread-timeout=true",
                    "infra.task.execution.pool.keep-alive=5s",
                    "infra.task.execution.pool.shutdown.accept-tasks-after-context-close=true",
                    "infra.task.execution.shutdown.await-termination=true",
                    "infra.task.execution.shutdown.await-termination-period=30s",
                    "infra.task.execution.thread-name-prefix=mytest-")
            .run(assertThreadPoolTaskExecutor((taskExecutor) -> {
              assertThat(taskExecutor).hasFieldOrPropertyWithValue("queueCapacity", 10);
              assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
              assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
              assertThat(taskExecutor).hasFieldOrPropertyWithValue("allowCoreThreadTimeOut", true);
              assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(5);
              assertThat(taskExecutor).hasFieldOrPropertyWithValue("acceptTasksAfterContextClose", true);
              assertThat(taskExecutor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
              assertThat(taskExecutor).hasFieldOrPropertyWithValue("awaitTerminationMillis", 30000L);
              assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
            }));
  }

  @Test
  void threadPoolTaskExecutorBuilderWhenHasCustomBuilderShouldUseCustomBuilder() {
    this.contextRunner.withUserConfiguration(CustomThreadPoolTaskExecutorBuilderConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
      assertThat(context.getBean(ThreadPoolTaskExecutorBuilder.class))
              .isSameAs(context.getBean(CustomThreadPoolTaskExecutorBuilderConfig.class).builder);
    });
  }

  @Test
  void threadPoolTaskExecutorBuilderShouldUseTaskDecorator() {
    this.contextRunner.withUserConfiguration(TaskDecoratorConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
      ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutorBuilder.class).build();
      assertThat(executor).extracting("taskDecorator").isSameAs(context.getBean(TaskDecorator.class));
    });
  }

  @Test
  void whenThreadPoolTaskExecutorIsAutoConfiguredThenItIsLazy() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(Executor.class).hasBean("applicationTaskExecutor");
      BeanDefinition beanDefinition = context.getSourceApplicationContext()
              .getBeanFactory()
              .getBeanDefinition("applicationTaskExecutor");
      assertThat(beanDefinition.isLazyInit()).isTrue();
      assertThat(context).getBean("applicationTaskExecutor").isInstanceOf(ThreadPoolTaskExecutor.class);
    });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void whenVirtualThreadsAreEnabledThenSimpleAsyncTaskExecutorWithVirtualThreadsIsAutoConfigured() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true").run((context) -> {
      assertThat(context).hasSingleBean(Executor.class).hasBean("applicationTaskExecutor");
      assertThat(context).getBean("applicationTaskExecutor").isInstanceOf(SimpleAsyncTaskExecutor.class);
      SimpleAsyncTaskExecutor taskExecutor = context.getBean("applicationTaskExecutor",
              SimpleAsyncTaskExecutor.class);
      assertThat(virtualThreadName(taskExecutor)).startsWith("task-");
    });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void whenTaskNamePrefixIsConfiguredThenSimpleAsyncTaskExecutorWithVirtualThreadsUsesIt() {
    this.contextRunner
            .withPropertyValues("infra.threads.virtual.enabled=true",
                    "infra.task.execution.thread-name-prefix=custom-")
            .run((context) -> {
              SimpleAsyncTaskExecutor taskExecutor = context.getBean("applicationTaskExecutor",
                      SimpleAsyncTaskExecutor.class);
              assertThat(virtualThreadName(taskExecutor)).startsWith("custom-");
            });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void whenVirtualThreadsAreAvailableButNotEnabledThenThreadPoolTaskExecutorIsAutoConfigured() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(Executor.class).hasBean("applicationTaskExecutor");
      assertThat(context).getBean("applicationTaskExecutor").isInstanceOf(ThreadPoolTaskExecutor.class);
    });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void whenTaskDecoratorIsDefinedThenSimpleAsyncTaskExecutorWithVirtualThreadsUsesIt() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true")
            .withUserConfiguration(TaskDecoratorConfig.class)
            .run((context) -> {
              SimpleAsyncTaskExecutor executor = context.getBean(SimpleAsyncTaskExecutor.class);
              assertThat(executor).extracting("taskDecorator").isSameAs(context.getBean(TaskDecorator.class));
            });
  }

  @Test
  void simpleAsyncTaskExecutorBuilderUsesPlatformThreadsByDefault() {
    this.contextRunner.run((context) -> {
      SimpleAsyncTaskExecutorBuilder builder = context.getBean(SimpleAsyncTaskExecutorBuilder.class);
      assertThat(builder).hasFieldOrPropertyWithValue("virtualThreads", null);
    });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void simpleAsyncTaskExecutorBuilderUsesVirtualThreadsWhenEnabled() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true").run((context) -> {
      SimpleAsyncTaskExecutorBuilder builder = context.getBean(SimpleAsyncTaskExecutorBuilder.class);
      assertThat(builder).hasFieldOrPropertyWithValue("virtualThreads", true);
    });
  }

  @Test
  void taskExecutorWhenHasCustomTaskExecutorShouldBackOff() {
    this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new).run((context) -> {
      assertThat(context).hasSingleBean(Executor.class);
      assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
    });
  }

  @Test
  void taskExecutorWhenModeIsAutoAndHasCustomTaskExecutorShouldBackOff() {
    this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new)
            .withPropertyValues("infra.task.execution.mode=auto")
            .run((context) -> {
              assertThat(context).hasSingleBean(Executor.class);
              assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
            });
  }

  @Test
  void taskExecutorWhenModeIsForceAndHasCustomTaskExecutorShouldCreateApplicationTaskExecutor() {
    this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new)
            .withPropertyValues("infra.task.execution.mode=force")
            .run((context) -> assertThat(context.getBeansOfType(Executor.class)).hasSize(2)
                    .containsKeys("customTaskExecutor", "applicationTaskExecutor"));
  }

  @Test
  void taskExecutorWhenModeIsForceAndHasCustomTaskExecutorWithReservedNameShouldThrowException() {
    this.contextRunner.withBean("applicationTaskExecutor", Executor.class, SyncTaskExecutor::new)
            .withPropertyValues("infra.task.execution.mode=force")
            .run((context) -> assertThat(context).hasFailed()
                    .getFailure()
                    .isInstanceOf(BeanDefinitionOverrideException.class));
  }

  @Test
  void taskExecutorWhenModeIsForceAndHasCustomBFPPCanRestoreTaskExecutorAlias() {
    this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new)
            .withPropertyValues("infra.task.execution.mode=force")
            .withBean(BeanFactoryPostProcessor.class,
                    () -> (beanFactory) -> beanFactory.registerAlias("applicationTaskExecutor", "taskExecutor"))
            .run((context) -> {
              assertThat(context.getBeansOfType(Executor.class)).hasSize(2)
                      .containsKeys("customTaskExecutor", "applicationTaskExecutor");
              assertThat(context).hasBean("taskExecutor");
              assertThat(context.getBean("taskExecutor")).isSameAs(context.getBean("applicationTaskExecutor"));

            });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void whenVirtualThreadsAreEnabledAndCustomTaskExecutorIsDefinedThenSimpleAsyncTaskExecutorThatUsesVirtualThreadsBacksOff() {
    this.contextRunner.withBean("customTaskExecutor", Executor.class, SyncTaskExecutor::new)
            .withPropertyValues("infra.threads.virtual.enabled=true")
            .run((context) -> {
              assertThat(context).hasSingleBean(Executor.class);
              assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
            });
  }

  @Test
  void enableAsyncUsesAutoConfiguredOneByDefault() {
    this.contextRunner.withPropertyValues("infra.task.execution.thread-name-prefix=auto-task-")
            .withUserConfiguration(AsyncConfiguration.class, TestBean.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(AsyncConfigurer.class);
              assertThat(context).hasSingleBean(TaskExecutor.class);
              TestBean bean = context.getBean(TestBean.class);
              String text = bean.echo("something").get();
              assertThat(text).contains("auto-task-").contains("something");
            });
  }

  @Test
  void enableAsyncUsesCustomExecutorIfPresent() {
    this.contextRunner.withPropertyValues("infra.task.execution.thread-name-prefix=auto-task-")
            .withBean("customTaskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"))
            .withUserConfiguration(AsyncConfiguration.class, TestBean.class)
            .run((context) -> {
              assertThat(context).doesNotHaveBean(AsyncConfigurer.class);
              assertThat(context).hasSingleBean(Executor.class);
              TestBean bean = context.getBean(TestBean.class);
              String text = bean.echo("something").get();
              assertThat(text).contains("custom-task-").contains("something");
            });
  }

  @Test
  void enableAsyncUsesAutoConfiguredExecutorWhenModeIsForceAndHasCustomTaskExecutor() {
    this.contextRunner
            .withPropertyValues("infra.task.execution.thread-name-prefix=auto-task-",
                    "infra.task.execution.mode=force")
            .withBean("customTaskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"))
            .withUserConfiguration(AsyncConfiguration.class, TestBean.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(AsyncConfigurer.class);
              assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
              TestBean bean = context.getBean(TestBean.class);
              String text = bean.echo("something").get();
              assertThat(text).contains("auto-task-").contains("something");
            });
  }

  @Test
  void enableAsyncUsesAutoConfiguredExecutorWhenModeIsForceAndHasCustomTaskExecutorWithReservedName() {
    this.contextRunner
            .withPropertyValues("infra.task.execution.thread-name-prefix=auto-task-",
                    "infra.task.execution.mode=force")
            .withBean("taskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"))
            .withUserConfiguration(AsyncConfiguration.class, TestBean.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(AsyncConfigurer.class);
              assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
              TestBean bean = context.getBean(TestBean.class);
              String text = bean.echo("something").get();
              assertThat(text).contains("auto-task-").contains("something");
            });
  }

  @Test
  void enableAsyncUsesAsyncConfigurerWhenModeIsForce() {
    this.contextRunner
            .withPropertyValues("infra.task.execution.thread-name-prefix=auto-task-",
                    "infra.task.execution.mode=force")
            .withBean("taskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"))
            .withBean("customAsyncConfigurer", AsyncConfigurer.class, () -> new AsyncConfigurer() {
              @Override
              public Executor getAsyncExecutor() {
                return createCustomAsyncExecutor("async-task-");
              }
            })
            .withUserConfiguration(AsyncConfiguration.class, TestBean.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(AsyncConfigurer.class);
              assertThat(context.getBeansOfType(Executor.class)).hasSize(2)
                      .containsOnlyKeys("taskExecutor", "applicationTaskExecutor");
              TestBean bean = context.getBean(TestBean.class);
              String text = bean.echo("something").get();
              assertThat(text).contains("async-task-").contains("something");
            });
  }

  @Test
  void enableAsyncUsesAutoConfiguredExecutorWhenModeIsForceAndHasPrimaryCustomTaskExecutor() {
    this.contextRunner
            .withPropertyValues("infra.task.execution.thread-name-prefix=auto-task-",
                    "infra.task.execution.mode=force")
            .withBean("taskExecutor", Executor.class, () -> createCustomAsyncExecutor("custom-task-"),
                    (bd) -> bd.setPrimary(true))
            .withUserConfiguration(AsyncConfiguration.class, TestBean.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(AsyncConfigurer.class);
              assertThat(context.getBeansOfType(Executor.class)).hasSize(2);
              TestBean bean = context.getBean(TestBean.class);
              String text = bean.echo("something").get();
              assertThat(text).contains("auto-task-").contains("something");
            });
  }

  private Executor createCustomAsyncExecutor(String threadNamePrefix) {
    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
    executor.setThreadNamePrefix(threadNamePrefix);
    return executor;
  }

  @Test
  void enableAsyncUsesAutoConfiguredOneByDefaultEvenThoughSchedulingIsConfigured() {
    this.contextRunner.withPropertyValues("infra.task.execution.thread-name-prefix=auto-task-")
            .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
            .withUserConfiguration(AsyncConfiguration.class, SchedulingConfiguration.class, TestBean.class)
            .run((context) -> {
              TestBean bean = context.getBean(TestBean.class);
              String text = bean.echo("something").get();
              assertThat(text).contains("auto-task-").contains("something");
            });
  }

  private ContextConsumer<AssertableApplicationContext> assertThreadPoolTaskExecutor(
          Consumer<ThreadPoolTaskExecutor> taskExecutor) {
    return (context) -> {
      assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
      ThreadPoolTaskExecutorBuilder builder = context.getBean(ThreadPoolTaskExecutorBuilder.class);
      taskExecutor.accept(builder.build());
    };
  }

  private ContextConsumer<AssertableApplicationContext> assertSimpleAsyncTaskExecutor(
          Consumer<SimpleAsyncTaskExecutor> taskExecutor) {
    return (context) -> {
      assertThat(context).hasSingleBean(SimpleAsyncTaskExecutorBuilder.class);
      SimpleAsyncTaskExecutorBuilder builder = context.getBean(SimpleAsyncTaskExecutorBuilder.class);
      taskExecutor.accept(builder.build());
    };
  }

  private String virtualThreadName(SimpleAsyncTaskExecutor taskExecutor) throws InterruptedException {
    AtomicReference<Thread> threadReference = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    taskExecutor.execute(() -> {
      Thread currentThread = Thread.currentThread();
      threadReference.set(currentThread);
      latch.countDown();
    });
    assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
    Thread thread = threadReference.get();
    assertThat(thread).extracting("virtual").as("%s is virtual", thread).isEqualTo(true);
    return thread.getName();
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomThreadPoolTaskExecutorBuilderConfig {

    private final ThreadPoolTaskExecutorBuilder builder = new ThreadPoolTaskExecutorBuilder();

    @Bean
    ThreadPoolTaskExecutorBuilder customThreadPoolTaskExecutorBuilder() {
      return this.builder;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TaskDecoratorConfig {

    @Bean
    TaskDecorator mockTaskDecorator() {
      return mock(TaskDecorator.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableAsync
  static class AsyncConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class SchedulingConfiguration {

  }

  static class TestBean {

    @Async
    public Future<String> echo(String text) {
      return CompletableFuture.completedFuture(Thread.currentThread().getName() + " " + text);
    }

  }

}