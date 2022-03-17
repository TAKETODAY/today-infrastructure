/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.bind;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.validation.BindException;
import cn.taketoday.validation.BindingResult;
import cn.taketoday.validation.ObjectError;
import cn.taketoday.web.ErrorResponse;

/**
 * Exception to be thrown when validation on an argument annotated with {@code @Valid} fails.
 * Extends {@link BindException}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 16:18
 */
@SuppressWarnings("serial")
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
    this.body = ProblemDetail.forRawStatusCode(getRawStatusCode()).withDetail("Invalid request content.");
  }

  @Override
  public int getRawStatusCode() {
    return HttpStatus.BAD_REQUEST.value();
  }

  @Override
  public ProblemDetail getBody() {
    return this.body;
  }

  /**
   * Return the method parameter that failed validation.
   */
  public final MethodParameter getParameter() {
    return this.parameter;
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
