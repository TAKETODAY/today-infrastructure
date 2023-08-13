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

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.core.task.SimpleAsyncTaskExecutor;
import cn.taketoday.core.task.SyncTaskExecutor;
import cn.taketoday.core.task.TaskDecorator;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ContextConsumer;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.scheduling.annotation.Async;
import cn.taketoday.scheduling.annotation.EnableAsync;
import cn.taketoday.scheduling.annotation.EnableScheduling;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.scheduling.support.SimpleAsyncTaskExecutorBuilder;
import cn.taketoday.scheduling.support.TaskExecutorBuilder;
import cn.taketoday.scheduling.support.TaskExecutorCustomizer;
import cn.taketoday.scheduling.support.ThreadPoolTaskExecutorBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
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
      assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
      assertThat(context).hasSingleBean(ThreadPoolTaskExecutorBuilder.class);
      assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
      assertThat(context).hasSingleBean(SimpleAsyncTaskExecutorBuilder.class);
    });
  }

  @Test
  void shouldNotSupplyThreadPoolTaskExecutorBuilderIfCustomTaskExecutorBuilderIsPresent() {
    this.contextRunner.withBean(TaskExecutorBuilder.class, TaskExecutorBuilder::new).run((context) -> {
      assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
      assertThat(context).doesNotHaveBean(ThreadPoolTaskExecutorBuilder.class);
      assertThat(context).hasSingleBean(ThreadPoolTaskExecutor.class);
    });
  }

  @Test
  void taskExecutorBuilderShouldApplyCustomSettings() {
    this.contextRunner.withPropertyValues("infra.task.execution.pool.queue-capacity=10",
        "infra.task.execution.pool.core-size=2",
        "infra.task.execution.pool.max-size=4",
        "infra.task.execution.pool.allow-core-thread-timeout=true",
        "infra.task.execution.pool.keep-alive=5s",
        "infra.task.execution.shutdown.await-termination=true",
        "infra.task.execution.shutdown.await-termination-period=30s",
        "infra.task.execution.thread-name-prefix=mytest-").run(assertTaskExecutor((taskExecutor) -> {
      assertThat(taskExecutor).hasFieldOrPropertyWithValue("queueCapacity", 10);
      assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
      assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
      assertThat(taskExecutor).hasFieldOrPropertyWithValue("allowCoreThreadTimeOut", true);
      assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(5);
      assertThat(taskExecutor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
      assertThat(taskExecutor).hasFieldOrPropertyWithValue("awaitTerminationMillis", 30000L);
      assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
    }));
  }

  @Test
  void simpleAsyncTaskExecutorBuilderShouldReadProperties() {
    this.contextRunner
        .withPropertyValues("infra.task.execution.thread-name-prefix=mytest-",
            "infra.task.execution.simple.concurrency-limit=1")
        .run(assertSimpleAsyncTaskExecutor((taskExecutor) -> {
          assertThat(taskExecutor.getConcurrencyLimit()).isEqualTo(1);
          assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
        }));
  }

  @Test
  void threadPoolTaskExecutorBuilderShouldApplyCustomSettings() {
    this.contextRunner
        .withPropertyValues("infra.task.execution.pool.queue-capacity=10",
            "infra.task.execution.pool.core-size=2", "infra.task.execution.pool.max-size=4",
            "infra.task.execution.pool.allow-core-thread-timeout=true",
            "infra.task.execution.pool.keep-alive=5s", "infra.task.execution.shutdown.await-termination=true",
            "infra.task.execution.shutdown.await-termination-period=30s",
            "infra.task.execution.thread-name-prefix=mytest-")
        .run(assertThreadPoolTaskExecutor((taskExecutor) -> {
          assertThat(taskExecutor).hasFieldOrPropertyWithValue("queueCapacity", 10);
          assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
          assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
          assertThat(taskExecutor).hasFieldOrPropertyWithValue("allowCoreThreadTimeOut", true);
          assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(5);
          assertThat(taskExecutor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
          assertThat(taskExecutor).hasFieldOrPropertyWithValue("awaitTerminationMillis", 30000L);
          assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
        }));
  }

  @Test
  void taskExecutorBuilderWhenHasCustomBuilderShouldUseCustomBuilder() {
    this.contextRunner.withUserConfiguration(CustomTaskExecutorBuilderConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
      assertThat(context.getBean(TaskExecutorBuilder.class))
          .isSameAs(context.getBean(CustomTaskExecutorBuilderConfig.class).taskExecutorBuilder);
    });
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
  void taskExecutorBuilderShouldUseTaskDecorator() {
    this.contextRunner.withUserConfiguration(TaskDecoratorConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
      ThreadPoolTaskExecutor executor = context.getBean(TaskExecutorBuilder.class).build();
      assertThat(executor).extracting("taskDecorator").isSameAs(context.getBean(TaskDecorator.class));
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
    this.contextRunner.withUserConfiguration(CustomTaskExecutorConfig.class).run((context) -> {
      assertThat(context).hasSingleBean(Executor.class);
      assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
    });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void whenVirtualThreadsAreEnabledAndCustomTaskExecutorIsDefinedThenSimpleAsyncTaskExecutorThatUsesVirtualThreadsBacksOff() {
    this.contextRunner.withUserConfiguration(CustomTaskExecutorConfig.class)
        .withPropertyValues("infra.threads.virtual.enabled=true")
        .run((context) -> {
          assertThat(context).hasSingleBean(Executor.class);
          assertThat(context.getBean(Executor.class)).isSameAs(context.getBean("customTaskExecutor"));
        });
  }

  @Test
  void taskExecutorBuilderShouldApplyCustomizer() {
    this.contextRunner.withUserConfiguration(TaskExecutorCustomizerConfig.class).run((context) -> {
      TaskExecutorCustomizer customizer = context.getBean(TaskExecutorCustomizer.class);
      ThreadPoolTaskExecutor executor = context.getBean(TaskExecutorBuilder.class).build();
      then(customizer).should().customize(executor);
    });
  }

  @Test
  void threadPoolTaskExecutorBuilderShouldApplyCustomizer() {
    this.contextRunner.withUserConfiguration(TaskExecutorCustomizerConfig.class).run((context) -> {
      TaskExecutorCustomizer customizer = context.getBean(TaskExecutorCustomizer.class);
      ThreadPoolTaskExecutor executor = context.getBean(ThreadPoolTaskExecutorBuilder.class).build();
      then(customizer).should().customize(executor);
    });
  }

  @Test
  void enableAsyncUsesAutoConfiguredOneByDefault() {
    this.contextRunner.withPropertyValues("infra.task.execution.thread-name-prefix=task-test-")
        .withUserConfiguration(AsyncConfiguration.class, TestBean.class)
        .run((context) -> {
          assertThat(context).hasSingleBean(TaskExecutor.class);
          TestBean bean = context.getBean(TestBean.class);
          String text = bean.echo("something").get();
          assertThat(text).contains("task-test-").contains("something");
        });
  }

  @Test
  void enableAsyncUsesAutoConfiguredOneByDefaultEvenThoughSchedulingIsConfigured() {
    this.contextRunner.withPropertyValues("infra.task.execution.thread-name-prefix=task-test-")
        .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
        .withUserConfiguration(AsyncConfiguration.class, SchedulingConfiguration.class, TestBean.class)
        .run((context) -> {
          TestBean bean = context.getBean(TestBean.class);
          String text = bean.echo("something").get();
          assertThat(text).contains("task-test-").contains("something");
        });
  }

  @Test
  void customTaskExecutorBuilderOverridesThreadPoolTaskExecutorBuilder() {
    this.contextRunner.withUserConfiguration(CustomTaskExecutorBuilderConfig.class).run((context) -> {
      ThreadPoolTaskExecutor bean = context.getBean(ThreadPoolTaskExecutor.class);
      assertThat(bean.getThreadNamePrefix()).isEqualTo("CustomTaskExecutorBuilderConfig-");
    });
  }

  @Test
  void threadPoolTaskExecutorBuilderAppliesTaskExecutorCustomizer() {
    this.contextRunner
        .withBean(TaskExecutorCustomizer.class,
            () -> (taskExecutor) -> taskExecutor.setThreadNamePrefix("custom-prefix-"))
        .run((context) -> {
          ThreadPoolTaskExecutor bean = context.getBean(ThreadPoolTaskExecutor.class);
          assertThat(bean.getThreadNamePrefix()).isEqualTo("custom-prefix-");
        });
  }

  private ContextConsumer<AssertableApplicationContext> assertTaskExecutor(
      Consumer<ThreadPoolTaskExecutor> taskExecutor) {
    return (context) -> {
      assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
      TaskExecutorBuilder builder = context.getBean(TaskExecutorBuilder.class);
      taskExecutor.accept(builder.build());
    };
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
  static class CustomTaskExecutorBuilderConfig {

    private final TaskExecutorBuilder taskExecutorBuilder = new TaskExecutorBuilder()
        .threadNamePrefix("CustomTaskExecutorBuilderConfig-");

    @Bean
    TaskExecutorBuilder customTaskExecutorBuilder() {
      return this.taskExecutorBuilder;
    }

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
  static class TaskExecutorCustomizerConfig {

    @Bean
    TaskExecutorCustomizer mockTaskExecutorCustomizer() {
      return mock(TaskExecutorCustomizer.class);
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
  static class CustomTaskExecutorConfig {

    @Bean
    Executor customTaskExecutor() {
      return new SyncTaskExecutor();
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
    Future<String> echo(String text) {
      return CompletableFuture.completedFuture(Thread.currentThread().getName() + " " + text);
    }

  }

}