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

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.scheduling.annotation.Scheduled;
import cn.taketoday.scheduling.annotation.Schedules;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScheduledBeanLazyInitializationExcludeFilter}.
 *
 * @author Stephane Nicoll
 */
class ScheduledBeanLazyInitializationExcludeFilterTests {

  private final ScheduledBeanLazyInitializationExcludeFilter filter = new ScheduledBeanLazyInitializationExcludeFilter();

  @Test
  void beanWithScheduledMethodIsDetected() {
    assertThat(isExcluded(TestBean.class)).isTrue();
  }

  @Test
  void beanWithSchedulesMethodIsDetected() {
    assertThat(isExcluded(AnotherTestBean.class)).isTrue();
  }

  @Test
  void beanWithoutScheduledMethodIsDetected() {
    assertThat(isExcluded(ScheduledBeanLazyInitializationExcludeFilterTests.class)).isFalse();
  }

  private boolean isExcluded(Class<?> type) {
    return this.filter.isExcluded("test", new RootBeanDefinition(type), type);
  }

  private static class TestBean {

    @Scheduled
    void doStuff() {
    }

  }

  private static class AnotherTestBean {

    @Schedules({ @Scheduled(fixedRate = 5000), @Scheduled(fixedRate = 2500) })
    void doStuff() {
    }

  }

}
