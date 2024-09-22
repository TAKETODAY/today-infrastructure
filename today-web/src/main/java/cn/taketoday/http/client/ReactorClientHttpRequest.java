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

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.concurrent.Future;
import cn.taketoday.util.concurrent.Promise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;

/**
 * {@link ClientHttpRequest} implementation for the Reactor-Netty HTTP client.
 * Created via the {@link ReactorClientHttpRequestFactory}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ReactorClientHttpRequest extends AbstractStreamingClientHttpRequest {

  private final HttpClient httpClient;

  private final HttpMethod method;

  private final URI uri;

  private final Duration exchangeTimeout;

  private final Duration readTimeout;

  public ReactorClientHttpRequest(HttpClient httpClient, URI uri, HttpMethod method,
          Duration exchangeTimeout, Duration readTimeout) {

    this.httpClient = httpClient;
    this.method = method;
    this.uri = uri;
    this.exchangeTimeout = exchangeTimeout;
    this.readTimeout = readTimeout;
  }

  @Override
  public HttpMethod getMethod() {
    return this.method;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  @Override
  protected ClientHttpResponse executeInternal(HttpHeaders headers, @Nullable Body body) throws IOException {
    HttpClient.RequestSender requestSender = httpClient
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()));

    requestSender = uri.isAbsolute() ? requestSender.uri(uri) : requestSender.uri(uri.toString());

    try {
      ReactorClientHttpResponse result = requestSender.send((reactorRequest, nettyOutbound) ->
                      send(headers, body, reactorRequest, nettyOutbound))
              .responseConnection((reactorResponse, connection) ->
                      Mono.just(new ReactorClientHttpResponse(reactorResponse, connection, readTimeout)))
              .next()
              .block(exchangeTimeout);

      if (result == null) {
        throw new IOException("HTTP exchange resulted in no result");
      }
      else {
        return result;
      }
    }
    catch (RuntimeException ex) {
      throw convertException(ex);
    }
  }

  private Publisher<Void> send(HttpHeaders headers, @Nullable Body body,
          HttpClientRequest reactorRequest, NettyOutbound nettyOutbound) {

    headers.forEach((key, value) -> reactorRequest.requestHeaders().set(key, value));

    if (body != null) {
      AtomicReference<Executor> executor = new AtomicReference<>();

      return nettyOutbound
              .withConnection(connection -> executor.set(connection.channel().eventLoop()))
              .send(FlowAdapters.toPublisher(OutputStreamPublisher.create(
                      outputStream -> body.writeTo(StreamUtils.nonClosing(outputStream)),
                      new ByteBufMapper(nettyOutbound.alloc()),
                      executor.getAndSet(null))));
    }
    else {
      return nettyOutbound;
    }
  }

  static IOException convertException(RuntimeException ex) {
    // Exceptions.ReactiveException is package private
    Throwable cause = ex.getCause();

    if (cause instanceof IOException ioEx) {
      return ioEx;
    }
    if (cause instanceof UncheckedIOException uioEx) {
      IOException ioEx = uioEx.getCause();
      if (ioEx != null) {
        return ioEx;
      }
    }
    return new IOException(ex.getMessage(), (cause != null ? cause : ex));
  }

  @Override
  protected Future<ClientHttpResponse> asyncInternal(HttpHeaders headers, @Nullable Body body) {
    HttpClient.RequestSender requestSender = httpClient
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()));

    requestSender = uri.isAbsolute() ? requestSender.uri(uri) : requestSender.uri(uri.toString());

    Promise<ClientHttpResponse> promise = Future.forPromise();
    requestSender.send((reactorRequest, nettyOutbound) -> send(headers, body, reactorRequest, nettyOutbound))
            .responseConnection((reactorResponse, connection) -> Mono.just(new ReactorClientHttpResponse(reactorResponse, connection, readTimeout)))
            .next()
            .subscribe(new CoreSubscriber<>() {

              volatile Subscription s;

              @Override
              public void onSubscribe(Subscription s) {
                s.request(1);
                promise.onCancelled(s::cancel);
                this.s = s;
              }

              @Override
              public void onNext(ReactorClientHttpResponse response) {
                promise.trySuccess(response);
              }

              @Override
              public void onError(Throwable throwable) {
                promise.tryFailure(throwable);
              }

              @Override
              public void onComplete() {

              }
            });
    return promise;
  }

  private static final class ByteBufMapper implements OutputStreamPublisher.ByteMapper<ByteBuf> {

    private final ByteBufAllocator allocator;

    public ByteBufMapper(ByteBufAllocator allocator) {
      this.allocator = allocator;
    }

    @Override
    public ByteBuf map(int b) {
      ByteBuf byteBuf = this.allocator.buffer(1);
      byteBuf.writeByte(b);
      return byteBuf;
    }

    @Override
    public ByteBuf map(byte[] b, int off, int len) {
      ByteBuf byteBuf = this.allocator.buffer(len);
      byteBuf.writeBytes(b, off, len);
      return byteBuf;
    }
  }

}
