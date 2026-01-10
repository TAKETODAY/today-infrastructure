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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnMissingClass @ConditionalOnMissingClass}.
 *
 * @author Dave Syer
 */
class ConditionalOnMissingClassTests {

  private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

  @Test
  void testVanillaOnClassCondition() {
    this.context.register(BasicConfiguration.class, FooConfiguration.class);
    this.context.refresh();
    assertThat(this.context.containsBean("bar")).isFalse();
    assertThat(this.context.getBean("foo")).isEqualTo("foo");
  }

  @Test
  void testMissingOnClassCondition() {
    this.context.register(MissingConfiguration.class, FooConfiguration.class);
    this.context.refresh();
    assertThat(this.context.containsBean("bar")).isTrue();
    assertThat(this.context.getBean("foo")).isEqualTo("foo");
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingClass("infra.context.condition.ConditionalOnMissingClassTests")
  static class BasicConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingClass("FOO")
  static class MissingConfiguration {

    @Bean
    String bar() {
      return "bar";
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class FooConfiguration {

    @Bean
    String foo() {
      return "foo";
    }

  }

}
