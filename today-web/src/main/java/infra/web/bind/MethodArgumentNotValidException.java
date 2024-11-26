/*
 * Copyright 2017 - 2024 the original author or authors.
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
