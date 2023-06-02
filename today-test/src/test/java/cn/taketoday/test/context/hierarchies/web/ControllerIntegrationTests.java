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

package cn.taketoday.test.context.hierarchies.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.ServletContext;

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

    ServletContext childServletContext = wac.getServletContext();
    assertThat(childServletContext).isNotNull();
    ServletContext rootServletContext = root.getServletContext();
    assertThat(rootServletContext).isNotNull();
    assertThat(rootServletContext).isSameAs(childServletContext);

    assertThat(rootServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
    assertThat(childServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).isSameAs(root);
  }

}
