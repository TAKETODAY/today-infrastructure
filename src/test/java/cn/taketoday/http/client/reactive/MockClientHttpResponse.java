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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Mock implementation of {@link ClientHttpResponse}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockClientHttpResponse implements ClientHttpResponse {

  private final int status;

  private final HttpHeaders headers = HttpHeaders.create();

  private final MultiValueMap<String, ResponseCookie> cookies = new DefaultMultiValueMap<>();

  private Flux<DataBuffer> body = Flux.empty();

  public MockClientHttpResponse(HttpStatus status) {
    Assert.notNull(status, "HttpStatus is required");
    this.status = status.value();
  }

  public MockClientHttpResponse(int status) {
    Assert.isTrue(status > 99 && status < 1000, "Status must be between 100 and 999");
    this.status = status;
  }

  @Override
  public HttpStatus getStatusCode() {
    return HttpStatus.valueOf(this.status);
  }

  @Override
  public int getRawStatusCode() {
    return this.status;
  }

  @Override
  public HttpHeaders getHeaders() {
    if (!getCookies().isEmpty() && this.headers.get(HttpHeaders.SET_COOKIE) == null) {
      getCookies().values().stream().flatMap(Collection::stream)
              .forEach(cookie -> this.headers.add(HttpHeaders.SET_COOKIE, cookie.toString()));
    }
    return this.headers;
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return this.cookies;
  }

  public void setBody(Publisher<DataBuffer> body) {
    this.body = Flux.from(body);
  }

  public void setBody(String body) {
    setBody(body, StandardCharsets.UTF_8);
  }

  public void setBody(String body, Charset charset) {
    DataBuffer buffer = toDataBuffer(body, charset);
    this.body = Flux.just(buffer);
  }

  private DataBuffer toDataBuffer(String body, Charset charset) {
    byte[] bytes = body.getBytes(charset);
    ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
    return DefaultDataBufferFactory.sharedInstance.wrap(byteBuffer);
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return this.body;
  }

  /**
   * Return the response body aggregated and converted to a String using the
   * charset of the Content-Type response or otherwise as "UTF-8".
   */
  public Mono<String> getBodyAsString() {
    return DataBufferUtils.join(getBody())
            .map(buffer -> {
              String s = buffer.toString(getCharset());
              DataBufferUtils.release(buffer);
              return s;
            })
            .defaultIfEmpty("");
  }

  private Charset getCharset() {
    Charset charset = null;
    MediaType contentType = getHeaders().getContentType();
    if (contentType != null) {
      charset = contentType.getCharset();
    }
    return (charset != null ? charset : StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    HttpStatus code = HttpStatus.resolve(this.status);
    return (code != null ? code.name() + "(" + this.status + ")" : "Status (" + this.status + ")") + this.headers;
  }
}
