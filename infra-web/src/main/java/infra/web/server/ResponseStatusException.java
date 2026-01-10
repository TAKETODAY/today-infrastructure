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

package infra.web.server;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.context.MessageSource;
import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.web.ErrorResponseException;

/**
 * Subclass of {@link ErrorResponseException} that accepts a "reason" and maps
 * it to the "detail" property of {@link ProblemDetail}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author TODAY 2021/5/6 19:16
 * @since 3.0.1
 */
public class ResponseStatusException extends ErrorResponseException {

  private final @Nullable String reason;

  /**
   * Constructor with a response status.
   *
   * @param status the HTTP status (required)
   */
  public ResponseStatusException(HttpStatusCode status) {
    this(status, null);
  }

  /**
   * Constructor with a response status and a reason to add to the exception
   * message as explanation.
   *
   * @param status the HTTP status (required)
   * @param reason the associated reason (optional)
   */
  public ResponseStatusException(HttpStatusCode status, @Nullable String reason) {
    this(status, reason, null);
  }

  /**
   * Constructor with a response status and a reason to add to the exception
   * message as explanation, as well as a nested exception.
   *
   * @param rawStatusCode the HTTP status code value
   * @param reason the associated reason (optional)
   * @param cause a nested exception (optional)
   */
  public ResponseStatusException(int rawStatusCode, @Nullable String reason, @Nullable Throwable cause) {
    this(HttpStatusCode.valueOf(rawStatusCode), reason, cause);
  }

  /**
   * Constructor with a response status and a reason to add to the exception
   * message as explanation, as well as a nested exception.
   *
   * @param status the HTTP status (required)
   * @param reason the associated reason (optional)
   * @param cause a nested exception (optional)
   */
  public ResponseStatusException(HttpStatusCode status, @Nullable String reason, @Nullable Throwable cause) {
    this(status, reason, cause, null, null);
  }

  /**
   * Constructor with a message code and arguments for resolving the error
   * "detail" via {@link infra.context.MessageSource}.
   *
   * @param status the HTTP status (required)
   * @param reason the associated reason (optional)
   * @param cause a nested exception (optional)
   * @since 5.0
   */
  protected ResponseStatusException(HttpStatusCode status, @Nullable String reason, @Nullable Throwable cause,
          @Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

    super(status, ProblemDetail.forStatus(status), cause, messageDetailCode, messageDetailArguments);
    this.reason = reason;
    setDetail(reason);
  }

  /**
   * The reason explaining the exception (potentially {@code null} or empty).
   */
  public @Nullable String getReason() {
    return this.reason;
  }

  /**
   * Return headers to add to the error response, for example, "Allow", "Accept", etc.
   */
  @Override
  public HttpHeaders getHeaders() {
    return HttpHeaders.empty();
  }

  @Override
  public ProblemDetail updateAndGetBody(@Nullable MessageSource messageSource, Locale locale) {
    super.updateAndGetBody(messageSource, locale);

    // The reason may be a code (consistent with ResponseStatusExceptionResolver)

    if (messageSource != null && getReason() != null && getReason().equals(getBody().getDetail())) {
      Object[] arguments = getDetailMessageArguments(messageSource, locale);
      String resolved = messageSource.getMessage(getReason(), arguments, null, locale);
      if (resolved != null) {
        getBody().setDetail(resolved);
      }
    }

    return getBody();
  }

  @Override
  public String getMessage() {
    if (reason != null) {
      return getStatusCode() + " \"" + this.reason + "\"";
    }
    return getStatusCode().toString();
  }

}
