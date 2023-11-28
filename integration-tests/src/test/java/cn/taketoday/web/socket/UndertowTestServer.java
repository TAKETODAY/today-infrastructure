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

package cn.taketoday.web.socket;

import org.xnio.OptionMap;
import org.xnio.Xnio;

import java.io.IOException;
import java.net.InetSocketAddress;

import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.WebApplicationContext;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

/**
 * Undertow-based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class UndertowTestServer implements WebSocketTestServer {

  private int port;

  private Undertow server;

  private DeploymentManager manager;

  @Override
  public void setup() {
  }

  @Override
  @SuppressWarnings("deprecation")
  public void deployConfig(WebApplicationContext wac, Filter... filters) {
    DispatcherServletInstanceFactory servletFactory = new DispatcherServletInstanceFactory(wac);
    // manually building WebSocketDeploymentInfo in order to avoid class cast exceptions
    // with tomcat's implementation when using undertow 1.1.0+
    WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
    try {
      info.setWorker(Xnio.getInstance().createWorker(OptionMap.EMPTY));
      info.setBuffers(new org.xnio.ByteBufferSlicePool(1024, 1024));
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }

    ServletInfo servletInfo = servlet("DispatcherServlet", DispatcherServlet.class, servletFactory)
            .addMapping("/").setAsyncSupported(true);
    DeploymentInfo servletBuilder = deployment()
            .setClassLoader(UndertowTestServer.class.getClassLoader())
            .setDeploymentName("undertow-websocket-test")
            .setContextPath("/")
            .addServlet(servletInfo)
            .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info);
    for (final Filter filter : filters) {
      String filterName = filter.getClass().getName();
      FilterInstanceFactory filterFactory = new FilterInstanceFactory(filter);
      FilterInfo filterInfo = new FilterInfo(filterName, filter.getClass(), filterFactory);
      servletBuilder.addFilter(filterInfo.setAsyncSupported(true));
      for (DispatcherType type : DispatcherType.values()) {
        servletBuilder.addFilterUrlMapping(filterName, "/*", type);
      }
    }
    try {
      this.manager = defaultContainer().addDeployment(servletBuilder);
      this.manager.deploy();
      HttpHandler httpHandler = this.manager.start();
      this.server = Undertow.builder().addHttpListener(0, "localhost").setHandler(httpHandler).build();
    }
    catch (ServletException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void undeployConfig() {
    this.manager.undeploy();
  }

  @Override
  public void start() throws Exception {
    this.server.start();
    Undertow.ListenerInfo info = this.server.getListenerInfo().get(0);
    this.port = ((InetSocketAddress) info.getAddress()).getPort();
  }

  @Override
  public void stop() throws Exception {
    this.server.stop();
    this.port = 0;
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public ServletContext getServletContext() {
    return this.manager.getDeployment().getServletContext();
  }

  private static class DispatcherServletInstanceFactory implements InstanceFactory<Servlet> {

    private final WebApplicationContext wac;

    public DispatcherServletInstanceFactory(WebApplicationContext wac) {
      this.wac = wac;
    }

    @Override
    public InstanceHandle<Servlet> createInstance() throws InstantiationException {
      return new InstanceHandle<>() {
        @Override
        public Servlet getInstance() {
          return new DispatcherServlet(wac);
        }

        @Override
        public void release() {
        }
      };
    }
  }

  private static class FilterInstanceFactory implements InstanceFactory<Filter> {

    private final Filter filter;

    private FilterInstanceFactory(Filter filter) {
      this.filter = filter;
    }

    @Override
    public InstanceHandle<Filter> createInstance() throws InstantiationException {
      return new InstanceHandle<>() {
        @Override
        public Filter getInstance() {
          return filter;
        }

        @Override
        public void release() { }
      };
    }
  }

}
