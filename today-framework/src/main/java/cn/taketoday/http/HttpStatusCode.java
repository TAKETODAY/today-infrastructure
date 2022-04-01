/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.http;

import cn.taketoday.lang.Assert;

/**
 * Represents an HTTP response status code. Implemented by {@link HttpStatus},
 * but defined as an interface to allow for values not in that enumeration.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://www.iana.org/assignments/http-status-codes">HTTP Status Code Registry</a>
 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">List of HTTP status codes - Wikipedia</a>
 * @since 4.0 2022/4/1 21:31
 */
public interface HttpStatusCode {

  /**
   * Return the integer value of this status code.
   */
  int value();

  /**
   * Whether this status code is in the Informational class ({@code 1xx}).
   *
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.1">RFC 2616</a>
   */
  boolean is1xxInformational();

  /**
   * Whether this status code is in the Successful class ({@code 2xx}).
   *
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.2">RFC 2616</a>
   */
  boolean is2xxSuccessful();

  /**
   * Whether this status code is in the Redirection class ({@code 3xx}).
   *
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.3">RFC 2616</a>
   */
  boolean is3xxRedirection();

  /**
   * Whether this status code is in the Client Error class ({@code 4xx}).
   *
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.4">RFC 2616</a>
   */
  boolean is4xxClientError();

  /**
   * Whether this status code is in the Server Error class ({@code 5xx}).
   *
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.5">RFC 2616</a>
   */
  boolean is5xxServerError();

  /**
   * Whether this status code is in the Client or Server Error class
   *
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.4">RFC 2616</a>
   * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.3">RFC 2616</a>
   * ({@code 4xx} or {@code 5xx}).
   * @see #is4xxClientError()
   * @see #is5xxServerError()
   */
  boolean isError();

  /**
   * Return an {@code HttpStatusCode} object for the given integer value.
   *
   * @param code the status code as integer
   * @return the corresponding {@code HttpStatusCode}
   * @throws IllegalArgumentException if {@code code} is not a three-digit
   * positive number
   */
  static HttpStatusCode valueOf(int code) {
    Assert.isTrue(code >= 100 && code <= 999, () -> "Code '" + code + "' should be a three-digit positive integer");
    HttpStatus status = HttpStatus.resolve(code);
    if (status == null) {
      return new SimpleHttpStatusCode(code);
    }
    return status;
  }

}
