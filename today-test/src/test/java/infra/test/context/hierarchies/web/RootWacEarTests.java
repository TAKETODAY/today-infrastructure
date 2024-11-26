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

package infra.test.context.hierarchies.web;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.web.WebAppConfiguration;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@WebAppConfiguration
@ContextHierarchy(@ContextConfiguration)
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
class RootWacEarTests extends EarTests {

  @Configuration(proxyBeanMethods = false)
  static class RootWacConfig {

    @Bean
    String root() {
      return "root";
    }
  }

  // -------------------------------------------------------------------------

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private String ear;

  @Autowired
  private String root;

  @Disabled("Superseded by verifyRootWacConfig()")
  @Test
  @Override
  void verifyEarConfig() {
    /* no-op */
  }

  @Test
  void verifyRootWacConfig() {
    ApplicationContext parent = wac.getParent();
    assertThat(parent).isNotNull();
    boolean condition = parent instanceof WebApplicationContext;
    assertThat(condition).isFalse();
    assertThat(ear).isEqualTo("ear");
    assertThat(root).isEqualTo("root");
  }

}
