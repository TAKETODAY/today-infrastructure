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

import org.reactivestreams.Publisher;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.NettyDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ZeroCopyHttpOutputMessage;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClientRequest;

/**
 * {@link ClientHttpRequest} implementation for the Reactor-Netty HTTP client.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
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
    getHeaders().forEach((key, value) -> this.request.requestHeaders().set(key, value));
  }

  @Override
  protected void applyCookies() {
    getCookies().values().stream().flatMap(Collection::stream)
            .map(cookie -> new DefaultCookie(cookie.getName(), cookie.getValue()))
            .forEach(this.request::addCookie);
  }

  @Override
  protected HttpHeaders initReadOnlyHeaders() {
    return HttpHeaders.readOnlyHttpHeaders(new NettyHeadersAdapter(this.request.requestHeaders()));
  }
}
