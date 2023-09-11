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

package cn.taketoday.web.testfixture.http.server.reactive.bootstrap;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import cn.taketoday.http.server.reactive.JettyHttpHandlerAdapter;
import cn.taketoday.http.server.reactive.ServletHttpHandlerAdapter;

/**
 * @author Rossen Stoyanchev
 * @author Sam Brannen
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

    this.contextHandler = new ServletContextHandler("", false, false);
    this.contextHandler.addServlet(servletHolder, "/");
    this.contextHandler.addServletContainerInitializer(new JettyWebSocketServletContainerInitializer());

    ServerConnector connector = new ServerConnector(this.jettyServer);
    connector.setHost(getHost());
    connector.setPort(getPort());
    this.jettyServer.addConnector(connector);
    this.jettyServer.setHandler(this.contextHandler);
    this.contextHandler.start();
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
          // Do not configure a large stop timeout. For example, setting a stop timeout
          // of 5000 adds an additional 1-2 seconds to the runtime of each test using
          // the Jetty sever, resulting in 2-4 extra minutes of overall build time.
          this.jettyServer.setStopTimeout(100);
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
        // Do not configure a large stop timeout. For example, setting a stop timeout
        // of 5000 adds an additional 1-2 seconds to the runtime of each test using
        // the Jetty sever, resulting in 2-4 extra minutes of overall build time.
        this.jettyServer.setStopTimeout(100);
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
