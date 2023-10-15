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

package cn.taketoday.http.client.reactive;

import java.net.URI;
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

  private static final Logger logger = LoggerFactory.getLogger(ReactorClientHttpConnector.class);

  private final static Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);

  private HttpClient httpClient;

  @Nullable
  private final ReactorResourceFactory resourceFactory;

  @Nullable
  private final Function<HttpClient, HttpClient> mapper;

  private volatile boolean running = true;

  private final Object lifecycleMonitor = new Object();

  /**
   * Default constructor. Initializes {@link HttpClient} via:
   * <pre class="code">
   * HttpClient.create().compress()
   * </pre>
   */
  public ReactorClientHttpConnector() {
    this.httpClient = defaultInitializer.apply(HttpClient.create());
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
   * resources are shut down when the Spring ApplicationContext is closed.
   *
   * @param resourceFactory the resource factory to obtain the resources from
   * @param mapper a mapper for further initialization of the created client
   */
  public ReactorClientHttpConnector(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
    this.httpClient = createHttpClient(resourceFactory, mapper);
    this.resourceFactory = resourceFactory;
    this.mapper = mapper;
  }

  private static HttpClient createHttpClient(ReactorResourceFactory resourceFactory, Function<HttpClient, HttpClient> mapper) {
    ConnectionProvider provider = resourceFactory.getConnectionProvider();
    Assert.notNull(provider, "No ConnectionProvider: is ReactorResourceFactory not initialized yet?");
    return defaultInitializer.andThen(mapper).andThen(applyLoopResources(resourceFactory))
            .apply(HttpClient.create(provider));
  }

  private static Function<HttpClient, HttpClient> applyLoopResources(ReactorResourceFactory factory) {
    return httpClient -> {
      LoopResources resources = factory.getLoopResources();
      Assert.notNull(resources, "No LoopResources: is ReactorResourceFactory not initialized yet?");
      return httpClient.runOn(resources);
    };
  }

  /**
   * Constructor with a pre-configured {@code HttpClient} instance.
   *
   * @param httpClient the client to use
   */
  public ReactorClientHttpConnector(HttpClient httpClient) {
    Assert.notNull(httpClient, "HttpClient is required");
    this.httpClient = httpClient;
    this.resourceFactory = null;
    this.mapper = null;
  }

  @Override
  public Mono<ClientHttpResponse> connect(HttpMethod method, URI uri,
          Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    AtomicReference<ReactorClientHttpResponse> responseRef = new AtomicReference<>();
    HttpClient.RequestSender requestSender = this.httpClient
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()));

    requestSender = setUri(requestSender, uri);

    return requestSender.send((request, outbound) -> requestCallback.apply(adaptRequest(method, uri, request, outbound)))
            .responseConnection((response, connection) -> {
              ReactorClientHttpResponse newValue = new ReactorClientHttpResponse(response, connection);
              responseRef.set(newValue);
              return Mono.just((ClientHttpResponse) newValue);
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
    synchronized(this.lifecycleMonitor) {
      if (!isRunning()) {
        if (this.resourceFactory != null && this.mapper != null) {
          this.httpClient = createHttpClient(this.resourceFactory, this.mapper);
        }
        else {
          logger.warn("Restarting a ReactorClientHttpConnector bean is only supported with externally managed Reactor Netty resources");
        }
        this.running = true;
      }
    }
  }

  @Override
  public void stop() {
    synchronized(this.lifecycleMonitor) {
      if (isRunning()) {
        this.running = false;
      }
    }
  }

  @Override
  public final void stop(Runnable callback) {
    synchronized(this.lifecycleMonitor) {
      stop();
      callback.run();
    }
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

  @Override
  public boolean isAutoStartup() {
    return false;
  }

  @Override
  public int getPhase() {
    // Start after ReactorResourceFactory
    return 1;
  }

}
