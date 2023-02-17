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

import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.reactive.client.ContentChunk;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.util.Callback;
import org.reactivestreams.Publisher;

import java.net.HttpCookie;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
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
    this.builder = ReactiveRequest.newBuilder(jettyRequest).abortOnCancel(true);
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.valueOf(jettyRequest.getMethod());
  }

  @Override
  public URI getURI() {
    return jettyRequest.getURI();
  }

  @Override
  public Mono<Void> setComplete() {
    return doCommit();
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return bufferFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getNativeRequest() {
    return (T) jettyRequest;
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return Mono.<Void>create(sink -> {
              ReactiveRequest.Content content = Flux.from(body)
                      .map(buffer -> toContentChunk(buffer, sink))
                      .as(chunks -> ReactiveRequest.Content.fromPublisher(chunks, getContentType()));
              builder.content(content);
              sink.success();
            })
            .then(doCommit());
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return writeWith(Flux.from(body)
            .flatMap(Function.identity())
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release));
  }

  private String getContentType() {
    MediaType contentType = getHeaders().getContentType();
    return contentType != null ? contentType.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
  }

  private ContentChunk toContentChunk(DataBuffer dataBuffer, MonoSink<Void> sink) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
    dataBuffer.toByteBuffer(byteBuffer);
    return new ContentChunk(byteBuffer, new Callback() {
      @Override
      public void succeeded() {
        DataBufferUtils.release(dataBuffer);
      }

      @Override
      public void failed(Throwable t) {
        DataBufferUtils.release(dataBuffer);
        sink.error(t);
      }
    });
  }

  @Override
  protected void applyCookies() {
    for (Map.Entry<String, List<cn.taketoday.http.HttpCookie>> entry : getCookies().entrySet()) {
      for (cn.taketoday.http.HttpCookie cookie : entry.getValue()) {
        jettyRequest.cookie(new HttpCookie(cookie.getName(), cookie.getValue()));
      }
    }
  }

  @Override
  protected void applyHeaders() {
    HttpHeaders headers = getHeaders();
    jettyRequest.headers(fields -> {
      for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
        String key = entry.getKey();
        for (String v : entry.getValue()) {
          fields.add(key, v);
        }
      }
      if (!headers.containsKey(HttpHeaders.ACCEPT)) {
        fields.add(HttpHeaders.ACCEPT, "*/*");
      }
    });
  }

  @Override
  protected HttpHeaders initReadOnlyHeaders() {
    return HttpHeaders.readOnlyHttpHeaders(new JettyHeadersAdapter(jettyRequest.getHeaders()));
  }

  public ReactiveRequest toReactiveRequest() {
    return builder.build();
  }

}
