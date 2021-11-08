/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.server.reactive.bootstrap;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

import cn.taketoday.http.server.reactive.JettyHttpHandlerAdapter;
import cn.taketoday.http.server.reactive.ServletHttpHandlerAdapter;

/**
 * @author Rossen Stoyanchev
 */
public class JettyHttpServer extends AbstractHttpServer {

  private Server jettyServer;

  private ServletContextHandler contextHandler;

  @Override
  protected void initServer() throws Exception {

    this.jettyServer = new Server();

    ServletHttpHandlerAdapter servlet = createServletAdapter();
    ServletHolder servletHolder = new ServletHolder(servlet);
    servletHolder.setAsyncSupported(true);

    this.contextHandler = new ServletContextHandler(this.jettyServer, "", false, false);
    this.contextHandler.addServlet(servletHolder, "/");
    this.contextHandler.addServletContainerInitializer(new JettyWebSocketServletContainerInitializer());
    this.contextHandler.start();

    ServerConnector connector = new ServerConnector(this.jettyServer);
    connector.setHost(getHost());
    connector.setPort(getPort());
    this.jettyServer.addConnector(connector);
  }

  private ServletHttpHandlerAdapter createServletAdapter() {
    return new JettyHttpHandlerAdapter(resolveHttpHandler());
  }

  @Override
  protected void startInternal() throws Exception {
    this.jettyServer.start();
    setPort(((ServerConnector) this.jettyServer.getConnectors()[0]).getLocalPort());
  }

  @Override
  protected void stopInternal() throws Exception {
    try {
      if (this.contextHandler.isRunning()) {
        this.contextHandler.stop();
      }
    }
    finally {
      try {
        if (this.jettyServer.isRunning()) {
          this.jettyServer.setStopTimeout(5000);
          this.jettyServer.stop();
          this.jettyServer.destroy();
        }
      }
      catch (Exception ex) {
        // ignore
      }
    }
  }

  @Override
  protected void resetInternal() {
    try {
      if (this.jettyServer.isRunning()) {
        this.jettyServer.setStopTimeout(5000);
        this.jettyServer.stop();
        this.jettyServer.destroy();
      }
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    finally {
      this.jettyServer = null;
      this.contextHandler = null;
    }
  }

}
