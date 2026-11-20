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
 * Contract for objects that expose an HTTP response status code.
 *
 * <p>Exceptions may implement this interface to supply a status code for error
 * handling. The {@link #getStatusCode(Throwable)} utility method resolves a
 * status code and message from an exception, giving this contract precedence
 * over other status mappings.
 *
 * <p>For example:
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
 * }</pre>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see HttpStatusCode
 * @see ResponseStatus
 */
public interface HttpStatusProvider {

  /**
   * Return the HTTP response status code associated with this object.
   *
   * @return the HTTP status code (never {@code null})
   */
  HttpStatusCode getStatusCode();

  /**
   * Resolve an HTTP status code and associated message from the given exception.
   *
   * <p>The first matching rule in the following order determines the result:
   * <ol>
   * <li>If the exception implements {@link HttpStatusProvider}, use its
   * {@link #getStatusCode() status code} and the exception message.</li>
   * <li>If the exception is a {@link ConversionException}, use
   * {@link HttpStatus#BAD_REQUEST BAD_REQUEST} and the exception message.</li>
   * <li>If {@link ResponseStatus} is found on the exception class, use its
   * {@link ResponseStatus#code() code} and {@link ResponseStatus#reason() reason}.
   * If the reason is empty or contains only whitespace, use the exception
   * message instead.</li>
   * <li>Otherwise, use {@link HttpStatus#INTERNAL_SERVER_ERROR INTERNAL_SERVER_ERROR}
   * and the exception message.</li>
   * </ol>
   *
   * <p>Only the supplied exception is considered; its cause chain is not traversed.
   * The resolved message may be {@code null} if the exception has no message and
   * no non-blank annotation reason is used.
   *
   * @param ex the exception to resolve (never {@code null})
   * @return a pair containing the HTTP status code as its first element and
   * the associated message, possibly {@code null}, as its second element
   * @throws NullPointerException if {@code ex} is {@code null}
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
