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

package cn.taketoday.framework.web.embedded.netty;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cn.taketoday.framework.web.server.GracefulShutdownCallback;
import cn.taketoday.framework.web.server.GracefulShutdownResult;
import cn.taketoday.framework.web.server.PortInUseException;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.framework.web.server.WebServerException;
import cn.taketoday.http.client.ReactorResourceFactory;
import cn.taketoday.http.server.reactive.ReactorHttpHandlerAdapter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
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
   * @deprecated since 3.2.0 for removal in 3.4.0 in favor of
   * {@link #ReactorNettyWebServer(HttpServer, ReactorHttpHandlerAdapter, Duration, Shutdown, ReactorResourceFactory)}
   */
  @Deprecated(since = "3.2.0", forRemoval = true)
  public ReactorNettyWebServer(HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter, Duration lifecycleTimeout,
          Shutdown shutdown) {
    this(httpServer, handlerAdapter, lifecycleTimeout, shutdown, null);
  }

  /**
   * Creates a new {@code NettyWebServer} instance.
   *
   * @param httpServer the HTTP server
   * @param handlerAdapter the handler adapter
   * @param lifecycleTimeout the lifecycle timeout, may be {@code null}
   * @param shutdown the shutdown, may be {@code null}
   * @param resourceFactory the factory for the server's {@link LoopResources loop
   * resources}, may be {@code null}
   * {@link #ReactorNettyWebServer(HttpServer, ReactorHttpHandlerAdapter, Duration, Shutdown, ReactorResourceFactory)}
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
              .info("Netty started{}", getStartedOnMessage(disposableServer));
      startDaemonAwaitThread(disposableServer);
    }
  }

  private String getStartedOnMessage(DisposableServer server) {
    StringBuilder message = new StringBuilder();
    tryAppend(message, "port %s", server::port);
    tryAppend(message, "path %s", server::path);
    return (message.length() > 0) ? "Netty started on " + message : "Netty started";
  }

  protected String getStartedLogMessage() {
    return getStartedOnMessage(this.disposableServer);
  }

  private void tryAppend(StringBuilder message, String format, Supplier<Object> supplier) {
    try {
      Object value = supplier.get();
      message.append((message.length() != 0) ? " " : "");
      message.append(String.format(format, value));
    }
    catch (UnsupportedOperationException ignored) { }
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
      Assert.notNull(resources, "No LoopResources: is ReactorResourceFactory not initialized yet?");
      server = server.runOn(resources);
    }

    if (this.lifecycleTimeout != null) {
      return server.bindNow(this.lifecycleTimeout);
    }
    return server.bindNow();
  }

  private boolean isPermissionDenied(Throwable bindExceptionCause) {
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
