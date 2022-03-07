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

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:01
 */
public class SchedulerBeanDefinitionParserTests {

  private ApplicationContext context;

  @BeforeEach
  public void setup() {
    this.context = new ClassPathXmlApplicationContext(
            "schedulerContext.xml", SchedulerBeanDefinitionParserTests.class);
  }

  @Test
  public void defaultScheduler() {
    ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) this.context.getBean("defaultScheduler");
    Integer size = (Integer) new DirectFieldAccessor(scheduler).getPropertyValue("poolSize");
    assertThat(size).isEqualTo(1);
  }

  @Test
  public void customScheduler() {
    ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) this.context.getBean("customScheduler");
    Integer size = (Integer) new DirectFieldAccessor(scheduler).getPropertyValue("poolSize");
    assertThat(size).isEqualTo(42);
  }

  @Test
  public void threadNamePrefix() {
    ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) this.context.getBean("customScheduler");
    assertThat(scheduler.getThreadNamePrefix()).isEqualTo("customScheduler-");
  }

}

