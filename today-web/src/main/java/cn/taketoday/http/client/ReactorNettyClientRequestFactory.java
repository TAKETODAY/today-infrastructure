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

package cn.taketoday.http.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * Reactor-Netty implementation of {@link ClientHttpRequestFactory}.
 *
 * <p>This class implements {@link SmartLifecycle} and can be optionally declared
 * as a Infra-managed bean in order to support JVM Checkpoint Restore.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactorNettyClientRequestFactory implements ClientHttpRequestFactory, SmartLifecycle {

  private static final Logger logger = LoggerFactory.getLogger(ReactorNettyClientRequestFactory.class);

  private static final Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);

  @Nullable
  private final ReactorResourceFactory resourceFactory;

  @Nullable
  private final Function<HttpClient, HttpClient> mapper;

  @Nullable
  private Integer connectTimeout;

  private Duration readTimeout = Duration.ofSeconds(10);

  private Duration exchangeTimeout = Duration.ofSeconds(5);

  @Nullable
  private volatile HttpClient httpClient;

  private final Object lifecycleMonitor = new Object();

  /**
   * Create a new instance of the {@code ReactorNettyClientRequestFactory}
   * with a default {@link HttpClient} that has compression enabled.
   */
  public ReactorNettyClientRequestFactory() {
    this.httpClient = defaultInitializer.apply(HttpClient.create());
    this.resourceFactory = null;
    this.mapper = null;
  }

  /**
   * Create a new instance of the {@code ReactorNettyClientRequestFactory}
   * based on the given {@link HttpClient}.
   *
   * @param httpClient the client to base on
   */
  public ReactorNettyClientRequestFactory(HttpClient httpClient) {
    Assert.notNull(httpClient, "HttpClient must not be null");
    this.httpClient = httpClient;
    this.resourceFactory = null;
    this.mapper = null;
  }

  /**
   * Constructor with externally managed Reactor Netty resources, including
   * {@link LoopResources} for event loop threads, and {@link ConnectionProvider}
   * for the connection pool.
   * <p>This constructor should be used only when you don't want the client
   * to participate in the Reactor Netty global resources. By default the
   * client participates in the Reactor Netty global resources held in
   * {@link reactor.netty.http.HttpResources}, which is recommended since
   * fixed, shared resources are favored for event loop concurrency. However,
   * consider declaring a {@link ReactorResourceFactory} bean with
   * {@code globalResources=true} in order to ensure the Reactor Netty global
   * resources are shut down when the Spring ApplicationContext is stopped or closed
   * and restarted properly when the Spring ApplicationContext is
   * (with JVM Checkpoint Restore for example).
   *
   * @param resourceFactory the resource factory to obtain the resources from
   * @param mapper a mapper for further initialization of the created client
   */
  public ReactorNettyClientRequestFactory(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
    this.resourceFactory = resourceFactory;
    this.mapper = mapper;
    if (resourceFactory.isRunning()) {
      this.httpClient = createHttpClient(resourceFactory, mapper);
    }
  }

  /**
   * Set the underlying connect timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   * <p>Default is 30 seconds.
   *
   * @see HttpClient#option(ChannelOption, Object)
   * @see ChannelOption#CONNECT_TIMEOUT_MILLIS
   */
  public void setConnectTimeout(int connectTimeout) {
    Assert.isTrue(connectTimeout >= 0, "Timeout must be a non-negative value");
    this.connectTimeout = connectTimeout;
    HttpClient httpClient = this.httpClient;
    if (httpClient != null) {
      this.httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
    }
  }

  /**
   * Set the underlying connect timeout in milliseconds.
   * A value of 0 specifies an infinite timeout.
   * <p>Default is 30 seconds.
   *
   * @see HttpClient#option(ChannelOption, Object)
   * @see ChannelOption#CONNECT_TIMEOUT_MILLIS
   */
  public void setConnectTimeout(Duration connectTimeout) {
    Assert.notNull(connectTimeout, "ConnectTimeout must not be null");
    setConnectTimeout((int) connectTimeout.toMillis());
  }

  /**
   * Set the underlying read timeout in milliseconds.
   * <p>Default is 10 seconds.
   */
  public void setReadTimeout(long readTimeout) {
    Assert.isTrue(readTimeout > 0, "Timeout must be a positive value");
    this.readTimeout = Duration.ofMillis(readTimeout);
  }

  /**
   * Set the underlying read timeout as {@code Duration}.
   * <p>Default is 10 seconds.
   */
  public void setReadTimeout(Duration readTimeout) {
    Assert.notNull(readTimeout, "ReadTimeout must not be null");
    Assert.isTrue(!readTimeout.isNegative(), "Timeout must be a non-negative value");
    this.readTimeout = readTimeout;
  }

  /**
   * Set the timeout for the HTTP exchange in milliseconds.
   * <p>Default is 5 seconds.
   */
  public void setExchangeTimeout(long exchangeTimeout) {
    Assert.isTrue(exchangeTimeout > 0, "Timeout must be a positive value");
    this.exchangeTimeout = Duration.ofMillis(exchangeTimeout);
  }

  /**
   * Set the timeout for the HTTP exchange.
   * <p>Default is 5 seconds.
   */
  public void setExchangeTimeout(Duration exchangeTimeout) {
    Assert.notNull(exchangeTimeout, "ExchangeTimeout must not be null");
    Assert.isTrue(!exchangeTimeout.isNegative(), "Timeout must be a non-negative value");
    this.exchangeTimeout = exchangeTimeout;
  }

  private HttpClient createHttpClient(ReactorResourceFactory factory, Function<HttpClient, HttpClient> mapper) {
    HttpClient httpClient = defaultInitializer.andThen(mapper)
            .apply(HttpClient.create(factory.getConnectionProvider()));
    httpClient = httpClient.runOn(factory.getLoopResources());
    if (this.connectTimeout != null) {
      httpClient = httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
    }
    return httpClient;
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
    HttpClient httpClient = this.httpClient;
    if (httpClient == null) {
      Assert.state(this.resourceFactory != null && this.mapper != null, "Illegal configuration");
      httpClient = createHttpClient(this.resourceFactory, this.mapper);
    }
    return new ReactorNettyClientRequest(httpClient, uri, httpMethod, this.exchangeTimeout, this.readTimeout);
  }

  @Override
  public void start() {
    if (this.resourceFactory != null && this.mapper != null) {
      synchronized(this.lifecycleMonitor) {
        if (this.httpClient == null) {
          this.httpClient = createHttpClient(this.resourceFactory, this.mapper);
        }
      }
    }
    else {
      logger.warn("Restarting a ReactorNettyClientRequestFactory bean is only supported " +
              "with externally managed Reactor Netty resources");
    }
  }

  @Override
  public void stop() {
    if (this.resourceFactory != null && this.mapper != null) {
      synchronized(this.lifecycleMonitor) {
        this.httpClient = null;
      }
    }
  }

  @Override
  public boolean isRunning() {
    return (this.httpClient != null);
  }

  @Override
  public int getPhase() {
    // Start after ReactorResourceFactory
    return 1;
  }

}
