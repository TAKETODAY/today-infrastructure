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
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link ContextConfiguration @ContextConfiguration} in conjunction with the
 * {@link InfraExtension} in a JUnit Jupiter environment with
 * {@link ExtensionContextScope#TEST_METHOD}.
 *
 * @author Sam Brannen
 * @see ConstructorInjectionTestClassScopedExtensionContextNestedTests
 * @see infra.test.context.junit4.nested.NestedTestsWithInfraRulesTests
 * @since 5.0
 */
@JUnitConfig(ContextConfigurationTestMethodScopedExtensionContextNestedTests.TopLevelConfig.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ContextConfigurationTestMethodScopedExtensionContextNestedTests {

  private static final String FOO = "foo";
  private static final String BAR = "bar";
  private static final String BAZ = "baz";

  @Autowired(required = false)
  @Qualifier("foo")
  String foo;

  @Test
  void topLevelTest() {
    assertThat(foo).isEqualTo(FOO);
  }

  @Nested
  @JUnitConfig(NestedConfig.class)
  class NestedTests {

    @Autowired(required = false)
    @Qualifier("foo")
    String localFoo;

    @Autowired
    String bar;

    @Test
    void test() {
      assertThat(foo).as("foo bean should not be present").isNull();
      assertThat(this.localFoo).as("local foo bean should not be present").isNull();
      assertThat(this.bar).isEqualTo(BAR);
    }
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  class NestedTestsWithInheritedConfigTests {

    @Autowired(required = false)
    @Qualifier("foo")
    String localFoo;

    @Autowired
    String bar;

    @Test
    void test() {
      // Since the configuration is inherited, the foo field in the outer instance
      // and the bar field in the inner instance should both have been injected
      // from the test ApplicationContext for the outer instance.
      assertThat(foo).isEqualTo(FOO);
      assertThat(this.localFoo).isEqualTo(FOO);
      assertThat(this.bar).isEqualTo(FOO);
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @JUnitConfig(NestedConfig.class)
    class DoubleNestedWithOverriddenConfigTests {

      @Autowired(required = false)
      @Qualifier("foo")
      String localFoo;

      @Autowired
      String bar;

      @Test
      void test() {
        assertThat(foo).as("foo bean should not be present").isNull();
        assertThat(this.localFoo).as("local foo bean should not be present").isNull();
        assertThat(this.bar).isEqualTo(BAR);
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigTests {

        @Autowired(required = false)
        @Qualifier("foo")
        String localFoo;

        @Autowired
        String bar;

        @Test
        void test() {
          assertThat(foo).as("foo bean should not be present").isNull();
          assertThat(this.localFoo).as("local foo bean should not be present").isNull();
          assertThat(this.bar).isEqualTo(BAR);
        }
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

        @Autowired(required = false)
        @Qualifier("foo")
        String localFoo;

        @Autowired
        @Qualifier("bar")
        String bar;

        @Autowired
        String baz;

        @Test
        void test() {
          assertThat(foo).as("foo bean should not be present").isNull();
          assertThat(this.localFoo).as("local foo bean should not be present").isNull();
          assertThat(this.bar).isEqualTo(BAR);
          assertThat(this.baz).isEqualTo(BAZ);
        }
      }
    }
  }

  // -------------------------------------------------------------------------

  @Configuration
  static class TopLevelConfig {

    @Bean
    String foo() {
      return FOO;
    }
  }

  @Configuration
  static class NestedConfig {

    @Bean
    String bar() {
      return BAR;
    }
  }

  @Configuration
  static class TestInterfaceConfig {

    @Bean
    String baz() {
      return BAZ;
    }
  }

  @ContextConfiguration(classes = TestInterfaceConfig.class)
  interface TestInterface {
  }

}
