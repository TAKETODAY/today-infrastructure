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

package cn.taketoday.http.client.reactive;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import cn.taketoday.context.Lifecycle;
import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ReactorResourceFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import io.netty.util.AttributeKey;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * Reactor-Netty implementation of {@link ClientHttpConnector}.
 *
 * <p>This class implements {@link Lifecycle} and can be optionally declared
 * as a Infra-managed bean.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see reactor.netty.http.client.HttpClient
 * @since 4.0
 */
public class ReactorClientHttpConnector implements ClientHttpConnector, SmartLifecycle {

  /**
   * Channel attribute key under which {@code WebClient} request attributes are stored as a Map.
   *
   * @since 5.0
   */
  public static final AttributeKey<Map<String, Object>> ATTRIBUTES_KEY =
          AttributeKey.valueOf(ReactorClientHttpRequest.class.getName() + ".ATTRIBUTES");

  private static final Logger logger = LoggerFactory.getLogger(ReactorClientHttpConnector.class);

  private static final Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);

  @Nullable
  private final ReactorResourceFactory resourceFactory;

  @Nullable
  private final Function<HttpClient, HttpClient> mapper;

  @Nullable
  private volatile HttpClient httpClient;

  private boolean lazyStart = false;

  private final Object lifecycleMonitor = new Object();

  /**
   * Default constructor. Initializes {@link HttpClient} via:
   * <pre class="code">HttpClient.create().compress(true)</pre>
   */
  public ReactorClientHttpConnector() {
    this.httpClient = defaultInitializer.apply(HttpClient.create());
    this.resourceFactory = null;
    this.mapper = null;
  }

  /**
   * Constructor with a pre-configured {@code HttpClient} instance.
   *
   * @param httpClient the client to use
   * @since 5.0
   */
  public ReactorClientHttpConnector(HttpClient httpClient) {
    Assert.notNull(httpClient, "HttpClient is required");
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
  public ReactorClientHttpConnector(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
    this.resourceFactory = resourceFactory;
    this.mapper = mapper;
    if (resourceFactory.isRunning()) {
      this.httpClient = createHttpClient(resourceFactory, mapper);
    }
    else {
      this.lazyStart = true;
    }
  }

  private static HttpClient createHttpClient(ReactorResourceFactory factory, Function<HttpClient, HttpClient> mapper) {
    return defaultInitializer.andThen(mapper)
            .andThen(httpClient -> httpClient.runOn(factory.getLoopResources()))
            .apply(HttpClient.create(factory.getConnectionProvider()));
  }

  @Override
  public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
          Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    HttpClient httpClient = this.httpClient;
    if (httpClient == null) {
      Assert.state(this.resourceFactory != null && this.mapper != null, "Illegal configuration");
      if (this.resourceFactory.isRunning()) {
        // Retain HttpClient instance if resource factory has been started in the meantime,
        // considering this connector instance as lazily started as well.
        synchronized(this.lifecycleMonitor) {
          httpClient = this.httpClient;
          if (httpClient == null && this.lazyStart) {
            httpClient = createHttpClient(this.resourceFactory, this.mapper);
            this.httpClient = httpClient;
            this.lazyStart = false;
          }
        }
      }
      if (httpClient == null) {
        httpClient = createHttpClient(this.resourceFactory, this.mapper);
      }
    }

    HttpClient.RequestSender requestSender = httpClient
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()));

    requestSender = setUri(requestSender, uri);
    AtomicReference<ReactorClientHttpResponse> responseRef = new AtomicReference<>();

    return requestSender
            .send((request, outbound) -> requestCallback.apply(adaptRequest(method, uri, request, outbound)))
            .responseConnection((response, connection) -> {
              responseRef.set(new ReactorClientHttpResponse(response, connection));
              return Mono.just((ClientHttpResponse) responseRef.get());
            })
            .next()
            .doOnCancel(() -> {
              ReactorClientHttpResponse response = responseRef.get();
              if (response != null) {
                response.releaseAfterCancel(method);
              }
            });
  }

  private static HttpClient.RequestSender setUri(HttpClient.RequestSender requestSender, URI uri) {
    if (uri.isAbsolute()) {
      try {
        return requestSender.uri(uri);
      }
      catch (Exception ex) {
        // Fall back on passing it in as a String
      }
    }
    return requestSender.uri(uri.toString());
  }

  private ReactorClientHttpRequest adaptRequest(HttpMethod method, URI uri, HttpClientRequest request,
          NettyOutbound nettyOutbound) {

    return new ReactorClientHttpRequest(method, uri, request, nettyOutbound);
  }

  @Override
  public void start() {
    if (this.resourceFactory != null && this.mapper != null) {
      synchronized(this.lifecycleMonitor) {
        if (this.httpClient == null) {
          this.httpClient = createHttpClient(this.resourceFactory, this.mapper);
          this.lazyStart = false;
        }
      }
    }
    else {
      logger.warn("Restarting a ReactorClientHttpConnector bean is only supported " +
              "with externally managed Reactor Netty resources");
    }
  }

  @Override
  public void stop() {
    if (this.resourceFactory != null && this.mapper != null) {
      synchronized(this.lifecycleMonitor) {
        this.httpClient = null;
        this.lazyStart = false;
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
