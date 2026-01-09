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
