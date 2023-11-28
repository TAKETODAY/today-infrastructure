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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnThreading}.
 *
 * @author Moritz Halbritter
 */
class ConditionalOnThreadingTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(BasicConfiguration.class);

  @Test
  @EnabledForJreRange(max = JRE.JAVA_20)
  void platformThreadsOnJdkBelow21IfVirtualThreadsPropertyIsEnabled() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true")
            .run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
  }

  @Test
  @EnabledForJreRange(max = JRE.JAVA_20)
  void platformThreadsOnJdkBelow21IfVirtualThreadsPropertyIsDisabled() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=false")
            .run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void virtualThreadsOnJdk21IfVirtualThreadsPropertyIsEnabled() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true")
            .run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.VIRTUAL));
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void platformThreadsOnJdk21IfVirtualThreadsPropertyIsDisabled() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=false")
            .run((context) -> assertThat(context.getBean(ThreadType.class)).isEqualTo(ThreadType.PLATFORM));
  }

  private enum ThreadType {

    PLATFORM, VIRTUAL

  }

  @Configuration(proxyBeanMethods = false)
  static class BasicConfiguration {

    @Bean
    @ConditionalOnThreading(Threading.VIRTUAL)
    ThreadType virtual() {
      return ThreadType.VIRTUAL;
    }

    @Bean
    @ConditionalOnThreading(Threading.PLATFORM)
    ThreadType platform() {
      return ThreadType.PLATFORM;
    }

  }

}
