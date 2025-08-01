/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.http.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

import infra.context.SmartLifecycle;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
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
public class ReactorClientHttpRequestFactory implements ClientHttpRequestFactory, SmartLifecycle {

  private static final Logger logger = LoggerFactory.getLogger(ReactorClientHttpRequestFactory.class);

  private static final Function<HttpClient, HttpClient> defaultInitializer =
          client -> client.compress(true)
                  .responseTimeout(Duration.ofSeconds(10))
                  .proxyWithSystemProperties();

  @Nullable
  private final ReactorResourceFactory resourceFactory;

  @Nullable
  private final Function<HttpClient, HttpClient> mapper;

  @Nullable
  private Integer connectTimeout;

  @Nullable
  private Duration readTimeout;

  @Nullable
  private Duration exchangeTimeout;

  @Nullable
  private volatile HttpClient httpClient;

  private final Object lifecycleMonitor = new Object();

  /**
   * Constructor with default client, created via {@link HttpClient#create()},
   * and with {@link HttpClient#compress compression} enabled.
   */
  public ReactorClientHttpRequestFactory() {
    this(defaultInitializer.apply(HttpClient.create()));
  }

  /**
   * Constructor with a given {@link HttpClient} instance.
   *
   * @param client the client to use
   */
  public ReactorClientHttpRequestFactory(HttpClient client) {
    Assert.notNull(client, "HttpClient is required");
    this.resourceFactory = null;
    this.mapper = null;
    this.httpClient = client;
  }

  /**
   * Constructor with externally managed Reactor Netty resources, including
   * {@link LoopResources} for event loop threads, and {@link ConnectionProvider}
   * for connection pooling.
   * <p>Generally, it is recommended to share resources for event loop
   * concurrency. This can be achieved either by participating in the JVM-wide,
   * global resources held in {@link reactor.netty.http.HttpResources}, or by
   * using a specific, shared set of resources through a
   * {@link ReactorResourceFactory} bean. The latter can ensure that resources
   * are shut down when the Spring ApplicationContext is stopped/closed and
   * restarted again (e.g. JVM checkpoint restore).
   *
   * @param resourceFactory the resource factory to get resources from
   * @param mapper for further initialization of the client
   */
  public ReactorClientHttpRequestFactory(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
    this.resourceFactory = resourceFactory;
    this.mapper = mapper;
    if (resourceFactory.isRunning()) {
      this.httpClient = createHttpClient(resourceFactory, mapper);
    }
  }

  private HttpClient createHttpClient(ReactorResourceFactory factory, Function<HttpClient, HttpClient> mapper) {
    HttpClient client = HttpClient.create(factory.getConnectionProvider());
    client = defaultInitializer.andThen(mapper).apply(client);
    client = client.runOn(factory.getLoopResources());
    if (this.connectTimeout != null) {
      client = client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectTimeout);
    }
    if (this.readTimeout != null) {
      client = client.responseTimeout(this.readTimeout);
    }
    return client;
  }

  /**
   * Set the connect timeout value on the underlying client.
   * Effectively, a shortcut for
   * {@code httpClient.option(CONNECT_TIMEOUT_MILLIS, timeout)}.
   * <p>By default, set to 30 seconds.
   *
   * @param connectTimeout the timeout value in millis; use 0 to never time out.
   * @see HttpClient#option(ChannelOption, Object)
   * @see ChannelOption#CONNECT_TIMEOUT_MILLIS
   * @see <a href="https://projectreactor.io/docs/netty/release/reference/index.html#connection-timeout">Connection Timeout</a>
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
   * Variant of {@link #setConnectTimeout(int)} with a {@link Duration} value.
   */
  public void setConnectTimeout(Duration connectTimeout) {
    Assert.notNull(connectTimeout, "ConnectTimeout is required");
    setConnectTimeout((int) connectTimeout.toMillis());
  }

  /**
   * Set the read timeout value on the underlying client.
   * Effectively, a shortcut for {@link HttpClient#responseTimeout(Duration)}.
   * <p>By default, set to 10 seconds.
   *
   * @param timeout the read timeout value in millis; must be > 0.
   */
  public void setReadTimeout(Duration timeout) {
    Assert.notNull(timeout, "ReadTimeout is required");
    Assert.isTrue(timeout.toMillis() > 0, "Timeout must be a positive value");
    this.readTimeout = timeout;
    HttpClient httpClient = this.httpClient;
    if (httpClient != null) {
      this.httpClient = httpClient.responseTimeout(timeout);
    }
  }

  /**
   * Variant of {@link #setReadTimeout(Duration)} with a long value.
   */
  public void setReadTimeout(long readTimeout) {
    setReadTimeout(Duration.ofMillis(readTimeout));
  }

  /**
   * Set the timeout for the HTTP exchange in milliseconds.
   *
   * @see #setConnectTimeout(int)
   * @see #setReadTimeout(Duration)
   */
  public void setExchangeTimeout(long exchangeTimeout) {
    Assert.isTrue(exchangeTimeout > 0, "Timeout must be a positive value");
    setExchangeTimeout(Duration.ofMillis(exchangeTimeout));
  }

  /**
   * Variant of {@link #setExchangeTimeout(long)} with a Duration value.
   *
   * @see #setConnectTimeout(int)
   * @see #setReadTimeout(Duration)
   */
  public void setExchangeTimeout(Duration exchangeTimeout) {
    Assert.notNull(exchangeTimeout, "ExchangeTimeout is required");
    this.exchangeTimeout = exchangeTimeout;
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
    HttpClient client = this.httpClient;
    if (client == null) {
      Assert.state(this.resourceFactory != null && this.mapper != null,
              "Expected HttpClient or ResourceFactory and mapper");
      client = createHttpClient(this.resourceFactory, this.mapper);
    }
    return new ReactorClientHttpRequest(client, httpMethod, uri, this.exchangeTimeout);
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
      logger.warn("Restarting a ReactorClientHttpRequestFactory bean is only supported " +
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
    return 1; // start after ReactorResourceFactory (0)
  }

}
