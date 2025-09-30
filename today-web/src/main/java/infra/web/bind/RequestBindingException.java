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

package infra.web.bind;

import org.jspecify.annotations.Nullable;

import infra.core.NestedRuntimeException;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.web.ErrorResponse;

/**
 * Fatal binding exception, thrown when we want to
 * treat binding exceptions as unrecoverable.
 *
 * <p>Extends RuntimeException for convenient throwing in any resource
 * (such as a Filter), and Nested*Exception for proper root cause handling
 * (as the plain Exception doesn't expose its root cause at all).
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/22 22:54
 */
public class RequestBindingException extends NestedRuntimeException implements ErrorResponse {

  private final ProblemDetail body = ProblemDetail.forStatus(getStatusCode());

  private final String messageDetailCode;

  private final Object @Nullable [] messageDetailArguments;

  /**
   * Constructor with a message only.
   *
   * @param msg the detail message
   */
  public RequestBindingException(@Nullable String msg) {
    this(msg, null, null);
  }

  /**
   * Constructor with a message and a cause.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public RequestBindingException(@Nullable String msg, @Nullable Throwable cause) {
    this(msg, cause, null, null);
  }

  /**
   * Constructor for RequestBindingException.
   *
   * @param msg the detail message
   * @param messageDetailCode the code to use to resolve the problem "detail"
   * through a {@link infra.context.MessageSource}
   * @param messageDetailArguments the arguments to make available when
   * resolving the problem "detail" through a {@code MessageSource}
   */
  protected RequestBindingException(@Nullable String msg, @Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {
    this(msg, null, messageDetailCode, messageDetailArguments);
  }

  /**
   * Constructor for RequestBindingException.
   *
   * @param msg the detail message
   * @param cause the root cause
   * @param messageDetailCode the code to use to resolve the problem "detail"
   * through a {@link infra.context.MessageSource}
   * @param messageDetailArguments the arguments to make available when
   * resolving the problem "detail" through a {@code MessageSource}
   * @since 5.0
   */
  protected RequestBindingException(@Nullable String msg, @Nullable Throwable cause,
          @Nullable String messageDetailCode, Object @Nullable [] messageDetailArguments) {

    super(msg, cause);
    if (messageDetailCode == null) {
      messageDetailCode = ErrorResponse.getDefaultDetailMessageCode(getClass(), null);
    }
    this.messageDetailCode = messageDetailCode;
    this.messageDetailArguments = messageDetailArguments;
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public ProblemDetail getBody() {
    return this.body;
  }

  @Override
  public String getDetailMessageCode() {
    return this.messageDetailCode;
  }

  @Override
  public Object @Nullable [] getDetailMessageArguments() {
    return this.messageDetailArguments;
  }

}

