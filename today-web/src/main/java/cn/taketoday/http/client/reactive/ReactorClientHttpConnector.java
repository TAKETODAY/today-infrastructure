/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.http.client.reactive;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

/**
 * Reactor-Netty implementation of {@link ClientHttpConnector}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see reactor.netty.http.client.HttpClient
 * @since 4.0
 */
public class ReactorClientHttpConnector implements ClientHttpConnector {

  private final static Function<HttpClient, HttpClient> defaultInitializer = client -> client.compress(true);

  private final HttpClient httpClient;

  /**
   * Default constructor. Initializes {@link HttpClient} via:
   * <pre class="code">
   * HttpClient.create().compress()
   * </pre>
   */
  public ReactorClientHttpConnector() {
    this.httpClient = defaultInitializer.apply(HttpClient.create());
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
   * resources are shut down when the ApplicationContext is closed.
   *
   * @param factory the resource factory to obtain the resources from
   * @param mapper a mapper for further initialization of the created client
   */
  public ReactorClientHttpConnector(ReactorResourceFactory factory, Function<HttpClient, HttpClient> mapper) {
    ConnectionProvider provider = factory.getConnectionProvider();
    Assert.notNull(provider, "No ConnectionProvider: is ReactorResourceFactory not initialized yet?");
    this.httpClient = defaultInitializer.andThen(mapper)
            .andThen(applyLoopResources(factory))
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
  }

  @Override
  public Mono<ClientHttpResponse> connect(
          HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {
    AtomicReference<ReactorClientHttpResponse> responseRef = new AtomicReference<>();
    return this.httpClient
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()))
            .uri(uri)
            .send((request, outbound) -> requestCallback.apply(adaptRequest(method, uri, request, outbound)))
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

  private ReactorClientHttpRequest adaptRequest(
          HttpMethod method, URI uri, HttpClientRequest request, NettyOutbound nettyOutbound) {
    return new ReactorClientHttpRequest(method, uri, request, nettyOutbound);
  }

}
