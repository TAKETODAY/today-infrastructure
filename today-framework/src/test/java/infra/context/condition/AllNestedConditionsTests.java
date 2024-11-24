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

import infra.app.test.context.assertj.AssertableApplicationContext;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.app.test.context.runner.ContextConsumer;
import infra.context.annotation.Bean;
import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.condition.AllNestedConditions;
import infra.context.condition.ConditionalOnProperty;
import infra.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AllNestedConditions}.
 */
class AllNestedConditionsTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void neither() {
    this.contextRunner.withUserConfiguration(Config.class).run(match(false));
  }

  @Test
  void propertyA() {
    this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("a:a").run(match(false));
  }

  @Test
  void propertyB() {
    this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("b:b").run(match(false));
  }

  @Test
  void both() {
    this.contextRunner.withUserConfiguration(Config.class).withPropertyValues("a:a", "b:b").run(match(true));
  }

  private ContextConsumer<AssertableApplicationContext> match(boolean expected) {
    return (context) -> {
      if (expected) {
        assertThat(context).hasBean("myBean");
      }
      else {
        assertThat(context).doesNotHaveBean("myBean");
      }
    };
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(OnPropertyAAndBCondition.class)
  static class Config {

    @Bean
    String myBean() {
      return "myBean";
    }

  }

  static class OnPropertyAAndBCondition extends AllNestedConditions {

    OnPropertyAAndBCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty("a")
    static class HasPropertyA {

    }

    @ConditionalOnProperty("b")
    static class HasPropertyB {

    }

    @Conditional(NonSpringBootCondition.class)
    static class SubclassC {

    }

  }

  static class NonSpringBootCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return true;
    }

  }

}
