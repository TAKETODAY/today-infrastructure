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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ClientHttpRequest} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class JdkClientHttpRequest extends AbstractClientHttpRequest {

  private final HttpMethod method;

  private final URI uri;

  private final DataBufferFactory bufferFactory;

  private final HttpRequest.Builder builder;

  public JdkClientHttpRequest(HttpMethod httpMethod, URI uri, DataBufferFactory bufferFactory) {
    Assert.notNull(httpMethod, "HttpMethod is required");
    Assert.notNull(uri, "URI is required");
    Assert.notNull(bufferFactory, "DataBufferFactory is required");

    this.method = httpMethod;
    this.uri = uri;
    this.bufferFactory = bufferFactory;
    this.builder = HttpRequest.newBuilder(uri);
  }

  @Override
  public HttpMethod getMethod() {
    return method;
  }

  @Override
  public URI getURI() {
    return uri;
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return bufferFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getNativeRequest() {
    return (T) builder.build();
  }

  @Override
  protected void applyHeaders() {
    for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
      if (entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
        // content-length is specified when writing
        continue;
      }
      for (String value : entry.getValue()) {
        builder.header(entry.getKey(), value);
      }
    }
    if (!getHeaders().containsKey(HttpHeaders.ACCEPT)) {
      builder.header(HttpHeaders.ACCEPT, "*/*");
    }
  }

  @Override
  protected void applyCookies() {
    builder.header(
            HttpHeaders.COOKIE, getCookies().values().stream()
                    .flatMap(List::stream)
                    .map(HttpCookie::toString)
                    .collect(Collectors.joining(";"))
    );
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return doCommit(() -> {
      builder.method(method.name(), toBodyPublisher(body));
      return Mono.empty();
    });
  }

  private HttpRequest.BodyPublisher toBodyPublisher(Publisher<? extends DataBuffer> body) {
    Publisher<ByteBuffer> byteBufferBody =
            body instanceof Mono
            ? Mono.from(body).map(this::toByteBuffer)
            : Flux.from(body).map(this::toByteBuffer);

    Flow.Publisher<ByteBuffer> bodyFlow = JdkFlowAdapter.publisherToFlowPublisher(byteBufferBody);

    return (getHeaders().getContentLength() > 0 ?
            HttpRequest.BodyPublishers.fromPublisher(bodyFlow, getHeaders().getContentLength()) :
            HttpRequest.BodyPublishers.fromPublisher(bodyFlow));
  }

  private ByteBuffer toByteBuffer(DataBuffer dataBuffer) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableByteCount());
    dataBuffer.toByteBuffer(byteBuffer);
    return byteBuffer;
  }

  @Override
  public Mono<Void> writeAndFlushWith(final Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return writeWith(Flux.from(body).flatMap(Function.identity()));
  }

  @Override
  public Mono<Void> setComplete() {
    return doCommit(() -> {
      builder.method(method.name(), HttpRequest.BodyPublishers.noBody());
      return Mono.empty();
    });
  }

}
