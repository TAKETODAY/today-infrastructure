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

package infra.test.context.hierarchies.standard;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@ContextConfiguration
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
class TestHierarchyLevelTwoWithBareContextConfigurationInSubclassTests extends
        TestHierarchyLevelOneWithBareContextConfigurationInSubclassTests {

  @Configuration
  static class Config {

    @Bean
    String foo() {
      return "foo-level-2";
    }

    @Bean
    String baz() {
      return "baz";
    }
  }

  @Autowired
  private String foo;

  @Autowired
  private String bar;

  @Autowired
  private String baz;

  @Autowired
  private ApplicationContext context;

  @Test
  @Override
  void loadContextHierarchy() {
    assertThat(context).as("child ApplicationContext").isNotNull();
    assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
    assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();
    assertThat(foo).isEqualTo("foo-level-2");
    assertThat(bar).isEqualTo("bar");
    assertThat(baz).isEqualTo("baz");
  }

}
