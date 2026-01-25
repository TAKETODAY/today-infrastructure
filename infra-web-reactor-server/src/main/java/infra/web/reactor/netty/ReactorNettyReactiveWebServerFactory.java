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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.http.reactive.server.HttpHandler;
import infra.http.reactive.server.ReactorHttpHandlerAdapter;
import infra.http.support.ReactorResourceFactory;
import infra.lang.Assert;
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
  public WebServer createWebServer(HttpHandler httpHandler) {
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
    HttpServer server = HttpServer.create().bindAddress(this::bindAddress);
    Ssl ssl = getSsl();
    if (Ssl.isEnabled(ssl)) {
      server = customizeSslConfiguration(ssl, server);
    }
    Compression compression = getCompression();
    if (Compression.isEnabled(compression)) {
      server = new CompressionCustomizer(compression).apply(server);
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

  private HttpServer applyCustomizers(HttpServer server) {
    for (ReactorNettyServerCustomizer customizer : this.serverCustomizers) {
      server = customizer.apply(server);
    }
    return server;
  }

}
