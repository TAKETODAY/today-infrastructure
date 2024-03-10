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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationCondition;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 */
class ConfigurationPhasesKnownSuperclassesTests {

  @Test
  void superclassSkippedInParseConfigurationPhaseShouldNotPreventSubsequentProcessingOfSameSuperclass() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ParseConfigurationPhase.class)) {
      assertThat(context.getBean("subclassBean")).isEqualTo("bravo");
      assertThat(context.getBean("superclassBean")).isEqualTo("superclass");
      assertThat(context.getBean("baseBean")).isEqualTo("base");
    }
  }

  @Test
  void superclassSkippedInRegisterBeanPhaseShouldNotPreventSubsequentProcessingOfSameSuperclass() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RegisterBeanPhase.class)) {
      assertThat(context.getBean("subclassBean")).isEqualTo("bravo");
      assertThat(context.getBean("superclassBean")).isEqualTo("superclass");
      assertThat(context.getBean("baseBean")).isEqualTo("base");
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class Base {

    @Bean
    String baseBean() {
      return "base";
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class Example extends Base {

    @Bean
    String superclassBean() {
      return "superclass";
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ RegisterBeanPhaseExample.class, BravoExample.class })
  static class RegisterBeanPhase {
  }

  @Conditional(NonMatchingRegisterBeanPhaseCondition.class)
  @Configuration(proxyBeanMethods = false)
  static class RegisterBeanPhaseExample extends Example {

    @Bean
    String subclassBean() {
      return "alpha";
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ ParseConfigurationPhaseExample.class, BravoExample.class })
  static class ParseConfigurationPhase {
  }

  @Conditional(NonMatchingParseConfigurationPhaseCondition.class)
  @Configuration(proxyBeanMethods = false)
  static class ParseConfigurationPhaseExample extends Example {

    @Bean
    String subclassBean() {
      return "alpha";
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class BravoExample extends Example {

    @Bean
    String subclassBean() {
      return "bravo";
    }
  }

  static class NonMatchingRegisterBeanPhaseCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }
  }

  static class NonMatchingParseConfigurationPhaseCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.PARSE_CONFIGURATION;
    }
  }

}
