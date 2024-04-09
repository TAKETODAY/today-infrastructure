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

package cn.taketoday.web.client;

import java.io.IOException;
import java.net.URI;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ClientHttpResponse;

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
   * Handle the error in the given response.
   * <p>This method is only called when {@link #hasError(ClientHttpResponse)}
   * has returned {@code true}.
   *
   * @param response the response with the error
   * @throws IOException in case of I/O errors
   */
  void handleError(ClientHttpResponse response) throws IOException;

  /**
   * Alternative to {@link #handleError(ClientHttpResponse)} with extra
   * information providing access to the request URL and HTTP method.
   *
   * @param url the request URL
   * @param method the HTTP method
   * @param response the response with the error
   * @throws IOException in case of I/O errors
   */
  default void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
    handleError(response);
  }

}
