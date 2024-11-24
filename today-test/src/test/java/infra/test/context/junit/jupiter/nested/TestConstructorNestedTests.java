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

package infra.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.TestConstructor;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static infra.test.context.TestConstructor.AutowireMode.ALL;
import static infra.test.context.TestConstructor.AutowireMode.ANNOTATED;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestConstructor @TestConstructor} in conjunction with the
 * {@link InfraExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig
@TestConstructor(autowireMode = ALL)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TestConstructorNestedTests {

  TestConstructorNestedTests(String text) {
    assertThat(text).isEqualTo("enigma");
  }

  @Test
  void test() {
  }

  @Nested
  @JUnitConfig(Config.class)
  @TestConstructor(autowireMode = ANNOTATED)
  class ConfigOverriddenByDefaultTests {

    @Autowired
    ConfigOverriddenByDefaultTests(String text) {
      assertThat(text).isEqualTo("enigma");
    }

    @Test
    void test() {
    }
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  class InheritedConfigTests {

    InheritedConfigTests(String text) {
      assertThat(text).isEqualTo("enigma");
    }

    @Test
    void test() {
    }

    @Nested
    class DoubleNestedWithImplicitlyInheritedConfigTests {

      DoubleNestedWithImplicitlyInheritedConfigTests(String text) {
        assertThat(text).isEqualTo("enigma");
      }

      @Test
      void test() {
      }

      @Nested
      class TripleNestedWithImplicitlyInheritedConfigTests {

        TripleNestedWithImplicitlyInheritedConfigTests(String text) {
          assertThat(text).isEqualTo("enigma");
        }

        @Test
        void test() {
        }
      }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @JUnitConfig(Config.class)
    @TestConstructor(autowireMode = ANNOTATED)
    class DoubleNestedWithOverriddenConfigTests {

      DoubleNestedWithOverriddenConfigTests(@Autowired String text) {
        assertThat(text).isEqualTo("enigma");
      }

      @Test
      void test() {
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigTests {

        @Autowired
        TripleNestedWithInheritedConfigTests(String text) {
          assertThat(text).isEqualTo("enigma");
        }

        @Test
        void test() {
        }
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

        TripleNestedWithInheritedConfigAndTestInterfaceTests(String text) {
          assertThat(text).isEqualTo("enigma");
        }

        @Test
        void test() {
        }
      }
    }
  }

  // -------------------------------------------------------------------------

  @Configuration
  static class Config {

    @Bean
    String text() {
      return "enigma";
    }
  }

  @TestConstructor(autowireMode = ALL)
  interface TestInterface {
  }

}
