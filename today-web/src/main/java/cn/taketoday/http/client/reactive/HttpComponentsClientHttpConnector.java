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

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStreamResetException;
import org.apache.hc.core5.http.Message;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.reactive.ReactiveResponseConsumer;
import org.reactivestreams.Publisher;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * {@link ClientHttpConnector} implementation for the Apache HttpComponents HttpClient 5.x.
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 * @since 4.0
 */
public class HttpComponentsClientHttpConnector implements ClientHttpConnector, Closeable {

  private final CloseableHttpAsyncClient client;
  private final BiFunction<HttpMethod, URI, ? extends HttpClientContext> contextProvider;
  private DataBufferFactory dataBufferFactory = DefaultDataBufferFactory.sharedInstance;

  /**
   * Default constructor that creates and starts a new instance of {@link CloseableHttpAsyncClient}.
   */
  public HttpComponentsClientHttpConnector() {
    this(HttpAsyncClients.createDefault());
  }

  /**
   * Constructor with a pre-configured {@link CloseableHttpAsyncClient} instance.
   *
   * @param client the client to use
   */
  public HttpComponentsClientHttpConnector(CloseableHttpAsyncClient client) {
    this(client, (method, uri) -> HttpClientContext.create());
  }

  /**
   * Constructor with a pre-configured {@link CloseableHttpAsyncClient} instance
   * and a {@link HttpClientContext} supplier lambda which is called before each request
   * and passed to the client.
   *
   * @param client the client to use
   * @param contextProvider a {@link HttpClientContext} supplier
   */
  public HttpComponentsClientHttpConnector(
          CloseableHttpAsyncClient client,
          BiFunction<HttpMethod, URI, ? extends HttpClientContext> contextProvider) {
    Assert.notNull(client, "Client must not be null");
    Assert.notNull(contextProvider, "ContextProvider must not be null");
    this.client = client;
    this.contextProvider = contextProvider;
    this.client.start();
  }

  /**
   * Set the buffer factory to use.
   */
  public void setBufferFactory(DataBufferFactory bufferFactory) {
    this.dataBufferFactory = bufferFactory;
  }

  @Override
  public Mono<ClientHttpResponse> connect(
          HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    HttpClientContext context = this.contextProvider.apply(method, uri);
    if (context.getCookieStore() == null) {
      context.setCookieStore(new BasicCookieStore());
    }

    var request = new HttpComponentsClientHttpRequest(method, uri, context, dataBufferFactory);
    return requestCallback.apply(request)
            .then(Mono.defer(() -> execute(request, context)));
  }

  private Mono<ClientHttpResponse> execute(HttpComponentsClientHttpRequest request, HttpClientContext context) {
    AsyncRequestProducer requestProducer = request.toRequestProducer();

    return Mono.create(sink -> {
      var responseConsumer = new ReactiveResponseConsumer(
              new MonoFutureCallbackAdapter(sink, this.dataBufferFactory, context));
      this.client.execute(requestProducer, responseConsumer, context, null);
    });
  }

  @Override
  public void close() throws IOException {
    this.client.close();
  }

  private record MonoFutureCallbackAdapter(
          MonoSink<ClientHttpResponse> sink, DataBufferFactory dataBufferFactory, HttpClientContext context)
          implements FutureCallback<Message<HttpResponse, Publisher<ByteBuffer>>> {

    @Override
    public void completed(Message<HttpResponse, Publisher<ByteBuffer>> result) {
      var response = new HttpComponentsClientHttpResponse(this.dataBufferFactory, result, this.context);
      this.sink.success(response);
    }

    @Override
    public void failed(Exception ex) {
      Throwable t = ex;
      if (t instanceof HttpStreamResetException) {
        HttpStreamResetException httpStreamResetException = (HttpStreamResetException) ex;
        t = httpStreamResetException.getCause();
      }
      this.sink.error(t);
    }

    @Override
    public void cancelled() { }
  }

}
