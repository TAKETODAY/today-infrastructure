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
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.NestedTestConfiguration;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link ContextHierarchy @ContextHierarchy} in conjunction with the
 * {@link InfraExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@ContextHierarchy(@ContextConfiguration(classes = ContextHierarchyNestedTests.ParentConfig.class))
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
class ContextHierarchyNestedTests {

  private static final String FOO = "foo";
  private static final String BAR = "bar";
  private static final String BAZ = "baz";
  private static final String QUX = "qux";

  @Autowired
  String foo;

  @Autowired
  ApplicationContext context;

  @Test
  void topLevelTest() {
    assertThat(this.context).as("local ApplicationContext").isNotNull();
    assertThat(this.context.getParent()).as("parent ApplicationContext").isNull();

    assertThat(foo).isEqualTo(FOO);
  }

  @Nested
  @ContextConfiguration(classes = NestedConfig.class)
  class NestedTests {

    @Autowired
    String bar;

    @Autowired
    ApplicationContext context;

    @Test
    void nestedTest() throws Exception {
      assertThat(this.context).as("local ApplicationContext").isNotNull();
      assertThat(this.context.getParent()).as("parent ApplicationContext").isNull();

      // In contrast to nested test classes running in JUnit 4, the foo
      // field in the outer instance should have been injected from the
      // test ApplicationContext for the outer instance.
      assertThat(foo).isEqualTo(FOO);
      assertThat(this.bar).isEqualTo(BAR);
    }
  }

  @Nested
  @NestedTestConfiguration(INHERIT)
  @ContextConfiguration(classes = Child1Config.class)
  class NestedTestCaseWithInheritedConfigTests {

    @Autowired
    String bar;

    @Autowired
    ApplicationContext context;

    @Test
    void nestedTest() throws Exception {
      assertThat(this.context).as("local ApplicationContext").isNotNull();
      assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();

      // Since the configuration is inherited, the foo field in the outer instance
      // and the bar field in the inner instance should both have been injected
      // from the test ApplicationContext for the outer instance.
      assertThat(foo).isEqualTo(FOO);
      assertThat(this.bar).isEqualTo(BAZ + 1);
      assertThat(this.context.getBean("foo", String.class)).as("child foo").isEqualTo(QUX + 1);
    }

    @Nested
    @NestedTestConfiguration(OVERRIDE)
    @ContextHierarchy({
            @ContextConfiguration(classes = ParentConfig.class),
            @ContextConfiguration(classes = Child2Config.class)
    })
    class DoubleNestedTestCaseWithOverriddenConfigTests {

      @Autowired
      String bar;

      @Autowired
      ApplicationContext context;

      @Test
      void nestedTest() throws Exception {
        assertThat(this.context).as("local ApplicationContext").isNotNull();
        assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();

        assertThat(foo).isEqualTo(FOO);
        assertThat(this.bar).isEqualTo(BAZ + 2);
        assertThat(this.context.getBean("foo", String.class)).as("child foo").isEqualTo(QUX + 2);
      }

      @Nested
      @NestedTestConfiguration(INHERIT)
      class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

        @Autowired
        @Qualifier("foo")
        String localFoo;

        @Autowired
        String bar;

        @Autowired
        ApplicationContext context;

        @Test
        void nestedTest() throws Exception {
          assertThat(this.context).as("local ApplicationContext").isNotNull();
          assertThat(this.context.getParent()).as("parent ApplicationContext").isNotNull();
          assertThat(this.context.getParent().getParent()).as("grandparent ApplicationContext").isNotNull();

          assertThat(foo).isEqualTo(FOO);
          assertThat(this.localFoo).isEqualTo("test interface");
          assertThat(this.bar).isEqualTo(BAZ + 2);
          assertThat(this.context.getParent().getBean("foo", String.class)).as("child foo").isEqualTo(QUX + 2);
        }
      }
    }
  }

  // -------------------------------------------------------------------------

  @Configuration
  static class ParentConfig {

    @Bean
    String foo() {
      return FOO;
    }
  }

  @Configuration
  static class Child1Config {

    @Bean
    String foo() {
      return QUX + 1;
    }

    @Bean
    String bar() {
      return BAZ + 1;
    }
  }

  @Configuration
  static class Child2Config {

    @Bean
    String foo() {
      return QUX + 2;
    }

    @Bean
    String bar() {
      return BAZ + 2;
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
    String foo() {
      return "test interface";
    }
  }

  @ContextConfiguration(classes = TestInterfaceConfig.class)
  interface TestInterface {
  }

}
