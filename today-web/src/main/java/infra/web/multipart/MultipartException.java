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

package infra.web.multipart;

import org.jspecify.annotations.Nullable;

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.web.ErrorResponseException;

/**
 * Exception thrown when multipart resolution fails.
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/1/17 10:41
 */
public class MultipartException extends ErrorResponseException {

  /**
   * Constructor for MultipartException.
   *
   * @param detail the detail message
   */
  public MultipartException(@Nullable String detail) {
    this(HttpStatus.BAD_REQUEST, detail, null);
  }

  /**
   * Constructor for MultipartException.
   *
   * @param detail the detail message
   * @param cause the root cause from the multipart parsing API in use
   */
  public MultipartException(@Nullable String detail, @Nullable Throwable cause) {
    this(HttpStatus.BAD_REQUEST, detail, cause);
  }

  /**
   * Constructor with an {@link HttpStatusCode} and an optional cause.
   *
   * @param status the HTTP status code
   * @param detail the detail message
   * @param cause the root cause from the multipart parsing API in use
   */
  protected MultipartException(HttpStatusCode status, @Nullable String detail, @Nullable Throwable cause) {
    super(status, cause);
    setDetail(detail);
  }

}