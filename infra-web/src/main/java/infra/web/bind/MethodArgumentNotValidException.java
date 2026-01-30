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

import java.util.Locale;

import infra.context.MessageSource;
import infra.core.MethodParameter;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ProblemDetail;
import infra.validation.BindException;
import infra.validation.BindingResult;
import infra.validation.ObjectError;
import infra.web.ErrorResponse;
import infra.web.util.BindErrorUtils;

/**
 * Exception to be thrown when validation on an argument annotated with {@code @Valid} fails.
 * Extends {@link BindException}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:18
 */
public class MethodArgumentNotValidException extends BindException implements ErrorResponse {

  private final MethodParameter parameter;

  private final ProblemDetail body;

  /**
   * Constructor for {@link MethodArgumentNotValidException}.
   *
   * @param parameter the parameter that failed validation
   * @param bindingResult the results of the validation
   */
  public MethodArgumentNotValidException(MethodParameter parameter, BindingResult bindingResult) {
    super(bindingResult);
    this.parameter = parameter;
    this.body = ProblemDetail.forStatusAndDetail(getStatusCode(), "Invalid request content.");
  }

  /**
   * Return the method parameter that failed validation.
   */
  public final MethodParameter getParameter() {
    return this.parameter;
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
  public Object[] getDetailMessageArguments(MessageSource source, Locale locale) {
    return new Object[] {
            BindErrorUtils.resolveAndJoin(getGlobalErrors(), source, locale),
            BindErrorUtils.resolveAndJoin(getFieldErrors(), source, locale) };
  }

  @Override
  public Object[] getDetailMessageArguments() {
    return new Object[] {
            BindErrorUtils.resolveAndJoin(getGlobalErrors()),
            BindErrorUtils.resolveAndJoin(getFieldErrors()) };
  }

  @Override
  public String getMessage() {
    StringBuilder sb = new StringBuilder("Validation failed for argument [")
            .append(this.parameter.getParameterIndex()).append("] in ")
            .append(this.parameter.getExecutable().toGenericString());
    BindingResult bindingResult = getBindingResult();
    if (bindingResult.getErrorCount() > 1) {
      sb.append(" with ").append(bindingResult.getErrorCount()).append(" errors");
    }
    sb.append(": ");
    for (ObjectError error : bindingResult.getAllErrors()) {
      sb.append('[').append(error).append("] ");
    }
    return sb.toString();
  }

}
