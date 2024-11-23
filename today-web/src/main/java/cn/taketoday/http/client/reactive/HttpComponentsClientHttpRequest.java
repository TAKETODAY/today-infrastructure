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

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.message.BasicHttpRequest;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.reactive.ReactiveEntityProducer;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static cn.taketoday.http.MediaType.ALL_VALUE;

/**
 * {@link ClientHttpRequest} implementation for the Apache HttpComponents HttpClient 5.x.
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 * @since 4.0
 */
class HttpComponentsClientHttpRequest extends AbstractClientHttpRequest {

  private final HttpRequest httpRequest;

  private final DataBufferFactory dataBufferFactory;

  private final HttpClientContext context;

  @Nullable
  private Flux<ByteBuffer> byteBufferFlux;

  private transient long contentLength = -1;

  public HttpComponentsClientHttpRequest(HttpMethod method, URI uri,
          HttpClientContext context, DataBufferFactory dataBufferFactory) {

    this.context = context;
    this.httpRequest = new BasicHttpRequest(method.name(), uri);
    this.dataBufferFactory = dataBufferFactory;
  }

  @Override
  public HttpMethod getMethod() {
    return HttpMethod.valueOf(this.httpRequest.getMethod());
  }

  @Override
  public URI getURI() {
    try {
      return this.httpRequest.getUri();
    }
    catch (URISyntaxException ex) {
      throw new IllegalArgumentException("Invalid URI syntax: " + ex.getMessage());
    }
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return this.dataBufferFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getNativeRequest() {
    return (T) this.httpRequest;
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return doCommit(() -> {
      this.byteBufferFlux = Flux.from(body).map(dataBuffer -> {
        ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
        dataBuffer.toByteBuffer(byteBuffer);
        return byteBuffer;
      });
      return Mono.empty();
    });
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return writeWith(Flux.from(body).flatMap(Function.identity()));
  }

  @Override
  public Mono<Void> setComplete() {
    return doCommit();
  }

  @Override
  protected void applyHeaders() {
    HttpHeaders headers = getHeaders();
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      String key = entry.getKey();
      if (!HttpHeaders.CONTENT_LENGTH.equals(key)) {
        for (String v : entry.getValue()) {
          httpRequest.addHeader(key, v);
        }
      }
    }

    if (!httpRequest.containsHeader(HttpHeaders.ACCEPT)) {
      httpRequest.addHeader(HttpHeaders.ACCEPT, ALL_VALUE);
    }

    this.contentLength = headers.getContentLength();
  }

  @Override
  protected void applyCookies() {
    if (!getCookies().isEmpty()) {
      this.httpRequest.setHeader(HttpHeaders.COOKIE, serializeCookies());
    }
  }

  private String serializeCookies() {
    boolean first = true;
    StringBuilder sb = new StringBuilder();
    for (List<HttpCookie> cookies : getCookies().values()) {
      for (HttpCookie cookie : cookies) {
        if (!first) {
          sb.append("; ");
        }
        else {
          first = false;
        }
        sb.append(cookie.getName()).append("=").append(cookie.getValue());
      }
    }
    return sb.toString();
  }

  /**
   * Applies the attributes to the {@link HttpClientContext}.
   */
  @Override
  protected void applyAttributes() {
    if (hasAttributes()) {
      getAttributes().forEach((key, value) -> {
        if (this.context.getAttribute(key) == null) {
          this.context.setAttribute(key, value);
        }
      });
    }
  }

  @Override
  protected HttpHeaders initReadOnlyHeaders() {
    return new HttpComponentsHeaders(httpRequest).asReadOnly();
  }

  public AsyncRequestProducer toRequestProducer() {
    ReactiveEntityProducer reactiveEntityProducer = null;

    if (byteBufferFlux != null) {
      HttpHeaders headers = getHeaders();
      String contentEncoding = headers.getFirst(HttpHeaders.CONTENT_ENCODING);
      ContentType contentType = null;
      MediaType mediaType = headers.getContentType();
      if (mediaType != null) {
        contentType = ContentType.create("%s/%s".formatted(mediaType.getType(), mediaType.getSubtype()), mediaType.getCharset());
      }
      reactiveEntityProducer = new ReactiveEntityProducer(
              byteBufferFlux, contentLength, contentType, contentEncoding);
    }

    return new BasicRequestProducer(httpRequest, reactiveEntityProducer);
  }

}
