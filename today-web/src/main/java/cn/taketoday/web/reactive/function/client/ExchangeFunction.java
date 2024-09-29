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

package cn.taketoday.web.reactive.function.client;

import reactor.core.publisher.Mono;

/**
 * Represents a function that exchanges a {@linkplain ClientRequest request} for a (delayed)
 * {@linkplain ClientResponse}. Can be used as an alternative to {@link WebClient}.
 *
 * <p>For example:
 * <pre>{@code
 * ExchangeFunction exchangeFunction =
 *         ExchangeFunctions.create(new ReactorClientHttpConnector());
 *
 * URI url = URI.create("https://example.com/resource");
 * ClientRequest request = ClientRequest.create(HttpMethod.GET, url).build();
 *
 * Mono<String> bodyMono = exchangeFunction
 *     .exchange(request)
 *     .flatMap(response -> response.bodyToMono(String.class));
 * }</pre>
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
@FunctionalInterface
public interface ExchangeFunction {

  /**
   * Exchange the given request for a {@link ClientResponse} promise.
   *
   * <p><strong>Note:</strong> When calling this method from an
   * {@link ExchangeFilterFunction} that handles the response in some way,
   * extra care must be taken to always consume its content or otherwise
   * propagate it downstream for further handling, for example by the
   * {@link WebClient}. Please, see the reference documentation for more
   * details on this.
   *
   * @param request the request to exchange
   * @return the delayed response
   */
  Mono<ClientResponse> exchange(ClientRequest request);

  /**
   * Filter the exchange function with the given {@code ExchangeFilterFunction},
   * resulting in a filtered {@code ExchangeFunction}.
   *
   * @param filter the filter to apply to this exchange
   * @return the filtered exchange
   * @see ExchangeFilterFunction#apply(ExchangeFunction)
   */
  default ExchangeFunction filter(ExchangeFilterFunction filter) {
    return filter.apply(this);
  }

}
