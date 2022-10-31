/*
 * Copyright 2012-2021 the original author or authors.
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

package cn.taketoday.annotation.config.web.reactive;

import java.util.Map;

import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.server.reactive.HttpHandler;

import static org.mockito.Mockito.spy;

/**
 * Mock {@link ReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 */
public class MockReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

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
