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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.mock.api.MockContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.context.web.WebAppConfiguration;
import infra.web.mock.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@ContextHierarchy({
        //
        @ContextConfiguration(name = "root", classes = ControllerIntegrationTests.AppConfig.class),
        @ContextConfiguration(name = "dispatcher", classes = ControllerIntegrationTests.WebConfig.class) //
})
@DisabledInAotMode // @ContextHierarchy is not supported in AOT.
class ControllerIntegrationTests {

  @Configuration
  static class AppConfig {

    @Bean
    String foo() {
      return "foo";
    }
  }

  @Configuration
  static class WebConfig {

    @Bean
    String bar() {
      return "bar";
    }
  }

  // -------------------------------------------------------------------------

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private String foo;

  @Autowired
  private String bar;

  @Test
  void verifyRootWacSupport() {
    assertThat(foo).isEqualTo("foo");
    assertThat(bar).isEqualTo("bar");

    ApplicationContext parent = wac.getParent();
    assertThat(parent).isNotNull();
    boolean condition = parent instanceof WebApplicationContext;
    assertThat(condition).isTrue();
    WebApplicationContext root = (WebApplicationContext) parent;
    assertThat(root.getBeansOfType(String.class).containsKey("bar")).isFalse();

    MockContext childMockContext = wac.getMockContext();
    assertThat(childMockContext).isNotNull();
    MockContext rootMockContext = root.getMockContext();
    assertThat(rootMockContext).isNotNull();
    assertThat(rootMockContext).isSameAs(childMockContext);

    assertThat(rootMockContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
    assertThat(childMockContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
  }

}
