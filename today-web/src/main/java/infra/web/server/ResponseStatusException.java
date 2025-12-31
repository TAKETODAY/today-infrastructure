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
