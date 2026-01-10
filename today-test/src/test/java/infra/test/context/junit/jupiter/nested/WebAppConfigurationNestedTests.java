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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.test.context.junit4.nested.NestedTestsWithInfraRulesTests;
import infra.test.context.web.WebAppConfiguration;
import infra.web.mock.WebApplicationContext;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link WebAppConfiguration @WebAppConfiguration} in conjunction with the
 * {@link InfraExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @see ConstructorInjectionNestedTests
 * @see NestedTestsWithInfraRulesTests
 * @since 4.0
 */
@JUnitWebConfig(WebAppConfigurationNestedTests.Config.class)
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class WebAppConfigurationNestedTests {

  @Test
  void test(ApplicationContext context) {
    assertThat(context).isInstanceOf(WebApplicationContext.class);
  }

  @Nested
  @JUnitConfig(Config.class)
  class ConfigOverriddenByDefaultTests {

    @Test
    void test(ApplicationContext context) {
      assertThat(context).isNotInstanceOf(WebApplicationContext.class);
    }
  }

  @Nested
  @JUnitWebConfig(Config.class)
  class ConfigOverriddenByDefaultWebTests {

    @Test
    void test(ApplicationContext context) {
      assertThat(context).isInstanceOf(WebApplicationContext.class);
    }
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  class NestedWithInheritedConfigTests {

    @Test
    void test(ApplicationContext context) {
      assertThat(context).isInstanceOf(WebApplicationContext.class);
    }

    @Nested
    class DoubleNestedWithImplicitlyInheritedConfigWebTests {

      @Test
      void test(ApplicationContext context) {
        assertThat(context).isInstanceOf(WebApplicationContext.class);
      }
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @JUnitConfig(Config.class)
    class DoubleNestedWithOverriddenConfigWebTests {

      @Test
      void test(ApplicationContext context) {
        assertThat(context).isNotInstanceOf(WebApplicationContext.class);
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigWebTests {

        @Test
        void test(ApplicationContext context) {
          assertThat(context).isNotInstanceOf(WebApplicationContext.class);
        }
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

        @Test
        void test(ApplicationContext context) {
          assertThat(context).isInstanceOf(WebApplicationContext.class);
        }
      }
    }
  }

  // -------------------------------------------------------------------------

  @Configuration
  static class Config {
  }

  @WebAppConfiguration
  interface TestInterface {
  }

}
