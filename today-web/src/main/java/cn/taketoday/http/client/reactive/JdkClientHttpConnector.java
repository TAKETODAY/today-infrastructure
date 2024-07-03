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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Mono;

/**
 * {@link ClientHttpConnector} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html">HttpClient</a>
 * @since 4.0
 */
public class JdkClientHttpConnector implements ClientHttpConnector {

  private final HttpClient httpClient;

  private DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;

  @Nullable
  private Duration readTimeout = null;

  /**
   * Default constructor that uses {@link HttpClient#newHttpClient()}.
   */
  public JdkClientHttpConnector() {
    this(HttpClient.newHttpClient());
  }

  /**
   * Constructor with an initialized {@link HttpClient} and a {@link DataBufferFactory}.
   */
  public JdkClientHttpConnector(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Constructor with a {@link JdkHttpClientResourceFactory} that provides
   * shared resources.
   *
   * @param clientBuilder a pre-initialized builder for the client that will
   * be further initialized with the shared resources to use
   * @param resourceFactory the {@link JdkHttpClientResourceFactory} to use
   */
  public JdkClientHttpConnector(
          HttpClient.Builder clientBuilder, @Nullable JdkHttpClientResourceFactory resourceFactory) {

    if (resourceFactory != null) {
      Executor executor = resourceFactory.getExecutor();
      clientBuilder.executor(executor);
    }
    this.httpClient = clientBuilder.build();
  }

  /**
   * Set the buffer factory to use.
   * <p>By default, this is {@link DefaultDataBufferFactory#sharedInstance}.
   */
  public void setBufferFactory(DataBufferFactory bufferFactory) {
    Assert.notNull(bufferFactory, "DataBufferFactory is required");
    this.bufferFactory = bufferFactory;
  }

  /**
   * Set the underlying {@code HttpClient}'s read timeout as a {@code Duration}.
   * <p>Default is the system's default timeout.
   *
   * @see java.net.http.HttpRequest.Builder#timeout
   * @since 5.0
   */
  public void setReadTimeout(Duration readTimeout) {
    Assert.notNull(readTimeout, "readTimeout is required");
    this.readTimeout = readTimeout;
  }

  @Override
  public Mono<ClientHttpResponse> connect(
          HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    JdkClientHttpRequest jdkClientHttpRequest = new JdkClientHttpRequest(method, uri, this.bufferFactory, this.readTimeout);

    return requestCallback.apply(jdkClientHttpRequest).then(Mono.defer(() -> {
      HttpRequest httpRequest = jdkClientHttpRequest.getNativeRequest();

      CompletableFuture<HttpResponse<Flow.Publisher<List<ByteBuffer>>>> future =
              this.httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofPublisher());

      return Mono.fromCompletionStage(future)
              .map(response -> new JdkClientHttpResponse(response, this.bufferFactory));
    }));
  }

}
