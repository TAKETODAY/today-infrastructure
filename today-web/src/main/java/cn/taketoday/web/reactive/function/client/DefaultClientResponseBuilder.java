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

package cn.taketoday.web.reactive.function.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.client.reactive.ClientHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;

/**
 * Default implementation of {@link ClientResponse.Builder}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
final class DefaultClientResponseBuilder implements ClientResponse.Builder {

  private static final HttpRequest EMPTY_REQUEST = new HttpRequest() {

    private final URI empty = URI.create("");

    @Override
    public HttpMethod getMethod() {
      return HttpMethod.GET;
    }

    @Override
    @Deprecated
    public String getMethodValue() {
      return "GET";
    }

    @Override
    public URI getURI() {
      return this.empty;
    }

    @Override
    public HttpHeaders getHeaders() {
      return HttpHeaders.empty();
    }
  };

  private final ExchangeStrategies strategies;

  private HttpStatusCode statusCode = HttpStatus.OK;

  @Nullable
  private HttpHeaders headers;

  @Nullable
  private MultiValueMap<String, ResponseCookie> cookies;

  private Flux<DataBuffer> body = Flux.empty();

  @Nullable
  private ClientResponse originalResponse;

  private HttpRequest request;

  DefaultClientResponseBuilder(ExchangeStrategies strategies) {
    Assert.notNull(strategies, "ExchangeStrategies must not be null");
    this.strategies = strategies;
    this.headers = HttpHeaders.create();
    this.cookies = new LinkedMultiValueMap<>();
    this.request = EMPTY_REQUEST;
  }

  DefaultClientResponseBuilder(ClientResponse other, boolean mutate) {
    Assert.notNull(other, "ClientResponse must not be null");
    this.strategies = other.strategies();
    this.statusCode = other.statusCode();
    if (mutate) {
      this.body = other.bodyToFlux(DataBuffer.class);
    }
    else {
      this.headers = HttpHeaders.create();
      this.headers.addAll(other.headers().asHttpHeaders());
    }
    this.originalResponse = other;
    this.request = (other instanceof DefaultClientResponse defaultClientResponse ?
                    defaultClientResponse.request() : EMPTY_REQUEST);
  }

  @Override
  public DefaultClientResponseBuilder statusCode(HttpStatusCode statusCode) {
    Assert.notNull(statusCode, "StatusCode must not be null");
    this.statusCode = statusCode;
    return this;
  }

  @Override
  public DefaultClientResponseBuilder rawStatusCode(int statusCode) {
    return statusCode(HttpStatusCode.valueOf(statusCode));
  }

  @Override
  public ClientResponse.Builder header(String headerName, String... headerValues) {
    for (String headerValue : headerValues) {
      getHeaders().add(headerName, headerValue);
    }
    return this;
  }

  @Override
  public ClientResponse.Builder headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(getHeaders());
    return this;
  }

  @SuppressWarnings("ConstantConditions")
  private HttpHeaders getHeaders() {
    if (this.headers == null) {
      this.headers = HttpHeaders.writableHttpHeaders(this.originalResponse.headers().asHttpHeaders());
    }
    return this.headers;
  }

  @Override
  public DefaultClientResponseBuilder cookie(String name, String... values) {
    for (String value : values) {
      getCookies().add(name, ResponseCookie.from(name, value).build());
    }
    return this;
  }

  @Override
  public ClientResponse.Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer) {
    cookiesConsumer.accept(getCookies());
    return this;
  }

  @SuppressWarnings("ConstantConditions")
  private MultiValueMap<String, ResponseCookie> getCookies() {
    if (this.cookies == null) {
      this.cookies = new LinkedMultiValueMap<>(this.originalResponse.cookies());
    }
    return this.cookies;
  }

  @Override
  public ClientResponse.Builder body(Function<Flux<DataBuffer>, Flux<DataBuffer>> transformer) {
    this.body = transformer.apply(this.body);
    return this;
  }

  @Override
  public ClientResponse.Builder body(Flux<DataBuffer> body) {
    Assert.notNull(body, "Body must not be null");
    releaseBody();
    this.body = body;
    return this;
  }

  @Override
  public ClientResponse.Builder body(String body) {
    Assert.notNull(body, "Body must not be null");
    releaseBody();
    this.body = Flux.just(body).
            map(s -> {
              byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
              return DefaultDataBufferFactory.sharedInstance.wrap(bytes);
            });
    return this;
  }

  private void releaseBody() {
    this.body.subscribe(DataBufferUtils.releaseConsumer());
  }

  @Override
  public ClientResponse.Builder request(HttpRequest request) {
    Assert.notNull(request, "Request must not be null");
    this.request = request;
    return this;
  }

  @Override
  public ClientResponse build() {

    ClientHttpResponse httpResponse = new BuiltClientHttpResponse(
            this.statusCode, this.headers, this.cookies, this.body, this.originalResponse);

    return new DefaultClientResponse(httpResponse, this.strategies,
            this.originalResponse != null ? this.originalResponse.logPrefix() : "",
            this.request.getMethod() + " " + this.request.getURI(),
            () -> this.request);
  }

  private record BuiltClientHttpResponse(
          HttpStatusCode statusCode, @Nullable HttpHeaders headers,
          @Nullable MultiValueMap<String, ResponseCookie> cookies,
          Flux<DataBuffer> body, @Nullable ClientResponse originalResponse) implements ClientHttpResponse {

    private BuiltClientHttpResponse(HttpStatusCode statusCode, @Nullable HttpHeaders headers,
            @Nullable MultiValueMap<String, ResponseCookie> cookies, Flux<DataBuffer> body,
            @Nullable ClientResponse originalResponse) {

      Assert.isTrue(headers != null || originalResponse != null,
              "Expected either headers or an original response with headers.");

      Assert.isTrue(cookies != null || originalResponse != null,
              "Expected either cookies or an original response with cookies.");

      this.statusCode = statusCode;
      this.headers = (headers != null ? HttpHeaders.readOnlyHttpHeaders(headers) : null);
      this.cookies = (cookies != null ? MultiValueMap.unmodifiable(cookies) : null);
      this.body = body;
      this.originalResponse = originalResponse;
    }

    @Override
    public HttpStatusCode getStatusCode() {
      return statusCode;
    }

    @Override
    public int getRawStatusCode() {
      return statusCode.value();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public HttpHeaders getHeaders() {
      return (this.headers != null ? this.headers : this.originalResponse.headers().asHttpHeaders());
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public MultiValueMap<String, ResponseCookie> getCookies() {
      return (this.cookies != null ? this.cookies : this.originalResponse.cookies());
    }

    @Override
    public Flux<DataBuffer> getBody() {
      return this.body;
    }
  }

}
