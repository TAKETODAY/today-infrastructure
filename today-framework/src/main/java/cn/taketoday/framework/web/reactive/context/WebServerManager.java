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

package cn.taketoday.framework.web.reactive.context;

import java.util.function.Supplier;

import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.GracefulShutdownCallback;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Assert;
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

  void shutDownGracefully(GracefulShutdownCallback callback) {
    webServer.shutDownGracefully(callback);
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
