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

package cn.taketoday.web.reactive.function.client;

import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.codec.Hints;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.http.codec.HttpMessageWriter;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.reactive.function.BodyInserter;
import cn.taketoday.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link ClientRequest.Builder}.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DefaultClientRequestBuilder implements ClientRequest.Builder {

  private HttpMethod method;

  private URI uri;

  private final HttpHeaders headers = HttpHeaders.forWritable();

  private final MultiValueMap<String, String> cookies = new LinkedMultiValueMap<>();

  private final LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();

  private BodyInserter<?, ? super ClientHttpRequest> body = BodyInserters.empty();

  @Nullable
  private Consumer<ClientHttpRequest> httpRequestConsumer;

  public DefaultClientRequestBuilder(ClientRequest other) {
    Assert.notNull(other, "ClientRequest is required");
    this.method = other.method();
    this.uri = other.uri();
    headers(other.headers());
    cookies(other.cookies());
    attributes(other.attributes());
    body(other.body());
    this.httpRequestConsumer = other.httpRequest();
  }

  public DefaultClientRequestBuilder(HttpMethod method, URI uri) {
    Assert.notNull(method, "HttpMethod is required");
    Assert.notNull(uri, "URI is required");
    this.method = method;
    this.uri = uri;
  }

  @Override
  public ClientRequest.Builder method(HttpMethod method) {
    Assert.notNull(method, "HttpMethod is required");
    this.method = method;
    return this;
  }

  @Override
  public ClientRequest.Builder uri(URI uri) {
    Assert.notNull(uri, "URI is required");
    this.uri = uri;
    return this;
  }

  @Override
  public ClientRequest.Builder header(String headerName, String... headerValues) {
    headers.setOrRemove(headerName, headerValues);
    return this;
  }

  @Override
  public ClientRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
    headersConsumer.accept(this.headers);
    return this;
  }

  @Override
  public ClientRequest.Builder headers(@Nullable HttpHeaders headers) {
    this.headers.setAll(headers);
    return this;
  }

  @Override
  public ClientRequest.Builder cookie(String name, String... values) {
    this.cookies.setOrRemove(name, values);
    return this;
  }

  @Override
  public ClientRequest.Builder cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
    cookiesConsumer.accept(this.cookies);
    return this;
  }

  @Override
  public ClientRequest.Builder cookies(@Nullable MultiValueMap<String, String> cookies) {
    this.cookies.setAll(cookies);
    return this;
  }

  @Override
  public <S, P extends Publisher<S>> ClientRequest.Builder body(P publisher, Class<S> elementClass) {
    this.body = BodyInserters.fromPublisher(publisher, elementClass);
    return this;
  }

  @Override
  public <S, P extends Publisher<S>> ClientRequest.Builder body(
          P publisher, ParameterizedTypeReference<S> typeReference) {

    this.body = BodyInserters.fromPublisher(publisher, typeReference);
    return this;
  }

  @Override
  public ClientRequest.Builder attribute(String name, Object value) {
    this.attributes.put(name, value);
    return this;
  }

  @Override
  public ClientRequest.Builder attributes(Consumer<Map<String, Object>> attributesConsumer) {
    attributesConsumer.accept(this.attributes);
    return this;
  }

  @Override
  public ClientRequest.Builder attributes(@Nullable Map<String, Object> attributes) {
    if (CollectionUtils.isNotEmpty(attributes)) {
      this.attributes.putAll(attributes);
    }
    return this;
  }

  @Override
  public ClientRequest.Builder httpRequest(Consumer<ClientHttpRequest> requestConsumer) {
    this.httpRequestConsumer = (this.httpRequestConsumer != null ?
            this.httpRequestConsumer.andThen(requestConsumer) : requestConsumer);
    return this;
  }

  @Override
  public ClientRequest.Builder body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
    this.body = inserter;
    return this;
  }

  @Override
  public ClientRequest build() {
    return new BodyInserterRequest(this.method, this.uri, this.headers, this.cookies,
            this.body, this.attributes, this.httpRequestConsumer);
  }

  private static class BodyInserterRequest implements ClientRequest {

    private final HttpMethod method;

    private final URI url;

    private final HttpHeaders headers;

    private final MultiValueMap<String, String> cookies;

    private final BodyInserter<?, ? super ClientHttpRequest> body;

    private final Map<String, Object> attributes;

    @Nullable
    private final Consumer<ClientHttpRequest> httpRequestConsumer;

    private final String logPrefix;

    public BodyInserterRequest(HttpMethod method, URI url, HttpHeaders headers,
            MultiValueMap<String, String> cookies, BodyInserter<?, ? super ClientHttpRequest> body,
            Map<String, Object> attributes, @Nullable Consumer<ClientHttpRequest> httpRequestConsumer) {

      this.url = url;
      this.method = method;
      this.headers = headers.asReadOnly();
      this.cookies = cookies.asReadOnly();
      this.body = body;
      this.attributes = Collections.unmodifiableMap(attributes);
      this.httpRequestConsumer = httpRequestConsumer;

      Object id = attributes.computeIfAbsent(LOG_ID_ATTRIBUTE, name -> ObjectUtils.getIdentityHexString(this));
      this.logPrefix = "[" + id + "] ";
    }

    @Override
    public HttpMethod method() {
      return this.method;
    }

    @Override
    public URI uri() {
      return this.url;
    }

    @Override
    public HttpHeaders headers() {
      return this.headers;
    }

    @Override
    public MultiValueMap<String, String> cookies() {
      return this.cookies;
    }

    @Override
    public BodyInserter<?, ? super ClientHttpRequest> body() {
      return this.body;
    }

    @Override
    public Map<String, Object> attributes() {
      return this.attributes;
    }

    @Override
    public Consumer<ClientHttpRequest> httpRequest() {
      return this.httpRequestConsumer;
    }

    @Override
    public String logPrefix() {
      return this.logPrefix;
    }

    @Override
    public Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies) {
      HttpHeaders requestHeaders = request.getHeaders();
      if (!this.headers.isEmpty()) {
        for (var entry : this.headers.entrySet()) {
          requestHeaders.putIfAbsent(entry.getKey(), entry.getValue());
        }
      }

      MultiValueMap<String, HttpCookie> requestCookies = request.getCookies();
      if (!this.cookies.isEmpty()) {
        for (var entry : cookies.entrySet()) {
          String name = entry.getKey();
          for (String value : entry.getValue()) {
            HttpCookie cookie = new HttpCookie(name, value);
            requestCookies.add(name, cookie);
          }
        }
      }

      request.setAttributes(attributes);

      if (this.httpRequestConsumer != null) {
        this.httpRequestConsumer.accept(request);
      }

      return this.body.insert(request, new BodyInserter.Context() {
        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
          return strategies.messageWriters();
        }

        @Override
        public Optional<ServerHttpRequest> serverRequest() {
          return Optional.empty();
        }

        @Override
        public Map<String, Object> hints() {
          return Hints.from(Hints.LOG_PREFIX_HINT, logPrefix());
        }
      });
    }
  }

}
