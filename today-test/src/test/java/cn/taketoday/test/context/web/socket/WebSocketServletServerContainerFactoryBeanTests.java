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

package cn.taketoday.test.context.web.socket;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.junit.jupiter.web.JUnitWebConfig;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.web.socket.EnableWebSocket;
import jakarta.websocket.server.ServerContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that validate support for {@link ServletServerContainerFactoryBean}
 * in conjunction with {@link WebAppConfiguration @WebAppConfiguration} and the
 * TestContext Framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Disabled("TODO-web-socket")
@JUnitWebConfig
class WebSocketServletServerContainerFactoryBeanTests {

  @Test
  void servletServerContainerFactoryBeanSupport(@Autowired ServerContainer serverContainer) {
    assertThat(serverContainer.getDefaultMaxTextMessageBufferSize()).isEqualTo(42);
  }

  @Configuration
  @EnableWebSocket
  static class WebSocketConfig {

//    @Bean
//    ServletServerContainerFactoryBean createWebSocketContainer() {
//      ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
//      container.setMaxTextMessageBufferSize(42);
//      return container;
//    }
  }

}
