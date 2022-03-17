/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.mock.http.client.reactive;

import org.reactivestreams.Publisher;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.reactive.AbstractClientHttpRequest;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.MimeType;
import cn.taketoday.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Mock implementation of {@link ClientHttpRequest}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockClientHttpRequest extends AbstractClientHttpRequest {

  private final HttpMethod httpMethod;

  private final URI url;

  private Flux<DataBuffer> body = Flux.error(
          new IllegalStateException("The body is not set. " +
                  "Did handling complete with success? Is a custom \"writeHandler\" configured?"));

  private Function<Flux<DataBuffer>, Mono<Void>> writeHandler;

  public MockClientHttpRequest(HttpMethod httpMethod, String urlTemplate, Object... vars) {
    this(httpMethod, UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(vars).encode().toUri());
  }

  public MockClientHttpRequest(HttpMethod httpMethod, URI url) {
    this.httpMethod = httpMethod;
    this.url = url;
    this.writeHandler = body -> {
      this.body = body.cache();
      return this.body.then();
    };
  }

  /**
   * Configure a custom handler for writing the request body.
   *
   * <p>The default write handler consumes and caches the request body so it
   * may be accessed subsequently, e.g. in test assertions. Use this property
   * when the request body is an infinite stream.
   *
   * @param writeHandler the write handler to use returning {@code Mono<Void>}
   * when the body has been "written" (i.e. consumed).
   */
  public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
    Assert.notNull(writeHandler, "'writeHandler' is required");
    this.writeHandler = writeHandler;
  }

  @Override
  public HttpMethod getMethod() {
    return this.httpMethod;
  }

  @Override
  public URI getURI() {
    return this.url;
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return DefaultDataBufferFactory.sharedInstance;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getNativeRequest() {
    return (T) this;
  }

  @Override
  protected void applyHeaders() {
  }

  @Override
  protected void applyCookies() {
    getCookies().values().stream().flatMap(Collection::stream)
            .forEach(cookie -> getHeaders().add(HttpHeaders.COOKIE, cookie.toString()));
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return doCommit(() -> Mono.defer(() -> this.writeHandler.apply(Flux.from(body))));
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return writeWith(Flux.from(body).flatMap(p -> p));
  }

  @Override
  public Mono<Void> setComplete() {
    return writeWith(Flux.empty());
  }

  /**
   * Return the request body, or an error stream if the body was never set
   * or when {@link #setWriteHandler} is configured.
   */
  public Flux<DataBuffer> getBody() {
    return this.body;
  }

  /**
   * Aggregate response data and convert to a String using the "Content-Type"
   * charset or "UTF-8" by default.
   */
  public Mono<String> getBodyAsString() {

    Charset charset = Optional.ofNullable(getHeaders().getContentType()).map(MimeType::getCharset)
            .orElse(StandardCharsets.UTF_8);

    return DataBufferUtils.join(getBody())
            .map(buffer -> {
              String s = buffer.toString(charset);
              DataBufferUtils.release(buffer);
              return s;
            })
            .defaultIfEmpty("");
  }

}
