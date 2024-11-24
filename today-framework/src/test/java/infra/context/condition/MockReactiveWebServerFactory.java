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

package infra.context.condition;

import java.util.Map;

import infra.web.server.reactive.AbstractReactiveWebServerFactory;
import infra.web.server.reactive.ReactiveWebServerFactory;
import infra.web.server.WebServer;
import infra.http.server.reactive.HttpHandler;

import static org.mockito.Mockito.spy;

/**
 * Mock {@link ReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 */
class MockReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

  private MockReactiveWebServer webServer;

  @Override
  public WebServer getWebServer(HttpHandler httpHandler) {
    this.webServer = spy(new MockReactiveWebServer(httpHandler, getPort()));
    return this.webServer;
  }

  public MockReactiveWebServer getWebServer() {
    return this.webServer;
  }

  static class MockReactiveWebServer implements WebServer {

    private final int port;

    private HttpHandler httpHandler;

    private Map<String, HttpHandler> httpHandlerMap;

    MockReactiveWebServer(HttpHandler httpHandler, int port) {
      this.httpHandler = httpHandler;
      this.port = port;
    }

    MockReactiveWebServer(Map<String, HttpHandler> httpHandlerMap, int port) {
      this.httpHandlerMap = httpHandlerMap;
      this.port = port;
    }

    HttpHandler getHttpHandler() {
      return this.httpHandler;
    }

    Map<String, HttpHandler> getHttpHandlerMap() {
      return this.httpHandlerMap;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public int getPort() {
      return this.port;
    }

  }

}
