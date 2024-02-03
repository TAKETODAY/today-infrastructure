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

import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;

/**
 * Base class for {@link ClientHttpResponse} implementations.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractClientHttpResponse implements ClientHttpResponse {

  private final HttpStatusCode statusCode;

  private final HttpHeaders headers;

  private final MultiValueMap<String, ResponseCookie> cookies;

  private final Flux<DataBuffer> body;

  protected AbstractClientHttpResponse(HttpStatusCode statusCode, HttpHeaders headers,
          MultiValueMap<String, ResponseCookie> cookies, Flux<DataBuffer> body) {

    Assert.notNull(statusCode, "StatusCode is required");
    Assert.notNull(headers, "Headers is required");
    Assert.notNull(body, "Body is required");

    this.statusCode = statusCode;
    this.headers = headers;
    this.cookies = cookies;
    this.body = singleSubscription(body);
  }

  private static Flux<DataBuffer> singleSubscription(Flux<DataBuffer> body) {
    AtomicBoolean subscribed = new AtomicBoolean();
    return body.doOnSubscribe(s -> {
      if (!subscribed.compareAndSet(false, true)) {
        throw new IllegalStateException("The client response body can only be consumed once");
      }
    });
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return this.statusCode;
  }

  @Override
  public int getRawStatusCode() {
    return statusCode.value();
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return this.cookies;
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return this.body;
  }
}
