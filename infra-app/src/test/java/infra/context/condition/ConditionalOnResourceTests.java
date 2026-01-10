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
