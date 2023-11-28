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

package cn.taketoday.web.socket;

import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;

import java.util.EnumSet;

import cn.taketoday.web.servlet.DispatcherServlet;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;

/**
 * Jetty based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class JettyWebSocketTestServer implements WebSocketTestServer {

  private Server jettyServer;

  private int port;

  private ServletContextHandler contextHandler;

  @Override
  public void setup() {
    // Let server pick its own random, available port.
    this.jettyServer = new Server(0);
  }

  @Override
  public void deployConfig(WebApplicationContext wac, Filter... filters) {
    ServletHolder servletHolder = new ServletHolder(new DispatcherServlet(wac));
    this.contextHandler = new ServletContextHandler();
    this.contextHandler.addServlet(servletHolder, "/");
    this.contextHandler.addServletContainerInitializer(new JettyWebSocketServletContainerInitializer());
    for (Filter filter : filters) {
      this.contextHandler.addFilter(new FilterHolder(filter), "/*", getDispatcherTypes());
    }
    this.jettyServer.setHandler(this.contextHandler);
  }

  private EnumSet<DispatcherType> getDispatcherTypes() {
    return EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC);
  }

  @Override
  public void undeployConfig() {
    // Stopping jetty will undeploy the servlet
  }

  @Override
  public void start() throws Exception {
    this.jettyServer.start();
    this.contextHandler.start();

    Connector[] connectors = jettyServer.getConnectors();
    NetworkConnector connector = (NetworkConnector) connectors[0];
    this.port = connector.getLocalPort();
  }

  @Override
  public void stop() throws Exception {
    try {
      if (this.contextHandler.isRunning()) {
        this.contextHandler.stop();
      }
    }
    finally {
      if (this.jettyServer.isRunning()) {
        this.jettyServer.setStopTimeout(5000);
        this.jettyServer.stop();
      }
    }
  }

  @Override
  public int getPort() {
    return this.port;
  }

  @Override
  public ServletContext getServletContext() {
    return this.contextHandler.getServletContext();
  }

}
