/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ThreadPool;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.framework.web.reactive.server.AbstractReactiveWebServerFactory;
import cn.taketoday.framework.web.reactive.server.ReactiveWebServerFactory;
import cn.taketoday.framework.web.server.Shutdown;
import cn.taketoday.framework.web.server.WebServer;
import cn.taketoday.http.client.reactive.JettyResourceFactory;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.JettyHttpHandlerAdapter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link JettyWebServer}s.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public class JettyReactiveWebServerFactory extends AbstractReactiveWebServerFactory
        implements ConfigurableJettyWebServerFactory {

  private static final Logger logger = LoggerFactory.getLogger(JettyReactiveWebServerFactory.class);

  /**
   * The number of acceptor threads to use.
   */
  private int acceptors = -1;

  /**
   * The number of selector threads to use.
   */
  private int selectors = -1;

  private boolean useForwardHeaders;

  private Set<JettyServerCustomizer> jettyServerCustomizers = new LinkedHashSet<>();

  @Nullable
  private JettyResourceFactory resourceFactory;

  @Nullable
  private ThreadPool threadPool;

  /**
   * Create a new {@link JettyServletWebServerFactory} instance.
   */
  public JettyReactiveWebServerFactory() { }

  /**
   * Create a new {@link JettyServletWebServerFactory} that listens for requests using
   * the specified port.
   *
   * @param port the port to listen on
   */
  public JettyReactiveWebServerFactory(int port) {
    super(port);
  }

  @Override
  public void setUseForwardHeaders(boolean useForwardHeaders) {
    this.useForwardHeaders = useForwardHeaders;
  }

  @Override
  public void setAcceptors(int acceptors) {
    this.acceptors = acceptors;
  }

  @Override
  public WebServer getWebServer(HttpHandler httpHandler) {
    JettyHttpHandlerAdapter servlet = new JettyHttpHandlerAdapter(httpHandler);
    Server server = createJettyServer(servlet);
    return new JettyWebServer(server, getPort() >= 0);
  }

  @Override
  public void addServerCustomizers(JettyServerCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    this.jettyServerCustomizers.addAll(Arrays.asList(customizers));
  }

  /**
   * Sets {@link JettyServerCustomizer}s that will be applied to the {@link Server}
   * before it is started. Calling this method will replace any existing customizers.
   *
   * @param customizers the Jetty customizers to apply
   */
  public void setServerCustomizers(Collection<? extends JettyServerCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    this.jettyServerCustomizers = new LinkedHashSet<>(customizers);
  }

  /**
   * Returns a mutable collection of Jetty {@link JettyServerCustomizer}s that will be
   * applied to the {@link Server} before it is created.
   *
   * @return the Jetty customizers
   */
  public Collection<JettyServerCustomizer> getServerCustomizers() {
    return this.jettyServerCustomizers;
  }

  /**
   * Returns a Jetty {@link ThreadPool} that should be used by the {@link Server}.
   *
   * @return a Jetty {@link ThreadPool} or {@code null}
   */
  @Nullable
  public ThreadPool getThreadPool() {
    return this.threadPool;
  }

  @Override
  public void setThreadPool(ThreadPool threadPool) {
    this.threadPool = threadPool;
  }

  @Override
  public void setSelectors(int selectors) {
    this.selectors = selectors;
  }

  /**
   * Set the {@link JettyResourceFactory} to get the shared resources from.
   *
   * @param resourceFactory the server resources
   * @since 4.0
   */
  public void setResourceFactory(@Nullable JettyResourceFactory resourceFactory) {
    this.resourceFactory = resourceFactory;
  }

  @Nullable
  public JettyResourceFactory getResourceFactory() {
    return this.resourceFactory;
  }

  protected Server createJettyServer(JettyHttpHandlerAdapter servlet) {
    int port = Math.max(getPort(), 0);
    InetSocketAddress address = new InetSocketAddress(getAddress(), port);
    Server server = new Server(getThreadPool());
    server.addConnector(createConnector(address, server));
    server.setStopTimeout(0);
    ServletHolder servletHolder = new ServletHolder(servlet);
    servletHolder.setAsyncSupported(true);
    ServletContextHandler contextHandler = new ServletContextHandler(server, "/", false, false);
    contextHandler.addServlet(servletHolder, "/");
    server.setHandler(addHandlerWrappers(contextHandler));
    JettyReactiveWebServerFactory.logger.info("Server initialized with port: " + port);
    if (getSsl() != null && getSsl().isEnabled()) {
      customizeSsl(server, address);
    }
    for (JettyServerCustomizer customizer : getServerCustomizers()) {
      customizer.customize(server);
    }
    if (this.useForwardHeaders) {
      new ForwardHeadersCustomizer().customize(server);
    }
    if (getShutdown() == Shutdown.GRACEFUL) {
      StatisticsHandler statisticsHandler = new StatisticsHandler();
      statisticsHandler.setHandler(server.getHandler());
      server.setHandler(statisticsHandler);
    }
    return server;
  }

  private AbstractConnector createConnector(InetSocketAddress address, Server server) {
    HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setSendServerVersion(false);
    List<ConnectionFactory> connectionFactories = new ArrayList<>();
    connectionFactories.add(new HttpConnectionFactory(httpConfiguration));
    if (getHttp2() != null && getHttp2().isEnabled()) {
      connectionFactories.add(new HTTP2CServerConnectionFactory(httpConfiguration));
    }
    JettyResourceFactory resourceFactory = getResourceFactory();
    ServerConnector connector;
    if (resourceFactory != null) {
      connector = new ServerConnector(server, resourceFactory.getExecutor(), resourceFactory.getScheduler(),
              resourceFactory.getByteBufferPool(), this.acceptors, this.selectors,
              connectionFactories.toArray(new ConnectionFactory[0]));
    }
    else {
      connector = new ServerConnector(server, this.acceptors, this.selectors,
              connectionFactories.toArray(new ConnectionFactory[0]));
    }
    connector.setHost(address.getHostString());
    connector.setPort(address.getPort());
    return connector;
  }

  private Handler addHandlerWrappers(Handler handler) {
    if (getCompression() != null && getCompression().isEnabled()) {
      handler = applyWrapper(handler, JettyHandlerWrappers.createGzipHandlerWrapper(getCompression()));
    }
    if (StringUtils.hasText(getServerHeader())) {
      handler = applyWrapper(handler, JettyHandlerWrappers.createServerHeaderHandlerWrapper(getServerHeader()));
    }
    return handler;
  }

  private Handler applyWrapper(Handler handler, HandlerWrapper wrapper) {
    wrapper.setHandler(handler);
    return wrapper;
  }

  private void customizeSsl(Server server, InetSocketAddress address) {
    new SslServerCustomizer(address, getSsl(), getOrCreateSslStoreProvider(), getHttp2()).customize(server);
  }

}
