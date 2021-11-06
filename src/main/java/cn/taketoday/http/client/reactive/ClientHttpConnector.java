/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.http.client.reactive;

import java.net.URI;
import java.util.function.Function;

import cn.taketoday.http.HttpMethod;
import reactor.core.publisher.Mono;

/**
 * Abstraction over HTTP clients driving the underlying HTTP client to connect
 * to the origin server and provide all necessary infrastructure to send a
 * {@link ClientHttpRequest} and receive a {@link ClientHttpResponse}.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public interface ClientHttpConnector {

  /**
   * Connect to the origin server using the given {@code HttpMethod} and
   * {@code URI} and apply the given {@code requestCallback} when the HTTP
   * request of the underlying API can be initialized and written to.
   *
   * @param method the HTTP request method
   * @param uri the HTTP request URI
   * @param requestCallback a function that prepares and writes to the request,
   * returning a publisher that signals when it's done writing.
   * Implementations can return a {@code Mono<Void>} by calling
   * {@link ClientHttpRequest#writeWith} or {@link ClientHttpRequest#setComplete}.
   * @return publisher for the {@link ClientHttpResponse}
   */
  Mono<ClientHttpResponse> connect(
          HttpMethod method, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback);

}
