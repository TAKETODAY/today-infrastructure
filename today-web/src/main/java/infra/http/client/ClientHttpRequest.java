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

package infra.http.client;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.Executor;

import infra.http.HttpMethod;
import infra.http.HttpOutputMessage;
import infra.http.HttpRequest;
import infra.util.concurrent.Future;

/**
 * Represents a client-side HTTP request.
 * Created via an implementation of the {@link ClientHttpRequestFactory}.
 *
 * <p>A {@code ClientHttpRequest} can be {@linkplain #execute() executed},
 * receiving a {@link ClientHttpResponse} which can be read from.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ClientHttpRequestFactory#createRequest(java.net.URI, HttpMethod)
 * @since 4.0
 */
public interface ClientHttpRequest extends HttpRequest, HttpOutputMessage {

  /**
   * Execute this request, resulting in a {@link ClientHttpResponse} that can be read.
   *
   * @return the response result of the execution
   * @throws IOException in case of I/O errors
   */
  ClientHttpResponse execute() throws IOException;

  /**
   * Execute this request async, resulting in a {@code Future<ClientHttpResponse>} that can be read.
   *
   * <p> The returned future completes exceptionally with:
   * <ul>
   * <li>{@link IOException} - if an I/O error occurs when sending or receiving</li>
   * </ul>
   *
   * <p>
   * NOT Fully async {@link ClientHttpResponse#getBody()}
   *
   * @return the async response result of the execution
   * @since 5.0
   */
  default Future<ClientHttpResponse> async() {
    return async(null);
  }

  /**
   * Execute this request async, resulting in a {@code Future<ClientHttpResponse>} that can be read.
   *
   * <p> The returned future completes exceptionally with:
   * <ul>
   * <li>{@link IOException} - if an I/O error occurs when sending or receiving</li>
   * </ul>
   *
   * <p>
   * NOT Fully async {@link ClientHttpResponse#getBody()}
   *
   * @param executor request executor. Virtual thread executor is better
   * @return the async response result of the execution
   * @since 5.0
   */
  Future<ClientHttpResponse> async(@Nullable Executor executor);

}
