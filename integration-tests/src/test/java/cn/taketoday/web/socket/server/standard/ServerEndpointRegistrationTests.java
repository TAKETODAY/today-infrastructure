/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.socket.server.standard;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ServerEndpointRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class ServerEndpointRegistrationTests {

  @Test
  public void endpointPerConnection() throws Exception {

    @SuppressWarnings("resource")
    ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(Config.class);

    ServerEndpointRegistration registration = new ServerEndpointRegistration("/path", EchoEndpoint.class);
    registration.setBeanFactory(context.getBeanFactory());

    EchoEndpoint endpoint = registration.getConfigurator().getEndpointInstance(EchoEndpoint.class);

    assertThat(endpoint).isNotNull();
  }

  @Test
  public void endpointSingleton() throws Exception {

    EchoEndpoint endpoint = new EchoEndpoint(new EchoService());
    ServerEndpointRegistration registration = new ServerEndpointRegistration("/path", endpoint);

    EchoEndpoint actual = registration.getConfigurator().getEndpointInstance(EchoEndpoint.class);

    assertThat(actual).isSameAs(endpoint);
  }

  @Configuration
  static class Config {

    @Bean
    public EchoService echoService() {
      return new EchoService();
    }
  }

  private static class EchoEndpoint extends Endpoint {

    @SuppressWarnings("unused")
    private final EchoService service;

    @Autowired
    public EchoEndpoint(EchoService service) {
      this.service = service;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
    }
  }

  private static class EchoService { }

}
