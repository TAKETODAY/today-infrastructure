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

package cn.taketoday.web.http.client.reactive;

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.util.Callback;
import org.reactivestreams.Publisher;

import java.net.HttpCookie;
import java.net.URI;
import java.util.Collection;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.PooledDataBuffer;
import cn.taketoday.util.MediaType;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpMethod;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * {@link ClientHttpRequest} implementation for the Jetty ReactiveStreams HTTP client.
 *
 * @author Sebastien Deleuze
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">Jetty ReactiveStreams HttpClient</a>
 * @since 4.0
 */
class JettyClientHttpRequest extends AbstractClientHttpRequest {

  private final Request jettyRequest;

  private final DataBufferFactory bufferFactory;

  private final ReactiveRequest.Builder builder;

  public JettyClientHttpRequest(Request jettyRequest, DataBufferFactory bufferFactory) {
    this.jettyRequest = jettyRequest;
    this.bufferFactory = bufferFactory;
    this.builder = ReactiveRequest.newBuilder(this.jettyRequest).abortOnCancel(true);
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.valueOf(this.jettyRequest.getMethod());
  }

  @Override
  public URI getURI() {
    return this.jettyRequest.getURI();
  }

  @Override
  public Mono<Void> setComplete() {
    return doCommit();
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return this.bufferFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getNativeRequest() {
    return (T) this.jettyRequest;
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return Mono.<Void>create(sink -> {
              ReactiveRequest.Content content = Flux.from(body)
                      .map(buffer -> toContentChunk(buffer, sink))
                      .as(chunks -> ReactiveRequest.Content.fromPublisher(chunks, getContentType()));
              this.builder.content(content);
              sink.success();
            })
            .then(doCommit());
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return writeWith(Flux.from(body)
                             .flatMap(Function.identity())
                             .doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
  }

  private String getContentType() {
    MediaType contentType = getHeaders().getContentType();
    return contentType != null ? contentType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
  }

  private ContentChunk toContentChunk(DataBuffer buffer, MonoSink<Void> sink) {
    return new ContentChunk(buffer.asByteBuffer(), new Callback() {
      @Override
      public void succeeded() {
        DataBufferUtils.release(buffer);
      }

      @Override
      public void failed(Throwable t) {
        DataBufferUtils.release(buffer);
        sink.error(t);
      }
    });
  }

  @Override
  protected void applyCookies() {
    getCookies().values().stream().flatMap(Collection::stream)
            .map(cookie -> new HttpCookie(cookie.getName(), cookie.getValue()))
            .forEach(this.jettyRequest::cookie);
  }

  @Override
  protected void applyHeaders() {
    HttpHeaders headers = getHeaders();
    this.jettyRequest.headers(fields -> {
      headers.forEach((key, value) -> value.forEach(v -> fields.add(key, v)));
      if (!headers.containsKey(HttpHeaders.ACCEPT)) {
        fields.add(HttpHeaders.ACCEPT, "*/*");
      }
    });
  }

  public ReactiveRequest toReactiveRequest() {
    return this.builder.build();
  }

}
