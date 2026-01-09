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

