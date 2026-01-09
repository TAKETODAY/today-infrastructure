/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.util.StreamUtils;
import infra.util.concurrent.Future;
import infra.util.concurrent.Promise;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
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

  private final Executor executor;

  /**
   * Create an instance.
   *
   * @param httpClient the client to perform the request with
   * @param method the HTTP method
   * @param uri the URI for the request
   */
  public ReactorClientHttpRequest(HttpClient httpClient, HttpMethod method, URI uri) {
    this(httpClient, method, uri, null);
  }

  /**
   * Create an instance.
   * <p>If no executor is provided, the request will use an {@link Schedulers#boundedElastic() elastic scheduler}
   * for performing blocking I/O operations.
   *
   * @param httpClient the client to perform the request with
   * @param executor the executor to use
   * @param method the HTTP method
   * @param uri the URI for the request
   */
  public ReactorClientHttpRequest(HttpClient httpClient, HttpMethod method, URI uri, @Nullable Executor executor) {
    this(httpClient, method, uri, executor, null);
  }

  /**
   * Package private constructor for use until exchangeTimeout is removed.
   */
  ReactorClientHttpRequest(HttpClient httpClient, HttpMethod method, URI uri, @Nullable Executor executor, @Nullable Duration exchangeTimeout) {
    this.httpClient = httpClient;
    this.method = method;
    this.uri = uri;
    this.executor = (executor != null) ? executor : Schedulers.boundedElastic()::schedule;
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

    return nettyOutbound.send(FlowAdapters.toPublisher(new OutputStreamPublisher<>(
            os -> body.writeTo(StreamUtils.nonClosing(os)), new ByteBufMapper(nettyOutbound.alloc()),
            executor, null)));
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
