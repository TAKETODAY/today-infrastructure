/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.annotation.Value;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.junit4.nested.NestedTestsWithInfraRulesTests;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes in conjunction
 * with the {@link InfraExtension} in a JUnit Jupiter environment ... when using
 * constructor injection as opposed to field injection
 *
 * @author Sam Brannen
 * @see ContextConfigurationNestedTests
 * @see NestedTestsWithInfraRulesTests
 * @since 4.0
 */
@JUnitConfig(ConstructorInjectionNestedTests.TopLevelConfig.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ConstructorInjectionNestedTests {

  final String foo;

  ConstructorInjectionNestedTests(TestInfo testInfo, @Autowired String foo) {
    this.foo = foo;
  }

  @Test
  void topLevelTest() {
    assertThat(foo).isEqualTo("foo");
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class AutowiredConstructorTests {

    final String bar;

    @Autowired
    AutowiredConstructorTests(String bar) {
      this.bar = bar;
    }

    @Test
    void nestedTest() throws Exception {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
    }
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class AutowiredConstructorParameterTests {

    final String bar;

    AutowiredConstructorParameterTests(@Autowired String bar) {
      this.bar = bar;
    }

    @Test
    void nestedTest() throws Exception {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
    }
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class QualifiedConstructorParameterTests {

    final String bar;

    QualifiedConstructorParameterTests(TestInfo testInfo, @Qualifier("bar") String s) {
      this.bar = s;
    }

    @Test
    void nestedTest() throws Exception {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
    }
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class SpelConstructorParameterTests {

    final String bar;
    final int answer;

    SpelConstructorParameterTests(@Autowired String bar, TestInfo testInfo, @Value("#{ 6 * 7 }") int answer) {
      this.bar = bar;
      this.answer = answer;
    }

    @Test
    void nestedTest() throws Exception {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
      assertThat(answer).isEqualTo(42);
    }
  }

  // -------------------------------------------------------------------------

  @Configuration
  static class TopLevelConfig {

    @Bean
    String foo() {
      return "foo";
    }
  }

  @Configuration
  static class NestedConfig {

    @Bean
    String bar() {
      return "bar";
    }
  }

}
