/*
 * Copyright 2017 - 2023 the original author or authors.
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

import org.reactivestreams.Publisher;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.reactive.function.client.WebClient;
import cn.taketoday.web.service.invoker.AbstractReactorHttpExchangeAdapter;
import cn.taketoday.web.service.invoker.HttpRequestValues;
import cn.taketoday.web.service.invoker.HttpServiceProxyFactory;
import cn.taketoday.web.service.invoker.ReactiveHttpRequestValues;
import cn.taketoday.web.service.invoker.ReactorHttpExchangeAdapter;
import cn.taketoday.web.util.UriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ReactorHttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory}
 * to use {@link WebClient} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@code HttpServiceProxyFactory} configured with a given {@code WebClient}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class WebClientAdapter extends AbstractReactorHttpExchangeAdapter {

  private final WebClient webClient;

  /**
   * Package private constructor. See static factory methods.
   */
  private WebClientAdapter(WebClient webClient) {
    this.webClient = webClient;
  }

  @Override
  public boolean supportsRequestAttributes() {
    return true;
  }

  @Override
  public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity().then();
  }

  @Override
  public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity().map(ResponseEntity::getHeaders);
  }

  @Override
  public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().bodyToMono(bodyType);
  }

  @Override
  public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().bodyToFlux(bodyType);
  }

  @Override
  public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity();
  }

  @Override
  public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().toEntity(bodyType);
  }

  @Override
  public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().toEntityFlux(bodyType);
  }

  @SuppressWarnings("ReactiveStreamsUnusedPublisher")
  private WebClient.RequestBodySpec newRequest(HttpRequestValues values) {

    HttpMethod httpMethod = values.getHttpMethod();
    Assert.notNull(httpMethod, "HttpMethod is required");

    WebClient.RequestBodyUriSpec uriSpec = this.webClient.method(httpMethod);

    WebClient.RequestBodySpec bodySpec;
    if (values.getUri() != null) {
      bodySpec = uriSpec.uri(values.getUri());
    }
    else if (values.getUriTemplate() != null) {
      UriBuilderFactory uriBuilderFactory = values.getUriBuilderFactory();
      if (uriBuilderFactory != null) {
        bodySpec = uriSpec.uri(uriBuilderFactory.expand(values.getUriTemplate(), values.getUriVariables()));
      }
      else {
        bodySpec = uriSpec.uri(values.getUriTemplate(), values.getUriVariables());
      }
    }
    else {
      throw new IllegalStateException("Neither full URL nor URI template");
    }

    bodySpec.headers(headers -> headers.putAll(values.getHeaders()));
    bodySpec.cookies(cookies -> cookies.putAll(values.getCookies()));
    bodySpec.attributes(attributes -> attributes.putAll(values.getAttributes()));

    if (values.getBodyValue() != null) {
      bodySpec.bodyValue(values.getBodyValue());
    }
    else if (values instanceof ReactiveHttpRequestValues reactiveRequestValues) {
      Publisher<?> body = reactiveRequestValues.getBodyPublisher();
      if (body != null) {
        ParameterizedTypeReference<?> elementType = reactiveRequestValues.getBodyPublisherElementType();
        Assert.notNull(elementType, "Publisher body element type is required");
        bodySpec.body(body, elementType);
      }
    }

    return bodySpec;
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
