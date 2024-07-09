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

package cn.taketoday.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.test.context.junit4.nested.NestedTestsWithInfraRulesTests;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.web.mock.WebApplicationContext;

import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
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
