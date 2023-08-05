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

package cn.taketoday.framework.web.servlet.server;

import java.util.Arrays;

import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.servlet.ServletContextInitializer;
import cn.taketoday.test.web.servlet.MockServletWebServer.RegisteredFilter;
import cn.taketoday.test.web.servlet.MockServletWebServer.RegisteredServlet;
import jakarta.servlet.ServletContext;

import static org.mockito.Mockito.spy;

/**
 * Mock {@link ServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class MockServletWebServerFactory extends AbstractServletWebServerFactory {

  private MockServletWebServer webServer;

  @Override
  public MockServletWebServer getWebServer(ServletContextInitializer... initializers) {
    this.webServer = spy(new MockServletWebServer(mergeInitializers(initializers), getPort()));
    return this.webServer;
  }

  public MockServletWebServer getWebServer() {
    return getWebServer(new ServletContextInitializer[0]);
  }

  public ServletContext getServletContext() {
    return (getWebServer() != null) ? getWebServer().getServletContext() : null;
  }

  public RegisteredServlet getRegisteredServlet(int index) {
    return (getWebServer() != null) ? getWebServer().getRegisteredServlet(index) : null;
  }

  public RegisteredFilter getRegisteredFilter(int index) {
    return (getWebServer() != null) ? getWebServer().getRegisteredFilters(index) : null;
  }

  public static class MockServletWebServer
          extends cn.taketoday.test.web.servlet.MockServletWebServer implements WebServer {

    MockServletWebServer(ServletContextInitializer[] initializers, int port) {
      super(Arrays.stream(initializers).map((initializer) -> (Initializer) initializer::onStartup)
              .toArray(Initializer[]::new), port);
    }

    @Override
    public void start() {
    }

  }

}
