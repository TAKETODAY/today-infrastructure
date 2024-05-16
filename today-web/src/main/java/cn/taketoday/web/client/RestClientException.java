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

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Nullable;

/**
 * Base class for exceptions thrown by {@link RestTemplate} in case a request
 * fails because of a server error response, as determined via
 * {@link ResponseErrorHandler#hasError(ClientHttpResponse)}, failure to decode
 * the response, or a low level I/O error.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RestClientException extends NestedRuntimeException {

  /**
   * Construct a new instance of {@code RestClientException} with the given message.
   *
   * @param msg the message
   */
  public RestClientException(@Nullable String msg) {
    super(msg);
  }

  /**
   * Construct a new instance of {@code RestClientException} with the given message and
   * exception.
   *
   * @param msg the message
   * @param ex the exception
   */
  public RestClientException(@Nullable String msg, @Nullable Throwable ex) {
    super(msg, ex);
  }

}
