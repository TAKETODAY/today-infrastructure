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

package cn.taketoday.http.client;

import org.reactivestreams.FlowAdapters;
import org.reactivestreams.Publisher;

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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientRequest;

/**
 * {@link ClientHttpRequest} implementation for the Reactor-Netty HTTP client.
 * Created via the {@link ReactorNettyClientRequestFactory}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ReactorNettyClientRequest extends AbstractStreamingClientHttpRequest {

  private final HttpClient httpClient;

  private final HttpMethod method;

  private final URI uri;

  private final Duration exchangeTimeout;

  private final Duration readTimeout;

  public ReactorNettyClientRequest(HttpClient httpClient, URI uri, HttpMethod method,
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
    HttpClient.RequestSender requestSender = this.httpClient
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(this.method.name()));

    requestSender = (this.uri.isAbsolute() ? requestSender.uri(this.uri) : requestSender.uri(this.uri.toString()));

    try {
      ReactorNettyClientResponse result = requestSender.send((reactorRequest, nettyOutbound) ->
                      send(headers, body, reactorRequest, nettyOutbound))
              .responseConnection((reactorResponse, connection) ->
                      Mono.just(new ReactorNettyClientResponse(reactorResponse, connection, this.readTimeout)))
              .next()
              .block(this.exchangeTimeout);

      if (result == null) {
        throw new IOException("HTTP exchange resulted in no result");
      }
      else {
        return result;
      }
    }
    catch (RuntimeException ex) { // Exceptions.ReactiveException is package private
      Throwable cause = ex.getCause();

      if (cause instanceof UncheckedIOException uioEx) {
        throw uioEx.getCause();
      }
      else if (cause instanceof IOException ioEx) {
        throw ioEx;
      }
      else {
        throw ex;
      }
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
