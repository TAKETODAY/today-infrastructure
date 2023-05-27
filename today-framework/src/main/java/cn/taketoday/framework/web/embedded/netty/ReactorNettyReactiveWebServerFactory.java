/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.Http2;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.Ssl;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.client.reactive.ReactorResourceFactory;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.ReactorHttpHandlerAdapter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link ReactorNettyWebServer}s.
 *
 * @author Brian Clozel
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

  public ReactorNettyReactiveWebServerFactory() { }

  public ReactorNettyReactiveWebServerFactory(int port) {
    super(port);
  }

  @Override
  public WebServer getWebServer(HttpHandler httpHandler) {
    HttpServer httpServer = createHttpServer();
    var handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);
    ReactorNettyWebServer webServer = createNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, getShutdown());
    webServer.setRouteProviders(this.routeProviders);
    return webServer;
  }

  ReactorNettyWebServer createNettyWebServer(
          HttpServer httpServer, ReactorHttpHandlerAdapter handlerAdapter,
          @Nullable Duration lifecycleTimeout, Shutdown shutdown) {
    return new ReactorNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, shutdown);
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
    Assert.notNull(serverCustomizers, "ServerCustomizers must not be null");
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
    HttpServer server = HttpServer.create();
    if (this.resourceFactory != null) {
      LoopResources resources = this.resourceFactory.getLoopResources();
      Assert.notNull(resources, "No LoopResources: is ReactorResourceFactory not initialized yet?");
      server = server.runOn(resources).bindAddress(this::getListenAddress);
    }
    else {
      server = server.bindAddress(this::getListenAddress);
    }
    if (Ssl.isEnabled(getSsl())) {
      server = customizeSslConfiguration(getSsl(), server);
    }
    if (getCompression() != null && getCompression().isEnabled()) {
      CompressionCustomizer compressionCustomizer = new CompressionCustomizer(getCompression());
      server = compressionCustomizer.apply(server);
    }
    server = server.protocol(listProtocols()).forwarded(this.useForwardHeaders);
    return applyCustomizers(server);
  }

  private HttpServer customizeSslConfiguration(Ssl ssl, HttpServer httpServer) {
    return new SslServerCustomizer(getHttp2(), ssl.getClientAuth(), getSslBundle()).apply(httpServer);
  }

  private HttpProtocol[] listProtocols() {
    ArrayList<HttpProtocol> protocols = new ArrayList<>();
    protocols.add(HttpProtocol.HTTP11);
    if (Http2.isEnabled(getHttp2())) {
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
