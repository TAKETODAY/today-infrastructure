/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
