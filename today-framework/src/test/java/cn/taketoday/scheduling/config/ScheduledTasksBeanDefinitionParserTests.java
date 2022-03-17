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

package cn.taketoday.scheduling.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.scheduling.Trigger;
import cn.taketoday.scheduling.TriggerContext;
import cn.taketoday.scheduling.support.ScheduledMethodRunnable;

import static org.assertj.core.api.Assertions.assertThat;

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
  public void setup() {
    this.context = new ClassPathXmlApplicationContext(
            "scheduledTasksContext.xml", ScheduledTasksBeanDefinitionParserTests.class);
    this.registrar = this.context.getBeansOfType(
            ScheduledTaskRegistrar.class).values().iterator().next();
    this.testBean = this.context.getBean("testBean");
  }

  @Test
  public void checkScheduler() {
    Object schedulerBean = this.context.getBean("testScheduler");
    Object schedulerRef = new DirectFieldAccessor(this.registrar).getPropertyValue("taskScheduler");
    assertThat(schedulerRef).isEqualTo(schedulerBean);
  }

  @Test
  public void checkTarget() {
    List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("fixedRateTasks");
    Runnable runnable = tasks.get(0).getRunnable();
    assertThat(runnable.getClass()).isEqualTo(ScheduledMethodRunnable.class);
    Object targetObject = ((ScheduledMethodRunnable) runnable).getTarget();
    Method targetMethod = ((ScheduledMethodRunnable) runnable).getMethod();
    assertThat(targetObject).isEqualTo(this.testBean);
    assertThat(targetMethod.getName()).isEqualTo("test");
  }

  @Test
  public void fixedRateTasks() {
    List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("fixedRateTasks");
    assertThat(tasks.size()).isEqualTo(3);
    assertThat(tasks.get(0).getInterval()).isEqualTo(1000L);
    assertThat(tasks.get(1).getInterval()).isEqualTo(2000L);
    assertThat(tasks.get(2).getInterval()).isEqualTo(4000L);
    assertThat(tasks.get(2).getInitialDelay()).isEqualTo(500);
  }

  @Test
  public void fixedDelayTasks() {
    List<IntervalTask> tasks = (List<IntervalTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("fixedDelayTasks");
    assertThat(tasks.size()).isEqualTo(2);
    assertThat(tasks.get(0).getInterval()).isEqualTo(3000L);
    assertThat(tasks.get(1).getInterval()).isEqualTo(3500L);
    assertThat(tasks.get(1).getInitialDelay()).isEqualTo(250);
  }

  @Test
  public void cronTasks() {
    List<CronTask> tasks = (List<CronTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("cronTasks");
    assertThat(tasks.size()).isEqualTo(1);
    assertThat(tasks.get(0).getExpression()).isEqualTo("*/4 * 9-17 * * MON-FRI");
  }

  @Test
  public void triggerTasks() {
    List<TriggerTask> tasks = (List<TriggerTask>) new DirectFieldAccessor(
            this.registrar).getPropertyValue("triggerTasks");
    assertThat(tasks.size()).isEqualTo(1);
    assertThat(tasks.get(0).getTrigger()).isInstanceOf(TestTrigger.class);
  }

  static class TestBean {

    public void test() {
    }
  }

  static class TestTrigger implements Trigger {

    @Override
    public Date nextExecutionTime(TriggerContext triggerContext) {
      return null;
    }
  }

}
