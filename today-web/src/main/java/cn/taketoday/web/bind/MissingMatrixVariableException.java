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

package cn.taketoday.web.bind;

import cn.taketoday.core.MethodParameter;

/**
 * {@link RequestBindingException} subclass that indicates that a matrix
 * variable expected in the method parameters of an {@code @RequestMapping}
 * method is not present among the matrix variables extracted from the URL.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MissingPathVariableException
 * @since 4.0 2022/1/23 22:30
 */
public class MissingMatrixVariableException extends MissingRequestValueException {

  private final String variableName;

  private final MethodParameter parameter;

  /**
   * Constructor for MissingMatrixVariableException.
   *
   * @param variableName the name of the missing matrix variable
   * @param parameter the method parameter
   */
  public MissingMatrixVariableException(String variableName, MethodParameter parameter) {
    this(variableName, parameter, false);
  }

  /**
   * Constructor for use when a value was present but converted to {@code null}.
   *
   * @param variableName the name of the missing matrix variable
   * @param parameter the method parameter
   * @param missingAfterConversion whether the value became null after conversion
   */
  public MissingMatrixVariableException(String variableName, MethodParameter parameter, boolean missingAfterConversion) {
    super("", missingAfterConversion, null, new Object[] { variableName });
    this.variableName = variableName;
    this.parameter = parameter;
    getBody().setDetail("Required path parameter '%s' is not present.".formatted(this.variableName));
  }

  @Override
  public String getMessage() {
    return "Required matrix variable '%s' for method parameter type %s is %s"
            .formatted(this.variableName, this.parameter.getNestedParameterType().getSimpleName(),
                    isMissingAfterConversion() ? "present but converted to null" : "not present");
  }

  /**
   * Return the expected name of the matrix variable.
   */
  public final String getVariableName() {
    return this.variableName;
  }

  /**
   * Return the method parameter bound to the matrix variable.
   */
  public final MethodParameter getParameter() {
    return this.parameter;
  }

}

