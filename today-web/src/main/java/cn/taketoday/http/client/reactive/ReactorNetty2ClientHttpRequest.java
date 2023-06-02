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

import org.reactivestreams.Publisher;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.Netty5DataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ZeroCopyHttpOutputMessage;
import io.netty5.buffer.Buffer;
import io.netty5.handler.codec.http.headers.DefaultHttpCookiePair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty5.NettyOutbound;
import reactor.netty5.http.client.HttpClientRequest;

/**
 * {@link ClientHttpRequest} implementation for the Reactor Netty 2 (Netty 5) HTTP client.
 *
 * <p>This class is based on {@link ReactorClientHttpRequest}.
 *
 * @author Violeta Georgieva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see reactor.netty5.http.client.HttpClient
 * @since 4.0
 */
class ReactorNetty2ClientHttpRequest extends AbstractClientHttpRequest implements ZeroCopyHttpOutputMessage {

  private final HttpMethod httpMethod;

  private final URI uri;

  private final HttpClientRequest request;

  private final NettyOutbound outbound;

  private final Netty5DataBufferFactory bufferFactory;

  public ReactorNetty2ClientHttpRequest(HttpMethod method, URI uri, HttpClientRequest request, NettyOutbound outbound) {
    this.httpMethod = method;
    this.uri = uri;
    this.request = request;
    this.outbound = outbound;
    this.bufferFactory = new Netty5DataBufferFactory(outbound.alloc());
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
        Mono<Buffer> bufferMono = Mono.from(body).map(Netty5DataBufferFactory::toBuffer);
        return this.outbound.send(bufferMono).then();

      }
      else {
        Flux<Buffer> bufferFlux = Flux.from(body).map(Netty5DataBufferFactory::toBuffer);
        return this.outbound.send(bufferFlux).then();
      }
    });
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    Publisher<Publisher<Buffer>> buffers = Flux.from(body).map(ReactorNetty2ClientHttpRequest::toBuffers);
    return doCommit(() -> this.outbound.sendGroups(buffers).then());
  }

  private static Publisher<Buffer> toBuffers(Publisher<? extends DataBuffer> dataBuffers) {
    return Flux.from(dataBuffers).map(Netty5DataBufferFactory::toBuffer);
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
        DefaultHttpCookiePair cookie = new DefaultHttpCookiePair(value.getName(), value.getValue());
        this.request.addCookie(cookie);
      }
    }
  }

  @Override
  protected HttpHeaders initReadOnlyHeaders() {
    return HttpHeaders.readOnlyHttpHeaders(new Netty5HeadersAdapter(this.request.requestHeaders()));
  }

}
