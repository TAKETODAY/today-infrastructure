/*
 * Copyright 2002-present the original author or authors.
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
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.annotation.Value;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.junit.jupiter.InfraExtensionConfig;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized variant of {@link ConstructorInjectionTestClassScopedExtensionContextNestedTests}
 * which tests {@link ParameterizedClass @ParameterizedClass} support.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@JUnitConfig(ParameterizedConstructorInjectionTestClassScopedExtensionContextNestedTests.TopLevelConfig.class)
@InfraExtensionConfig(useTestClassScopedExtensionContext = true)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
@ParameterizedClass
@ValueSource(strings = { "foo", "bar" })
class ParameterizedConstructorInjectionTestClassScopedExtensionContextNestedTests {

  final String beanName;
  final String foo;
  final ApplicationContext context;

  ParameterizedConstructorInjectionTestClassScopedExtensionContextNestedTests(
          String beanName, TestInfo testInfo, @Autowired String foo, ApplicationContext context) {

    this.context = context;
    this.beanName = beanName;
    this.foo = foo;
  }

  @Test
  void topLevelTest() {
    assertThat(foo).isEqualTo("foo");
    if (beanName.equals("foo")) {
      assertThat(context.getBean(beanName, String.class)).isEqualTo(beanName);
    }
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class AutowiredConstructorTests {

    final String bar;
    final ApplicationContext localContext;

    @Autowired
    AutowiredConstructorTests(String bar, ApplicationContext context) {
      this.bar = bar;
      this.localContext = context;
    }

    @Test
    void nestedTest() {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
      if (beanName.equals("bar")) {
        assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
      }
    }
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class AutowiredConstructorParameterTests {

    final String bar;
    final ApplicationContext localContext;

    AutowiredConstructorParameterTests(@Autowired String bar, ApplicationContext context) {
      this.bar = bar;
      this.localContext = context;
    }

    @Test
    void nestedTest() {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
      if (beanName.equals("bar")) {
        assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
      }
    }
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class QualifiedConstructorParameterTests {

    final String bar;
    final ApplicationContext localContext;

    QualifiedConstructorParameterTests(TestInfo testInfo, @Qualifier("bar") String s, ApplicationContext context) {
      this.bar = s;
      this.localContext = context;
    }

    @Test
    void nestedTest() {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
      if (beanName.equals("bar")) {
        assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
      }
    }
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class SpelConstructorParameterTests {

    final String bar;
    final int answer;
    final ApplicationContext localContext;

    SpelConstructorParameterTests(@Autowired String bar, TestInfo testInfo, @Value("#{ 6 * 7 }") int answer, ApplicationContext context) {
      this.bar = bar;
      this.answer = answer;
      this.localContext = context;
    }

    @Test
    void nestedTest() {
      assertThat(foo).isEqualTo("foo");
      assertThat(bar).isEqualTo("bar");
      assertThat(answer).isEqualTo(42);
      if (beanName.equals("bar")) {
        assertThat(localContext.getBean(beanName, String.class)).isEqualTo(beanName);
      }
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class TopLevelConfig {

    @Bean
    String foo() {
      return "foo";
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class NestedConfig {

    @Bean
    String bar() {
      return "bar";
    }
  }

}
