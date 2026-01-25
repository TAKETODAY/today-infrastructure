/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapterRegistry;
import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.ResponseEntity;
import infra.web.reactive.client.ClientResponse;
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
    return Mono.just(HttpHeaders.forWritable());
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

  @Override
  public @Nullable RequestExecution<HttpRequestValues> createRequestExecution(Method method, MethodParameter returnType, boolean isFuture) {
    if (returnType.getParameterType() == infra.web.reactive.client.ClientResponse.class) {
      return this::exchangeMono;
    }
    return null;
  }

  public Mono<ClientResponse> exchangeMono(HttpRequestValues requestValues) {
    saveInput("exchangeMono", requestValues, null);
    return Mono.just(ClientResponse.create(HttpStatus.OK).build());
  }

}
