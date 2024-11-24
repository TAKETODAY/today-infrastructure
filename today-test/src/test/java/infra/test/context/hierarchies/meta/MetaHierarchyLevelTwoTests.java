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

package infra.test.context.hierarchies.meta;

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Profile;
import infra.test.context.ActiveProfiles;
import infra.test.context.ContextConfiguration;
import infra.test.context.aot.DisabledInAotMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
@ActiveProfiles("prod")
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
class MetaHierarchyLevelTwoTests extends MetaHierarchyLevelOneTests {

  @Configuration
  @Profile("prod")
  static class Config {

    @Bean
    String bar() {
      return "Prod Bar";
    }
  }

  @Autowired
  protected ApplicationContext context;

  @Autowired
  private String bar;

  @Test
  void bar() {
    assertThat(bar).isEqualTo("Prod Bar");
  }

  @Test
  void contextHierarchy() {
    assertThat(context).as("child ApplicationContext").isNotNull();
    assertThat(context.getParent()).as("parent ApplicationContext").isNotNull();
    assertThat(context.getParent().getParent()).as("grandparent ApplicationContext").isNull();
  }

}
