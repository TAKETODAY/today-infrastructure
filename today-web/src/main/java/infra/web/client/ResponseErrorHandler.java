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

package infra.web.client;

import java.io.IOException;

import infra.http.HttpRequest;
import infra.http.client.ClientHttpResponse;

/**
 * Strategy interface used by the {@link RestTemplate} to determine
 * whether a particular response has an error or not.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface ResponseErrorHandler {

  /**
   * Indicate whether the given response has any errors.
   * <p>Implementations will typically inspect the
   * {@link ClientHttpResponse#getStatusCode() HttpStatus} of the response.
   *
   * @param response the response to inspect
   * @return {@code true} if the response indicates an error; {@code false} otherwise
   * @throws IOException in case of I/O errors
   */
  boolean hasError(ClientHttpResponse response) throws IOException;

  /**
   * Handle the error in the given response. with extra information providing
   * access to the request URL and HTTP method.
   *
   * <p>This method is only called when {@link #hasError(ClientHttpResponse)}
   * has returned {@code true}.
   *
   * @param request the request
   * @param response the response with the error
   * @throws IOException in case of I/O errors
   * @since 5.0
   */
  void handleError(HttpRequest request, ClientHttpResponse response) throws IOException;

}
