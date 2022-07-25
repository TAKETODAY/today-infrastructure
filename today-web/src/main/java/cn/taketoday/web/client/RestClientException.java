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

package cn.taketoday.web.client;

import java.io.Serial;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.http.client.ClientHttpResponse;

/**
 * Base class for exceptions thrown by {@link RestTemplate} in case a request
 * fails because of a server error response, as determined via
 * {@link ResponseErrorHandler#hasError(ClientHttpResponse)}, failure to decode
 * the response, or a low level I/O error.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public class RestClientException extends NestedRuntimeException {

  @Serial
  private static final long serialVersionUID = -4084444984163796577L;

  /**
   * Construct a new instance of {@code RestClientException} with the given message.
   *
   * @param msg the message
   */
  public RestClientException(String msg) {
    super(msg);
  }

  /**
   * Construct a new instance of {@code RestClientException} with the given message and
   * exception.
   *
   * @param msg the message
   * @param ex the exception
   */
  public RestClientException(String msg, Throwable ex) {
    super(msg, ex);
  }

}
