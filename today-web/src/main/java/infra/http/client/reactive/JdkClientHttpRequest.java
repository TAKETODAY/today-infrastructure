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

package infra.http.client.reactive;

import org.reactivestreams.Publisher;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.stream.Collectors;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.MultiValueMap;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ClientHttpRequest} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JdkClientHttpRequest extends AbstractClientHttpRequest {

  private final HttpMethod method;

  private final URI uri;

  private final DataBufferFactory bufferFactory;

  private final HttpRequest.Builder builder;

  public JdkClientHttpRequest(HttpMethod httpMethod, URI uri, DataBufferFactory bufferFactory, @Nullable Duration readTimeout) {
    Assert.notNull(httpMethod, "HttpMethod is required");
    Assert.notNull(uri, "URI is required");
    Assert.notNull(bufferFactory, "DataBufferFactory is required");

    this.method = httpMethod;
    this.uri = uri;
    this.bufferFactory = bufferFactory;
    this.builder = HttpRequest.newBuilder(uri);
    if (readTimeout != null) {
      this.builder.timeout(readTimeout);
    }
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
  public DataBufferFactory bufferFactory() {
    return this.bufferFactory;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getNativeRequest() {
    return (T) this.builder.build();
  }

  @Override
  protected void applyHeaders() {
    for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
      if (entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
        // content-length is specified when writing
        continue;
      }
      for (String value : entry.getValue()) {
        this.builder.header(entry.getKey(), value);
      }
    }
    if (!getHeaders().containsKey(HttpHeaders.ACCEPT)) {
      this.builder.header(HttpHeaders.ACCEPT, "*/*");
    }
  }

  @Override
  protected void applyCookies() {
    MultiValueMap<String, HttpCookie> cookies = getCookies();
    if (cookies.isEmpty()) {
      return;
    }
    this.builder.header(HttpHeaders.COOKIE, cookies.values().stream()
            .flatMap(List::stream).map(HttpCookie::toString).collect(Collectors.joining(";")));
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return doCommit(() -> {
      this.builder.method(this.method.name(), toBodyPublisher(body));
      return Mono.empty();
    });
  }

  private HttpRequest.BodyPublisher toBodyPublisher(Publisher<? extends DataBuffer> body) {
    Publisher<ByteBuffer> byteBufferBody = (body instanceof Mono ?
            Mono.from(body).map(this::toByteBuffer) :
            Flux.from(body).map(this::toByteBuffer));

    Flow.Publisher<ByteBuffer> bodyFlow = JdkFlowAdapter.publisherToFlowPublisher(byteBufferBody);

    return (getHeaders().getContentLength() > 0 ?
            HttpRequest.BodyPublishers.fromPublisher(bodyFlow, getHeaders().getContentLength()) :
            HttpRequest.BodyPublishers.fromPublisher(bodyFlow));
  }

  private ByteBuffer toByteBuffer(DataBuffer dataBuffer) {
    ByteBuffer byteBuffer = ByteBuffer.allocate(dataBuffer.readableBytes());
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
      this.builder.method(this.method.name(), HttpRequest.BodyPublishers.noBody());
      return Mono.empty();
    });
  }

}
