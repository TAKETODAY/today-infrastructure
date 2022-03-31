/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.env.Environment;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link TestPropertySource @TestPropertySource} in conjunction with the
 * {@link ApplicationExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.3
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
