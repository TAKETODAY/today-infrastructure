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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.framework.LazyInitializationBeanFactoryPostProcessor;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.annotation.EnableScheduling;
import cn.taketoday.scheduling.annotation.Scheduled;
import cn.taketoday.scheduling.annotation.SchedulingConfigurer;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskScheduler;
import cn.taketoday.scheduling.config.ScheduledTaskRegistrar;
import cn.taketoday.scheduling.support.SimpleAsyncTaskSchedulerBuilder;
import cn.taketoday.scheduling.support.SimpleAsyncTaskSchedulerCustomizer;
import cn.taketoday.scheduling.support.ThreadPoolTaskSchedulerBuilder;
import cn.taketoday.scheduling.support.ThreadPoolTaskSchedulerCustomizer;
import cn.taketoday.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TaskSchedulingAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class TaskSchedulingAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(TestConfiguration.class)
          .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class));

  @Test
  void noSchedulingDoesNotExposeTaskScheduler() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(TaskScheduler.class));
  }

  @Test
  void noSchedulingDoesNotExposeScheduledBeanLazyInitializationExcludeFilter() {
    this.contextRunner
            .run((context) -> assertThat(context).doesNotHaveBean(ScheduledBeanLazyInitializationExcludeFilter.class));
  }

  @Test
  void shouldSupplyBeans() {
    this.contextRunner.withUserConfiguration(SchedulingConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(ThreadPoolTaskSchedulerBuilder.class);
      assertThat(context).hasSingleBean(ThreadPoolTaskScheduler.class);
    });
  }

  @Test
  void shouldNotSupplyThreadPoolTaskSchedulerBuilderIfCustomTaskSchedulerBuilderIsPresent() {
    this.contextRunner.withUserConfiguration(SchedulingConfiguration.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(ThreadPoolTaskSchedulerBuilder.class);
              assertThat(context).hasSingleBean(ThreadPoolTaskScheduler.class);
            });
  }

  @Test
  void enableSchedulingWithNoTaskExecutorAutoConfiguresOne() {
    this.contextRunner
            .withPropertyValues("infra.task.scheduling.shutdown.await-termination=true",
                    "infra.task.scheduling.shutdown.await-termination-period=30s",
                    "infra.task.scheduling.thread-name-prefix=scheduling-test-")
            .withUserConfiguration(SchedulingConfiguration.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(TaskExecutor.class);
              TaskExecutor taskExecutor = context.getBean(TaskExecutor.class);
              TestBean bean = context.getBean(TestBean.class);
              assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
              assertThat(taskExecutor).hasFieldOrPropertyWithValue("waitForTasksToCompleteOnShutdown", true);
              assertThat(taskExecutor).hasFieldOrPropertyWithValue("awaitTerminationMillis", 30000L);
              assertThat(bean.threadNames).allMatch((name) -> name.contains("scheduling-test-"));
            });
  }

  @Test
  void simpleAsyncTaskSchedulerBuilderShouldReadProperties() {
    this.contextRunner.withPropertyValues("infra.task.scheduling.simple.concurrency-limit=1",
                    "infra.task.scheduling.thread-name-prefix=scheduling-test-",
                    "infra.task.scheduling.shutdown.await-termination=true",
                    "infra.task.scheduling.shutdown.await-termination-period=30s")
            .withUserConfiguration(SchedulingConfiguration.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(SimpleAsyncTaskSchedulerBuilder.class);
              SimpleAsyncTaskSchedulerBuilder builder = context.getBean(SimpleAsyncTaskSchedulerBuilder.class);
              assertThat(builder).hasFieldOrPropertyWithValue("threadNamePrefix", "scheduling-test-");
              assertThat(builder).hasFieldOrPropertyWithValue("concurrencyLimit", 1);
              assertThat(builder).hasFieldOrPropertyWithValue("taskTerminationTimeout", Duration.ofSeconds(30));
            });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void simpleAsyncTaskSchedulerBuilderShouldUseVirtualThreadsIfEnabled() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true")
            .withUserConfiguration(SchedulingConfiguration.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(SimpleAsyncTaskSchedulerBuilder.class);
              SimpleAsyncTaskSchedulerBuilder builder = context.getBean(SimpleAsyncTaskSchedulerBuilder.class);
              assertThat(builder).hasFieldOrPropertyWithValue("virtualThreads", true);
            });
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void simpleAsyncTaskSchedulerBuilderShouldUsePlatformThreadsByDefault() {
    this.contextRunner.withUserConfiguration(SchedulingConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(SimpleAsyncTaskSchedulerBuilder.class);
      SimpleAsyncTaskSchedulerBuilder builder = context.getBean(SimpleAsyncTaskSchedulerBuilder.class);
      assertThat(builder).hasFieldOrPropertyWithValue("virtualThreads", null);
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  void simpleAsyncTaskSchedulerBuilderShouldApplyCustomizers() {
    SimpleAsyncTaskSchedulerCustomizer customizer = (scheduler) -> {
    };
    this.contextRunner.withBean(SimpleAsyncTaskSchedulerCustomizer.class, () -> customizer)
            .withUserConfiguration(SchedulingConfiguration.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(SimpleAsyncTaskSchedulerBuilder.class);
              SimpleAsyncTaskSchedulerBuilder builder = context.getBean(SimpleAsyncTaskSchedulerBuilder.class);
              Set<SimpleAsyncTaskSchedulerCustomizer> customizers = (Set<SimpleAsyncTaskSchedulerCustomizer>) ReflectionTestUtils
                      .getField(builder, "customizers");
              assertThat(customizers).as("SimpleAsyncTaskSchedulerBuilder.customizers").contains(customizer);
            });
  }

  @Test
  void enableSchedulingWithNoTaskExecutorAppliesTaskSchedulerCustomizers() {
    this.contextRunner.withPropertyValues("infra.task.scheduling.thread-name-prefix=scheduling-test-")
            .withUserConfiguration(SchedulingConfiguration.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(TaskExecutor.class);
              TestBean bean = context.getBean(TestBean.class);
              assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
              assertThat(bean.threadNames).allMatch((name) -> name.contains("scheduling-test-"));
            });
  }

  @Test
  void enableSchedulingWithNoTaskExecutorAppliesCustomizers() {
    this.contextRunner.withPropertyValues("infra.task.scheduling.thread-name-prefix=scheduling-test-")
            .withUserConfiguration(SchedulingConfiguration.class, ThreadPoolTaskSchedulerCustomizerConfiguration.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(TaskExecutor.class);
              TestBean bean = context.getBean(TestBean.class);
              assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
              assertThat(bean.threadNames).allMatch((name) -> name.contains("customized-scheduler-"));
            });
  }

  @Test
  void enableSchedulingWithExistingTaskSchedulerBacksOff() {
    this.contextRunner.withUserConfiguration(SchedulingConfiguration.class, TaskSchedulerConfiguration.class)
            .run((context) -> {
              assertThat(context).hasSingleBean(TaskScheduler.class);
              assertThat(context.getBean(TaskScheduler.class)).isInstanceOf(TestTaskScheduler.class);
              TestBean bean = context.getBean(TestBean.class);
              assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
              assertThat(bean.threadNames).containsExactly("test-1");
            });
  }

  @Test
  void enableSchedulingWithExistingScheduledExecutorServiceBacksOff() {
    this.contextRunner
            .withUserConfiguration(SchedulingConfiguration.class, ScheduledExecutorServiceConfiguration.class)
            .run((context) -> {
              assertThat(context).doesNotHaveBean(TaskScheduler.class);
              assertThat(context).hasSingleBean(ScheduledExecutorService.class);
              TestBean bean = context.getBean(TestBean.class);
              assertThat(bean.latch.await(30, TimeUnit.SECONDS)).isTrue();
              assertThat(bean.threadNames).allMatch((name) -> name.contains("pool-"));
            });
  }

  @Test
  void enableSchedulingWithLazyInitializationInvokeScheduledMethods() {
    List<String> threadNames = new ArrayList<>();
    new ApplicationContextRunner()
            .withInitializer(
                    (context) -> context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
            .withPropertyValues("infra.task.scheduling.thread-name-prefix=scheduling-test-")
            .withBean(LazyTestBean.class, () -> new LazyTestBean(threadNames))
            .withUserConfiguration(SchedulingConfiguration.class)
            .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
            .run((context) -> {
              // No lazy lookup.
              Awaitility.waitAtMost(Duration.ofSeconds(3)).until(() -> !threadNames.isEmpty());
              assertThat(threadNames).allMatch((name) -> name.contains("scheduling-test-"));
            });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class SchedulingConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class TaskSchedulerConfiguration {

    @Bean
    TaskScheduler customTaskScheduler() {
      return new TestTaskScheduler();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ScheduledExecutorServiceConfiguration {

    @Bean
    ScheduledExecutorService customScheduledExecutorService() {
      return Executors.newScheduledThreadPool(2);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ThreadPoolTaskSchedulerCustomizerConfiguration {

    @Bean
    ThreadPoolTaskSchedulerCustomizer testTaskSchedulerCustomizer() {
      return ((taskScheduler) -> taskScheduler.setThreadNamePrefix("customized-scheduler-"));
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class SchedulingConfigurerConfiguration implements SchedulingConfigurer {

    private final TaskScheduler taskScheduler = new TestTaskScheduler();

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
      taskRegistrar.setScheduler(this.taskScheduler);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {

    @Bean
    TestBean testBean() {
      return new TestBean();
    }

  }

  static class TestBean {

    private final Set<String> threadNames = ConcurrentHashMap.newKeySet();

    private final CountDownLatch latch = new CountDownLatch(1);

    @Scheduled(fixedRate = 60000)
    void accumulate() {
      this.threadNames.add(Thread.currentThread().getName());
      this.latch.countDown();
    }

  }

  static class LazyTestBean {

    private final List<String> threadNames;

    LazyTestBean(List<String> threadNames) {
      this.threadNames = threadNames;
    }

    @Scheduled(fixedRate = 2000)
    void accumulate() {
      this.threadNames.add(Thread.currentThread().getName());
    }

  }

  static class TestTaskScheduler extends ThreadPoolTaskScheduler {

    TestTaskScheduler() {
      setPoolSize(1);
      setThreadNamePrefix("test-");
      afterPropertiesSet();
    }

  }

}
