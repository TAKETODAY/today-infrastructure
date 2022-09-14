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

package cn.taketoday.web.reactive.function.client.support;

import cn.taketoday.core.TypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.reactive.function.client.WebClient;
import cn.taketoday.web.service.invoker.HttpClientAdapter;
import cn.taketoday.web.service.invoker.HttpRequestValues;
import cn.taketoday.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link HttpClientAdapter} that enables an {@link HttpServiceProxyFactory} to
 * use {@link WebClient} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@code HttpServiceProxyFactory} configured with a given {@code WebClient}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class WebClientAdapter implements HttpClientAdapter {

  private final WebClient webClient;

  /**
   * Package private constructor. See static factory methods.
   */
  private WebClientAdapter(WebClient webClient) {
    this.webClient = webClient;
  }

  @Override
  public Mono<Void> requestToVoid(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity().then();
  }

  @Override
  public Mono<HttpHeaders> requestToHeaders(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity().map(ResponseEntity::getHeaders);
  }

  @Override
  public <T> Mono<T> requestToBody(HttpRequestValues requestValues, TypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().bodyToMono(bodyType);
  }

  @Override
  public <T> Flux<T> requestToBodyFlux(HttpRequestValues requestValues, TypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().bodyToFlux(bodyType);
  }

  @Override
  public Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity();
  }

  @Override
  public <T> Mono<ResponseEntity<T>> requestToEntity(HttpRequestValues requestValues, TypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().toEntity(bodyType);
  }

  @Override
  public <T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(HttpRequestValues requestValues, TypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().toEntityFlux(bodyType);
  }

  @SuppressWarnings("ReactiveStreamsUnusedPublisher")
  private WebClient.RequestBodySpec newRequest(HttpRequestValues requestValues) {

    HttpMethod httpMethod = requestValues.getHttpMethod();
    Assert.notNull(httpMethod, "HttpMethod is required");

    WebClient.RequestBodyUriSpec uriSpec = this.webClient.method(httpMethod);

    WebClient.RequestBodySpec bodySpec;
    if (requestValues.getUri() != null) {
      bodySpec = uriSpec.uri(requestValues.getUri());
    }
    else if (requestValues.getUriTemplate() != null) {
      bodySpec = uriSpec.uri(requestValues.getUriTemplate(), requestValues.getUriVariables());
    }
    else {
      throw new IllegalStateException("Neither full URL nor URI template");
    }

    bodySpec.headers(headers -> headers.putAll(requestValues.getHeaders()));
    bodySpec.cookies(cookies -> cookies.putAll(requestValues.getCookies()));
    bodySpec.attributes(attributes -> attributes.putAll(requestValues.getAttributes()));

    if (requestValues.getBodyValue() != null) {
      bodySpec.bodyValue(requestValues.getBodyValue());
    }
    else if (requestValues.getBody() != null) {
      Assert.notNull(requestValues.getBodyElementType(), "Publisher body element type is required");
      bodySpec.body(requestValues.getBody(), requestValues.getBodyElementType());
    }

    return bodySpec;
  }

  /**
   * Static method to create a {@link HttpServiceProxyFactory} configured to
   * use the given {@link WebClient} instance. Effectively a shortcut for:
   * <pre>
   * WebClientAdapter adapter = WebClientAdapter.forClient(webClient);
   * HttpServiceProxyFactory proxyFactory = new HttpServiceProxyFactory(adapter);
   * </pre>
   *
   * @param webClient the client to use
   * @return the created {@code HttpServiceProxyFactory} instance
   */
  public static HttpServiceProxyFactory createHttpServiceProxyFactory(WebClient webClient) {
    return new HttpServiceProxyFactory(new WebClientAdapter(webClient));
  }

  /**
   * Variant of {@link #createHttpServiceProxyFactory(WebClient)} that accepts
   * a {@link WebClient.Builder} and uses it to create the client.
   *
   * @param webClientBuilder a builder to create the client to use with
   * @return the created {@code HttpServiceProxyFactory} instance
   */
  public static HttpServiceProxyFactory createHttpServiceProxyFactory(WebClient.Builder webClientBuilder) {
    return createHttpServiceProxyFactory(webClientBuilder.build());
  }

  /**
   * Create a {@link WebClientAdapter} for the given {@code WebClient} instance.
   *
   * @param webClient the client to use
   * @return the created adapter instance
   */
  public static WebClientAdapter forClient(WebClient webClient) {
    return new WebClientAdapter(webClient);
  }

}
