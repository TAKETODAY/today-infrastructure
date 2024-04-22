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

package cn.taketoday.web;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.ResponseEntity;

/**
 * Representation of a complete RFC 7807 error response including status,
 * headers, and an RFC 7807 formatted {@link ProblemDetail} body. Allows any
 * exception to expose HTTP error response information.
 *
 * <p>{@link ErrorResponseException} is a default implementation of this
 * interface and a convenient base class for other exceptions to use.
 *
 * <p>An {@code @ExceptionHandler} method can use {@link ResponseEntity#of(ProblemDetail)}
 * to map an {@code ErrorResponse} to a {@code ResponseEntity}.
 *
 * <p>{@code ErrorResponse} is supported as a return value from
 * {@code @ExceptionHandler} methods that render directly to the response, e.g.
 * by being marked {@code @ResponseBody}, or declared in an
 * {@code @RestController} or {@code RestControllerAdvice} class.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ErrorResponseException
 * @see ResponseEntity#of(ProblemDetail)
 * @since 4.0 2022/3/2 13:34
 */
public interface ErrorResponse extends HttpStatusProvider {

  /**
   * Return the HTTP status code to use for the response.
   */
  @Override
  HttpStatusCode getStatusCode();

  /**
   * Return headers to use for the response.
   */
  default HttpHeaders getHeaders() {
    return HttpHeaders.empty();
  }

  /**
   * Return the body for the response, formatted as an RFC 7807
   * {@link ProblemDetail} whose {@link ProblemDetail#getStatus() status}
   * should match the response status.
   */
  ProblemDetail getBody();

}
