/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server;

import org.jspecify.annotations.Nullable;

import infra.http.HttpStatus;

/**
 * Exception thrown when an internal server error occurs (HTTP status code 500).
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-12-02 09:14
 */
public class InternalServerException extends ResponseStatusException {

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message (can be null)
   * @param cause the cause (can be null)
   */
  public InternalServerException(@Nullable String message, @Nullable Throwable cause) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
  }

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message (can be null)
   */
  public InternalServerException(@Nullable String message) {
    super(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  /**
   * Constructs a new exception with no detail message.
   */
  public InternalServerException() {
    super(HttpStatus.INTERNAL_SERVER_ERROR, null);
  }

  /**
   * Creates a new instance of {@link InternalServerException} with no detail message.
   *
   * @return a new instance of {@link InternalServerException}
   */
  public static InternalServerException failed() {
    return new InternalServerException();
  }

  /**
   * Creates a new instance of {@link InternalServerException} with the specified detail message.
   *
   * @param msg the detail message (can be null)
   * @return a new instance of {@link InternalServerException}
   */
  public static InternalServerException failed(@Nullable String msg) {
    return new InternalServerException(msg);
  }

  /**
   * Creates a new instance of {@link InternalServerException} with the specified detail message and cause.
   *
   * @param msg the detail message (can be null)
   * @param cause the cause (can be null)
   * @return a new instance of {@link InternalServerException}
   */
  public static InternalServerException failed(@Nullable String msg, @Nullable Throwable cause) {
    return new InternalServerException(msg, cause);
  }

}
