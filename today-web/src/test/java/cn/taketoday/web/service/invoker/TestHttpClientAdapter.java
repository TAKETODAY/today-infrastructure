/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.service.invoker;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HttpClientAdapter} with stubbed responses.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("unchecked")
class TestHttpClientAdapter implements HttpClientAdapter {

  @Nullable
  private String invokedMethodName;

  @Nullable
  private HttpRequestValues requestValues;

  @Nullable
  private ParameterizedTypeReference<?> bodyType;

  public String getInvokedMethodName() {
    assertThat(this.invokedMethodName).isNotNull();
    return this.invokedMethodName;
  }

  public HttpRequestValues getRequestValues() {
    assertThat(this.requestValues).isNotNull();
    return this.requestValues;
  }

  @Nullable
  public ParameterizedTypeReference<?> getBodyType() {
    return this.bodyType;
  }

  // HttpClientAdapter implementation

  @Override
  public Mono<Void> requestToVoid(HttpRequestValues requestValues) {
    saveInput("requestToVoid", requestValues, null);
    return Mono.empty();
  }

  @Override
  public Mono<HttpHeaders> requestToHeaders(HttpRequestValues requestValues) {
    saveInput("requestToHeaders", requestValues, null);
    return Mono.just(HttpHeaders.create());
  }

  @Override
  public <T> Mono<T> requestToBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    saveInput("requestToBody", requestValues, bodyType);
    return (Mono<T>) Mono.just(getInvokedMethodName());
  }

  @Override
  public <T> Flux<T> requestToBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    saveInput("requestToBodyFlux", requestValues, bodyType);
    return (Flux<T>) Flux.just("request", "To", "Body", "Flux");
  }

  @Override
  public Mono<ResponseEntity<Void>> requestToBodilessEntity(HttpRequestValues requestValues) {
    saveInput("requestToBodilessEntity", requestValues, null);
    return Mono.just(ResponseEntity.ok().build());
  }

  @Override
  public <T> Mono<ResponseEntity<T>> requestToEntity(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> type) {

    saveInput("requestToEntity", requestValues, type);
    return Mono.just((ResponseEntity<T>) ResponseEntity.ok("requestToEntity"));
  }

  @Override
  public <T> Mono<ResponseEntity<Flux<T>>> requestToEntityFlux(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

    saveInput("requestToEntityFlux", requestValues, bodyType);
    return Mono.just(ResponseEntity.ok((Flux<T>) Flux.just("request", "To", "Entity", "Flux")));
  }

  private <T> void saveInput(
          String methodName, HttpRequestValues requestValues, @Nullable ParameterizedTypeReference<T> bodyType) {

    this.invokedMethodName = methodName;
    this.requestValues = requestValues;
    this.bodyType = bodyType;
  }

}
