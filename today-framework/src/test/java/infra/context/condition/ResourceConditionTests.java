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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ResourceCondition}.
 *
 * @author Stephane Nicoll
 */
class ResourceConditionTests {

  private ConfigurableApplicationContext context;

  @AfterEach
  void tearDown() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  void defaultResourceAndNoExplicitKey() {
    load(DefaultLocationConfiguration.class);
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  @Test
  void unknownDefaultLocationAndNoExplicitKey() {
    load(UnknownDefaultLocationConfiguration.class);
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Test
  void unknownDefaultLocationAndExplicitKeyToResource() {
    load(UnknownDefaultLocationConfiguration.class, "spring.foo.test.config=logging.properties");
    assertThat(this.context.containsBean("foo")).isTrue();
  }

  private void load(Class<?> config, String... environment) {
    AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
    TestPropertyValues.of(environment).applyTo(applicationContext);
    applicationContext.register(config);
    applicationContext.refresh();
    this.context = applicationContext;
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(DefaultLocationResourceCondition.class)
  static class DefaultLocationConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(UnknownDefaultLocationResourceCondition.class)
  static class UnknownDefaultLocationConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  static class DefaultLocationResourceCondition extends ResourceCondition {

    DefaultLocationResourceCondition() {
      super("test", "spring.foo.test.config", "classpath:/logging-nondefault.properties");
    }

  }

  static class UnknownDefaultLocationResourceCondition extends ResourceCondition {

    UnknownDefaultLocationResourceCondition() {
      super("test", "spring.foo.test.config", "classpath:/this-file-does-not-exist.xml");
    }

  }

}
