/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

    @Conditional(NonInfraCondition.class)
    static class SubclassC {

    }

  }

  static class NonInfraCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return true;
    }

  }

}
