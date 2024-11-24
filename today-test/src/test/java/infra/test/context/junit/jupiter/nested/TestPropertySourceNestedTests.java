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
import infra.context.annotation.Configuration;
import infra.core.env.Environment;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestPropertySource @TestPropertySource} in conjunction with the
 * {@link InfraExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(TestPropertySourceNestedTests.Config.class)
@TestPropertySource(properties = "p1 = v1")
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TestPropertySourceNestedTests {

  @Autowired
  Environment env1;

  @Test
  void propertiesInEnvironment() {
    assertThat(env1.getProperty("p1")).isEqualTo("v1");
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  class InheritedConfigTests {

    @Autowired
    Environment env2;

    @Test
    void propertiesInEnvironment() {
      assertThat(env1.getProperty("p1")).isEqualTo("v1");
      assertThat(env2.getProperty("p1")).isEqualTo("v1");
      assertThat(env1).isSameAs(env2);
    }
  }

  @Nested
  @JUnitConfig(Config.class)
  @TestPropertySource(properties = "p2 = v2")
  class ConfigOverriddenByDefaultTests {

    @Autowired
    Environment env2;

    @Test
    void propertiesInEnvironment() {
      assertThat(env1.getProperty("p1")).isEqualTo("v1");
      assertThat(env1).isNotSameAs(env2);
      assertThat(env2.getProperty("p1")).isNull();
      assertThat(env2.getProperty("p2")).isEqualTo("v2");
    }
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  @TestPropertySource(properties = "p2a = v2a")
  @TestPropertySource(properties = "p2b = v2b")
  class InheritedAndExtendedConfigTests {

    @Autowired
    Environment env2;

    @Test
    void propertiesInEnvironment() {
      assertThat(env1.getProperty("p1")).isEqualTo("v1");
      assertThat(env1).isNotSameAs(env2);
      assertThat(env2.getProperty("p1")).isEqualTo("v1");
      assertThat(env2.getProperty("p2a")).isEqualTo("v2a");
      assertThat(env2.getProperty("p2b")).isEqualTo("v2b");
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @JUnitConfig(Config.class)
    @TestPropertySource(properties = "p3 = v3")
    class L3WithOverriddenConfigTests {

      @Autowired
      Environment env3;

      @Test
      void propertiesInEnvironment() {
        assertThat(env1.getProperty("p1")).isEqualTo("v1");
        assertThat(env1).isNotSameAs(env2);
        assertThat(env2.getProperty("p1")).isEqualTo("v1");
        assertThat(env2.getProperty("p2a")).isEqualTo("v2a");
        assertThat(env2.getProperty("p2b")).isEqualTo("v2b");
        assertThat(env2).isNotSameAs(env3);
        assertThat(env3.getProperty("p1")).isNull();
        assertThat(env3.getProperty("p2")).isNull();
        assertThat(env3.getProperty("p3")).isEqualTo("v3");
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      @TestPropertySource(properties = { "p3 = v34", "p4 = v4" }, inheritProperties = false)
      class L4WithInheritedConfigButOverriddenTestPropertiesTests {

        @Autowired
        Environment env4;

        @Test
        void propertiesInEnvironment() {
          assertThat(env1.getProperty("p1")).isEqualTo("v1");
          assertThat(env1).isNotSameAs(env2);
          assertThat(env2.getProperty("p1")).isEqualTo("v1");
          assertThat(env2.getProperty("p2a")).isEqualTo("v2a");
          assertThat(env2.getProperty("p2b")).isEqualTo("v2b");
          assertThat(env2).isNotSameAs(env3);
          assertThat(env3.getProperty("p1")).isNull();
          assertThat(env3.getProperty("p2")).isNull();
          assertThat(env3.getProperty("p3")).isEqualTo("v3");
          assertThat(env3).isNotSameAs(env4);
          assertThat(env4.getProperty("p1")).isNull();
          assertThat(env4.getProperty("p2")).isNull();
          assertThat(env4.getProperty("p3")).isEqualTo("v34");
          assertThat(env4.getProperty("p4")).isEqualTo("v4");
        }

        @Nested
        class L5WithInheritedConfigAndTestInterfaceTests implements TestInterface {

          @Autowired
          Environment env5;

          @Test
          void propertiesInEnvironment() {
            assertThat(env4).isNotSameAs(env5);
            assertThat(env5.getProperty("foo")).isEqualTo("bar");
            assertThat(env5.getProperty("enigma")).isEqualTo("42");
            assertThat(env5.getProperty("p1")).isNull();
            assertThat(env5.getProperty("p2")).isNull();
            assertThat(env5.getProperty("p3")).isEqualTo("v34");
            assertThat(env5.getProperty("p4")).isEqualTo("v4");
          }
        }
      }
    }
  }

  // -------------------------------------------------------------------------

  @Configuration
  static class Config {
    /* no user beans required for these tests */
  }

  @TestPropertySource(properties = { "foo = bar", "enigma: 42" })
  interface TestInterface {
  }

}
