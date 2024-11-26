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

package infra.web.client.reactive.support;

import java.util.List;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.http.ResponseEntity;
import infra.http.client.reactive.ClientHttpResponse;
import infra.lang.Assert;
import infra.util.MultiValueMap;
import infra.web.client.reactive.ClientResponse;
import infra.web.client.reactive.ExchangeFilterFunction;
import infra.web.client.reactive.ExchangeStrategies;
import infra.web.client.reactive.WebClientResponseException;
import infra.web.reactive.function.BodyExtractor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implementation of the {@link ClientResponse} interface that can be subclassed
 * to adapt the request in a
 * {@link ExchangeFilterFunction exchange filter function}.
 * All methods default to calling through to the wrapped request.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ClientResponseWrapper implements ClientResponse {

  private final ClientResponse delegate;

  /**
   * Create a new {@code ClientResponseWrapper} that wraps the given response.
   *
   * @param delegate the response to wrap
   */
  public ClientResponseWrapper(ClientResponse delegate) {
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  /**
   * Return the wrapped request.
   */
  public ClientResponse response() {
    return this.delegate;
  }

  @Override
  public ExchangeStrategies strategies() {
    return this.delegate.strategies();
  }

  @Override
  public HttpStatusCode statusCode() {
    return this.delegate.statusCode();
  }

  @Override
  public int rawStatusCode() {
    return this.delegate.rawStatusCode();
  }

  @Override
  public Headers headers() {
    return this.delegate.headers();
  }

  @Override
  public MultiValueMap<String, ResponseCookie> cookies() {
    return this.delegate.cookies();
  }

  @Override
  public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
    return this.delegate.body(extractor);
  }

  @Override
  public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
    return this.delegate.bodyToMono(elementClass);
  }

  @Override
  public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
    return this.delegate.bodyToMono(elementTypeRef);
  }

  @Override
  public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
    return this.delegate.bodyToFlux(elementClass);
  }

  @Override
  public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
    return this.delegate.bodyToFlux(elementTypeRef);
  }

  @Override
  public Mono<Void> releaseBody() {
    return this.delegate.releaseBody();
  }

  @Override
  public Mono<ResponseEntity<Void>> toBodilessEntity() {
    return this.delegate.toBodilessEntity();
  }

  @Override
  public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
    return this.delegate.toEntity(bodyType);
  }

  @Override
  public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference) {
    return this.delegate.toEntity(bodyTypeReference);
  }

  @Override
  public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
    return this.delegate.toEntityList(elementClass);
  }

  @Override
  public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
    return this.delegate.toEntityList(elementTypeRef);
  }

  @Override
  public Mono<WebClientResponseException> createException() {
    return this.delegate.createException();
  }

  @Override
  public <T> Mono<T> createError() {
    return this.delegate.createError();
  }

  @Override
  public String logPrefix() {
    return this.delegate.logPrefix();
  }

}
