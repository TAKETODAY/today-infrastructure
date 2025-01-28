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

import java.io.IOException;

import infra.http.HttpRequest;
import infra.http.client.support.HttpRequestDecorator;
import infra.lang.Assert;

/**
 * Contract to intercept client-side HTTP requests. Implementations can be
 * registered with {@link infra.web.client.RestClient} or
 * {@link infra.web.client.RestTemplate} to modify the outgoing
 * request and/or the incoming response.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface ClientHttpRequestInterceptor {

  /**
   * Intercept the given request, and return a response. The given
   * {@link ClientHttpRequestExecution} allows the interceptor to pass on the
   * request and response to the next entity in the chain.
   * <p>A typical implementation of this method would follow the following pattern:
   * <ol>
   * <li>Examine the {@linkplain HttpRequest request} and body.</li>
   * <li>Optionally {@linkplain HttpRequestDecorator
   * wrap} the request to filter HTTP attributes.</li>
   * <li>Optionally modify the body of the request.</li>
   * <ul>
   * <li><strong>Either</strong>
   * <li>execute the request using
   * {@link ClientHttpRequestExecution#execute(infra.http.HttpRequest, byte[])},</li>
   * <li><strong>or</strong></li>
   * <li>do not execute the request to block the execution altogether.</li>
   * </ul>
   * <li>Optionally wrap the response to filter HTTP attributes.</li>
   * </ol>
   * <p>Note: if the interceptor throws an exception after receiving a response,
   * it must close the response via {@link ClientHttpResponse#close()}.
   *
   * @param request the request, containing method, URI, and headers
   * @param body the body of the request
   * @param execution the request execution
   * @return the response
   * @throws IOException in case of I/O errors
   */
  ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
          throws IOException;

  /**
   * Return a new interceptor that invokes {@code this} interceptor first, and
   * then the one that's passed in.
   *
   * @param interceptor the next interceptor
   * @return a new interceptor that chains the two
   * @since 5.0
   */
  default ClientHttpRequestInterceptor andThen(ClientHttpRequestInterceptor interceptor) {
    Assert.notNull(interceptor, "ClientHttpRequestInterceptor is required");
    return (request, body, execution) -> {
      ClientHttpRequestExecution nextExecution = (nextRequest, nextBody)
              -> interceptor.intercept(nextRequest, nextBody, execution);
      return intercept(request, body, nextExecution);
    };
  }

  /**
   * Return a new execution that invokes {@code this} interceptor, and then
   * delegates to the given execution.
   *
   * @param execution the execution to delegate to
   * @return a new execution instance
   * @since 5.0
   */
  default ClientHttpRequestExecution apply(ClientHttpRequestExecution execution) {
    Assert.notNull(execution, "ClientHttpRequestExecution is required");
    return (request, body) -> intercept(request, body, execution);
  }

}
