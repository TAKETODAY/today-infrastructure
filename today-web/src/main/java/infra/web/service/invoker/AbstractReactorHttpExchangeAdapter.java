/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.service.invoker;

import java.time.Duration;

import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapterRegistry;
import infra.http.ResponseEntity;
import infra.lang.Assert;
import infra.lang.Nullable;
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
