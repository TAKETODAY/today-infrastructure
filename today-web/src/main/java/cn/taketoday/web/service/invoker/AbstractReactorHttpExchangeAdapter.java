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

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

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
  public void exchange(HttpRequestValues requestValues) {
    if (this.blockTimeout != null) {
      exchangeForMono(requestValues).block(this.blockTimeout);
    }
    else {
      exchangeForMono(requestValues).block();
    }
  }

  @Override
  public HttpHeaders exchangeForHeaders(HttpRequestValues requestValues) {
    HttpHeaders headers = (this.blockTimeout != null ?
                           exchangeForHeadersMono(requestValues).block(this.blockTimeout) :
                           exchangeForHeadersMono(requestValues).block());
    Assert.state(headers != null, "Expected HttpHeaders");
    return headers;
  }

  @Override
  public <T> T exchangeForBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return (this.blockTimeout != null ?
            exchangeForBodyMono(requestValues, bodyType).block(this.blockTimeout) :
            exchangeForBodyMono(requestValues, bodyType).block());
  }

  @Override
  public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues requestValues) {
    ResponseEntity<Void> entity = (this.blockTimeout != null ?
                                   exchangeForBodilessEntityMono(requestValues).block(this.blockTimeout) :
                                   exchangeForBodilessEntityMono(requestValues).block());
    Assert.state(entity != null, "Expected ResponseEntity");
    return entity;
  }

  @Override
  public <T> ResponseEntity<T> exchangeForEntity(
          HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {

    ResponseEntity<T> entity = (this.blockTimeout != null ?
                                exchangeForEntityMono(requestValues, bodyType).block(this.blockTimeout) :
                                exchangeForEntityMono(requestValues, bodyType).block());
    Assert.state(entity != null, "Expected ResponseEntity");
    return entity;
  }
 
}
