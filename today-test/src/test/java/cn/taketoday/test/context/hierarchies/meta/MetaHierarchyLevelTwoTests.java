/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.test.context.hierarchies.meta;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.aot.DisabledInAotMode;

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
