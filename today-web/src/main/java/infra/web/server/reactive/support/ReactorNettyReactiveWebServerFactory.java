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

package infra.web.server.reactive.support;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.http.client.ReactorResourceFactory;
import infra.http.server.reactive.HttpHandler;
import infra.http.server.reactive.ReactorHttpHandlerAdapter;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.CollectionUtils;
import infra.web.server.Compression;
import infra.web.server.Shutdown;
import infra.web.server.Ssl;
import infra.web.server.WebServer;
import infra.web.server.reactive.AbstractReactiveWebServerFactory;
import infra.web.server.reactive.ReactiveWebServerFactory;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link ReactorNettyWebServer}s.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactorNettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

  private Set<ReactorNettyServerCustomizer> serverCustomizers = new LinkedHashSet<>();

  private final List<NettyRouteProvider> routeProviders = new ArrayList<>();

  @Nullable
  private Duration lifecycleTimeout;

  private boolean useForwardHeaders;

  @Nullable
  private ReactorResourceFactory resourceFactory;

  public ReactorNettyReactiveWebServerFactory() {

  }

  public ReactorNettyReactiveWebServerFactory(int port) {
    super(port);
  }

  @Override
  public WebServer getWebServer(HttpHandler httpHandler) {
    HttpServer httpServer = createHttpServer();
    var handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
    var webServer = createNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, getShutdown());
    webServer.setRouteProviders(this.routeProviders);
    return webServer;
  }

  ReactorNettyWebServer createNettyWebServer(HttpServer httpServer,
          ReactorHttpHandlerAdapter handlerAdapter, @Nullable Duration lifecycleTimeout, Shutdown shutdown) {
    return new ReactorNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, shutdown, resourceFactory);
  }

  /**
   * Returns a mutable collection of the {@link ReactorNettyServerCustomizer}s that will be
   * applied to the Netty server builder.
   *
   * @return the customizers that will be applied
   */
  public Collection<ReactorNettyServerCustomizer> getServerCustomizers() {
    return this.serverCustomizers;
  }

  /**
   * Set {@link ReactorNettyServerCustomizer}s that should be applied to the Netty server
   * builder. Calling this method will replace any existing customizers.
   *
   * @param serverCustomizers the customizers to set
   */
  public void setServerCustomizers(Collection<? extends ReactorNettyServerCustomizer> serverCustomizers) {
    Assert.notNull(serverCustomizers, "ServerCustomizers is required");
    this.serverCustomizers = new LinkedHashSet<>(serverCustomizers);
  }

  /**
   * Add {@link ReactorNettyServerCustomizer}s that should be applied while building the
   * server.
   *
   * @param serverCustomizers the customizers to add
   */
  public void addServerCustomizers(ReactorNettyServerCustomizer... serverCustomizers) {
    CollectionUtils.addAll(this.serverCustomizers, serverCustomizers);
  }

  /**
   * Add {@link NettyRouteProvider}s that should be applied, in order, before the
   * handler for the Framework application.
   *
   * @param routeProviders the route providers to add
   */
  public void addRouteProviders(NettyRouteProvider... routeProviders) {
    CollectionUtils.addAll(this.routeProviders, routeProviders);
  }

  /**
   * Set the maximum amount of time that should be waited when starting or stopping the
   * server.
   *
   * @param lifecycleTimeout the lifecycle timeout
   */
  public void setLifecycleTimeout(Duration lifecycleTimeout) {
    this.lifecycleTimeout = lifecycleTimeout;
  }

  /**
   * Set if x-forward-* headers should be processed.
   *
   * @param useForwardHeaders if x-forward headers should be used
   */
  public void setUseForwardHeaders(boolean useForwardHeaders) {
    this.useForwardHeaders = useForwardHeaders;
  }

  /**
   * Set the {@link ReactorResourceFactory} to get the shared resources from.
   *
   * @param resourceFactory the server resources
   */
  public void setResourceFactory(ReactorResourceFactory resourceFactory) {
    this.resourceFactory = resourceFactory;
  }

  private HttpServer createHttpServer() {
    HttpServer server = HttpServer.create().bindAddress(this::getListenAddress);
    if (Ssl.isEnabled(getSsl())) {
      server = customizeSslConfiguration(getSsl(), server);
    }
    if (Compression.isEnabled(getCompression())) {
      server = new CompressionCustomizer(getCompression()).apply(server);
    }
    server = server.protocol(listProtocols()).forwarded(this.useForwardHeaders);
    return applyCustomizers(server);
  }

  private HttpServer customizeSslConfiguration(Ssl ssl, HttpServer httpServer) {
    SslServerCustomizer customizer = new SslServerCustomizer(
            isHttp2Enabled(), ssl, getSslBundle(), getServerNameSslBundles());
    addBundleUpdateHandler(ssl, customizer::updateSslBundle);
    return customizer.apply(httpServer);
  }

  private HttpProtocol[] listProtocols() {
    ArrayList<HttpProtocol> protocols = new ArrayList<>();
    protocols.add(HttpProtocol.HTTP11);
    if (isHttp2Enabled()) {
      if (Ssl.isEnabled(getSsl())) {
        protocols.add(HttpProtocol.H2);
      }
      else {
        protocols.add(HttpProtocol.H2C);
      }
    }
    return protocols.toArray(new HttpProtocol[0]);
  }

  private InetSocketAddress getListenAddress() {
    if (getAddress() != null) {
      return new InetSocketAddress(getAddress().getHostAddress(), getPort());
    }
    return new InetSocketAddress(getPort());
  }

  private HttpServer applyCustomizers(HttpServer server) {
    for (ReactorNettyServerCustomizer customizer : this.serverCustomizers) {
      server = customizer.apply(server);
    }
    return server;
  }

}
