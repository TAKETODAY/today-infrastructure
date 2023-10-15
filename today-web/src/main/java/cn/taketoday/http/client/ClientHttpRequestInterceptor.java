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

package cn.taketoday.http.client;

import java.io.IOException;

import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.client.support.HttpRequestDecorator;

/**
 * Contract to intercept client-side HTTP requests. Implementations can be
 * registered with {@link cn.taketoday.web.client.RestClient} or
 * {@link cn.taketoday.web.client.RestTemplate} to modify the outgoing
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
   * {@link ClientHttpRequestExecution#execute(cn.taketoday.http.HttpRequest, byte[])},</li>
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

}
