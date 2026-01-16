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

package infra.web.reactor.netty;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import infra.http.client.ReactorResourceFactory;
import infra.http.server.reactive.ReactorHttpHandlerAdapter;
import infra.lang.Assert;
import infra.logging.LoggerFactory;
import infra.web.server.GracefulShutdownCallback;
import infra.web.server.GracefulShutdownResult;
import infra.web.server.PortInUseException;
import infra.web.server.Shutdown;
import infra.web.server.WebServer;
import infra.web.server.WebServerException;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.unix.Errors.NativeIoException;
import io.netty.util.concurrent.DefaultEventExecutor;
import reactor.netty.ChannelBindException;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerRoutes;
import reactor.netty.resources.LoopResources;

/**
 * {@link WebServer} that can be used to control a Reactor Netty web server. Usually this
 * class should be created using the {@link ReactorNettyReactiveWebServerFactory} and not
 * directly.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactorNettyWebServer implements WebServer {

  /**
   * Permission denied error code from {@code errno.h}.
   */
  private static final int ERROR_NO_EACCES = -13;

  private static final Predicate<HttpServerRequest> ALWAYS = request -> true;

  private final HttpServer httpServer;

  private final ReactorHttpHandlerAdapter handler;

  @Nullable
  private final Duration lifecycleTimeout;

  @Nullable
  private final GracefulShutdown gracefulShutdown;

  @Nullable
  private final ReactorResourceFactory resourceFactory;

  private List<NettyRouteProvider> routeProviders = Collections.emptyList();

  @Nullable
  private volatile DisposableServer disposableServer;

  /**
   * Creates a new {@code NettyWebServer} instance.
   *
   * @param httpServer the HTTP server
   * @param handlerAdapter the handler adapter
   * @param lifecycleTimeout the lifecycle timeout, may be {@code null}
   * @param shutdown the shutdown, may be {@code null}
   * @param resourceFactory the factory for the server's {@link LoopResources loop
   * resources}, may be {@code null}
   */
  public ReactorNettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter,
          @Nullable Duration lifecycleTimeout, Shutdown shutdown, @Nullable ReactorResourceFactory resourceFactory) {
    Assert.notNull(httpServer, "HttpServer is required");
    Assert.notNull(handlerAdapter, "HandlerAdapter is required");
    this.handler = handlerAdapter;
    this.resourceFactory = resourceFactory;
    this.lifecycleTimeout = lifecycleTimeout;
    this.httpServer = httpServer.channelGroup(new DefaultChannelGroup(new DefaultEventExecutor()));
    this.gracefulShutdown = (shutdown == Shutdown.GRACEFUL)
            ? new GracefulShutdown(() -> this.disposableServer)
            : null;
  }

  public void setRouteProviders(List<NettyRouteProvider> routeProviders) {
    this.routeProviders = routeProviders;
  }

  @Override
  public void start() throws WebServerException {
    DisposableServer disposableServer = this.disposableServer;
    if (disposableServer == null) {
      try {
        disposableServer = startHttpServer();
        this.disposableServer = disposableServer;
      }
      catch (Exception ex) {
        PortInUseException.ifCausedBy(ex, ChannelBindException.class, (bindException) -> {
          if (bindException.localPort() > 0 && !isPermissionDenied(bindException.getCause())) {
            throw new PortInUseException(bindException.localPort(), ex);
          }
        });
        throw new WebServerException("Unable to start Netty", ex);
      }
      LoggerFactory.getLogger(ReactorNettyWebServer.class)
              .info(getStartedOnMessage(disposableServer));
      startDaemonAwaitThread(disposableServer);
    }
  }

  private String getStartedOnMessage(DisposableServer server) {
    StringBuilder message = new StringBuilder();
    tryAppend(message, "port %s", () -> server.port()
            + ((this.httpServer.configuration().sslProvider() != null) ? " (https)" : " (http)"));
    tryAppend(message, "path %s", server::path);
    return (!message.isEmpty()) ? "Netty started on " + message : "Netty started";
  }

  @SuppressWarnings("NullAway")
  protected String getStartedLogMessage() {
    return getStartedOnMessage(this.disposableServer);
  }

  private void tryAppend(StringBuilder message, String format, Supplier<Object> supplier) {
    try {
      Object value = supplier.get();
      message.append((!message.isEmpty()) ? " " : "");
      message.append(String.format(format, value));
    }
    catch (UnsupportedOperationException ignored) {
    }
  }

  DisposableServer startHttpServer() {
    HttpServer server = this.httpServer;
    if (this.routeProviders.isEmpty()) {
      server = server.handle(this.handler);
    }
    else {
      server = server.route(this::applyRouteProviders);
    }
    if (this.resourceFactory != null) {
      LoopResources resources = this.resourceFactory.getLoopResources();
      server = server.runOn(resources);
    }

    if (this.lifecycleTimeout != null) {
      return server.bindNow(this.lifecycleTimeout);
    }
    return server.bindNow();
  }

  private boolean isPermissionDenied(@Nullable Throwable bindExceptionCause) {
    try {
      if (bindExceptionCause instanceof NativeIoException e) {
        return e.expectedErr() == ERROR_NO_EACCES;
      }
    }
    catch (Throwable ignored) {
    }
    return false;
  }

  @Override
  public void shutDownGracefully(GracefulShutdownCallback callback) {
    if (this.gracefulShutdown == null) {
      callback.shutdownComplete(GracefulShutdownResult.IMMEDIATE);
      return;
    }
    this.gracefulShutdown.shutDownGracefully(callback);
  }

  private void applyRouteProviders(HttpServerRoutes routes) {
    for (NettyRouteProvider provider : this.routeProviders) {
      routes = provider.apply(routes);
    }
    routes.route(ALWAYS, this.handler);
  }

  private void startDaemonAwaitThread(DisposableServer disposableServer) {
    Thread awaitThread = new Thread("server") {

      @Override
      public void run() {
        disposableServer.onDispose().block();
      }

    };
    awaitThread.setContextClassLoader(getClass().getClassLoader());
    awaitThread.setDaemon(false);
    awaitThread.start();
  }

  @Override
  public void stop() throws WebServerException {
    DisposableServer disposableServer = this.disposableServer;
    if (disposableServer != null) {
      if (this.gracefulShutdown != null) {
        this.gracefulShutdown.abort();
      }
      try {
        if (this.lifecycleTimeout != null) {
          disposableServer.disposeNow(this.lifecycleTimeout);
        }
        else {
          disposableServer.disposeNow();
        }
      }
      catch (IllegalStateException ex) {
        // Continue
      }
      this.disposableServer = null;
    }
  }

  @Override
  public int getPort() {
    DisposableServer disposableServer = this.disposableServer;
    if (disposableServer != null) {
      try {
        return disposableServer.port();
      }
      catch (UnsupportedOperationException ex) {
        return -1;
      }
    }
    return -1;
  }

}
