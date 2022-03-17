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

package cn.taketoday.web.testfixture.http.server.reactive;

import org.reactivestreams.Publisher;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.server.reactive.AbstractServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Mock extension of {@link AbstractServerHttpResponse} for use in tests without
 * an actual server.
 *
 * <p>By default response content is consumed in full upon writing and cached
 * for subsequent access, however it is also possible to set a custom
 * {@link #setWriteHandler(Function) writeHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockServerHttpResponse extends AbstractServerHttpResponse {

  private Flux<DataBuffer> body = Flux.error(new IllegalStateException(
          "No content was written nor was setComplete() called on this response."));

  private Function<Flux<DataBuffer>, Mono<Void>> writeHandler;

  public MockServerHttpResponse() {
    this(DefaultDataBufferFactory.sharedInstance);
  }

  public MockServerHttpResponse(DataBufferFactory dataBufferFactory) {
    super(dataBufferFactory);
    this.writeHandler = body -> {
      // Avoid .then() that causes data buffers to be discarded and released
      Sinks.Empty<Void> completion = Sinks.unsafe().empty();
      this.body = body.cache();
      this.body.subscribe(aVoid -> { }, completion::tryEmitError, completion::tryEmitEmpty); // Signals are serialized
      return completion.asMono();
    };
  }

  /**
   * Configure a custom handler to consume the response body.
   * <p>By default, response body content is consumed in full and cached for
   * subsequent access in tests. Use this option to take control over how the
   * response body is consumed.
   *
   * @param writeHandler the write handler to use returning {@code Mono<Void>}
   * when the body has been "written" (i.e. consumed).
   */
  public void setWriteHandler(Function<Flux<DataBuffer>, Mono<Void>> writeHandler) {
    Assert.notNull(writeHandler, "'writeHandler' is required");
    this.body = Flux.error(new IllegalStateException("Not available with custom write handler."));
    this.writeHandler = writeHandler;
  }

  @Override
  public <T> T getNativeResponse() {
    throw new IllegalStateException("This is a mock. No running server, no native response.");
  }

  @Override
  protected void applyStatusCode() {
  }

  @Override
  protected void applyHeaders() {
  }

  @Override
  protected void applyCookies() {
    for (List<ResponseCookie> cookies : getCookies().values()) {
      for (ResponseCookie cookie : cookies) {
        getHeaders().add(HttpHeaders.SET_COOKIE, cookie.toString());
      }
    }
  }

  @Override
  protected Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body) {
    return this.writeHandler.apply(Flux.from(body));
  }

  @Override
  protected Mono<Void> writeAndFlushWithInternal(
          Publisher<? extends Publisher<? extends DataBuffer>> body) {

    return this.writeHandler.apply(Flux.from(body).concatMap(Flux::from));
  }

  @Override
  public Mono<Void> setComplete() {
    return doCommit(() -> Mono.defer(() -> this.writeHandler.apply(Flux.empty())));
  }

  /**
   * Return the response body or an error stream if the body was not set.
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
