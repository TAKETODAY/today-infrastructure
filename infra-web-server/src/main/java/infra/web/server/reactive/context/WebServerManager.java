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

package infra.web.server.reactive.context;

import java.util.function.Supplier;

import infra.http.reactive.server.HttpHandler;
import infra.http.reactive.server.ServerHttpRequest;
import infra.http.reactive.server.ServerHttpResponse;
import infra.lang.Assert;
import infra.web.server.WebServer;
import infra.web.server.reactive.ReactiveWebServerFactory;
import reactor.core.publisher.Mono;

/**
 * Internal class used to manage the server and the {@link HttpHandler}, taking care not
 * to initialize the handler too early.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Andy Wilkinson
 * @since 4.0
 */
final class WebServerManager {

  public final ReactiveWebServerApplicationContext applicationContext;

  public final DelayedInitializationHttpHandler handler;

  public final WebServer webServer;

  WebServerManager(ReactiveWebServerApplicationContext applicationContext,
          ReactiveWebServerFactory factory, Supplier<HttpHandler> handlerSupplier, boolean lazyInit) {
    Assert.notNull(factory, "Factory is required");
    this.applicationContext = applicationContext;
    this.handler = new DelayedInitializationHttpHandler(handlerSupplier, lazyInit);
    this.webServer = factory.getWebServer(handler);
  }

  void start() {
    handler.initializeHandler();
    webServer.start();
    applicationContext.publishEvent(new ReactiveWebServerInitializedEvent(webServer, applicationContext));
  }

  void stop() {
    webServer.stop();
  }

  /**
   * A delayed {@link HttpHandler} that doesn't initialize things too early.
   */
  static final class DelayedInitializationHttpHandler implements HttpHandler {

    private final Supplier<HttpHandler> handlerSupplier;

    private final boolean lazyInit;

    volatile HttpHandler delegate = this::handleUninitialized;

    private DelayedInitializationHttpHandler(Supplier<HttpHandler> handlerSupplier, boolean lazyInit) {
      this.handlerSupplier = handlerSupplier;
      this.lazyInit = lazyInit;
    }

    private Mono<Void> handleUninitialized(ServerHttpRequest request, ServerHttpResponse response) {
      throw new IllegalStateException("The HttpHandler has not yet been initialized");
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      return delegate.handle(request, response);
    }

    void initializeHandler() {
      this.delegate = lazyInit ? new LazyHttpHandler(handlerSupplier) : handlerSupplier.get();
    }

  }

  /**
   * {@link HttpHandler} that initializes its delegate on first request.
   */
  private static final class LazyHttpHandler implements HttpHandler {

    private final Mono<HttpHandler> delegate;

    private LazyHttpHandler(Supplier<HttpHandler> handlerSupplier) {
      this.delegate = Mono.fromSupplier(handlerSupplier);
    }

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      return delegate.flatMap(handler -> handler.handle(request, response));
    }

  }

}
