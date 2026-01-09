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

package infra.web.service.invoker;

import org.jspecify.annotations.Nullable;

import java.time.Duration;

import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapterRegistry;
import infra.http.ResponseEntity;
import infra.lang.Assert;
import infra.util.concurrent.Future;
import infra.web.client.ClientResponse;
import reactor.core.publisher.Mono;

/**
 * Convenient base class for a {@link ReactorHttpExchangeAdapter} implementation
 * adapting to the synchronous {@link HttpExchangeAdapter} contract.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractReactorHttpExchangeAdapter implements ReactorHttpExchangeAdapter {

  private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

  @Nullable
  private Duration blockTimeout;

  /**
   * Protected constructor, for subclasses.
   */
  protected AbstractReactorHttpExchangeAdapter() {

  }

  /**
   * Configure the {@link ReactiveAdapterRegistry} to use.
   * <p>By default, this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
   */
  public void setReactiveAdapterRegistry(ReactiveAdapterRegistry reactiveAdapterRegistry) {
    this.reactiveAdapterRegistry = reactiveAdapterRegistry;
  }

  @Override
  public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
    return this.reactiveAdapterRegistry;
  }

  /**
   * Configure how long to block for the response of an HTTP service method
   * as described in {@link #getBlockTimeout()}.
   */
  public void setBlockTimeout(@Nullable Duration blockTimeout) {
    this.blockTimeout = blockTimeout;
  }

  @Override
  @Nullable
  public Duration getBlockTimeout() {
    return this.blockTimeout;
  }

  @Override
  public abstract ClientResponse exchange(HttpRequestValues requestValues);

  @Override
  public abstract Future<ClientResponse> exchangeAsync(HttpRequestValues requestValues);

  @Nullable
  @Override
  public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return blockingGet(exchangeForBodyMono(requestValues, bodyType));
  }

  @Override
  public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
    ResponseEntity<Void> entity = blockingGet(exchangeForBodilessEntityMono(requestValues));
    Assert.state(entity != null, "Expected ResponseEntity");
    return entity;
  }

  @Override
  public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    ResponseEntity<T> entity = blockingGet(exchangeForEntityMono(requestValues, bodyType));
    Assert.state(entity != null, "Expected ResponseEntity");
    return entity;
  }

  @Override
  public <T> Future<ResponseEntity<T>> exchangeForEntityAsync(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return Future.forAdaption(exchangeForEntityMono(requestValues, bodyType).toFuture());
  }

  @Override
  public Future<ResponseEntity<Void>> exchangeForBodilessEntityAsync(HttpRequestValues requestValues) {
    return Future.forAdaption(exchangeForBodilessEntityMono(requestValues).toFuture());
  }

  @Nullable
  protected final <T> T blockingGet(Mono<T> mono) {
    if (blockTimeout != null) {
      return mono.block(blockTimeout);
    }
    return mono.block();
  }

}
