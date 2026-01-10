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

import infra.core.Pair;
import infra.core.annotation.AnnotationUtils;
import infra.core.conversion.ConversionException;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.util.StringUtils;
import infra.web.annotation.ResponseStatus;

/**
 * Interface to provide the HTTP status code for a response. This is typically
 * used in error handling scenarios to determine the appropriate HTTP status
 * code based on the exception or context.
 *
 * <p>Implementations of this interface should override the
 * {@link #getStatusCode()} method to return the desired HTTP status code.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * public class CustomException extends RuntimeException implements HttpStatusProvider {
 *
 *   private final HttpStatusCode statusCode;
 *
 *   public CustomException(String message, HttpStatusCode statusCode) {
 *     super(message);
 *     this.statusCode = statusCode;
 *   }
 *
 *   @Override
 *   public HttpStatusCode getStatusCode() {
 *     return this.statusCode;
 *   }
 * }
 *
 * public class ErrorHandlingExample {
 *   public static void main(String[] args) {
 *     try {
 *       throw new CustomException("Bad Request", HttpStatus.BAD_REQUEST);
 *     } catch (Throwable ex) {
 *       Pair<HttpStatusCode, String> result = HttpStatusProvider.getStatusCode(ex);
 *       System.out.println("Status Code: " + result.getFirst());
 *       System.out.println("Message: " + result.getSecond());
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>In the example above, the {@code CustomException} implements
 * {@code HttpStatusProvider} to provide a specific HTTP status code. The
 * {@code HttpStatusProvider.getStatusCode(Throwable)} method is then used to
 * extract the status code and message from the exception.
 *
 * <p>This interface also provides a static utility method,
 * {@link #getStatusCode(Throwable)}, which determines the HTTP status code
 * based on the type of exception and its annotations.
 */
public interface HttpStatusProvider {

  /**
   * Returns the HTTP status code associated with this object.
   *
   * <p>This method is typically used in scenarios where an HTTP response needs
   * to be determined based on the context or exception. Implementations should
   * ensure that the returned status code accurately reflects the intended HTTP
   * response.
   *
   * <p><strong>Usage Example:</strong>
   * <pre>{@code
   * public class CustomException extends RuntimeException implements HttpStatusProvider {
   *
   *   private final HttpStatusCode statusCode;
   *
   *   public CustomException(String message, HttpStatusCode statusCode) {
   *     super(message);
   *     this.statusCode = statusCode;
   *   }
   *
   *   @Override
   *   public HttpStatusCode getStatusCode() {
   *     return this.statusCode;
   *   }
   * }
   *
   * public class ErrorHandlingExample {
   *   public static void main(String[] args) {
   *     try {
   *       throw new CustomException("Not Found", HttpStatus.NOT_FOUND);
   *     }
   *     catch (Throwable ex) {
   *       HttpStatusCode statusCode = ((HttpStatusProvider) ex).getStatusCode();
   *       System.out.println("Status Code: " + statusCode.value());
   *     }
   *   }
   * }
   * }</pre>
   *
   * <p>In the example above, the {@code CustomException} implements
   * {@code HttpStatusProvider} to provide a specific HTTP status code. The
   * {@code getStatusCode()} method is then used to retrieve and print the status
   * code when the exception is caught.
   *
   * @return the {@link HttpStatusCode} representing the HTTP status code
   */
  HttpStatusCode getStatusCode();

  /**
   * Retrieves the HTTP status code and associated message from the given exception.
   *
   * <p>This method determines the appropriate HTTP status code based on the type
   * and characteristics of the provided exception. It supports exceptions that
   * implement {@link HttpStatusProvider}, {@link ConversionException}, or are
   * annotated with {@link ResponseStatus}. If none of these conditions are met,
   * a default status code of {@code 500 Internal Server Error} is returned.
   *
   * <p><strong>Usage Example:</strong>
   * <pre>{@code
   * try {
   *   throw new CustomException("Bad Request", HttpStatus.BAD_REQUEST);
   * }
   * catch (Throwable ex) {
   *   Pair<HttpStatusCode, String> result = getStatusCode(ex);
   *   System.out.println("Status Code: " + result.getFirst().value());
   *   System.out.println("Message: " + result.getSecond());
   * }
   * }</pre>
   *
   * <p>In the example above, the {@code CustomException} implements
   * {@code HttpStatusProvider} to provide a specific HTTP status code. The
   * {@code getStatusCode()} method retrieves the status code and message when
   * the exception is caught.
   *
   * @param ex the exception from which to extract the HTTP status code and message
   * @return a {@link Pair} containing the {@link HttpStatusCode} and an associated
   * message derived from the exception
   */
  static Pair<HttpStatusCode, String> getStatusCode(Throwable ex) {
    if (ex instanceof HttpStatusProvider provider) {
      return Pair.of(provider.getStatusCode(), ex.getMessage());
    }
    if (ex instanceof ConversionException) {
      return Pair.of(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    ResponseStatus status = AnnotationUtils.getAnnotation(ex.getClass(), ResponseStatus.class);
    if (status != null) {
      String reason = status.reason();
      return Pair.of(status.code(), StringUtils.hasText(reason) ? reason : ex.getMessage());
    }
    return Pair.of(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
  }
}
