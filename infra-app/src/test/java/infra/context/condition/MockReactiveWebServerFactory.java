/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.condition;

import java.util.Map;

import infra.http.server.reactive.HttpHandler;
import infra.web.server.WebServer;
import infra.web.server.reactive.AbstractReactiveWebServerFactory;
import infra.web.server.reactive.ReactiveWebServerFactory;

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
