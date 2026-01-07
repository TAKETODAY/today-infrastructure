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

package infra.annotation.config.task;

import org.junit.jupiter.api.Test;

import infra.beans.factory.support.RootBeanDefinition;
import infra.scheduling.annotation.Scheduled;
import infra.scheduling.annotation.Schedules;

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
