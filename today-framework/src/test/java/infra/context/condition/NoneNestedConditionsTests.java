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

    @Conditional(NonSpringBootCondition.class)
    static class SubClassC {

    }

  }

  static class NonSpringBootCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

  }

}
