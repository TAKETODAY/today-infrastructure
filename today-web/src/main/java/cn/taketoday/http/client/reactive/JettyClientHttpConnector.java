/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.reactive.client.ContentChunk;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ClientHttpConnector} for the Jetty Reactive Streams HttpClient.
 *
 * @author Sebastien Deleuze
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">Jetty ReactiveStreams HttpClient</a>
 * @since 4.0
 */
public class JettyClientHttpConnector implements ClientHttpConnector {

  private final HttpClient httpClient;

  private DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;

  /**
   * Default constructor that creates a new instance of {@link HttpClient}.
   */
  public JettyClientHttpConnector() {
    this(new HttpClient());
  }

  /**
   * Constructor with an initialized {@link HttpClient}.
   */
  public JettyClientHttpConnector(HttpClient httpClient) {
    this(httpClient, null);
  }

  /**
   * Constructor with an initialized {@link HttpClient} and configures it
   * with the given {@link JettyResourceFactory}.
   *
   * @param httpClient the {@link HttpClient} to use
   * @param resourceFactory the {@link JettyResourceFactory} to use
   */
  public JettyClientHttpConnector(HttpClient httpClient, @Nullable JettyResourceFactory resourceFactory) {
    Assert.notNull(httpClient, "HttpClient is required");
    if (resourceFactory != null) {
      httpClient.setExecutor(resourceFactory.getExecutor());
      httpClient.setByteBufferPool(resourceFactory.getByteBufferPool());
      httpClient.setScheduler(resourceFactory.getScheduler());
    }
    this.httpClient = httpClient;
  }

  /**
   * Constructor with an {@link JettyResourceFactory} that will manage shared resources.
   *
   * @param resourceFactory the {@link JettyResourceFactory} to use
   * @param customizer the lambda used to customize the {@link HttpClient}
   */
  public JettyClientHttpConnector(
          JettyResourceFactory resourceFactory, @Nullable Consumer<HttpClient> customizer) {
    this(new HttpClient(), resourceFactory);
    if (customizer != null) {
      customizer.accept(this.httpClient);
    }
  }

  /**
   * Set the buffer factory to use.
   */
  public void setBufferFactory(DataBufferFactory bufferFactory) {
    this.bufferFactory = bufferFactory;
  }

  @Override
  public Mono<ClientHttpResponse> connect(
          HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    if (!uri.isAbsolute()) {
      return Mono.error(new IllegalArgumentException("URI is not absolute: " + uri));
    }
    if (!httpClient.isStarted()) {
      try {
        httpClient.start();
      }
      catch (Exception ex) {
        return Mono.error(ex);
      }
    }

    Request jettyRequest = httpClient.newRequest(uri).method(method.toString());
    JettyClientHttpRequest request = new JettyClientHttpRequest(jettyRequest, this.bufferFactory);
    return requestCallback.apply(request)
            .then(execute(request));
  }

  private Mono<ClientHttpResponse> execute(JettyClientHttpRequest request) {
    return Mono.fromDirect(
            request.toReactiveRequest()
                    .response((reactiveResponse, chunkPublisher) -> {
                      Flux<DataBuffer> content = Flux.from(chunkPublisher).map(this::toDataBuffer);
                      return Mono.just(new JettyClientHttpResponse(reactiveResponse, content));
                    })
    );
  }

  private DataBuffer toDataBuffer(ContentChunk chunk) {
    // Originally we copy due to do:
    // https://github.com/eclipse/jetty.project/issues/2429

    // Now that the issue is marked fixed we need to replace the below with a
    // PooledDataBuffer that adapts "release()" to "succeeded()", and also
    // evaluate if the concern here is addressed.

    DataBuffer buffer = this.bufferFactory.allocateBuffer(chunk.buffer.capacity());
    buffer.write(chunk.buffer);
    chunk.callback.succeeded();
    return buffer;
  }

}
