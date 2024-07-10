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

import org.reactivestreams.Publisher;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ZeroCopyHttpOutputMessage;
import cn.taketoday.http.support.Netty4HttpHeaders;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.channel.ChannelOperations;
import reactor.netty.http.client.HttpClientRequest;

/**
 * {@link ClientHttpRequest} implementation for the Reactor-Netty HTTP client.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see reactor.netty.http.client.HttpClient
 * @since 4.0
 */
class ReactorClientHttpRequest extends AbstractClientHttpRequest implements ZeroCopyHttpOutputMessage {

  private final URI uri;

  private final HttpMethod httpMethod;

  private final NettyOutbound outbound;

  private final HttpClientRequest request;

  private final NettyDataBufferFactory bufferFactory;

  public ReactorClientHttpRequest(HttpMethod method, URI uri, HttpClientRequest request, NettyOutbound outbound) {
    this.httpMethod = method;
    this.uri = uri;
    this.request = request;
    this.outbound = outbound;
    this.bufferFactory = new NettyDataBufferFactory(outbound.alloc());
  }

  @Override
  public HttpMethod getMethod() {
    return this.httpMethod;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return this.bufferFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getNativeRequest() {
    return (T) this.request;
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return doCommit(() -> {
      // Send as Mono if possible as an optimization hint to Reactor Netty
      if (body instanceof Mono) {
        Mono<ByteBuf> byteBufMono = Mono.from(body).map(NettyDataBufferFactory::toByteBuf);
        return this.outbound.send(byteBufMono).then();
      }
      else {
        Flux<ByteBuf> byteBufFlux = Flux.from(body).map(NettyDataBufferFactory::toByteBuf);
        return this.outbound.send(byteBufFlux).then();
      }
    });
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    Publisher<Publisher<ByteBuf>> byteBufs = Flux.from(body).map(ReactorClientHttpRequest::toByteBufs);
    return doCommit(() -> this.outbound.sendGroups(byteBufs).then());
  }

  private static Publisher<ByteBuf> toByteBufs(Publisher<? extends DataBuffer> dataBuffers) {
    return Flux.from(dataBuffers).map(NettyDataBufferFactory::toByteBuf);
  }

  @Override
  public Mono<Void> writeWith(Path file, long position, long count) {
    return doCommit(() -> this.outbound.sendFile(file, position, count).then());
  }

  @Override
  public Mono<Void> setComplete() {
    return doCommit(this.outbound::then);
  }

  @Override
  protected void applyHeaders() {
    for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
      request.requestHeaders().set(entry.getKey(), entry.getValue());
    }
  }

  @Override
  protected void applyCookies() {
    for (List<HttpCookie> values : getCookies().values()) {
      for (HttpCookie value : values) {
        DefaultCookie cookie = new DefaultCookie(value.getName(), value.getValue());
        this.request.addCookie(cookie);
      }
    }
  }

  /**
   * Saves the {@link #getAttributes() request attributes} to the
   * {@link reactor.netty.channel.ChannelOperations#channel() channel} as a single map
   * attribute under the key {@link ReactorClientHttpConnector#ATTRIBUTES_KEY}.
   */
  @Override
  protected void applyAttributes() {
    if (!getAttributes().isEmpty()) {
      ((ChannelOperations<?, ?>) this.request).channel()
              .attr(ReactorClientHttpConnector.ATTRIBUTES_KEY).set(getAttributes());
    }
  }

  @Override
  protected HttpHeaders initReadOnlyHeaders() {
    return new Netty4HttpHeaders(this.request.requestHeaders()).asReadOnly();
  }

}
