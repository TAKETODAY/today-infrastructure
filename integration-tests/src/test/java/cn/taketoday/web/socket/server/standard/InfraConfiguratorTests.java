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

package cn.taketoday.web.socket.server.standard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ComponentScan;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.servlet.ContextLoader;
import cn.taketoday.web.servlet.support.AnnotationConfigWebApplicationContext;
import jakarta.websocket.server.ServerEndpoint;

import static org.assertj.core.api.Assertions.assertThat;

public class InfraConfiguratorTests {

  private MockServletContext servletContext;

  private ContextLoader contextLoader;

  private AnnotationConfigWebApplicationContext webAppContext;

  private InfraConfigurator configurator;

  @BeforeEach
  public void setup() {
    this.servletContext = new MockServletContext();

    this.webAppContext = new AnnotationConfigWebApplicationContext();
    this.webAppContext.register(Config.class);

    this.contextLoader = new ContextLoader(this.webAppContext);
    this.contextLoader.initWebApplicationContext(this.servletContext);

    this.configurator = new InfraConfigurator();
  }

  @AfterEach
  public void destroy() {
    this.contextLoader.closeWebApplicationContext(this.servletContext);
  }

  @Test
  public void getEndpointPerConnection() throws Exception {
    PerConnectionEchoEndpoint endpoint = this.configurator.getEndpointInstance(PerConnectionEchoEndpoint.class);
    assertThat(endpoint).isNotNull();
  }

  @Test
  public void getEndpointSingletonByType() throws Exception {
    EchoEndpoint expected = this.webAppContext.getBean(EchoEndpoint.class);
    EchoEndpoint actual = this.configurator.getEndpointInstance(EchoEndpoint.class);
    assertThat(actual).isSameAs(expected);
  }

  @Test
  public void getEndpointSingletonByComponentName() throws Exception {
    ComponentEchoEndpoint expected = this.webAppContext.getBean(ComponentEchoEndpoint.class);
    ComponentEchoEndpoint actual = this.configurator.getEndpointInstance(ComponentEchoEndpoint.class);
    assertThat(actual).isSameAs(expected);
  }

  @Configuration
  @ComponentScan(basePackageClasses = InfraConfiguratorTests.class)
  static class Config {

    @Bean
    public EchoEndpoint javaConfigEndpoint() {
      return new EchoEndpoint(echoService());
    }

    @Bean
    public EchoService echoService() {
      return new EchoService();
    }
  }

  @ServerEndpoint("/echo")
  private static class EchoEndpoint {

    @SuppressWarnings("unused")
    private final EchoService service;

    @Autowired
    public EchoEndpoint(EchoService service) {
      this.service = service;
    }
  }

  @Component("myComponentEchoEndpoint")
  @ServerEndpoint("/echo")
  private static class ComponentEchoEndpoint {

    @SuppressWarnings("unused")
    private final EchoService service;

    @Autowired
    public ComponentEchoEndpoint(EchoService service) {
      this.service = service;
    }
  }

  @ServerEndpoint("/echo")
  private static class PerConnectionEchoEndpoint {

    @SuppressWarnings("unused")
    private final EchoService service;

    @Autowired
    public PerConnectionEchoEndpoint(EchoService service) {
      this.service = service;
    }
  }

  private static class EchoService { }

}
