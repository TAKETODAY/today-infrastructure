/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.scheduling.config;

import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.TriggerContext;
import cn.taketoday.scheduling.support.ScheduledMethodRunnable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * @author Mark Fisher
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:00
 */
@SuppressWarnings("unchecked")
public class ScheduledTasksBeanDefinitionParserTests {

  private ApplicationContext context;

  private ScheduledTaskRegistrar registrar;

  private Object testBean;

  @BeforeEach
  void setup() {
    this.context = new ClassPathXmlApplicationContext(
            "scheduledTasksContext.xml", ScheduledTasksBeanDefinitionParserTests.class);
    this.registrar = this.context.getBeansOfType(
            ScheduledTaskRegistrar.class).values().iterator().next();
    this.testBean = this.context.getBean("testBean");
  }

  @Test
  void checkScheduler() {
    Object schedulerBean = this.context.getBean("testScheduler");
    Object schedulerRef = new DirectFieldAccessor(this.registrar).getPropertyValue("taskScheduler");
    assertThat(schedulerRef).isEqualTo(schedulerBean);
  }

  @Test
  void checkTarget() {
    List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("fixedRateTasks");
    Runnable runnable = tasks.get(0).getRunnable();

    ObjectAssert<ScheduledMethodRunnable> runnableAssert = assertThat(runnable)
            .extracting("runnable")
            .isInstanceOf(ScheduledMethodRunnable.class)
            .asInstanceOf(type(ScheduledMethodRunnable.class));
    runnableAssert.extracting("target").isEqualTo(testBean);
    runnableAssert.extracting("method.name").isEqualTo("test");
  }

  @Test
  void fixedRateTasks() {
    List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("fixedRateTasks");
    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getIntervalDuration()).isEqualTo(Duration.ofMillis(1000L));
    assertThat(tasks.get(1).getIntervalDuration()).isEqualTo(Duration.ofMillis(2000L));
    assertThat(tasks.get(2).getIntervalDuration()).isEqualTo(Duration.ofMillis(4000L));
    assertThat(tasks.get(2).getInitialDelayDuration()).isEqualTo(Duration.ofMillis(500));
  }

  @Test
  void fixedDelayTasks() {
    List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("fixedDelayTasks");
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getIntervalDuration()).isEqualTo(Duration.ofMillis(3000L));
    assertThat(tasks.get(1).getIntervalDuration()).isEqualTo(Duration.ofMillis(3500L));
    assertThat(tasks.get(1).getInitialDelayDuration()).isEqualTo(Duration.ofMillis(250));
  }

  @Test
  void cronTasks() {
    List<CronTask> tasks = (List<CronTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("cronTasks");
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getExpression()).isEqualTo("*/4 * 9-17 * * MON-FRI");
  }

  @Test
  void triggerTasks() {
    List<TriggerTask> tasks = (List<TriggerTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("triggerTasks");
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getTrigger()).isInstanceOf(TestTrigger.class);
  }

  static class TestBean {

    public void test() {
    }
  }

  static class TestTrigger implements Trigger {

    @Override
    public Instant nextExecution(TriggerContext triggerContext) {
      return null;
    }
  }

}
