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

package cn.taketoday.annotation.config.web.servlet;

import org.junit.jupiter.api.Test;

import cn.taketoday.framework.web.embedded.undertow.UndertowServletWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UndertowServletWebServerFactoryCustomizer}
 *
 * @author Andy Wilkinson
 */
class UndertowServletWebServerFactoryCustomizerTests {

  @Test
  void eagerFilterInitCanBeDisabled() {
    UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory(0);
    assertThat(factory.isEagerFilterInit()).isTrue();
    ServerProperties serverProperties = new ServerProperties();
    serverProperties.undertow.eagerFilterInit = (false);
    new UndertowServletWebServerFactoryCustomizer(serverProperties).customize(factory);
    assertThat(factory.isEagerFilterInit()).isFalse();
  }

  @Test
  void preservePathOnForwardCanBeEnabled() {
    UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory(0);
    assertThat(factory.isPreservePathOnForward()).isFalse();
    ServerProperties serverProperties = new ServerProperties();
    serverProperties.undertow.preservePathOnForward = (true);
    new UndertowServletWebServerFactoryCustomizer(serverProperties).customize(factory);
    assertThat(factory.isPreservePathOnForward()).isTrue();
  }

}
