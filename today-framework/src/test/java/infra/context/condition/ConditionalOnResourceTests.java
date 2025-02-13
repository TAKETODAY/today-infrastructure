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
import infra.context.annotation.Configuration;
import infra.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnResource @ConditionalOnResource}.
 *
 * @author Dave Syer
 */
class ConditionalOnResourceTests {

  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @Test
  void testResourceExists() {
    this.context.register(BasicConfiguration.class);
    this.context.refresh();
    assertThat(this.context.containsBean("foo")).isTrue();
    assertThat(this.context.getBean("foo")).isEqualTo("foo");
  }

  @Test
  void testResourceExistsWithPlaceholder() {
    TestPropertyValues.of("schema=schema.sql").applyTo(this.context);
    this.context.register(PlaceholderConfiguration.class);
    this.context.refresh();
    assertThat(this.context.containsBean("foo")).isTrue();
    assertThat(this.context.getBean("foo")).isEqualTo("foo");
  }

  @Test
  void testResourceNotExists() {
    this.context.register(MissingConfiguration.class);
    this.context.refresh();
    assertThat(this.context.containsBean("foo")).isFalse();
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnResource("foo")
  static class MissingConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnResource("schema.sql")
  static class BasicConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnResource("${schema}")
  static class PlaceholderConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

}
