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

package cn.taketoday.scheduling.annotation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.core.testfixture.EnabledForTestGroups;
import cn.taketoday.scheduling.TaskScheduler;
import cn.taketoday.scheduling.concurrent.SimpleAsyncTaskScheduler;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskScheduler;
import cn.taketoday.scheduling.config.IntervalTask;
import cn.taketoday.scheduling.config.ScheduledTaskHolder;
import cn.taketoday.scheduling.config.ScheduledTaskRegistrar;
import cn.taketoday.scheduling.config.TaskManagementConfigUtils;

import static cn.taketoday.core.testfixture.TestGroup.LONG_RUNNING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests use of @EnableScheduling on @Configuration classes.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 4.0
 */
public class EnableSchedulingTests {

  private AnnotationConfigApplicationContext ctx;

  @AfterEach
  public void tearDown() {
    if (ctx != null) {
      ctx.close();
    }
  }

  /*
   * Tests compatibility between default executor in TaskSchedulerRouter
   * and explicit ThreadPoolTaskScheduler in configuration subclass.
   */
  @ParameterizedTest
  @ValueSource(classes = { FixedRateTaskConfig.class, FixedRateTaskConfigSubclass.class })
  @EnabledForTestGroups(LONG_RUNNING)
  public void withFixedRateTask(Class<?> configClass) throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(configClass);
    assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(2);

    Thread.sleep(110);
    assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThanOrEqualTo(10);
  }

  /*
   * Tests compatibility between SimpleAsyncTaskScheduler in regular configuration
   * and explicit ThreadPoolTaskScheduler in configuration subclass. This includes
   * pause/resume behavior and a controlled shutdown with a 1s termination timeout.
   */
  @ParameterizedTest
  @ValueSource(classes = { ExplicitSchedulerConfig.class, ExplicitSchedulerConfigSubclass.class })
  @Timeout(2)  // should actually complete within 1s
  @EnabledForTestGroups(LONG_RUNNING)
  public void withExplicitScheduler(Class<?> configClass) throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(configClass);
    assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

    Thread.sleep(110);
    ctx.stop();
    int count1 = ctx.getBean(AtomicInteger.class).get();
    assertThat(count1).isGreaterThanOrEqualTo(10).isLessThan(20);
    Thread.sleep(110);
    int count2 = ctx.getBean(AtomicInteger.class).get();
    assertThat(count2).isGreaterThanOrEqualTo(10).isLessThan(20);
    ctx.start();
    Thread.sleep(110);
    int count3 = ctx.getBean(AtomicInteger.class).get();
    assertThat(count3).isGreaterThanOrEqualTo(20);

    TaskExecutor executor = ctx.getBean(TaskExecutor.class);
    AtomicInteger count = new AtomicInteger(0);
    for (int i = 0; i < 2; i++) {
      executor.execute(() -> {
        try {
          Thread.sleep(10000);  // try to break test timeout
        }
        catch (InterruptedException ex) {
          // expected during executor shutdown
          try {
            Thread.sleep(500);
            // should get here within task termination timeout (1000)
            count.incrementAndGet();
          }
          catch (InterruptedException ex2) {
            // not expected
          }
        }
      });
    }

    assertThat(ctx.getBean(ExplicitSchedulerConfig.class).threadName).startsWith("explicitScheduler-");
    assertThat(Arrays.asList(ctx.getBeanFactory().getDependentBeans("myTaskScheduler"))
            .contains(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();

    // Include executor shutdown in test timeout (2 seconds),
    // expecting interruption of the sleeping thread...
    ctx.close();
    assertThat(count.intValue()).isEqualTo(2);
  }

  @Test
  public void withExplicitSchedulerAmbiguity_andSchedulingEnabled() {
    // No exception raised as of 4.3, aligned with the behavior for @Async methods (SPR-14030)
    ctx = new AnnotationConfigApplicationContext(AmbiguousExplicitSchedulerConfig.class);
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  public void withExplicitScheduledTaskRegistrar() throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(ExplicitScheduledTaskRegistrarConfig.class);
    assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

    Thread.sleep(110);
    assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThanOrEqualTo(10);
    assertThat(ctx.getBean(ExplicitScheduledTaskRegistrarConfig.class).threadName).startsWith("explicitScheduler1");
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  public void withQualifiedScheduler() throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(QualifiedExplicitSchedulerConfig.class);
    assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

    Thread.sleep(110);
    assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThanOrEqualTo(10);
    assertThat(ctx.getBean(QualifiedExplicitSchedulerConfig.class).threadName).startsWith("explicitScheduler1");
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  public void withQualifiedSchedulerAndPlaceholder() throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(QualifiedExplicitSchedulerConfigWithPlaceholder.class);
    assertThat(ctx.getBean(ScheduledTaskHolder.class).getScheduledTasks()).hasSize(1);

    Thread.sleep(110);
    assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThanOrEqualTo(10);
    assertThat(ctx.getBean(QualifiedExplicitSchedulerConfigWithPlaceholder.class).threadName).startsWith("explicitScheduler1");
  }

  @Test
  public void withAmbiguousTaskSchedulers_butNoActualTasks() {
    ctx = new AnnotationConfigApplicationContext(SchedulingEnabled_withAmbiguousTaskSchedulers_butNoActualTasks.class);
  }

  @Test
  public void withAmbiguousTaskSchedulers_andSingleTask() {
    // No exception raised as of 4.3, aligned with the behavior for @Async methods (SPR-14030)
    ctx = new AnnotationConfigApplicationContext(SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask.class);
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  public void withAmbiguousTaskSchedulers_andSingleTask_disambiguatedByScheduledTaskRegistrarBean() throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(
            SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedByScheduledTaskRegistrar.class);

    Thread.sleep(110);
    assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread).startsWith("explicitScheduler2-");
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  public void withAmbiguousTaskSchedulers_andSingleTask_disambiguatedBySchedulerNameAttribute() throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(
            SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedBySchedulerNameAttribute.class);

    Thread.sleep(110);
    assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread).startsWith("explicitScheduler2-");
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  public void withTaskAddedVia_configureTasks() throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(SchedulingEnabled_withTaskAddedVia_configureTasks.class);

    Thread.sleep(110);
    assertThat(ctx.getBean(ThreadAwareWorker.class).executedByThread).startsWith("taskScheduler-");
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  public void withTriggerTask() throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(TriggerTaskConfig.class);

    Thread.sleep(110);
    assertThat(ctx.getBean(AtomicInteger.class).get()).isGreaterThan(1);
  }

  @Test
  @EnabledForTestGroups(LONG_RUNNING)
  public void withInitiallyDelayedFixedRateTask() throws InterruptedException {
    ctx = new AnnotationConfigApplicationContext(FixedRateTaskConfig_withInitialDelay.class);

    Thread.sleep(1950);
    AtomicInteger counter = ctx.getBean(AtomicInteger.class);

    // The @Scheduled method should have been called at least once but
    // not more times than the delay allows.
    assertThat(counter.get()).isBetween(1, 10);
  }

  @Configuration
  @EnableScheduling
  static class FixedRateTaskConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
      taskRegistrar.addFixedRateTask(() -> { }, Duration.ofMillis(100));
    }

    @Bean
    public AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Scheduled(fixedRate = 10)
    public void task() {
      counter().incrementAndGet();
    }
  }

  @Configuration
  static class FixedRateTaskConfigSubclass extends FixedRateTaskConfig {

    @Bean
    public TaskScheduler taskScheduler() {
      return new ThreadPoolTaskScheduler();
    }
  }

  @Configuration
  @EnableScheduling
  static class ExplicitSchedulerConfig {

    String threadName;

    @Bean
    public TaskScheduler myTaskScheduler() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler-");
      scheduler.setTaskTerminationTimeout(1000);
      return scheduler;
    }

    @Bean
    public AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Scheduled(fixedRate = 10)
    public void task() {
      threadName = Thread.currentThread().getName();
      counter().incrementAndGet();
    }
  }

  @Configuration
  static class ExplicitSchedulerConfigSubclass extends ExplicitSchedulerConfig {

    @Bean
    @Override
    public TaskScheduler myTaskScheduler() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler-");
      scheduler.setAwaitTerminationMillis(1000);
      scheduler.setPoolSize(2);
      return scheduler;
    }
  }

  @Configuration
  @EnableScheduling
  static class AmbiguousExplicitSchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler1() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler1");
      return scheduler;
    }

    @Bean
    public TaskScheduler taskScheduler2() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler2");
      return scheduler;
    }

    @Scheduled(fixedRate = 10)
    public void task() {
    }
  }

  @Configuration
  @EnableScheduling
  static class ExplicitScheduledTaskRegistrarConfig implements SchedulingConfigurer {

    String threadName;

    @Bean
    public TaskScheduler taskScheduler1() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler1");
      return scheduler;
    }

    @Bean
    public TaskScheduler taskScheduler2() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler2");
      return scheduler;
    }

    @Bean
    public AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Scheduled(fixedRate = 10)
    public void task() {
      threadName = Thread.currentThread().getName();
      counter().incrementAndGet();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
      taskRegistrar.setScheduler(taskScheduler1());
    }
  }

  @Configuration
  @EnableScheduling
  static class QualifiedExplicitSchedulerConfig {

    String threadName;

    @Bean
    @Qualifier("myScheduler")
    public TaskScheduler taskScheduler1() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler1");
      return scheduler;
    }

    @Bean
    public TaskScheduler taskScheduler2() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler2");
      return scheduler;
    }

    @Bean
    public AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Scheduled(fixedRate = 10, scheduler = "myScheduler")
    public void task() {
      threadName = Thread.currentThread().getName();
      counter().incrementAndGet();
    }
  }

  @Configuration
  @EnableScheduling
  static class QualifiedExplicitSchedulerConfigWithPlaceholder {

    String threadName;

    @Bean
    @Qualifier("myScheduler")
    public TaskScheduler taskScheduler1() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler1");
      return scheduler;
    }

    @Bean
    public TaskScheduler taskScheduler2() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler2");
      return scheduler;
    }

    @Bean
    public AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Scheduled(fixedRate = 10, scheduler = "${scheduler}")
    public void task() {
      threadName = Thread.currentThread().getName();
      counter().incrementAndGet();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
      PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
      Properties props = new Properties();
      props.setProperty("scheduler", "myScheduler");
      pspc.setProperties(props);
      return pspc;
    }
  }

  @Configuration
  @EnableScheduling
  static class SchedulingEnabled_withAmbiguousTaskSchedulers_butNoActualTasks {

    @Bean
    public TaskScheduler taskScheduler1() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler1");
      return scheduler;
    }

    @Bean
    public TaskScheduler taskScheduler2() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler2");
      return scheduler;
    }
  }

  @Configuration
  @EnableScheduling
  static class SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask {

    @Scheduled(fixedRate = 10L)
    public void task() {
    }

    @Bean
    public TaskScheduler taskScheduler1() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler1");
      scheduler.setConcurrencyLimit(1);
      return scheduler;
    }

    @Bean
    public TaskScheduler taskScheduler2() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler2");
      return scheduler;
    }
  }

  static class ThreadAwareWorker {

    String executedByThread;
  }

  @Configuration
  @EnableScheduling
  static class SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedByScheduledTaskRegistrar implements SchedulingConfigurer {

    @Scheduled(fixedRate = 10)
    public void task() {
      worker().executedByThread = Thread.currentThread().getName();
    }

    @Bean
    public ThreadAwareWorker worker() {
      return new ThreadAwareWorker();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
      taskRegistrar.setScheduler(taskScheduler2());
    }

    @Bean
    public TaskScheduler taskScheduler1() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler1-");
      scheduler.setConcurrencyLimit(1);
      return scheduler;
    }

    @Bean
    public TaskScheduler taskScheduler2() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler2-");
      return scheduler;
    }
  }

  @Configuration
  @EnableScheduling
  static class SchedulingEnabled_withAmbiguousTaskSchedulers_andSingleTask_disambiguatedBySchedulerNameAttribute implements SchedulingConfigurer {

    @Scheduled(fixedRate = 10)
    public void task() {
      worker().executedByThread = Thread.currentThread().getName();
    }

    @Bean
    public ThreadAwareWorker worker() {
      return new ThreadAwareWorker();
    }

    @Bean
    public TaskScheduler taskScheduler1() {
      SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler1-");
      scheduler.setConcurrencyLimit(1);
      return scheduler;
    }

    @Bean
    public TaskScheduler taskScheduler2() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setThreadNamePrefix("explicitScheduler2-");
      return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
      taskRegistrar.setScheduler(taskScheduler2());
    }
  }

  @Configuration
  @EnableScheduling
  static class SchedulingEnabled_withTaskAddedVia_configureTasks implements SchedulingConfigurer {

    @Bean
    public ThreadAwareWorker worker() {
      return new ThreadAwareWorker();
    }

    @Bean
    public TaskScheduler taskScheduler() {
      return new ThreadPoolTaskScheduler();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
      taskRegistrar.setScheduler(taskScheduler());
      taskRegistrar.addFixedRateTask(new IntervalTask(
              () -> worker().executedByThread = Thread.currentThread().getName(),
              Duration.ofMillis(10)));
    }
  }

  @Configuration
  static class TriggerTaskConfig {

    @Bean
    public AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Bean
    public TaskScheduler scheduler() {
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.initialize();
      scheduler.schedule(() -> counter().incrementAndGet(),
              triggerContext -> Instant.now().plus(10, ChronoUnit.MILLIS));
      return scheduler;
    }
  }

  @Configuration
  @EnableScheduling
  static class FixedRateTaskConfig_withInitialDelay {

    @Bean
    public AtomicInteger counter() {
      return new AtomicInteger();
    }

    @Scheduled(initialDelay = 1000, fixedRate = 100)
    public void task() {
      counter().incrementAndGet();
    }
  }

}
