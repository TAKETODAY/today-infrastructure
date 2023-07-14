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

package cn.taketoday.web.service.invoker;

import java.time.Duration;
import java.util.Collections;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ReactorHttpExchangeAdapter} with stubbed responses.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 */
@SuppressWarnings("unchecked")
class TestReactorExchangeAdapter extends TestExchangeAdapter implements ReactorHttpExchangeAdapter {

  @Override
  public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
    return ReactiveAdapterRegistry.getSharedInstance();
  }

  @Override
  public Duration getBlockTimeout() {
    return Duration.ofSeconds(5);
  }

  @Override
  public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
    saveInput("exchangeForMono", requestValues, null);
    return Mono.empty();
  }

  @Override
  public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
    saveInput("exchangeForHeadersMono", requestValues, null);
    return Mono.just(HttpHeaders.create());
  }

  @Override
  public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    saveInput("exchangeForBodyMono", requestValues, bodyType);
    return bodyType.getType().getTypeName().contains("List") ?
           (Mono<T>) Mono.just(Collections.singletonList(getInvokedMethodName())) :
           (Mono<T>) Mono.just(getInvokedMethodName());
  }

  @Override
  public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    saveInput("exchangeForBodyFlux", requestValues, bodyType);
    return (Flux<T>) Flux.just("exchange", "For", "Body", "Flux");
  }

  @Override
  public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
    saveInput("exchangeForBodilessEntityMono", requestValues, null);
    return Mono.just(ResponseEntity.ok().build());
  }

  @Override
  public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

    saveInput("exchangeForEntityMono", requestValues, bodyType);
    return Mono.just((ResponseEntity<T>) ResponseEntity.ok("exchangeForEntityMono"));
  }

  @Override
  public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

    saveInput("exchangeForEntityFlux", requestValues, bodyType);
    return Mono.just(ResponseEntity.ok((Flux<T>) Flux.just("exchange", "For", "Entity", "Flux")));
  }

}
