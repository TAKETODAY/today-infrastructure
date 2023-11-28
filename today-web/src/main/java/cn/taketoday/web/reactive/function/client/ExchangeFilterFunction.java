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

package cn.taketoday.web.reactive.function.client;

import java.util.function.Function;

import cn.taketoday.lang.Assert;
import reactor.core.publisher.Mono;

/**
 * Represents a function that filters an {@linkplain ExchangeFunction exchange function}.
 * <p>The filter is executed when a {@code Subscriber} subscribes to the
 * {@code Publisher} returned by the {@code WebClient}.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
@FunctionalInterface
public interface ExchangeFilterFunction {

  /**
   * Apply this filter to the given request and exchange function.
   * <p>The given {@linkplain ExchangeFunction} represents the next entity
   * in the chain, to be invoked via
   * {@linkplain ExchangeFunction#exchange(ClientRequest) invoked} in order to
   * proceed with the exchange, or not invoked to shortcut the chain.
   *
   * <p><strong>Note:</strong> When a filter handles the response after the
   * call to {@link ExchangeFunction#exchange}, extra care must be taken to
   * always consume its content or otherwise propagate it downstream for
   * further handling, for example by the {@link WebClient}. Please, see the
   * reference documentation for more details on this.
   *
   * @param request the current request
   * @param next the next exchange function in the chain
   * @return the filtered response
   */
  Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next);

  /**
   * Return a composed filter function that first applies this filter, and
   * then applies the given {@code "after"} filter.
   *
   * @param afterFilter the filter to apply after this filter
   * @return the composed filter
   */
  default ExchangeFilterFunction andThen(ExchangeFilterFunction afterFilter) {
    Assert.notNull(afterFilter, "ExchangeFilterFunction is required");
    return (request, next) ->
            filter(request, afterRequest -> afterFilter.filter(afterRequest, next));
  }

  /**
   * Apply this filter to the given {@linkplain ExchangeFunction}, resulting
   * in a filtered exchange function.
   *
   * @param exchange the exchange function to filter
   * @return the filtered exchange function
   */
  default ExchangeFunction apply(ExchangeFunction exchange) {
    Assert.notNull(exchange, "ExchangeFunction is required");
    return request -> this.filter(request, exchange);
  }

  /**
   * Adapt the given request processor function to a filter function that only
   * operates on the {@code ClientRequest}.
   *
   * @param processor the request processor
   * @return the resulting filter adapter
   */
  static ExchangeFilterFunction ofRequestProcessor(Function<ClientRequest, Mono<ClientRequest>> processor) {
    Assert.notNull(processor, "ClientRequest Function is required");
    return (request, next) -> processor.apply(request).flatMap(next::exchange);
  }

  /**
   * Adapt the given response processor function to a filter function that
   * only operates on the {@code ClientResponse}.
   *
   * @param processor the response processor
   * @return the resulting filter adapter
   */
  static ExchangeFilterFunction ofResponseProcessor(Function<ClientResponse, Mono<ClientResponse>> processor) {
    Assert.notNull(processor, "ClientResponse Function is required");
    return (request, next) -> next.exchange(request).flatMap(processor);
  }

}
