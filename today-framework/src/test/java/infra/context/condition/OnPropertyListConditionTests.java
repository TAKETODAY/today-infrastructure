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

package infra.context.condition;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OnPropertyListCondition}.
 *
 * @author Stephane Nicoll
 */
class OnPropertyListConditionTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(TestConfig.class);

  @Test
  void propertyNotDefined() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean("foo"));
  }

  @Test
  void propertyDefinedAsCommaSeparated() {
    this.contextRunner.withPropertyValues("spring.test.my-list=value1")
            .run((context) -> assertThat(context).hasBean("foo"));
  }

  @Test
  void propertyDefinedAsList() {
    this.contextRunner.withPropertyValues("spring.test.my-list[0]=value1")
            .run((context) -> assertThat(context).hasBean("foo"));
  }

  @Test
  void propertyDefinedAsCommaSeparatedRelaxed() {
    this.contextRunner.withPropertyValues("spring.test.myList=value1")
            .run((context) -> assertThat(context).hasBean("foo"));
  }

  @Test
  void propertyDefinedAsListRelaxed() {
    this.contextRunner.withPropertyValues("spring.test.myList[0]=value1")
            .run((context) -> assertThat(context).hasBean("foo"));
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(TestPropertyListCondition.class)
  static class TestConfig {

    @Bean
    String foo() {
      return "foo";
    }

  }

  static class TestPropertyListCondition extends OnPropertyListCondition {

    TestPropertyListCondition() {
      super("spring.test.my-list", () -> ConditionMessage.forCondition("test"));
    }

  }

}
