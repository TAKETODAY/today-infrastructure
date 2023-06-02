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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.mock.web.MockServletContext;
import cn.taketoday.web.servlet.support.AnnotationConfigWebApplicationContext;
import jakarta.servlet.ServletContext;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test fixture for {@link ServerEndpointExporter}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class ServerEndpointExporterTests {

  private ServerContainer serverContainer;

  private ServletContext servletContext;

  private AnnotationConfigWebApplicationContext webAppContext;

  private ServerEndpointExporter exporter;

  @BeforeEach
  public void setup() {
    this.serverContainer = mock(ServerContainer.class);

    this.servletContext = new MockServletContext();
    this.servletContext.setAttribute("jakarta.websocket.server.ServerContainer", this.serverContainer);

    this.webAppContext = new AnnotationConfigWebApplicationContext();
    this.webAppContext.register(Config.class);
    this.webAppContext.setServletContext(this.servletContext);
    this.webAppContext.refresh();

    this.exporter = new ServerEndpointExporter();
  }

  @Test
  public void addAnnotatedEndpointClasses() throws Exception {
    this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class);
    this.exporter.setApplicationContext(this.webAppContext);
    this.exporter.afterPropertiesSet();
    this.exporter.afterSingletonsInstantiated();

    verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
    verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
  }

  @Test
  public void addAnnotatedEndpointClassesWithServletContextOnly() throws Exception {
    this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class, AnnotatedDummyEndpointBean.class);
    this.exporter.setServletContext(this.servletContext);
    this.exporter.afterPropertiesSet();
    this.exporter.afterSingletonsInstantiated();

    verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
    verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
  }

  @Test
  public void addAnnotatedEndpointClassesWithExplicitServerContainerOnly() throws Exception {
    this.exporter.setAnnotatedEndpointClasses(AnnotatedDummyEndpoint.class, AnnotatedDummyEndpointBean.class);
    this.exporter.setServerContainer(this.serverContainer);
    this.exporter.afterPropertiesSet();
    this.exporter.afterSingletonsInstantiated();

    verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpoint.class);
    verify(this.serverContainer).addEndpoint(AnnotatedDummyEndpointBean.class);
  }

  @Test
  public void addServerEndpointConfigBean() throws Exception {
    ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
    this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);

    this.exporter.setApplicationContext(this.webAppContext);
    this.exporter.afterPropertiesSet();
    this.exporter.afterSingletonsInstantiated();

    verify(this.serverContainer).addEndpoint(endpointRegistration);
  }

  @Test
  public void addServerEndpointConfigBeanWithExplicitServletContext() throws Exception {
    ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
    this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);

    this.exporter.setServletContext(this.servletContext);
    this.exporter.setApplicationContext(this.webAppContext);
    this.exporter.afterPropertiesSet();
    this.exporter.afterSingletonsInstantiated();

    verify(this.serverContainer).addEndpoint(endpointRegistration);
  }

  @Test
  public void addServerEndpointConfigBeanWithExplicitServerContainer() throws Exception {
    ServerEndpointRegistration endpointRegistration = new ServerEndpointRegistration("/dummy", new DummyEndpoint());
    this.webAppContext.getBeanFactory().registerSingleton("dummyEndpoint", endpointRegistration);
    this.servletContext.removeAttribute("jakarta.websocket.server.ServerContainer");

    this.exporter.setServerContainer(this.serverContainer);
    this.exporter.setApplicationContext(this.webAppContext);
    this.exporter.afterPropertiesSet();
    this.exporter.afterSingletonsInstantiated();

    verify(this.serverContainer).addEndpoint(endpointRegistration);
  }

  private static class DummyEndpoint extends Endpoint {

    @Override
    public void onOpen(Session session, EndpointConfig config) {
    }
  }

  @ServerEndpoint("/path")
  private static class AnnotatedDummyEndpoint {
  }

  @ServerEndpoint("/path")
  private static class AnnotatedDummyEndpointBean {
  }

  @Configuration
  static class Config {

    @Bean
    public AnnotatedDummyEndpointBean annotatedEndpoint1() {
      return new AnnotatedDummyEndpointBean();
    }
  }

}
