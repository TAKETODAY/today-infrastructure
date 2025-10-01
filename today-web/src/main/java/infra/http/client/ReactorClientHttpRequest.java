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

import org.jspecify.annotations.Nullable;
import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.util.StreamUtils;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
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

  @Nullable
  private final Duration exchangeTimeout;

  /**
   * Package private constructor for use until exchangeTimeout is removed.
   */
  ReactorClientHttpRequest(HttpClient httpClient, HttpMethod method, URI uri, @Nullable Duration exchangeTimeout) {
    this.httpClient = httpClient;
    this.method = method;
    this.uri = uri;
    this.exchangeTimeout = exchangeTimeout;
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
    HttpClient.RequestSender sender = httpClient
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()));

    sender = uri.isAbsolute() ? sender.uri(uri) : sender.uri(uri.toString());

    try {
      Mono<ReactorClientHttpResponse> mono = sender.send((request, outbound) -> send(headers, body, request, outbound))
              .responseConnection((response, conn) -> Mono.just(new ReactorClientHttpResponse(response, conn)))
              .next();

      ReactorClientHttpResponse clientResponse =
              exchangeTimeout != null ? mono.block(exchangeTimeout) : mono.block();

      if (clientResponse == null) {
        throw new IOException("HTTP exchange resulted in no result");
      }
      return clientResponse;
    }
    catch (RuntimeException ex) {
      throw convertException(ex);
    }
  }

  private Publisher<Void> send(HttpHeaders headers, @Nullable Body body,
          HttpClientRequest reactorRequest, NettyOutbound nettyOutbound) {

    var entries = reactorRequest.requestHeaders();
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      entries.set(entry.getKey(), entry.getValue());
    }

    if (body == null) {
      // NettyOutbound#subscribe calls then() and that expects a body
      // Use empty Mono instead for a more optimal send
      return Mono.empty();
    }

    ByteBufMapper byteMapper = new ByteBufMapper(nettyOutbound.alloc());
    AtomicReference<Executor> executor = new AtomicReference<>();

    return nettyOutbound
            .withConnection(connection -> executor.set(connection.channel().eventLoop()))
            .send(FlowAdapters.toPublisher(new OutputStreamPublisher<>(
                    os -> body.writeTo(StreamUtils.nonClosing(os)), byteMapper,
                    executor.getAndSet(null), null)));
  }

  static IOException convertException(RuntimeException ex) {
    Throwable cause = ex.getCause(); // Exceptions.ReactiveException is private

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
  protected Future<ClientHttpResponse> asyncInternal(HttpHeaders headers, @Nullable Body body, @Nullable Executor executor) {
    HttpClient.RequestSender requestSender = httpClient
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(method.name()));

    requestSender = uri.isAbsolute() ? requestSender.uri(uri) : requestSender.uri(uri.toString());

    Promise<ClientHttpResponse> promise = Future.forPromise(executor);
    requestSender.send((request, nettyOutbound) -> send(headers, body, request, nettyOutbound))
            .responseConnection((reactorResponse, connection) -> Mono.just(new ReactorClientHttpResponse(reactorResponse, connection)))
            .next()
            .subscribe(new CoreSubscriber<>() {

              @SuppressWarnings("NullAway")
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
    if (exchangeTimeout != null) {
      return promise.timeout(exchangeTimeout);
    }
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
