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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.core.NestedRuntimeException;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;

/**
 * Exception thrown when invalid or inconsistent configuration is detected in
 * the Web framework or one of its components.
 *
 * <p>This exception indicates a framework-level configuration problem, such as
 * an invalid component registration or incompatible component configuration. It
 * is not intended for application business errors or client input errors.
 *
 * <p>By default, this exception is exposed as an {@link ErrorResponse} with an
 * {@linkplain HttpStatus#INTERNAL_SERVER_ERROR HTTP 500} status and a
 * {@link ProblemDetail} containing the exception message. Subclasses may
 * override {@link #getStatusCode()} to provide a more specific status code.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 3.0 2021/4/26 22:20
 */
public class WebConfigurationException extends NestedRuntimeException implements ErrorResponse {

  private final ProblemDetail body = ProblemDetail.forStatusAndDetail(getStatusCode(), getMessage());

  /**
   * Create a new {@code WebConfigurationException} with the specified detail
   * message.
   *
   * @param message the detail message, or {@code null} if none
   */
  public WebConfigurationException(@Nullable String message) {
    super(message);
  }

  /**
   * Create a new {@code WebConfigurationException} with the specified detail
   * message and cause.
   *
   * @param message the detail message, or {@code null} if none
   * @param cause the underlying cause, or {@code null} if none
   */
  public WebConfigurationException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public ProblemDetail getBody() {
    return body;
  }

}
