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

package infra.web.client;

import java.io.IOException;
import java.lang.reflect.Type;

import infra.http.HttpOutputMessage;
import infra.http.client.ClientHttpRequest;

/**
 * Callback interface for code that operates on a {@link ClientHttpRequest}.
 * Allows manipulating the request headers, and write to the request body.
 *
 * <p>Used internally by the {@link RestTemplate}, but also useful for
 * application code. There several available factory methods:
 * <ul>
 * <li>{@link RestTemplate#acceptHeaderRequestCallback(Class)}
 * <li>{@link RestTemplate#httpEntityCallback(Object)}
 * <li>{@link RestTemplate#httpEntityCallback(Object, Type)}
 * </ul>
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see RestTemplate#execute
 * @since 4.0
 */
@FunctionalInterface
public interface RequestCallback {

  /**
   * Gets called by {@link RestTemplate#execute} with an opened {@code ClientHttpRequest}.
   * Does not need to care about closing the request or about handling errors:
   * this will all be handled by the {@code RestTemplate}.
   * <p><strong>Note:</strong> In order to stream request body content directly
   * to the underlying HTTP library, implementations must check if the request
   * is an implementation of {@link infra.http.StreamingHttpOutputMessage},
   * and set the request body through it. Use of the {@link HttpOutputMessage#getBody()}
   * is also supported, but results in full content aggregation prior to execution.
   * All built-in request implementations support {@code StreamingHttpOutputMessage}.
   *
   * @param request the active HTTP request
   * @throws IOException in case of I/O errors
   */
  void doWithRequest(ClientHttpRequest request) throws IOException;

}
