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

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.core.type.AnnotatedTypeMetadata;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoneNestedConditions}.
 */
class NoneNestedConditionsTests {

  @Test
  void neither() {
    AnnotationConfigApplicationContext context = load(Config.class);
    assertThat(context.containsBean("myBean")).isTrue();
    context.close();
  }

  @Test
  void propertyA() {
    AnnotationConfigApplicationContext context = load(Config.class, "a:a");
    assertThat(context.containsBean("myBean")).isFalse();
    context.close();
  }

  @Test
  void propertyB() {
    AnnotationConfigApplicationContext context = load(Config.class, "b:b");
    assertThat(context.containsBean("myBean")).isFalse();
    context.close();
  }

  @Test
  void both() {
    AnnotationConfigApplicationContext context = load(Config.class, "a:a", "b:b");
    assertThat(context.containsBean("myBean")).isFalse();
    context.close();
  }

  private AnnotationConfigApplicationContext load(Class<?> config, String... env) {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    TestPropertyValues.of(env).applyTo(context);
    context.register(config);
    context.refresh();
    return context;
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(NeitherPropertyANorPropertyBCondition.class)
  static class Config {

    @Bean
    String myBean() {
      return "myBean";
    }

  }

  static class NeitherPropertyANorPropertyBCondition extends NoneNestedConditions {

    NeitherPropertyANorPropertyBCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty("a")
    static class HasPropertyA {

    }

    @ConditionalOnProperty("b")
    static class HasPropertyB {

    }

    @Conditional(NonInfraCondition.class)
    static class SubClassC {

    }

  }

  static class NonInfraCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

  }

}
