/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.condition;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnVirtualThreads @ConditionalOnVirtualThreads}.
 *
 * @author Moritz Halbritter
 */
class ConditionalOnVirtualThreadsTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(BasicConfiguration.class);

  @Test
  @EnabledForJreRange(max = JRE.JAVA_20)
  void isDisabledOnJdkBelow21EvenIfPropertyIsSet() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true")
            .run((context) -> assertThat(context).doesNotHaveBean("someBean"));
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void isDisabledOnJdk21IfPropertyIsNotSet() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("someBean"));
  }

  @Test
  @EnabledForJreRange(min = JRE.JAVA_21)
  void isEnabledOnJdk21IfPropertyIsSet() {
    this.contextRunner.withPropertyValues("infra.threads.virtual.enabled=true")
            .run((context) -> assertThat(context).hasBean("someBean"));
  }

  @Configuration(proxyBeanMethods = false)
  static class BasicConfiguration {

    @Bean
    @ConditionalOnVirtualThreads
    String someBean() {
      return "someBean";
    }

  }

}
