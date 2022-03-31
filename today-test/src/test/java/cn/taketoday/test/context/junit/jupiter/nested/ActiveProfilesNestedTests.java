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

import java.util.List;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.NestedTestConfiguration;
import cn.taketoday.test.context.junit.jupiter.ApplicationExtension;
import cn.taketoday.test.context.junit.jupiter.JUnitConfig;

import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static cn.taketoday.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link ActiveProfiles @ActiveProfiles} in conjunction with the
 * {@link ApplicationExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitConfig(ActiveProfilesNestedTests.Config1.class)
@ActiveProfiles("1")
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class ActiveProfilesNestedTests {

  @Autowired
  List<String> strings;

  @Test
  void test() {
    assertThat(this.strings).containsExactlyInAnyOrder("X", "A1");
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  class InheritedConfigTests {

    @Autowired
    List<String> localStrings;

    @Test
    void test() {
      assertThat(strings).containsExactlyInAnyOrder("X", "A1");
      assertThat(this.localStrings).containsExactlyInAnyOrder("X", "A1");
    }
  }

  @Nested
  @JUnitConfig(Config2.class)
  @ActiveProfiles("2")
  class ConfigOverriddenByDefaultTests {

    @Autowired
    List<String> localStrings;

    @Test
    void test() {
      assertThat(strings).containsExactlyInAnyOrder("X", "A1");
      assertThat(this.localStrings).containsExactlyInAnyOrder("Y", "A2");
    }
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  @ContextConfiguration(classes = Config2.class)
  @ActiveProfiles("2")
  class InheritedAndExtendedConfigTests {

    @Autowired
    List<String> localStrings;

    @Test
    void test() {
      assertThat(strings).containsExactlyInAnyOrder("X", "A1");
      assertThat(this.localStrings).containsExactlyInAnyOrder("X", "A1", "Y", "A2");
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @JUnitConfig({ Config1.class, Config2.class, Config3.class })
    @ActiveProfiles("3")
    class DoubleNestedWithOverriddenConfigTests {

      @Autowired
      List<String> localStrings;

      @Test
      void test() {
        assertThat(strings).containsExactlyInAnyOrder("X", "A1");
        assertThat(this.localStrings).containsExactlyInAnyOrder("X", "Y", "Z", "A3");
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      @ActiveProfiles(profiles = "2", inheritProfiles = false)
      class TripleNestedWithInheritedConfigButOverriddenProfilesTests {

        @Autowired
        List<String> localStrings;

        @Test
        void test() {
          assertThat(strings).containsExactlyInAnyOrder("X", "A1");
          assertThat(this.localStrings).containsExactlyInAnyOrder("X", "Y", "Z", "A2");
        }
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

        @Autowired
        List<String> localStrings;

        @Test
        void test() {
          assertThat(strings).containsExactlyInAnyOrder("X", "A1");
          assertThat(this.localStrings).containsExactlyInAnyOrder("X", "Y", "Z", "A2", "A3");
        }
      }
    }

  }

  // -------------------------------------------------------------------------

  @Configuration
  static class Config1 {

    @Bean
    String x() {
      return "X";
    }

    @Bean
    @Profile("1")
    String a1() {
      return "A1";
    }
  }

  @Configuration
  static class Config2 {

    @Bean
    String y() {
      return "Y";
    }

    @Bean
    @Profile("2")
    String a2() {
      return "A2";
    }
  }

  @Configuration
  static class Config3 {

    @Bean
    String z() {
      return "Z";
    }

    @Bean
    @Profile("3")
    String a3() {
      return "A3";
    }
  }

  @ActiveProfiles("2")
  interface TestInterface {
  }

}
