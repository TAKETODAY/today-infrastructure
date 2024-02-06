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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.test.classpath.ClassPathExclusions;
import cn.taketoday.test.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionalOnCheckpointRestoreTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(BasicConfiguration.class);

  @Test
  @ClassPathExclusions("crac-*.jar")
  void whenCracIsUnavailableThenConditionDoesNotMatch() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("someBean"));
  }

  @Test
  @ClassPathOverrides("org.crac:crac:1.3.0")
  void whenCracIsAvailableThenConditionMatches() {
    this.contextRunner.run((context) -> assertThat(context).hasBean("someBean"));
  }

  @Configuration(proxyBeanMethods = false)
  static class BasicConfiguration {

    @Bean
    @ConditionalOnCheckpointRestore
    String someBean() {
      return "someBean";
    }

  }

}